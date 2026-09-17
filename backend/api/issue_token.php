<?php
if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} elseif (file_exists(__DIR__ . '/config/db.php')) {
    require_once __DIR__ . '/config/db.php';
} else {
    // Fallback direct connection
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

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["success" => false, "error" => "Method Not Allowed"]);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true) ?: $_POST;

$patientName = trim($input['patient_name'] ?? '');
$phoneNumber = trim($input['phone_number'] ?? '');
$city = trim($input['city'] ?? '');
$deviceId = trim($input['device_id'] ?? '');
$lat = floatval($input['latitude'] ?? 0.0);
$long = floatval($input['longitude'] ?? 0.0);
$distanceKm = floatval($input['distance_km'] ?? 0.0);
$photoUrl = trim($input['photo_url'] ?? '');
$registeredBy = trim($input['registered_by'] ?? 'ONLINE_DEVOTEE');
$originAddress = trim($input['origin_address'] ?? '');
$destinationAddress = trim($input['destination_address'] ?? 'श्री बालाजी कृपा धाम, डूँगरा जाट');
$darbarDate = trim($input['darbar_date'] ?? date('Y-m-d'));

if (empty($patientName) || empty($phoneNumber)) {
    http_response_code(400);
    echo json_encode(["success" => false, "error" => "नाम और मोबाइल नंबर अनिवार्य हैं।"], JSON_UNESCAPED_UNICODE);
    exit;
}

$pdo = getDB();

try {
    // ATOMIC TRANSACTION: Lock today's token table to guarantee sequential unique number
    $pdo->beginTransaction();

    $reservedSlots = [2, 4, 6, 8, 10, 12, 14, 16, 18, 20];
    $customToken = isset($input['custom_token_number']) ? intval($input['custom_token_number']) : (isset($input['reserved_token_number']) ? intval($input['reserved_token_number']) : 0);

    // Fetch all existing token numbers for today with FOR UPDATE row lock
    $existingStmt = $pdo->prepare("SELECT token_number FROM tokens WHERE darbar_date = :darbar_date FOR UPDATE");
    $existingStmt->execute([':darbar_date' => $darbarDate]);
    $usedNumbers = $existingStmt->fetchAll(PDO::FETCH_COLUMN);
    $usedSet = array_flip($usedNumbers);

    if ($customToken > 0) {
        // Admin issuing a specific token (e.g. VIP reserved slot 2, 4, 6... 20)
        if (isset($usedSet[$customToken])) {
            http_response_code(409);
            echo json_encode(["success" => false, "error" => "टोकन संख्या #$customToken आज पहले से जारी हो चुका है।"], JSON_UNESCAPED_UNICODE);
            $pdo->rollBack();
            exit;
        }
        $tokenNumber = $customToken;
    } else {
        // Regular public devotee or standard sequential generation:
        // MUST strictly skip reserved slots [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
        $candidate = 1;
        while (true) {
            if (!isset($usedSet[$candidate])) {
                // If candidate is in reserved slots, skip it for regular issuance
                if (in_array($candidate, $reservedSlots)) {
                    $candidate++;
                    continue;
                }
                $tokenNumber = $candidate;
                break;
            }
            $candidate++;
        }
    }

    // 2. Insert new token
    $insert = $pdo->prepare("INSERT INTO tokens (
        darbar_date, token_number, patient_name, phone_number, city, device_id,
        latitude, longitude, distance_km, origin_address, destination_address,
        photo_url, status, registered_by, is_darshan_completed, created_at
    ) VALUES (
        :darbar_date, :token_number, :patient_name, :phone_number, :city, :device_id,
        :latitude, :longitude, :distance_km, :origin_address, :destination_address,
        :photo_url, 'WAITING', :registered_by, 0, :created_at
    )");

    $createdAt = time() * 1000;
    $insert->execute([
        ':darbar_date' => $darbarDate,
        ':token_number' => $tokenNumber,
        ':patient_name' => $patientName,
        ':phone_number' => $phoneNumber,
        ':city' => $city,
        ':device_id' => $deviceId,
        ':latitude' => $lat,
        ':longitude' => $long,
        ':distance_km' => $distanceKm,
        ':origin_address' => $originAddress,
        ':destination_address' => $destinationAddress,
        ':photo_url' => $photoUrl,
        ':registered_by' => $registeredBy,
        ':created_at' => $createdAt
    ]);

    $lastId = $pdo->lastInsertId();

    // 3. Upsert into devotee_profiles
    $profileStmt = $pdo->prepare("INSERT INTO devotee_profiles (
        phone_number, patient_name, city, photo_url, total_darshans, last_darbar_date, registered_by
    ) VALUES (
        :phone, :name, :city, :photo, 1, :darbar_date, :registered_by
    ) ON DUPLICATE KEY UPDATE
        patient_name = VALUES(patient_name),
        city = VALUES(city),
        photo_url = IF(VALUES(photo_url) != '', VALUES(photo_url), photo_url),
        total_darshans = total_darshans + 1,
        last_darbar_date = VALUES(last_darbar_date)");
    
    $profileStmt->execute([
        ':phone' => $phoneNumber,
        ':name' => $patientName,
        ':city' => $city,
        ':photo' => $photoUrl,
        ':darbar_date' => $darbarDate,
        ':registered_by' => $registeredBy
    ]);

    $pdo->commit();

    echo json_encode([
        "success" => true,
        "token_number" => $tokenNumber,
        "id" => $lastId,
        "darbar_date" => $darbarDate,
        "patient_name" => $patientName,
        "phone_number" => $phoneNumber,
        "city" => $city,
        "status" => "WAITING",
        "created_at" => $createdAt,
        "message" => "टोकन नंबर $tokenNumber सफलतापूर्वक जारी हुआ!"
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

} catch (Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
