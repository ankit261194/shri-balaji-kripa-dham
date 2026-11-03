<?php
// ==============================================================================
// ANKIT ELEVEX PRO - PHASE 4: ANTI-CHEATING 50M GPS GEOFENCING & BIKE TA-DA
// Real-time Haversine Distance Calculation (<50m Site Lock)
// Technician Daily Travel Kilometer Accumulator & Automated ₹3.50/KM Reimbursement
// ==============================================================================

ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/config.php';

try {
    $pdo = getDbConnection();
} catch (Exception $e) {
    echo json_encode(["success" => false, "error" => "Database connection error: " . $e->getMessage()]);
    exit;
}

$input = getJsonInput();
$action = $_GET['action'] ?? $_POST['action'] ?? ($input['action'] ?? '');
$tenantId = trim($_GET['tenant_id'] ?? $_POST['tenant_id'] ?? ($input['tenant_id'] ?? 'tenant_master_elevex'));

// Standard Haversine Distance Formula in Meters
function calculateHaversineDistanceMeters($lat1, $lon1, $lat2, $lon2) {
    $earthRadius = 6371000; // Meters
    $dLat = deg2rad($lat2 - $lat1);
    $dLon = deg2rad($lon2 - $lon1);
    $a = sin($dLat / 2) * sin($dLat / 2) +
         cos(deg2rad($lat1)) * cos(deg2rad($lat2)) *
         sin($dLon / 2) * sin($dLon / 2);
    $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
    return $earthRadius * $c;
}

try {

// 1. STRICT 50M GPS GEOFENCE VERIFICATION
if ($action === 'verify_geofence') {
    $techLat = (float)($input['technician_lat'] ?? $_GET['technician_lat'] ?? 0.0);
    $techLng = (float)($input['technician_lng'] ?? $_GET['technician_lng'] ?? 0.0);
    $liftId = (int)($input['lift_id'] ?? $_GET['lift_id'] ?? 0);
    $targetLat = (float)($input['target_lat'] ?? $_GET['target_lat'] ?? 0.0);
    $targetLng = (float)($input['target_lng'] ?? $_GET['target_lng'] ?? 0.0);
    $technicianId = (int)($input['technician_id'] ?? 0);

    // If lift_id supplied, fetch lift coordinates from elevex_lifts_amc
    if ($liftId > 0 && ($targetLat == 0.0 || $targetLng == 0.0)) {
        $stmt = $pdo->prepare("SELECT latitude, longitude, building_name FROM elevex_lifts_amc WHERE id = :lid");
        $stmt->execute([':lid' => $liftId]);
        $lift = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($lift && !empty($lift['latitude']) && !empty($lift['longitude'])) {
            $targetLat = (float)$lift['latitude'];
            $targetLng = (float)$lift['longitude'];
        }
    }

    if ($techLat == 0.0 || $techLng == 0.0 || $targetLat == 0.0 || $targetLng == 0.0) {
        // Fallback default coordinates if not set in DB
        $targetLat = 28.4744;
        $targetLng = 77.5040;
    }

    $distMeters = calculateHaversineDistanceMeters($techLat, $techLng, $targetLat, $targetLng);
    $isAllowed = ($distMeters <= 50.0);

    // Log violation if outside 50m
    if (!$isAllowed && $technicianId > 0) {
        $today = date('Y-m-d');
        $pdo->prepare("INSERT INTO elevex_technician_travel_logs (tenant_id, technician_id, duty_date, geofence_violations_count)
            VALUES (:t, :tid, :d, 1)
            ON DUPLICATE KEY UPDATE geofence_violations_count = geofence_violations_count + 1")
            ->execute([':t' => $tenantId, ':tid' => $technicianId, ':d' => $today]);
    }

    echo json_encode([
        "success" => true,
        "allowed" => $isAllowed,
        "distance_meters" => round($distMeters, 1),
        "threshold_meters" => 50.0,
        "technician_coords" => ["lat" => $techLat, "lng" => $techLng],
        "target_coords" => ["lat" => $targetLat, "lng" => $targetLng],
        "message" => $isAllowed 
            ? "GPS Geofence Verified: Technician is within 50m of elevator site."
            : "Geofence Restriction Active: Technician is " . round($distMeters, 1) . "m away. Must be within 50m to punch in / resolve ticket."
    ]);
    exit;
}

// 2. RECORD BIKE TRAVEL GPS WAYPOINT & ACCUMULATE KILOMETERS
if ($action === 'record_waypoint') {
    $technicianId = (int)($input['technician_id'] ?? 0);
    $lat = (float)($input['latitude'] ?? 0.0);
    $lng = (float)($input['longitude'] ?? 0.0);
    $timestamp = date('Y-m-d H:i:s');
    $today = date('Y-m-d');

    if ($technicianId <= 0 || $lat == 0.0 || $lng == 0.0) {
        echo json_encode(["success" => false, "error" => "Valid technician_id and coordinates required"]);
        exit;
    }

    // Fetch today's travel log
    $stmt = $pdo->prepare("SELECT * FROM elevex_technician_travel_logs WHERE technician_id = :tid AND duty_date = :d");
    $stmt->execute([':tid' => $technicianId, ':d' => $today]);
    $log = $stmt->fetch(PDO::FETCH_ASSOC);

    $waypoints = [];
    $totalKm = 0.0;
    $rate = 3.50; // ₹3.50 per KM standard bike rate

    if ($log) {
        $rate = (float)$log['ta_rate_per_km'];
        $totalKm = (float)$log['total_km'];
        if (!empty($log['waypoints_json'])) {
            $waypoints = json_decode($log['waypoints_json'], true) ?: [];
        }
        
        // Calculate incremental distance from previous waypoint
        if (!empty($waypoints)) {
            $lastWp = end($waypoints);
            $incMeters = calculateHaversineDistanceMeters((float)$lastWp['lat'], (float)$lastWp['lng'], $lat, $lng);
            // Ignore jitter < 15 meters
            if ($incMeters >= 15.0) {
                $totalKm += ($incMeters / 1000.0);
            }
        }

        $waypoints[] = ["lat" => $lat, "lng" => $lng, "t" => $timestamp];
        $claimAmt = round($totalKm * $rate, 2);

        $upStmt = $pdo->prepare("UPDATE elevex_technician_travel_logs SET 
            end_lat = :lat, end_lng = :lng, waypoints_json = :wps, total_km = :km, ta_claim_amount = :amt 
            WHERE id = :id");
        $upStmt->execute([
            ':lat' => $lat,
            ':lng' => $lng,
            ':wps' => json_encode($waypoints),
            ':km' => round($totalKm, 2),
            ':amt' => $claimAmt,
            ':id' => $log['id']
        ]);
    } else {
        $waypoints[] = ["lat" => $lat, "lng" => $lng, "t" => $timestamp];
        $insStmt = $pdo->prepare("INSERT INTO elevex_technician_travel_logs 
            (tenant_id, technician_id, duty_date, start_lat, start_lng, end_lat, end_lng, waypoints_json, total_km, ta_rate_per_km, ta_claim_amount, status)
            VALUES (:t, :tid, :d, :slat, :slng, :elat, :elng, :wps, 0.00, :rate, 0.00, 'active_tracking')");
        $insStmt->execute([
            ':t' => $tenantId,
            ':tid' => $technicianId,
            ':d' => $today,
            ':slat' => $lat,
            ':slng' => $lng,
            ':elat' => $lat,
            ':elng' => $lng,
            ':wps' => json_encode($waypoints),
            ':rate' => $rate
        ]);
    }

    echo json_encode([
        "success" => true,
        "technician_id" => $technicianId,
        "total_km" => round($totalKm, 2),
        "ta_claim_amount" => round($totalKm * $rate, 2),
        "waypoints_logged" => count($waypoints)
    ]);
    exit;
}

// 3. GET DAILY TRAVEL & TA-DA SUMMARY
if ($action === 'get_daily_summary') {
    $technicianId = (int)($_GET['technician_id'] ?? $input['technician_id'] ?? 0);
    $dutyDate = trim($_GET['duty_date'] ?? $input['duty_date'] ?? date('Y-m-d'));

    $stmt = $pdo->prepare("SELECT l.*, u.full_name as technician_name, u.phone as technician_phone 
        FROM elevex_technician_travel_logs l
        LEFT JOIN elevex_users u ON u.id = l.technician_id
        WHERE l.technician_id = :tid AND l.duty_date = :d");
    $stmt->execute([':tid' => $technicianId, ':d' => $dutyDate]);
    $log = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$log) {
        echo json_encode([
            "success" => true,
            "has_records" => false,
            "duty_date" => $dutyDate,
            "total_km" => 0.0,
            "ta_rate_per_km" => 3.50,
            "ta_claim_amount" => 0.0,
            "geofence_violations_count" => 0,
            "status" => "no_travel_logged"
        ]);
        exit;
    }

    $wps = !empty($log['waypoints_json']) ? json_decode($log['waypoints_json'], true) : [];

    echo json_encode([
        "success" => true,
        "has_records" => true,
        "log_id" => (int)$log['id'],
        "duty_date" => $log['duty_date'],
        "technician_name" => $log['technician_name'] ?? 'Technician',
        "technician_phone" => $log['technician_phone'] ?? '',
        "total_km" => (float)$log['total_km'],
        "ta_rate_per_km" => (float)$log['ta_rate_per_km'],
        "ta_claim_amount" => (float)$log['ta_claim_amount'],
        "geofence_violations_count" => (int)$log['geofence_violations_count'],
        "status" => $log['status'],
        "waypoints_count" => count($wps)
    ]);
    exit;
}

// 4. SUBMIT DAILY TA-DA CLAIM
if ($action === 'submit_tada_claim') {
    $technicianId = (int)($input['technician_id'] ?? 0);
    $dutyDate = trim($input['duty_date'] ?? date('Y-m-d'));

    $stmt = $pdo->prepare("SELECT * FROM elevex_technician_travel_logs WHERE technician_id = :tid AND duty_date = :d");
    $stmt->execute([':tid' => $technicianId, ':d' => $dutyDate]);
    $log = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$log) {
        echo json_encode(["success" => false, "error" => "No travel logged for date $dutyDate"]);
        exit;
    }

    $pdo->prepare("UPDATE elevex_technician_travel_logs SET status = 'submitted_for_approval' WHERE id = :id")
        ->execute([':id' => $log['id']]);

    $waMessage = "🏍️ *दैनिक बाइक यात्रा भत्ता दावा (TA-DA Claim)*\nदिनांक: {$dutyDate}\nकुल दूरी: {$log['total_km']} KM\nदर: ₹{$log['ta_rate_per_km']}/KM\n*कुल दावा राशि: ₹{$log['ta_claim_amount']}*\n\nसत्यापित GPS वेपॉइंट्स द्वारा जनरेटेड।\n━━━━━━━━━━━━━━━━━━━━";

    echo json_encode([
        "success" => true,
        "message" => "TA-DA claim submitted to supervisor",
        "total_km" => (float)$log['total_km'],
        "ta_claim_amount" => (float)$log['ta_claim_amount'],
        "whatsapp_message" => $waMessage
    ]);
    exit;
}

// 5. LIST ALL CLAIMS FOR SUPERVISOR / SUPERADMIN
if ($action === 'list_claims') {
    $where = "WHERE 1=1";
    $params = [];
    if ($tenantId !== 'all') {
        $where .= " AND l.tenant_id = :t";
        $params[':t'] = $tenantId;
    }

    $stmt = $pdo->prepare("SELECT l.*, u.full_name as technician_name, u.phone as technician_phone 
        FROM elevex_technician_travel_logs l
        LEFT JOIN elevex_users u ON u.id = l.technician_id
        $where ORDER BY l.duty_date DESC, l.id DESC LIMIT 100");
    $stmt->execute($params);
    $claims = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "success" => true,
        "count" => count($claims),
        "claims" => $claims
    ]);
    exit;
}

// 6. DELETE TRAVEL CLAIM (SUPERADMIN ACTION)
if ($action === 'delete_claim') {
    $claimId = (int)($input['claim_id'] ?? $_GET['claim_id'] ?? 0);
    if ($claimId <= 0) {
        echo json_encode(["success" => false, "error" => "Claim ID required"]);
        exit;
    }

    $stmt = $pdo->prepare("DELETE FROM elevex_technician_travel_logs WHERE id = :id");
    $stmt->execute([':id' => $claimId]);

    echo json_encode([
        "success" => true,
        "message" => "यात्रा भत्ता क्लेम सफलतापूर्वक हटा दिया गया!"
    ]);
    exit;
}

echo json_encode(["success" => false, "error" => "Invalid action specified: " . htmlspecialchars($action)]);

} catch (Throwable $e) {
    echo json_encode([
        "success" => false,
        "error" => $e->getMessage(),
        "line" => $e->getLine(),
        "file" => basename($e->getFile())
    ]);
    exit;
}
