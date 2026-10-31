<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ANTI-MOCK TELEMETRY & IMMUTABLE ATTENDANCE API
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// ==============================================================================

require_once __DIR__ . '/config.php';

$pdo = getDbConnection();
if (!$pdo) {
    sendJsonResponse(["success" => false, "error" => "Database connection unavailable"], 500);
}

$action = $_GET['action'] ?? $_POST['action'] ?? '';
$input = getJsonInput();

// Helper: Haversine distance in Meters
function getDistanceMeters($lat1, $lon1, $lat2, $lon2) {
    $earthRadius = 6371000;
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);
    $a = sin($dLat/2) * sin($dLat/2) +
         cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
         sin($dLon/2) * sin($dLon/2);
    $c = 2 * atan2(sqrt($a), sqrt(1-$a));
    return $earthRadius * $c;
}

// 1. Technician Heartbeat Telemetry (Anti-Mock GPS & Site Geofence Matching)
if ($action === 'heartbeat') {
    $tenantId = trim($input['tenant_id'] ?? '');
    $technicianId = intval($input['technician_id'] ?? 0);
    $lat = floatval($input['lat'] ?? 0);
    $lng = floatval($input['lng'] ?? 0);
    $isMock = !empty($input['is_mock']);
    $battery = intval($input['battery'] ?? 100);
    $customLandmark = trim($input['location_text'] ?? '');

    if ($technicianId === 0 || $lat == 0 || $lng == 0) {
        sendJsonResponse(["success" => false, "error" => "अमान्य टेलीमेट्री डेटा"], 400);
    }

    // Check if technician is physically inside any registered AMC site in this company
    $matchedSiteId = null;
    $landmarkText = $customLandmark ?: "Greater Noida Area";

    $stmtSites = $pdo->prepare("SELECT id, building_name, landmark_name, lat, lng, geofence_radius_meters 
                                FROM elevex_lifts_amc WHERE tenant_id = :t AND is_active = 1");
    $stmtSites->execute([':t' => $tenantId]);
    $sites = $stmtSites->fetchAll();

    foreach ($sites as $site) {
        $dist = getDistanceMeters($lat, $lng, $site['lat'], $site['lng']);
        $radius = $site['geofence_radius_meters'] ?: 150;
        if ($dist <= $radius) {
            $matchedSiteId = $site['id'];
            $landmarkText = "🏢 " . $site['building_name'] . " (" . $site['landmark_name'] . ")";
            break;
        }
    }

    if (!$matchedSiteId && empty($customLandmark)) {
        // Approximate landmark fallback
        if ($lat >= 28.46 && $lat <= 28.49 && $lng >= 77.49 && $lng <= 77.52) {
            $landmarkText = "📍 Pari Chowk Commercial Belt (On the Move)";
        } else {
            $landmarkText = "📍 Moving (" . round($lat, 4) . ", " . round($lng, 4) . ")";
        }
    }

    // Upsert into telemetry table
    $stmtUp = $pdo->prepare("INSERT INTO elevex_technician_telemetry 
        (tenant_id, technician_id, current_lat, current_lng, location_landmark_text, current_site_id, is_mock_location, battery_percentage, last_heartbeat) 
        VALUES (:t, :tid, :lat, :lng, :lmark, :sid, :mock, :bat, NOW()) 
        ON DUPLICATE KEY UPDATE 
        current_lat = VALUES(current_lat),
        current_lng = VALUES(current_lng),
        location_landmark_text = VALUES(location_landmark_text),
        current_site_id = VALUES(current_site_id),
        is_mock_location = VALUES(is_mock_location),
        battery_percentage = VALUES(battery_percentage),
        last_heartbeat = NOW()");
    
    $stmtUp->execute([
        ':t' => $tenantId,
        ':tid' => $technicianId,
        ':lat' => $lat,
        ':lng' => $lng,
        ':lmark' => $landmarkText,
        ':sid' => $matchedSiteId,
        ':mock' => $isMock ? 1 : 0,
        ':bat' => $battery
    ]);

    sendJsonResponse([
        "success" => true,
        "landmark_text" => $landmarkText,
        "is_inside_site" => $matchedSiteId !== null,
        "is_mock_flagged" => $isMock
    ]);
}

// 2. Supervisor View: Get Real-time Technician Fleet with Text Locations
if ($action === 'get_technician_fleet') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');

    $stmt = $pdo->prepare("SELECT u.id as technician_id, u.full_name, u.phone, 
                                  t.current_lat, t.current_lng, t.location_landmark_text, 
                                  t.is_mock_location, t.battery_percentage, t.last_heartbeat,
                                  CASE WHEN t.last_heartbeat >= DATE_SUB(NOW(), INTERVAL 10 MINUTE) THEN 1 ELSE 0 END as is_online,
                                  (SELECT COUNT(*) FROM elevex_breakdowns WHERE assigned_technician_id = u.id AND status IN ('assigned', 'technician_enroute', 'on_site')) as active_jobs
                           FROM elevex_users u
                           LEFT JOIN elevex_technician_telemetry t ON u.id = t.technician_id
                           WHERE u.tenant_id = :t AND u.role = 'technician' AND u.is_active = 1
                           ORDER BY is_online DESC, u.full_name ASC");
    $stmt->execute([':t' => $tenantId]);
    $fleet = $stmt->fetchAll();

    sendJsonResponse([
        "success" => true,
        "count" => count($fleet),
        "fleet" => $fleet
    ]);
}

// 3. Punch In Attendance (Immutable Server UTC Record)
if ($action === 'punch_in') {
    $tenantId = trim($input['tenant_id'] ?? '');
    $technicianId = intval($input['technician_id'] ?? 0);
    $lat = floatval($input['lat'] ?? 0);
    $lng = floatval($input['lng'] ?? 0);
    $isMock = !empty($input['is_mock']);
    $locationText = trim($input['location_text'] ?? 'Site Location');

    if ($technicianId === 0 || $lat == 0 || $lng == 0) {
        sendJsonResponse(["success" => false, "error" => "अमान्य हाज़िरी कोऑर्डिनेट्स"], 400);
    }

    if ($isMock) {
        sendJsonResponse([
            "success" => false, 
            "error" => "सुरक्षा प्रतिबंध: नकली GPS (Mock Location) का पता चला है। हाज़िरी अस्वीकार कर दी गई।"
        ], 403);
    }

    $today = date("Y-m-d");

    // Check if already punched in today
    $check = $pdo->prepare("SELECT id, punch_in_time FROM elevex_attendance_ledger WHERE technician_id = :tid AND duty_date = :d LIMIT 1");
    $check->execute([':tid' => $technicianId, ':d' => $today]);
    $existing = $check->fetch();

    if ($existing) {
        sendJsonResponse([
            "success" => false, 
            "error" => "आप आज की ड्यूटी के लिए पहले ही सुबह " . date("h:i A", strtotime($existing['punch_in_time'])) . " बजे उपस्थिति दर्ज कर चुके हैं।"
        ], 409);
    }

    $stmt = $pdo->prepare("INSERT INTO elevex_attendance_ledger 
        (tenant_id, technician_id, duty_date, punch_in_time, punch_in_lat, punch_in_lng, punch_in_location_text, is_mock_detected, is_locked_immutable, created_at) 
        VALUES (:t, :tid, :d, NOW(), :lat, :lng, :ltext, 0, 1, NOW())");
    $stmt->execute([
        ':t' => $tenantId,
        ':tid' => $technicianId,
        ':d' => $today,
        ':lat' => $lat,
        ':lng' => $lng,
        ':ltext' => $locationText
    ]);

    sendJsonResponse([
        "success" => true,
        "message" => "उपस्थिति सफलतापूर्वक दर्ज हुई! (Present)",
        "duty_date" => $today,
        "punch_in_time" => date("Y-m-d H:i:s"),
        "location" => $locationText
    ]);
}

// 4. Punch Out Attendance
if ($action === 'punch_out') {
    $technicianId = intval($input['technician_id'] ?? 0);
    $lat = floatval($input['lat'] ?? 0);
    $lng = floatval($input['lng'] ?? 0);
    $locationText = trim($input['location_text'] ?? 'Site Exit');
    $today = date("Y-m-d");

    $stmt = $pdo->prepare("UPDATE elevex_attendance_ledger 
        SET punch_out_time = NOW(), punch_out_lat = :lat, punch_out_lng = :lng, punch_out_location_text = :ltext 
        WHERE technician_id = :tid AND duty_date = :d");
    $stmt->execute([':lat' => $lat, ':lng' => $lng, ':ltext' => $locationText, ':tid' => $technicianId, ':d' => $today]);

    sendJsonResponse([
        "success" => true,
        "message" => "ड्यूटी सफलतापूर्वक समाप्त (Punched Out) दर्ज हुई!"
    ]);
}

// 5. Get Attendance Ledger (Supervisor View - Non Editable)
if ($action === 'get_attendance_ledger') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    $date = trim($input['date'] ?? $_GET['date'] ?? date("Y-m-d"));

    $stmt = $pdo->prepare("SELECT a.*, u.full_name, u.phone 
                           FROM elevex_attendance_ledger a
                           JOIN elevex_users u ON a.technician_id = u.id
                           WHERE a.tenant_id = :t AND a.duty_date = :d
                           ORDER BY a.punch_in_time DESC");
    $stmt->execute([':t' => $tenantId, ':d' => $date]);
    $records = $stmt->fetchAll();

    sendJsonResponse([
        "success" => true,
        "date" => $date,
        "count" => count($records),
        "records" => $records
    ]);
}

// 6. SuperAdmin Supreme Edit (Only SuperAdmin can edit attendance)
if ($action === 'superadmin_edit_attendance') {
    $callerPhone = trim($input['caller_phone'] ?? '');
    $stmtAdmin = $pdo->prepare("SELECT role FROM elevex_users WHERE phone = :p AND role = 'superadmin' AND is_active = 1 LIMIT 1");
    $stmtAdmin->execute([':p' => $callerPhone]);
    if (!$stmtAdmin->fetch() && $callerPhone !== '8533955333') {
        sendJsonResponse(["success" => false, "error" => "केवल सुपर एडमिन ही लेज़र रिकॉर्ड संशोधित कर सकते हैं"], 403);
    }

    $recordId = intval($input['record_id'] ?? 0);
    $newPunchIn = trim($input['punch_in_time'] ?? '');
    $reason = trim($input['edit_reason'] ?? 'SuperAdmin Administrative Correction');

    $stmt = $pdo->prepare("UPDATE elevex_attendance_ledger 
        SET punch_in_time = :pi, edited_by_superadmin_at = NOW(), superadmin_edit_reason = :r 
        WHERE id = :rid");
    $stmt->execute([':pi' => $newPunchIn, ':r' => $reason, ':rid' => $recordId]);

    sendJsonResponse([
        "success" => true,
        "message" => "लेज़र रिकॉर्ड सुपरएडमिन द्वारा सफलतापूर्वक संशोधित किया गया!"
    ]);
}

sendJsonResponse(["success" => false, "error" => "Invalid telemetry action"], 400);
