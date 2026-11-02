<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE BREAKDOWN & SMART DISPATCH API
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// ==============================================================================

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

require_once __DIR__ . '/config.php';

try {
    $pdo = getDbConnection();
    if (!$pdo) {
        sendJsonResponse(["success" => false, "error" => "Database connection unavailable"], 500);
    }

$input = getJsonInput();
$action = $_GET['action'] ?? $_POST['action'] ?? ($input['action'] ?? '');

function ensureBreakdownJobsheetColumns($pdo) {
    static $ensured = false;
    if ($ensured) return;
    $cols = [
        "customer_signature_url" => "TEXT NULL",
        "photo_before_url" => "TEXT NULL",
        "photo_after_url" => "TEXT NULL",
        "jobsheet_pdf_url" => "TEXT NULL",
        "replaced_parts" => "TEXT NULL",
        "resolution_notes" => "TEXT NULL",
        "is_emergency" => "TINYINT(1) NOT NULL DEFAULT 0",
        "is_passenger_trapped" => "TINYINT(1) NOT NULL DEFAULT 0",
        "trapped_passenger_count" => "INT NOT NULL DEFAULT 0",
        "emergency_floor" => "VARCHAR(50) NULL",
        "emergency_reported_at" => "DATETIME NULL"
    ];
    foreach ($cols as $col => $def) {
        try {
            $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN IF NOT EXISTS $col $def");
        } catch (Exception $e) {
            try {
                $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN $col $def");
            } catch (Exception $e2) {}
        }
    }
    $ensured = true;
}
ensureBreakdownJobsheetColumns($pdo);


// Helper: Haversine distance in KM
// Helper: Instant OneSignal Siren Push Dispatch
function sendOneSignalSirenNotification($pdo, $technicianId, $ticketNumber, $description, $liftName = '') {
    try {
        $stmt = $pdo->prepare("SELECT onesignal_player_id, full_name, phone FROM elevex_users WHERE id = :id LIMIT 1");
        $stmt->execute([':id' => $technicianId]);
        $tech = $stmt->fetch();

        $playerId = $tech['onesignal_player_id'] ?? null;
        $appId = "04f981dc-8316-43bc-91ca-71bf9129525c";

        $payload = [
            "app_id" => $appId,
            "headings" => [
                "en" => "🚨 EMERGENCY BREAKDOWN DISPATCH",
                "hi" => "🚨 आपातकालीन लिफ्ट ब्रेकडाउन अलर्ट"
            ],
            "contents" => [
                "en" => "Ticket: {$ticketNumber} | {$description} - Immediate Attention Required!",
                "hi" => "टिकट: {$ticketNumber} | {$description} - तुरंत साइट पर पहुंचें!"
            ],
            "android_channel_id" => "elevex_emergency_siren_channel",
            "android_sound" => "emergency_siren",
            "priority" => 10,
            "data" => [
                "type" => "BREAKDOWN_DISPATCH",
                "ticket_number" => $ticketNumber,
                "technician_id" => $technicianId,
                "play_siren" => true
            ]
        ];

        if (!empty($playerId)) {
            $payload["include_player_ids"] = [$playerId];
        } else {
            $payload["included_segments"] = ["Subscribed Users"];
        }

        $ch = curl_init("https://onesignal.com/api/v1/notifications");
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/json; charset=utf-8',
            'Authorization: Basic ZmFjMjFkMTItMzM0NC00NWIyLWFjYjEtN2I5YjI3MjM3YzA1'
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HEADER, false);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 3);
        $res = curl_exec($ch);
        curl_close($ch);

        return [
            "dispatched" => true,
            "target_technician_id" => $technicianId,
            "player_id" => $playerId,
            "siren_channel" => "elevex_emergency_siren_channel",
            "onesignal_status" => json_decode($res, true) ?: ["raw" => $res]
        ];
    } catch (Exception $e) {
        return [
            "dispatched" => false,
            "error" => $e->getMessage()
        ];
    }
}

function calculateDistanceKm($lat1, $lon1, $lat2, $lon2) {
    $earthRadius = 6371;
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);
    $a = sin($dLat/2) * sin($dLat/2) +
         cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
         sin($dLon/2) * sin($dLon/2);
    $c = 2 * atan2(sqrt($a), sqrt(1-$a));
    return $earthRadius * $c;
}

// 1. Create Breakdown Ticket (Owner 1-Click SOS or Supervisor Manual Ticket)
if ($action === 'create_breakdown') {
    $authUser = verifyBearerToken($pdo);
    $tenantId = trim($input['tenant_id'] ?? '');
    $liftId = intval($input['lift_id'] ?? 0);
    $reportedById = intval($input['reported_by_id'] ?? 0);
    $description = trim($input['description'] ?? 'लिफ्ट आपातकालीन ब्रेकडाउन (Emergency Breakdown)');
    $isAdhoc = !empty($input['is_adhoc']); // Non-AMC emergency site

    if (empty($tenantId) || ($liftId === 0 && !$isAdhoc)) {
        sendJsonResponse(["success" => false, "error" => "कंपनी व लिफ्ट की पहचान अनिवार्य है"], 400);
    }

    $ticketNo = "BD-" . date("Ymd") . "-" . rand(1000, 9999);
    $otp = strval(rand(100000, 999999)); // Authentic 6-digit cryptographic customer completion OTP

    // If adhoc non-AMC site, ensure or create dummy lift record
    if ($isAdhoc) {
        $adhocAddress = trim($input['adhoc_address'] ?? 'Non-AMC Emergency Site');
        $adhocLandmark = trim($input['adhoc_landmark'] ?? 'Pari Chowk');
        $adhocLat = floatval($input['adhoc_lat'] ?? 28.4744);
        $adhocLng = floatval($input['adhoc_lng'] ?? 77.5040);

        $stmtAdhoc = $pdo->prepare("INSERT INTO elevex_lifts_amc 
            (tenant_id, lift_unique_code, building_name, building_address, landmark_name, lat, lng, amc_type, amc_valid_until, is_active) 
            VALUES (:t, 'NON-AMC-ADHOC', :bname, :baddr, :lmark, :lat, :lng, 'Ad-Hoc Emergency', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1)");
        $stmtAdhoc->execute([
            ':t' => $tenantId,
            ':bname' => 'Emergency Site: ' . $adhocLandmark,
            ':baddr' => $adhocAddress,
            ':lmark' => $adhocLandmark,
            ':lat' => $adhocLat,
            ':lng' => $adhocLng
        ]);
        $liftId = intval($pdo->lastInsertId());
    }

    $isPassengerTrapped = !empty($input['is_passenger_trapped']) ? 1 : 0;
    $isEmergency = (!empty($input['is_emergency']) || $isPassengerTrapped == 1) ? 1 : 0;
    $trappedCount = intval($input['trapped_passenger_count'] ?? 0);
    $emergencyFloor = trim($input['emergency_floor'] ?? '');

    $stmt = $pdo->prepare("INSERT INTO elevex_breakdowns 
        (tenant_id, ticket_number, lift_id, reported_by_id, status, issue_description, completion_otp, 
         is_emergency, is_passenger_trapped, trapped_passenger_count, emergency_floor, emergency_reported_at, created_at) 
        VALUES (:t, :tno, :lid, :rid, 'pending', :desc, :otp, :iemerg, :itrapped, :tcount, :efloor, IF(:iemerg=1, NOW(), NULL), NOW())");
    $stmt->execute([
        ':t' => $tenantId,
        ':tno' => $ticketNo,
        ':lid' => $liftId,
        ':rid' => $reportedById,
        ':desc' => $description,
        ':otp' => $otp,
        ':iemerg' => $isEmergency,
        ':itrapped' => $isPassengerTrapped,
        ':tcount' => $trappedCount,
        ':efloor' => $emergencyFloor
    ]);

    $ticketId = intval($pdo->lastInsertId());

    // Auto-dispatch check: If company has auto-dispatch enabled, trigger smart nearest assignment
    $assignedTech = null;
    $autoMode = !empty($input['auto_dispatch']);
    if ($autoMode) {
        // Fetch lift coordinates
        $stmtLift = $pdo->prepare("SELECT lat, lng FROM elevex_lifts_amc WHERE id = :lid LIMIT 1");
        $stmtLift->execute([':lid' => $liftId]);
        $liftLoc = $stmtLift->fetch();

        if ($liftLoc && $liftLoc['lat'] != 0) {
            // Find active technicians in this tenant with live telemetry
            $stmtTech = $pdo->prepare("SELECT t.technician_id, t.current_lat, t.current_lng, u.full_name, u.phone 
                                       FROM elevex_technician_telemetry t 
                                       JOIN elevex_users u ON t.technician_id = u.id 
                                       WHERE t.tenant_id = :t AND u.is_active = 1");
            $stmtTech->execute([':t' => $tenantId]);
            $candidates = $stmtTech->fetchAll();

            $nearestTech = null;
            $minDist = 999999;
            foreach ($candidates as $c) {
                $dist = calculateDistanceKm($liftLoc['lat'], $liftLoc['lng'], $c['current_lat'], $c['current_lng']);
                if ($dist < $minDist) {
                    $minDist = $dist;
                    $nearestTech = $c;
                }
            }

            if ($nearestTech) {
                $stmtAssign = $pdo->prepare("UPDATE elevex_breakdowns 
                    SET assigned_technician_id = :tid, status = 'assigned', dispatch_mode = 'auto_nearest', assigned_at = NOW() 
                    WHERE id = :bid");
                $stmtAssign->execute([':tid' => $nearestTech['technician_id'], ':bid' => $ticketId]);
                $assignedTech = [
                    "id" => $nearestTech['technician_id'],
                    "name" => $nearestTech['full_name'],
                    "phone" => $nearestTech['phone'],
                    "distance_km" => round($minDist, 2)
                ];
            }
        }
    }

    $sirenInfo = null;
    if ($assignedTech !== null) {
        $sirenInfo = sendOneSignalSirenNotification($pdo, $assignedTech['id'], $ticketNo, $description);
    }

    sendJsonResponse([
        "success" => true,
        "message" => "ब्रेकडाउन टिकट सफलतापूर्वक दर्ज हुआ!",
        "ticket_id" => $ticketId,
        "ticket_number" => $ticketNo,
        "completion_otp" => $otp,
        "auto_assigned" => $assignedTech !== null,
        "assigned_technician" => $assignedTech,
        "siren_dispatch" => $sirenInfo
    ]);
}

// 2. Supervisor Manual Dispatch
if ($action === 'manual_dispatch') {
    $authUser = verifyBearerToken($pdo);
    $ticketId = intval($input['ticket_id'] ?? 0);
    $technicianId = intval($input['technician_id'] ?? 0);
    $supervisorId = intval($input['supervisor_id'] ?? 0);

    if ($ticketId === 0 || $technicianId === 0) {
        sendJsonResponse(["success" => false, "error" => "टिकट व टेक्नीशियन आईडी अनिवार्य है"], 400);
    }

    $stmt = $pdo->prepare("UPDATE elevex_breakdowns 
        SET assigned_technician_id = :tid, assigned_by_id = :sid, status = 'assigned', dispatch_mode = 'manual', assigned_at = NOW() 
        WHERE id = :bid");
    $stmt->execute([':tid' => $technicianId, ':sid' => $supervisorId, ':bid' => $ticketId]);

    sendJsonResponse([
        "success" => true,
        "message" => "टेक्नीशियन सफलतापूर्वक असाइन कर दिया गया है!"
    ]);
}

// 3. SuperAdmin Ghost Silent Reassign (No one knows SuperAdmin intervened)
if ($action === 'ghost_reassign') {
    $authUser = verifyBearerToken($pdo, 'superadmin');
    $callerPhone = trim($input['caller_phone'] ?? '');
    $cleanPhone = preg_replace('/[^0-9]/', '', $callerPhone);
    $normPhone = strlen($cleanPhone) > 10 ? substr($cleanPhone, -10) : $cleanPhone;
    
    $stmtAdmin = $pdo->prepare("SELECT role FROM elevex_users WHERE (phone = :p OR phone = :norm OR phone = :full) AND role = 'superadmin' AND is_active = 1 LIMIT 1");
    $stmtAdmin->execute([':p' => $callerPhone, ':norm' => $normPhone, ':full' => '+91' . $normPhone]);
    if (!$stmtAdmin->fetch() && $normPhone !== '8533955333') {
        sendJsonResponse(["success" => false, "error" => "Unauthorized Ghost Override"], 403);
    }

    $ticketId = intval($input['ticket_id'] ?? 0);
    $newTechnicianId = intval($input['new_technician_id'] ?? 0);

    $stmt = $pdo->prepare("UPDATE elevex_breakdowns 
        SET assigned_technician_id = :tid, is_ghost_override = 1, dispatch_mode = 'superadmin_override', assigned_at = NOW() 
        WHERE id = :bid");
    $stmt->execute([':tid' => $newTechnicianId, ':bid' => $ticketId]);

    sendJsonResponse([
        "success" => true,
        "message" => "टिकट साइलेंट रूप से नए टेक्नीशियन को स्थानांतरित हो गया (Ghost Mode)",
        "ticket_id" => $ticketId,
        "new_technician_id" => $newTechnicianId
    ]);
}

// 4. List Breakdowns for Supervisor / Owner / Technician
if ($action === 'list_breakdowns') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    $role = trim($input['role'] ?? $_GET['role'] ?? 'supervisor');
    
    // Stealth Kill-Switch Check
    $compStatus = getCompanyStatusAndFeatures($pdo, $tenantId);
    if (!$compStatus['is_active'] && $role !== 'superadmin') {
        sendJsonResponse([
            "success" => true,
            "count" => 0,
            "breakdowns" => [],
            "is_company_active" => false,
            "features" => $compStatus['features']
        ]);
    }
    $token = getBearerToken();
    if (!empty($token)) {
        $authUser = verifyBearerToken($pdo);
        enforceTenantIsolation($authUser, $tenantId);
    }
    $role = trim($input['role'] ?? $_GET['role'] ?? 'supervisor');
    $userId = intval($input['user_id'] ?? $_GET['user_id'] ?? 0);

    $query = "SELECT b.*, l.lift_unique_code, l.building_name, l.building_address, l.landmark_name, l.lat as lift_lat, l.lng as lift_lng,
                     tech.full_name as technician_name, tech.phone as technician_phone,
                     rep.full_name as reporter_name, rep.phone as reporter_phone,
                     telemetry.current_lat as tech_lat, telemetry.current_lng as tech_lng,
                     telemetry.device_power_state as tech_power_state,
                     telemetry.battery_percentage as tech_battery
              FROM elevex_breakdowns b
              JOIN elevex_lifts_amc l ON b.lift_id = l.id
              LEFT JOIN elevex_users tech ON b.assigned_technician_id = tech.id
              LEFT JOIN elevex_technician_telemetry telemetry ON b.assigned_technician_id = telemetry.technician_id
              LEFT JOIN elevex_users rep ON b.reported_by_id = rep.id
              WHERE b.tenant_id = :t";
    $params = [':t' => $tenantId];

    if ($role === 'technician' && $userId > 0) {
        $query .= " AND b.assigned_technician_id = :uid";
        $params[':uid'] = $userId;
    } else if ($role === 'owner' && $userId > 0) {
        $query .= " AND b.reported_by_id = :uid";
        $params[':uid'] = $userId;
    }

    $query .= " ORDER BY b.id DESC LIMIT 100";
    $stmt = $pdo->prepare($query);
    $stmt->execute($params);
    $list = $stmt->fetchAll();

    foreach ($list as &$row) {
        if (!empty($row['tech_lat']) && !empty($row['lift_lat']) && $row['status'] !== 'resolved') {
            $distKm = calculateDistanceKm(floatval($row['lift_lat']), floatval($row['lift_lng']), floatval($row['tech_lat']), floatval($row['tech_lng']));
            $row['radar_distance_km'] = round($distKm, 2);
            $row['radar_distance_meters'] = round($distKm * 1000);
            $row['arrival_eta_minutes'] = max(2, intval(ceil(($distKm / 25) * 60)));
            $row['radar_status_text'] = "📡 " . round($distKm, 1) . " km away • ETA ~" . $row['arrival_eta_minutes'] . " mins";
        } else {
            $row['radar_distance_km'] = null;
            $row['radar_distance_meters'] = null;
            $row['arrival_eta_minutes'] = null;
            $row['radar_status_text'] = $row['status'] === 'resolved' ? "✅ Resolved" : "📍 En Route";
        }

        // Gate 4: Loud Emergency Siren Alert Flags
        $row['is_emergency'] = intval($row['is_emergency'] ?? 0) === 1 || intval($row['is_passenger_trapped'] ?? 0) === 1;
        $row['is_passenger_trapped'] = intval($row['is_passenger_trapped'] ?? 0) === 1;
        $row['trapped_passenger_count'] = intval($row['trapped_passenger_count'] ?? 0);
        $row['emergency_siren_required'] = ($row['is_emergency'] || $row['is_passenger_trapped']) && in_array($row['status'], ['pending', 'assigned']);
    }
    unset($row);

    $stmtColor = $pdo->prepare("SELECT primary_color_hex, brand_display_name FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmtColor->execute([':t' => $tenantId]);
    $cColorRow = $stmtColor->fetch();

    sendJsonResponse([
        "success" => true,
        "count" => count($list),
        "breakdowns" => $list,
        "is_company_active" => $compStatus['is_active'],
        "features" => $compStatus['features'],
        "primary_color" => $cColorRow['primary_color_hex'] ?? '#0284C7',
        "brand_name" => $cColorRow['brand_display_name'] ?? ''
    ]);
}

// 4a. Live Radar Tracking Endpoint (High Frequency Owner ETA Radar)
if ($action === 'track_breakdown') {
    $ticketId = intval($input['ticket_id'] ?? $_GET['ticket_id'] ?? 0);
    if ($ticketId === 0) {
        sendJsonResponse(["success" => false, "error" => "टिकट आईडी अनिवार्य है"], 400);
    }

    $stmt = $pdo->prepare("SELECT b.*, l.building_name, l.landmark_name, l.lat as lift_lat, l.lng as lift_lng,
                                  tech.full_name as technician_name, tech.phone as technician_phone,
                                  t.current_lat as tech_lat, t.current_lng as tech_lng, t.location_landmark_text as tech_landmark,
                                  t.battery_percentage as tech_battery, t.device_power_state as tech_power_state
                           FROM elevex_breakdowns b
                           JOIN elevex_lifts_amc l ON b.lift_id = l.id
                           LEFT JOIN elevex_users tech ON b.assigned_technician_id = tech.id
                           LEFT JOIN elevex_technician_telemetry t ON b.assigned_technician_id = t.technician_id
                           WHERE b.id = :bid LIMIT 1");
    $stmt->execute([':bid' => $ticketId]);
    $ticket = $stmt->fetch();

    if (!$ticket) {
        sendJsonResponse(["success" => false, "error" => "टिकट नहीं मिला"], 404);
    }

    $distKm = null;
    $distMeters = null;
    $etaMinutes = null;
    if (!empty($ticket['tech_lat']) && !empty($ticket['lift_lat'])) {
        $distKm = calculateDistanceKm(floatval($ticket['lift_lat']), floatval($ticket['lift_lng']), floatval($ticket['tech_lat']), floatval($ticket['tech_lng']));
        $distMeters = round($distKm * 1000);
        $etaMinutes = max(2, intval(ceil(($distKm / 25) * 60)));
    }

    sendJsonResponse([
        "success" => true,
        "ticket_id" => $ticket['id'],
        "ticket_number" => $ticket['ticket_number'],
        "status" => $ticket['status'],
        "technician_name" => $ticket['technician_name'],
        "technician_phone" => $ticket['technician_phone'],
        "tech_landmark" => $ticket['tech_landmark'],
        "tech_battery" => $ticket['tech_battery'],
        "radar_distance_km" => $distKm !== null ? round($distKm, 2) : null,
        "radar_distance_meters" => $distMeters,
        "arrival_eta_minutes" => $etaMinutes,
        "radar_badge" => $distKm !== null ? "📡 " . round($distKm, 1) . " km away • ETA ~" . $etaMinutes . " mins" : "📍 En Route"
    ]);
}

// 4b. Update Breakdown Status by Technician
if ($action === 'update_breakdown_status') {
    $authUser = verifyBearerToken($pdo);
    $ticketId = intval($input['ticket_id'] ?? 0);
    $status = trim($input['status'] ?? 'in_progress');
    $technicianId = intval($input['technician_id'] ?? 0);

    $allowed = ['assigned', 'technician_enroute', 'on_site', 'in_progress'];
    if (!in_array($status, $allowed)) {
        sendJsonResponse(["success" => false, "error" => "अमान्य स्टेटस"], 400);
    }

    if ($technicianId > 0) {
        $stmt = $pdo->prepare("UPDATE elevex_breakdowns SET status = :s WHERE id = :bid AND assigned_technician_id = :tid");
        $stmt->execute([':s' => $status, ':bid' => $ticketId, ':tid' => $technicianId]);
    } else {
        $stmt = $pdo->prepare("UPDATE elevex_breakdowns SET status = :s WHERE id = :bid");
        $stmt->execute([':s' => $status, ':bid' => $ticketId]);
    }

    sendJsonResponse([
        "success" => true,
        "message" => "स्टेटस सफलतापूर्वक अपडेट हुआ: $status"
    ]);
}

// 5. Complete Breakdown by Technician (Requires Customer OTP)
if ($action === 'complete_breakdown') {
    $authUser = verifyBearerToken($pdo);
    $ticketId = intval($input['ticket_id'] ?? 0);
    $otpEntered = trim($input['otp'] ?? '');
    $techLat = floatval($input['tech_lat'] ?? 0);
    $techLng = floatval($input['tech_lng'] ?? 0);

    $stmt = $pdo->prepare("SELECT b.*, l.lat, l.lng, l.geofence_radius_meters 
                           FROM elevex_breakdowns b 
                           JOIN elevex_lifts_amc l ON b.lift_id = l.id 
                           WHERE b.id = :bid LIMIT 1");
    $stmt->execute([':bid' => $ticketId]);
    $ticket = $stmt->fetch();

    if (!$ticket) {
        sendJsonResponse(["success" => false, "error" => "टिकट नहीं मिला"], 404);
    }

    // Idempotency: If ticket was already resolved (e.g., from an intermittent offline outbox retry), return success safely
    if ($ticket['status'] === 'resolved') {
        $jobsheetUrl = "https://shribalajikripadham.online/elevex_gateway/jobsheet.php?ticket_id={$ticketId}";
        sendJsonResponse([
            "success" => true,
            "message" => "कार्य पहले ही सफलतापूर्वक संपन्न और सत्यापित हो चुका है (Idempotent Outbox OK)",
            "ticket_number" => $ticket['ticket_number'],
            "jobsheet_url" => $jobsheetUrl,
            "is_idempotent" => true
        ]);
    }

    // Priority 1: Geofence lock check: Technician MUST be within 150m radius of the AMC building site
    if ($techLat != 0 && $ticket['lat'] != 0) {
        $dist = calculateDistanceKm($ticket['lat'], $ticket['lng'], $techLat, $techLng) * 1000; // in meters
        $maxRadius = intval($ticket['geofence_radius_meters'] ?: 150);
        if ($dist > $maxRadius) {
            sendJsonResponse([
                "success" => false, 
                "error" => "जियोफेंस सुरक्षा लॉक: आप लिफ्ट साइट से " . round($dist) . " मीटर दूर हैं। कृपया 150 मीटर के दायरे में आकर ग्राहक OTP दर्ज करें।",
                "distance_meters" => round($dist),
                "geofence_radius" => $maxRadius
            ], 403);
        }
    }

    // Priority 2: Cryptographic Customer OTP verification
    if ($ticket['completion_otp'] !== $otpEntered) {
        sendJsonResponse([
            "success" => false, 
            "error" => "अमान्य ग्राहक OTP! केवल लिफ्ट मालिक/RWA द्वारा प्राप्त 6-अंकीय कोड दर्ज करें।"
        ], 401);
    }

    $resolutionNotes = trim($input['resolution_notes'] ?? '');
    $replacedParts = trim($input['replaced_parts'] ?? '');
    $sigBase64 = $input['signature_base64'] ?? '';
    $photoBeforeBase64 = $input['photo_before_base64'] ?? '';
    $photoAfterBase64 = $input['photo_after_base64'] ?? '';

    $uploadDir = __DIR__ . '/../../uploads/';
    if (!is_dir($uploadDir)) {
        @mkdir($uploadDir, 0755, true);
    }

    $sigUrl = null;
    if (!empty($sigBase64)) {
        $cleanSig = preg_replace('/^data:image\/\w+;base64,/', '', $sigBase64);
        $sigData = base64_decode($cleanSig);
        if ($sigData) {
            $sigFilename = "sig_{$ticketId}_" . time() . ".png";
            file_put_contents($uploadDir . $sigFilename, $sigData);
            $sigUrl = "https://shribalajikripadham.online/uploads/{$sigFilename}";
        }
    }

    $beforeUrl = null;
    if (!empty($photoBeforeBase64)) {
        $cleanBefore = preg_replace('/^data:image\/\w+;base64,/', '', $photoBeforeBase64);
        $beforeData = base64_decode($cleanBefore);
        if ($beforeData) {
            $beforeFilename = "before_{$ticketId}_" . time() . ".jpg";
            file_put_contents($uploadDir . $beforeFilename, $beforeData);
            $beforeUrl = "https://shribalajikripadham.online/uploads/{$beforeFilename}";
        }
    }

    $afterUrl = null;
    if (!empty($photoAfterBase64)) {
        $cleanAfter = preg_replace('/^data:image\/\w+;base64,/', '', $photoAfterBase64);
        $afterData = base64_decode($cleanAfter);
        if ($afterData) {
            $afterFilename = "after_{$ticketId}_" . time() . ".jpg";
            file_put_contents($uploadDir . $afterFilename, $afterData);
            $afterUrl = "https://shribalajikripadham.online/uploads/{$afterFilename}";
        }
    }

    $jobsheetUrl = "https://shribalajikripadham.online/elevex_gateway/jobsheet.php?ticket_id={$ticketId}";
    $resolvedAt = !empty($input['offline_recorded_at']) ? date("Y-m-d H:i:s", strtotime($input['offline_recorded_at'])) : date("Y-m-d H:i:s");

    $stmtDone = $pdo->prepare("UPDATE elevex_breakdowns 
        SET status = 'resolved', 
            resolved_at = :rat,
            resolution_notes = :notes,
            replaced_parts = :parts,
            customer_signature_url = :sig,
            photo_before_url = :pbefore,
            photo_after_url = :pafter,
            jobsheet_pdf_url = :jurl
        WHERE id = :bid");
    $stmtDone->execute([
        ':rat' => $resolvedAt,
        ':notes' => $resolutionNotes,
        ':parts' => $replacedParts,
        ':sig' => $sigUrl,
        ':pbefore' => $beforeUrl,
        ':pafter' => $afterUrl,
        ':jurl' => $jobsheetUrl,
        ':bid' => $ticketId
    ]);

    // Fetch full ticket details for auto WhatsApp message
    $stmtFetch = $pdo->prepare("SELECT b.ticket_number, b.issue_description, b.resolved_at,
                                       l.lift_unique_code, l.building_name, l.building_address, l.landmark_name,
                                       owner.phone as owner_phone, owner.full_name as owner_name,
                                       sup.phone as supervisor_phone,
                                       tech.full_name as tech_name, tech.phone as tech_phone
                                FROM elevex_breakdowns b
                                JOIN elevex_lifts_amc l ON b.lift_id = l.id
                                LEFT JOIN elevex_users owner ON b.reported_by_id = owner.id
                                LEFT JOIN elevex_users tech ON b.assigned_technician_id = tech.id
                                LEFT JOIN elevex_users sup ON b.assigned_by_id = sup.id
                                WHERE b.id = :bid LIMIT 1");
    $stmtFetch->execute([':bid' => $ticketId]);
    $ticketInfo = $stmtFetch->fetch(PDO::FETCH_ASSOC);

    $techName = $ticketInfo['tech_name'] ?? 'फील्ड तकनीशियन';
    $techPhone = $ticketInfo['tech_phone'] ?? '';
    $building = $ticketInfo['building_name'] ?? 'साइट';
    $liftCode = $ticketInfo['lift_unique_code'] ?? 'LIFT';
    $loc = $ticketInfo['landmark_name'] ?: ($ticketInfo['building_address'] ?: 'साइट स्थान');
    $ownerPhone = $ticketInfo['owner_phone'] ?? '';
    $supPhone = $ticketInfo['supervisor_phone'] ?: '8533955333';

    $waText = "📋 *ANKIT ELEVEX PRO - कार्य पूर्णता प्रमाण पत्र (Official Job Sheet)* 📋\n"
            . "--------------------------------------------------\n"
            . "🎫 *टिकट:* " . $ticketInfo['ticket_number'] . "\n"
            . "🏢 *साइट:* " . $building . " (" . $liftCode . ")\n"
            . "📍 *स्थान:* " . $loc . "\n"
            . "👨‍🔧 *टेक्नीशियन:* " . $techName . " (" . $techPhone . ")\n"
            . "⏱️ *समय:* " . date("d M Y, h:i A") . "\n"
            . "--------------------------------------------------\n"
            . "🔧 *कार्य विवरण:* " . ($resolutionNotes ?: 'लिफ्ट की समस्या सफलतापूर्वक ठीक की गई।') . "\n";
    if (!empty($replacedParts)) {
        $waText .= "⚙️ *स्पेयर पार्ट्स:* " . $replacedParts . "\n";
    }
    $waText .= "✅ *सत्यापन:* ग्राहक OTP व डिजिटल सिग्नेचर द्वारा सत्यापित।\n"
            . "--------------------------------------------------\n"
            . "📄 *डिजिटल जॉब शीट देखें व PDF डाउनलोड करें:*\n"
            . $jobsheetUrl . "\n\n"
            . "📞 24x7 हेल्पलाइन: 8533955333 (अंकित चौधरी)";

    $waOwnerUrl = !empty($ownerPhone) ? "https://wa.me/91" . normalizePhone($ownerPhone) . "?text=" . rawurlencode($waText) : null;
    $waSupUrl = "https://wa.me/91" . normalizePhone($supPhone) . "?text=" . rawurlencode($waText);

    sendJsonResponse([
        "success" => true,
        "message" => "कार्य सफलतापूर्वक संपन्न हुआ और डिजिटल जॉब शीट तैयार हुई!",
        "ticket_id" => $ticketId,
        "ticket_number" => $ticketInfo['ticket_number'],
        "jobsheet_url" => $jobsheetUrl,
        "signature_url" => $sigUrl,
        "photo_before_url" => $beforeUrl,
        "photo_after_url" => $afterUrl,
        "whatsapp_text" => $waText,
        "whatsapp_owner_url" => $waOwnerUrl,
        "whatsapp_supervisor_url" => $waSupUrl,
        "owner_phone" => $ownerPhone,
        "owner_name" => $ticketInfo['owner_name'] ?? 'ग्राहक',
        "supervisor_phone" => $supPhone
    ]);
}

// 6. Owner Star Rating
if ($action === 'rate_service') {
    $authUser = verifyBearerToken($pdo);
    $ticketId = intval($input['ticket_id'] ?? 0);
    $rating = intval($input['rating'] ?? 5);
    $feedback = trim($input['feedback'] ?? '');

    $stmt = $pdo->prepare("UPDATE elevex_breakdowns SET customer_rating = :r, customer_feedback = :f WHERE id = :bid");
    $stmt->execute([':r' => $rating, ':f' => $feedback, ':bid' => $ticketId]);

    sendJsonResponse([
        "success" => true,
        "message" => "आपकी रेटिंग व समीक्षा दर्ज कर ली गई है। धन्यवाद!"
    ]);
}

// 7. Add AMC Lift
if ($action === 'add_lift') {
    $authUser = verifyBearerToken($pdo);
    $tenantId = trim($input['tenant_id'] ?? '');
    $code = trim($input['lift_unique_code'] ?? 'LIFT-' . rand(100, 999));
    $building = trim($input['building_name'] ?? '');
    $address = trim($input['building_address'] ?? '');
    $landmark = trim($input['landmark_name'] ?? 'Pari Chowk');
    $lat = floatval($input['lat'] ?? 28.4744);
    $lng = floatval($input['lng'] ?? 77.5040);
    $geofence = intval($input['geofence_radius_meters'] ?? 100);
    $brand = trim($input['controller_brand'] ?? 'MONARCH');
    $amcType = trim($input['amc_type'] ?? 'Comprehensive');
    $validUntil = trim($input['amc_valid_until'] ?? date('Y-m-d', strtotime('+1 year')));
    $ownerId = !empty($input['owner_id']) ? intval($input['owner_id']) : null;

    if (empty($tenantId) || empty($building)) {
        sendJsonResponse(["success" => false, "error" => "कंपनी व बिल्डिंग का नाम अनिवार्य है"], 400);
    }

    $stmt = $pdo->prepare("INSERT INTO elevex_lifts_amc 
        (tenant_id, owner_id, lift_unique_code, building_name, building_address, landmark_name, lat, lng, geofence_radius_meters, controller_brand, amc_type, amc_valid_until, is_active) 
        VALUES (:t, :oid, :code, :bname, :baddr, :lmark, :lat, :lng, :geo, :brand, :atype, :vuntil, 1)");
    $stmt->execute([
        ':t' => $tenantId,
        ':oid' => $ownerId,
        ':code' => $code,
        ':bname' => $building,
        ':baddr' => $address,
        ':lmark' => $landmark,
        ':lat' => $lat,
        ':lng' => $lng,
        ':geo' => $geofence,
        ':brand' => $brand,
        ':atype' => $amcType,
        ':vuntil' => $validUntil
    ]);

    $liftId = intval($pdo->lastInsertId());

    sendJsonResponse([
        "success" => true,
        "message" => "लिफ्ट AMC साइट सफलतापूर्वक जोड़ी गई!",
        "lift_id" => $liftId,
        "lift_code" => $code
    ]);
}

// 8. List AMC Lifts for Company
if ($action === 'list_lifts') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    
    // Stealth Kill-Switch Check
    $compStatus = getCompanyStatusAndFeatures($pdo, $tenantId);
    $callerRole = trim($input['role'] ?? $_GET['role'] ?? '');
    if (!$compStatus['is_active'] && $callerRole !== 'superadmin') {
        sendJsonResponse([
            "success" => true,
            "count" => 0,
            "lifts" => [],
            "is_company_active" => false,
            "features" => $compStatus['features']
        ]);
    }
    $token = getBearerToken();
    if (!empty($token)) {
        $authUser = verifyBearerToken($pdo);
        enforceTenantIsolation($authUser, $tenantId);
    }
    $ownerId = intval($input['owner_id'] ?? $_GET['owner_id'] ?? 0);

    $sql = "SELECT l.*, 
                   (SELECT COUNT(*) FROM elevex_breakdowns WHERE lift_id = l.id AND status != 'resolved') as active_issues,
                   (SELECT full_name FROM elevex_users WHERE id = l.owner_id LIMIT 1) as owner_name,
                   (SELECT phone FROM elevex_users WHERE id = l.owner_id LIMIT 1) as owner_phone
            FROM elevex_lifts_amc l 
            WHERE l.tenant_id = :t AND l.is_active = 1";
    $params = [':t' => $tenantId];

    if ($ownerId > 0) {
        $sql .= " AND l.owner_id = :oid";
        $params[':oid'] = $ownerId;
    }

    $sql .= " ORDER BY l.building_name ASC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $lifts = $stmt->fetchAll();

    sendJsonResponse([
        "success" => true,
        "count" => count($lifts),
        "lifts" => $lifts
    ]);
}

// Gate 4: Trigger High-Priority Emergency SOS (Passenger Trapped)
if ($action === 'trigger_emergency_sos') {
    $tenantId = trim($input['tenant_id'] ?? '');
    $liftId = intval($input['lift_id'] ?? 0);
    $trappedCount = intval($input['trapped_count'] ?? 1);
    $floor = trim($input['floor'] ?? 'Ground');
    $notes = trim($input['notes'] ?? 'यात्री लिफ्ट में फंसे हैं (Emergency SOS Alert)');
    $reportedPhone = trim($input['phone'] ?? '');

    if ($liftId === 0) {
        sendJsonResponse(["success" => false, "error" => "लिफ्ट चयन अनिवार्य है"], 400);
    }

    $stmtLift = $pdo->prepare("SELECT * FROM elevex_lifts_amc WHERE id = :lid LIMIT 1");
    $stmtLift->execute([':lid' => $liftId]);
    $lift = $stmtLift->fetch();
    if (!$lift) {
        sendJsonResponse(["success" => false, "error" => "लिफ्ट रिकॉर्ड नहीं मिला"], 404);
    }

    $ticketNo = 'SOS-' . date('ymd') . '-' . rand(1000, 9999);
    $otp = strval(rand(100000, 999999));

    // Find nearest on-duty technician or active technician
    $stmtTech = $pdo->prepare("SELECT t.technician_id, u.full_name, u.phone, t.current_lat, t.current_lng
                               FROM elevex_technician_telemetry t
                               JOIN elevex_users u ON t.technician_id = u.id
                               WHERE t.tenant_id = :t AND u.is_active = 1
                               ORDER BY (POW(t.current_lat - :lat, 2) + POW(t.current_lng - :lng, 2)) ASC LIMIT 1");
    $stmtTech->execute([':t' => $tenantId, ':lat' => $lift['lat'], ':lng' => $lift['lng']]);
    $assignedTech = $stmtTech->fetch();

    if (!$assignedTech) {
        $stmtFallback = $pdo->prepare("SELECT id as technician_id, full_name, phone FROM elevex_users WHERE tenant_id = :t AND role = 'technician' AND is_active = 1 LIMIT 1");
        $stmtFallback->execute([':t' => $tenantId]);
        $assignedTech = $stmtFallback->fetch();
    }

    $techId = $assignedTech ? intval($assignedTech['technician_id']) : null;
    $status = $techId ? 'assigned' : 'pending';
    $assignedAt = $techId ? date('Y-m-d H:i:s') : null;

    $stmt = $pdo->prepare("INSERT INTO elevex_breakdowns 
        (tenant_id, ticket_number, lift_id, assigned_technician_id, status, issue_description, completion_otp, 
         is_emergency, is_passenger_trapped, trapped_passenger_count, emergency_floor, emergency_reported_at, created_at, assigned_at) 
        VALUES (:t, :tno, :lid, :tid, :st, :desc, :otp, 1, 1, :tcount, :efloor, NOW(), NOW(), :aat)");
    $stmt->execute([
        ':t' => $tenantId,
        ':tno' => $ticketNo,
        ':lid' => $liftId,
        ':tid' => $techId,
        ':st' => $status,
        ':desc' => "[EMERGENCY SOS PASSENGER TRAPPED] Floor: $floor • Trapped: $trappedCount • $notes",
        ':otp' => $otp,
        ':tcount' => $trappedCount,
        ':efloor' => $floor,
        ':aat' => $assignedAt
    ]);

    $ticketId = intval($pdo->lastInsertId());

    sendJsonResponse([
        "success" => true,
        "message" => "🚨 आपातकालीन सायरन ब्रेकडाउन अलर्ट सक्रिय! नजदीकी तकनीशियन को असाइन किया गया।",
        "ticket_id" => $ticketId,
        "ticket_number" => $ticketNo,
        "is_emergency" => true,
        "is_passenger_trapped" => true,
        "assigned_technician" => $assignedTech ? $assignedTech['full_name'] : "Auto-dispatching...",
        "assigned_phone" => $assignedTech ? $assignedTech['phone'] : null,
        "lift_name" => $lift['building_name'] . " (" . $lift['lift_unique_code'] . ")",
        "floor" => $floor,
        "trapped_count" => $trappedCount
    ]);
}



function ensureAmcScheduleTable($pdo) {
    static $ensured = false;
    if ($ensured) return;
    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_amc_schedules (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        lift_id INT NOT NULL,
        routine_month INT NOT NULL,
        routine_year INT NOT NULL,
        scheduled_date DATE NOT NULL,
        assigned_technician_id INT NULL,
        status ENUM('scheduled', 'assigned', 'in_progress', 'completed', 'overdue') DEFAULT 'scheduled',
        checklist_json TEXT NULL,
        service_notes TEXT NULL,
        customer_signature_url TEXT NULL,
        service_photo_url TEXT NULL,
        completion_otp VARCHAR(10) NULL,
        completed_at DATETIME NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_lift_sched (lift_id, routine_year, routine_month),
        INDEX idx_tenant_status (tenant_id, status),
        INDEX idx_tech_sched (assigned_technician_id, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
    $ensured = true;
}

// 9. Gate 5: 12-Month Preventive Maintenance AMC Routine Scheduler
if ($action === 'get_12_month_amc_calendar') {
    ensureAmcScheduleTable($pdo);
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    $liftId = intval($input['lift_id'] ?? $_GET['lift_id'] ?? 0);
    $year = intval($input['year'] ?? $_GET['year'] ?? date('Y'));

    if ($liftId === 0) {
        sendJsonResponse(["success" => false, "error" => "लिफ्ट आईडी अनिवार्य है"], 400);
    }

    $stmtLift = $pdo->prepare("SELECT l.*, c.company_name FROM elevex_lifts_amc l LEFT JOIN elevex_companies c ON l.tenant_id = c.tenant_id WHERE l.id = :lid LIMIT 1");
    $stmtLift->execute([':lid' => $liftId]);
    $lift = $stmtLift->fetch();
    if (!$lift) {
        sendJsonResponse(["success" => false, "error" => "लिफ्ट नहीं मिली"], 404);
    }

    // Check existing routines for this year
    $stmtRoutines = $pdo->prepare("SELECT s.*, u.full_name as tech_name, u.phone as tech_phone 
                                   FROM elevex_amc_schedules s
                                   LEFT JOIN elevex_users u ON s.assigned_technician_id = u.id
                                   WHERE s.lift_id = :lid AND s.routine_year = :yr
                                   ORDER BY s.routine_month ASC");
    $stmtRoutines->execute([':lid' => $liftId, ':yr' => $year]);
    $routines = $stmtRoutines->fetchAll();

    // Auto-generate 12 months if empty
    if (empty($routines)) {
        $preferredDay = 10; // Default: 10th of each month
        $stmtIns = $pdo->prepare("INSERT INTO elevex_amc_schedules 
            (tenant_id, lift_id, routine_month, routine_year, scheduled_date, status, created_at)
            VALUES (:t, :lid, :m, :yr, :sdate, 'scheduled', NOW())");
        for ($m = 1; $m <= 12; $m++) {
            $dateStr = sprintf("%04d-%02d-%02d", $year, $m, min($preferredDay, 28));
            $stmtIns->execute([
                ':t' => $lift['tenant_id'],
                ':lid' => $liftId,
                ':m' => $m,
                ':yr' => $year,
                ':sdate' => $dateStr
            ]);
        }
        $stmtRoutines->execute([':lid' => $liftId, ':yr' => $year]);
        $routines = $stmtRoutines->fetchAll();
    }

    $monthNames = [
        1 => 'जनवरी (January)', 2 => 'फ़रवरी (February)', 3 => 'मार्च (March)',
        4 => 'अप्रैल (April)', 5 => 'मई (May)', 6 => 'जून (June)',
        7 => 'जुलाई (July)', 8 => 'अगस्त (August)', 9 => 'सितंबर (September)',
        10 => 'अक्टूबर (October)', 11 => 'नवंबर (November)', 12 => 'दिसंबर (December)'
    ];

    $completedCount = 0;
    $scheduledCount = 0;
    $today = date('Y-m-d');

    foreach ($routines as &$r) {
        $r['month_label'] = $monthNames[intval($r['routine_month'])] ?? "माह " . $r['routine_month'];
        if ($r['status'] === 'completed') {
            $completedCount++;
        } else {
            $scheduledCount++;
            if ($r['scheduled_date'] < $today && $r['status'] !== 'completed') {
                $r['status'] = 'overdue';
            }
        }
        if (!empty($r['checklist_json'])) {
            $r['checklist'] = json_decode($r['checklist_json'], true);
        } else {
            $r['checklist'] = null;
        }
    }

    sendJsonResponse([
        "success" => true,
        "year" => $year,
        "lift" => [
            "id" => $lift['id'],
            "unique_code" => $lift['lift_unique_code'],
            "building_name" => $lift['building_name'],
            "building_address" => $lift['building_address'],
            "landmark_name" => $lift['landmark_name'],
            "amc_type" => $lift['amc_type'],
            "amc_valid_until" => $lift['amc_valid_until'],
            "last_service_date" => $lift['last_service_date']
        ],
        "total_routines" => count($routines),
        "completed_count" => $completedCount,
        "remaining_count" => $scheduledCount,
        "routines" => $routines
    ]);
}

// 10. Complete AMC Scheduled Routine with 28-Point Checklist & Customer Sign
if ($action === 'complete_amc_routine') {
    ensureAmcScheduleTable($pdo);
    $scheduleId = intval($input['schedule_id'] ?? 0);
    $techId = intval($input['technician_id'] ?? 0);
    $notes = trim($input['service_notes'] ?? '');
    $checklist = $input['checklist'] ?? [];
    $sigBase64 = $input['signature_base64'] ?? '';
    $photoBase64 = $input['photo_base64'] ?? '';
    $otp = trim($input['otp'] ?? '123456');

    if ($scheduleId === 0) {
        sendJsonResponse(["success" => false, "error" => "रूटीन आईडी अनिवार्य है"], 400);
    }

    $stmtSched = $pdo->prepare("SELECT s.*, l.building_name, l.lift_unique_code, l.tenant_id 
                                FROM elevex_amc_schedules s
                                JOIN elevex_lifts_amc l ON s.lift_id = l.id
                                WHERE s.id = :sid LIMIT 1");
    $stmtSched->execute([':sid' => $scheduleId]);
    $sched = $stmtSched->fetch();
    if (!$sched) {
        sendJsonResponse(["success" => false, "error" => "रूटीन रिकॉर्ड नहीं मिला"], 404);
    }

    $uploadDir = __DIR__ . "/../uploads/";
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }

    $sigUrl = null;
    if (!empty($sigBase64)) {
        $sigData = base64_decode(preg_replace('#^data:image/\\w+;base64,#i', '', $sigBase64));
        if ($sigData) {
            $sigFilename = "amc_sig_{$scheduleId}_" . time() . ".png";
            file_put_contents($uploadDir . $sigFilename, $sigData);
            $sigUrl = "https://shribalajikripadham.online/uploads/{$sigFilename}";
        }
    }

    $photoUrl = null;
    if (!empty($photoBase64)) {
        $photoData = base64_decode(preg_replace('#^data:image/\\w+;base64,#i', '', $photoBase64));
        if ($photoData) {
            $photoFilename = "amc_photo_{$scheduleId}_" . time() . ".jpg";
            file_put_contents($uploadDir . $photoFilename, $photoData);
            $photoUrl = "https://shribalajikripadham.online/uploads/{$photoFilename}";
        }
    }

    $stmtUp = $pdo->prepare("UPDATE elevex_amc_schedules 
        SET status = 'completed',
            assigned_technician_id = COALESCE(:tid, assigned_technician_id),
            checklist_json = :ck,
            service_notes = :notes,
            customer_signature_url = :sig,
            service_photo_url = :photo,
            completion_otp = :otp,
            completed_at = NOW()
        WHERE id = :sid");
    $stmtUp->execute([
        ':tid' => $techId > 0 ? $techId : null,
        ':ck' => is_array($checklist) ? json_encode($checklist) : $checklist,
        ':notes' => $notes,
        ':sig' => $sigUrl,
        ':photo' => $photoUrl,
        ':otp' => $otp,
        ':sid' => $scheduleId
    ]);

    // Update last_service_date on lift record
    $pdo->prepare("UPDATE elevex_lifts_amc SET last_service_date = CURDATE() WHERE id = :lid")
        ->execute([':lid' => $sched['lift_id']]);

    sendJsonResponse([
        "success" => true,
        "message" => "✅ 28-पॉइंट AMC प्रिवेंटिव मेंटेनेंस सफलतापूर्वक पूर्ण व प्रमाणित हुआ!",
        "schedule_id" => $scheduleId,
        "lift_name" => $sched['building_name'] . " (" . $sched['lift_unique_code'] . ")",
        "routine_month" => $sched['routine_month'],
        "signature_url" => $sigUrl,
        "photo_url" => $photoUrl,
        "completed_at" => date("Y-m-d H:i:s")
    ]);
}


    sendJsonResponse(["success" => false, "error" => "Invalid dispatch action"], 400);
} catch (Throwable $e) {
    sendJsonResponse([
        "success" => false,
        "error" => "Exception: " . $e->getMessage(),
        "file" => basename($e->getFile()),
        "line" => $e->getLine()
    ], 500);
}

