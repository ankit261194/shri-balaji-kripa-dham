<?php
if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} elseif (file_exists(__DIR__ . '/config/db.php')) {
    require_once __DIR__ . '/config/db.php';
} else {
    if (!defined('DB_HOST')) define('DB_HOST', 'localhost');
    if (!defined('DB_NAME')) define('DB_NAME', 'u237101617_balaji');
    if (!defined('DB_USER')) define('DB_USER', 'u237101617_ankitantim0');
    if (!defined('DB_PASS')) define('DB_PASS', 'Aa@8006518960');
    function getDB() {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";
        return new PDO($dsn, DB_USER, DB_PASS, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
        ]);
    }
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$pdo = getDB();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    $lat = floatval($input['latitude'] ?? 28.3972915);
    $long = floatval($input['longitude'] ?? 78.1460410);
    $radius = floatval($input['allowed_radius_meters'] ?? 200.0);
    $enforced = intval($input['is_geofence_enforced'] ?? 1);
    $outstationAllowed = intval($input['is_outstation_advance_allowed'] ?? 1);
    $outstationKm = floatval($input['outstation_min_distance_km'] ?? 30.0);
    $servingToken = intval($input['current_serving_token'] ?? 0);

    $stmt = $pdo->prepare("UPDATE ashram_settings SET
        ashram_latitude = :lat,
        ashram_longitude = :long,
        allowed_radius_meters = :rad,
        is_geofence_enforced = :enf,
        is_outstation_advance_allowed = :out_allow,
        outstation_min_distance_km = :out_km,
        current_serving_token = :serving
        WHERE id = 1");
    $stmt->execute([
        ':lat' => $lat,
        ':long' => $long,
        ':rad' => $radius,
        ':enf' => $enforced,
        ':out_allow' => $outstationAllowed,
        ':out_km' => $outstationKm,
        ':serving' => $servingToken
    ]);

    echo json_encode(["success" => true, "message" => "लोकेशन व परिधि नियम सुरक्षित!"], JSON_UNESCAPED_UNICODE);
    exit;
}

$stmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
$row = $stmt->fetch() ?: [];

echo json_encode([
    "success" => true,
    "ashram_name" => $row['ashram_name'] ?? 'श्री बालाजी कृपा धाम',
    "latitude" => floatval($row['ashram_latitude'] ?? 28.3972915),
    "longitude" => floatval($row['ashram_longitude'] ?? 78.1460410),
    "allowed_radius_meters" => floatval($row['allowed_radius_meters'] ?? 200.0),
    "is_geofence_enforced" => boolval($row['is_geofence_enforced'] ?? true),
    "is_outstation_advance_allowed" => boolval($row['is_outstation_advance_allowed'] ?? true),
    "outstation_min_distance_km" => floatval($row['outstation_min_distance_km'] ?? 30.0),
    "current_serving_token" => intval($row['current_serving_token'] ?? 0)
], JSON_UNESCAPED_UNICODE);
