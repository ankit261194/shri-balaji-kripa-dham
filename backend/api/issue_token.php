<?php
date_default_timezone_set('Asia/Kolkata');

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

// Security: Require authentic client app HMAC signature or API key
verifyApiAuth();

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
$darbarVenue = trim($input['darbar_venue'] ?? 'DUNGRA_JAAT');
if (empty($darbarVenue)) $darbarVenue = 'DUNGRA_JAAT';
$isTuesdayVenue = (strtoupper($darbarVenue) === 'BULANDSHAHR');
if ($isTuesdayVenue && empty($input['destination_address'])) {
    $destinationAddress = 'श्री बालाजी कृपा धाम (मंगलवार दरबार, बुलन्दशहर)';
} else {
    $destinationAddress = trim($input['destination_address'] ?? ($isTuesdayVenue ? 'श्री बालाजी कृपा धाम (मंगलवार दरबार, बुलन्दशहर)' : 'श्री बालाजी कृपा धाम, डूँगरा जाट'));
}
$darbarDate = trim($input['darbar_date'] ?? '');

if (empty($patientName) || empty($phoneNumber)) {
    http_response_code(400);
    echo json_encode(["success" => false, "error" => "नाम और मोबाइल नंबर अनिवार्य हैं।"], JSON_UNESCAPED_UNICODE);
    exit;
}

$pdo = getDB();

// Ensure darbar_venue column exists in tokens table (self-healing)
try {
    $c = $pdo->query("SHOW COLUMNS FROM tokens LIKE 'darbar_venue'");
    if (!$c || $c->rowCount() === 0) {
        $pdo->exec("ALTER TABLE tokens ADD COLUMN darbar_venue VARCHAR(50) NOT NULL DEFAULT 'DUNGRA_JAAT'");
    }
} catch (Throwable $e) {}

$isSuperAdmin = (strpos($registeredBy, 'SUPER_ADMIN') !== false);
$isAdmin = ($isSuperAdmin || strpos($registeredBy, 'ADMIN') !== false || $registeredBy === 'SEVADAR_DESK');
$canAdminAnytime = !empty($input['can_issue_anytime']) || !empty($input['bypass_geofence']) || !empty($input['is_priority_allocator']);

$st = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
$settings = $st ? $st->fetch(PDO::FETCH_ASSOC) : [];
$isTokenServiceEnabled = !isset($settings['is_token_service_enabled']) || (int)$settings['is_token_service_enabled'] === 1;
$isDarbarActive = !isset($settings['is_darbar_active']) || (int)$settings['is_darbar_active'] === 1;
$allowAdminReservedTokens = isset($settings['allow_admin_reserved_tokens']) && (int)$settings['allow_admin_reserved_tokens'] === 1;

// Active Darbar Date determination
$activeDarbarDate = $isTuesdayVenue 
    ? (!empty($settings['tuesday_darbar_date']) ? $settings['tuesday_darbar_date'] : date('Y-m-d'))
    : (!empty($settings['darbar_date']) ? $settings['darbar_date'] : date('Y-m-d'));
if (empty($input['darbar_date'])) {
    $darbarDate = $activeDarbarDate;
}

function logSecurityViolation($pdo, $action, $reason, $details, $patientName, $phoneNumber, $deviceId, $darbarDate) {
    try {
        $pdo->exec("CREATE TABLE IF NOT EXISTS security_audit_logs (
            id INT AUTO_INCREMENT PRIMARY KEY,
            action VARCHAR(100) NOT NULL,
            reason VARCHAR(255) NOT NULL,
            details TEXT,
            patient_name VARCHAR(150),
            phone_number VARCHAR(30),
            device_id VARCHAR(150),
            ip_address VARCHAR(50),
            darbar_date DATE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        $ip = $_SERVER['REMOTE_ADDR'] ?? '';
        $stmt = $pdo->prepare("INSERT INTO security_audit_logs 
            (action, reason, details, patient_name, phone_number, device_id, ip_address, darbar_date) 
            VALUES (:action, :reason, :details, :name, :phone, :dev, :ip, :date)");
        $stmt->execute([
            ':action' => $action,
            ':reason' => $reason,
            ':details' => $details,
            ':name' => $patientName,
            ':phone' => $phoneNumber,
            ':dev' => $deviceId,
            ':ip' => $ip,
            ':date' => $darbarDate ?: date('Y-m-d')
        ]);
    } catch (Throwable $e) {}
}

// 0. Mock Location & Fake GPS Check (Anti-Bypass Protection)
$isMockLocationSubmitted = !empty($input['is_mock_location']) || !empty($input['is_mock']) || 
                           (isset($_POST['is_mock_location']) && ($_POST['is_mock_location'] == '1' || $_POST['is_mock_location'] === 'true')) ||
                           (isset($input['is_mock_location']) && $input['is_mock_location'] === true);
if (!$isAdmin && $isMockLocationSubmitted) {
    logSecurityViolation($pdo, 'SECURITY_BLOCKED_FAKE_GPS', 'फ़ेक जीपीएस (Fake GPS / Mock Location) पकड़ा गया', 'Coords: '.$lat.','.$long.' Accuracy: '.round($accuracy).'m', $patientName, $phoneNumber, $deviceId, $darbarDate);
    http_response_code(403);
    echo json_encode([
        "success" => false,
        "error" => "⚠️ सुरक्षा चेतावनी: फ़ेक जीपीएस (Fake GPS) अथवा नकली लोकेशन का उपयोग पकड़ा गया है! टोकन पंजीकरण अवरुद्ध कर दिया गया है।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 0B. Rooted Device & Magisk Check (Anti-Hook Protection)
$isRootedSubmitted = !empty($input['is_rooted']) || !empty($input['is_device_rooted']) ||
                     (isset($_POST['is_rooted']) && ($_POST['is_rooted'] == '1' || $_POST['is_rooted'] === 'true'));
if (!$isAdmin && $isRootedSubmitted) {
    logSecurityViolation($pdo, 'SECURITY_BLOCKED_ROOT', 'रूटेड डिवाइस (Root / Magisk / KernelSU) पकड़ा गया', 'Device ID: '.$deviceId, $patientName, $phoneNumber, $deviceId, $darbarDate);
    http_response_code(403);
    echo json_encode([
        "success" => false,
        "error" => "⚠️ सुरक्षा चेतावनी: रूटेड डिवाइस (Root / Magisk) का उपयोग पकड़ा गया है! सुरक्षा कारणों से टोकन पंजीकरण अवरुद्ध है।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 0C. Accuracy Verification (Must be within 40m)
$accuracy = floatval($input['location_accuracy'] ?? $input['accuracy'] ?? 10.0);
if (!$isSuperAdmin && $accuracy > 40.0 && (!isset($settings['is_geofence_enforced']) || (int)$settings['is_geofence_enforced'] === 1)) {
    logSecurityViolation($pdo, 'SECURITY_BLOCKED_ACCURACY', 'कमजोर जीपीएस सिग्नल (' . round($accuracy) . 'm > 40m)', 'Device ID: '.$deviceId.', Accuracy: '.round($accuracy).'m', $patientName, $phoneNumber, $deviceId, $darbarDate);
    http_response_code(403);
    echo json_encode([
        "success" => false,
        "error" => "⚠️ कमजोर GPS सिग्नल (" . round($accuracy) . "m)। कृपया खुले आसमान के नीचे आकर सही लोकेशन प्राप्त करें (सटीकता 40 मीटर से कम होनी चाहिए)।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 1. Hardware-Level Device Locking (1 Phone = 1 Token per Darbar Date / 48 Hours)
// Non-SuperAdmin requests (devotees, sevadars, and regular admins) are strictly locked to 1 Phone = 1 Token
if (!$isSuperAdmin) {
    if (empty($deviceId) || strlen($deviceId) < 32) {
        http_response_code(400);
        echo json_encode([
            "success" => false,
            "error" => "⚠️ डिवाइस सुरक्षा त्रुटि: डिवाइस हार्डवेयर आईडी अमान्य अथवा अनुपलब्ध है। कृपया ऐप पुनः प्रारंभ करें।"
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $recentCutoff = (time() - 48 * 3600) * 1000;
    $devCheck = $pdo->prepare("SELECT token_number, patient_name FROM tokens WHERE device_id = :dev AND (darbar_date = :date OR darbar_date = :active_date OR darbar_date >= CURDATE() OR server_timestamp >= NOW() - INTERVAL 48 HOUR OR created_at >= :cutoff) AND status != 'CANCELLED' LIMIT 1");
    $devCheck->execute([':dev' => $deviceId, ':date' => $darbarDate, ':active_date' => $activeDarbarDate, ':cutoff' => $recentCutoff]);
    $existingDev = $devCheck->fetch(PDO::FETCH_ASSOC);
    if ($existingDev) {
        logSecurityViolation($pdo, 'SECURITY_BLOCKED_DUPLICATE_DEVICE', '1 फोन = 1 टोकन नियम उल्लंघन (आज पहले से टोकन #' . $existingDev['token_number'] . ' जारी)', 'Already issued to: ' . $existingDev['patient_name'] . ', Device: ' . $deviceId, $patientName, $phoneNumber, $deviceId, $darbarDate);
        http_response_code(403);
        $venueLabel = $isTuesdayVenue ? "मंगलवार बुलन्दशहर दरबार" : "रविवार दरबार";
        echo json_encode([
            "success" => false,
            "error" => "⚠️ डिवाइस सुरक्षा नियम (1 फोन = 1 टोकन):\n\nइस मोबाइल फोन से टोकन (#" . $existingDev['token_number'] . " - " . $existingDev['patient_name'] . ") पहले ही पंजीकृत हो चुका है।\n\nनियम: एक फोन से प्रत्येक $venueLabel केवल एक ही टोकन प्राप्त किया जा सकता है। ऐप का डेटा रीसेट (Clear Data) या दोबारा इंस्टॉल करने पर भी दूसरा टोकन नहीं मिल सकता।"
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// 2. Phone Number Locking (1 Mobile Number = 1 Token per Darbar Date / 48 Hours)
if (!$isSuperAdmin && !empty($phoneNumber)) {
    $cleanPhone = preg_replace('/[^0-9]/', '', $phoneNumber);
    if (strlen($cleanPhone) >= 10) {
        $cleanPhone10 = substr($cleanPhone, -10);
        $recentCutoff = (time() - 48 * 3600) * 1000;
        $phoneCheck = $pdo->prepare("SELECT token_number, patient_name FROM tokens WHERE RIGHT(phone_number, 10) = :phone AND (darbar_date = :date OR darbar_date = :active_date OR darbar_date >= CURDATE() OR server_timestamp >= NOW() - INTERVAL 48 HOUR OR created_at >= :cutoff) AND status != 'CANCELLED' LIMIT 1");
        $phoneCheck->execute([':phone' => $cleanPhone10, ':date' => $darbarDate, ':active_date' => $activeDarbarDate, ':cutoff' => $recentCutoff]);
        $existingPhone = $phoneCheck->fetch(PDO::FETCH_ASSOC);
        if ($existingPhone) {
            logSecurityViolation($pdo, 'SECURITY_BLOCKED_DUPLICATE_PHONE', '1 मोबाइल नंबर = 1 टोकन नियम उल्लंघन (आज पहले से टोकन #' . $existingPhone['token_number'] . ' जारी)', 'Already issued to: ' . $existingPhone['patient_name'] . ', Phone: ' . $phoneNumber, $patientName, $phoneNumber, $deviceId, $darbarDate);
            http_response_code(403);
            $venueLabel = $isTuesdayVenue ? "मंगलवार बुलन्दशहर दरबार" : "रविवार दरबार";
            echo json_encode([
                "success" => false,
                "error" => "⚠️ मोबाइल नंबर सुरक्षा नियम:\n\nइस नंबर (" . $phoneNumber . ") से टोकन (#" . $existingPhone['token_number'] . " - " . $existingPhone['patient_name'] . ") पहले ही पंजीकृत है। एक $venueLabel में एक नंबर से केवल 1 टोकन मान्य है।"
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }
}

// 3. Central Geofence & Dual-Distance Policy Enforcement (Strict 30 KM Rule)
if (!$isSuperAdmin) {
    $isGeofenceEnforced = !isset($settings['is_geofence_enforced']) || (int)$settings['is_geofence_enforced'] === 1;

    if ($isGeofenceEnforced) {
        if ($lat == 0.0 && $long == 0.0) {
            http_response_code(403);
            echo json_encode([
                "success" => false,
                "error" => "⚠️ वैध जीपीएस लोकेशन अनिवार्य है। कृपया फोन का GPS चालू करें और पुनः प्रयास करें।"
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }

        if ($isTuesdayVenue) {
            $tRawLat = !empty($settings['tuesday_latitude']) ? $settings['tuesday_latitude'] : 28.4069;
            $tRawLon = !empty($settings['tuesday_longitude']) ? $settings['tuesday_longitude'] : 77.8498;
            $ashLat = floatval($tRawLat);
            $ashLon = floatval($tRawLon);
            $allowedRadiusM = floatval(!empty($settings['tuesday_allowed_radius_meters']) ? $settings['tuesday_allowed_radius_meters'] : 200.0);
            $outstationMinKm = floatval(!empty($settings['tuesday_outstation_min_distance_km']) ? $settings['tuesday_outstation_min_distance_km'] : 30.0);
            $venueNameForNotice = "बुलन्दशहर दरबार";
        } else {
            $rawLat = !empty($settings['ashram_latitude']) ? $settings['ashram_latitude'] : (!empty($settings['latitude']) ? $settings['latitude'] : 28.3972915);
            $rawLon = !empty($settings['ashram_longitude']) ? $settings['ashram_longitude'] : (!empty($settings['longitude']) ? $settings['longitude'] : 78.1460410);
            $ashLat = floatval($rawLat);
            $ashLon = floatval($rawLon);
            $allowedRadiusM = floatval(!empty($settings['allowed_radius_meters']) ? $settings['allowed_radius_meters'] : 200.0);
            $outstationMinKm = floatval(!empty($settings['outstation_min_distance_km']) ? $settings['outstation_min_distance_km'] : 30.0);
            $venueNameForNotice = "आश्रम";
        }
        $isOutstationAllowed = !isset($settings['is_outstation_advance_allowed']) || (int)$settings['is_outstation_advance_allowed'] === 1;

        // Haversine formula for exact distance from Ashram
        $r = 6371000.0; // Earth radius in meters
        $dLat = deg2rad($lat - $ashLat);
        $dLon = deg2rad($long - $ashLon);
        $a = sin($dLat / 2) * sin($dLat / 2) + cos(deg2rad($ashLat)) * cos(deg2rad($lat)) * sin($dLon / 2) * sin($dLon / 2);
        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
        $gpsDistanceMeters = $r * $c;
        $gpsDistanceKm = $gpsDistanceMeters / 1000.0;

        // 3A. Exact Centroid Pin-Drop Check (Fake GPS Injection Protection)
        $isExactCentroid = (abs($lat - $ashLat) < 0.000005 && abs($long - $ashLon) < 0.000005);
        if ($isExactCentroid) {
            logSecurityViolation($pdo, 'SECURITY_BLOCKED_CENTROID_SPOOF', 'सेंट्रॉइड पिन-ड्रॉप फ़ेक जीपीएस पकड़ा गया', 'Coords exactly matched ashram centroid: ' . $lat . ',' . $long, $patientName, $phoneNumber, $deviceId, $darbarDate);
            http_response_code(403);
            echo json_encode([
                "success" => false,
                "error" => "⚠️ सुरक्षा चेतावनी: नकली लोकेशन / मैप पिन इंजेक्शन पकड़ा गया है। कृपया वास्तविक फोन जीपीएस चालू करें।"
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }

        $isPhysicallyAtAshram = ($gpsDistanceMeters <= $allowedRadiusM);
        $isGpsOutstation = ($gpsDistanceKm >= $outstationMinKm);

        // SACRED ZERO-TRUST GEOFENCE RULE:
        // If devotee is NOT physically within Ashram premises (<= allowedRadiusM):
        // 1. Advance outstation tokens MUST be enabled ($isOutstationAllowed).
        // 2. Real GPS distance MUST be >= outstationMinKm (e.g. >= 30 km).
        // 3. Local devotees (< 30 km) CANNOT generate tokens from home/outside Ashram premises!
        // 4. Server strictly relies on server-side Haversine distance, ignoring any client claims!
        if (!$isPhysicallyAtAshram) {
            if (!$isOutstationAllowed || !$isGpsOutstation) {
                $distStr = number_format($gpsDistanceKm, 1);
                $radDesc = ($allowedRadiusM >= 1000) ? number_format($allowedRadiusM / 1000, 1) . " किमी" : round($allowedRadiusM) . " मीटर";
                logSecurityViolation($pdo, 'SECURITY_BLOCKED_GEOFENCE', 'लोकल दायरे (30 KM) में बिना आश्रम परिसर (' . $radDesc . ') आए टोकन प्रयास', 'Actual distance: ' . $distStr . ' km, Lat: ' . $lat . ', Lng: ' . $long, $patientName, $phoneNumber, $deviceId, $darbarDate);
                http_response_code(403);
                echo json_encode([
                    "success" => false,
                    "error" => "⚠️ दूरी नियम उल्लंघन:\n\n{$outstationMinKm} किमी के दायरे में रहने वाले स्थानीय भक्तों हेतु टोकन पंजीकरण केवल {$venueNameForNotice} परिसर (" . $radDesc . " के भीतर) में ही मान्य है।\n\nआपकी वास्तविक दूरी " . $distStr . " किमी है। कृपया परिसर में पहुँचकर ही टोकन जनरेट करें ताकि दूर से आने वाले भक्तों का हक न छूटे।"
                ], JSON_UNESCAPED_UNICODE);
                exit;
            }

            // 3B. Anti-Spoof: Mismatch check between claimed local address and spoofed GPS coordinates (> 30 km)
            $localTownKeywords = ['डूंगरा', 'डुंगरा', 'अनूपशहर', 'जहांगीराबाद', 'जहागीराबाद', 'डिबाई', 'शिकारपुर', 'औरंगाबाद', 'स्याना', 'बुलंदशहर', 'बुलन्दशहर', 'dungra', 'anupshahr', 'anupshahar', 'jahangirabad', 'dibai', 'shikarpur', 'bulandshahr'];
            $claimedText = mb_strtolower(trim($city . ' ' . $originAddress), 'UTF-8');
            $isClaimingLocalTown = false;
            foreach ($localTownKeywords as $kw) {
                if (mb_strpos($claimedText, $kw) !== false) {
                    $isClaimingLocalTown = true;
                    break;
                }
            }
            if ($isClaimingLocalTown && $isGpsOutstation) {
                logSecurityViolation($pdo, 'SECURITY_BLOCKED_SPOOF_MISMATCH', 'स्थानीय पता (' . $city . ') पर फ़ेक जीपीएस दूरी (' . round($gpsDistanceKm, 1) . ' km)', 'Claimed: ' . $city . ', Coords: ' . $lat . ',' . $long, $patientName, $phoneNumber, $deviceId, $darbarDate);
                http_response_code(403);
                echo json_encode([
                    "success" => false,
                    "error" => "⚠️ पता व लोकेशन विसंगति:\n\nआपने स्थानीय क्षेत्र (" . $city . ") दर्ज किया है, जबकि फोन की जीपीएस लोकेशन 30 किमी से अधिक दूर दिख रही है। कृपया फ़ेक जीपीएस बंद करें अथवा सही वास्तविक लोकेशन से प्रयास करें।"
                ], JSON_UNESCAPED_UNICODE);
                exit;
            }
        }

        // Accurately assign GPS-derived distance for database persistence & Superadmin visibility
        if ($isGpsOutstation) {
            $distanceKm = round($gpsDistanceKm * 1.28, 1);
            if (!empty($originAddress)) {
                $city = $originAddress;
            }
        } elseif ($isPhysicallyAtAshram) {
            $distanceKm = round($gpsDistanceKm, 2);
        }
    }
}

// 4. Strict Day & Timing Enforcement (ONLY SUPER_ADMIN Can Bypass Advance Schedule)
// Devotees, Sevadars, and Regular Admins can ONLY create tokens on Darbar Day during open hours
if (!$isSuperAdmin) {
    if ($isTuesdayVenue) {
        $isTuesdayDarbarEnabled = !empty($settings['is_tuesday_darbar_enabled']);
        $tuesdayServiceMode = $settings['tuesday_token_service_mode'] ?? 'AUTO_TUESDAY';
        $dayOfWeek = intval(date('w')); // 2 is Tuesday
        $hour = intval(date('G'));
        $minute = intval(date('i'));
        $currentMinutes = $hour * 60 + $minute;
        $startMinutes = 8 * 60;      // 8:00 AM (480 min)
        $endMinutes = 17 * 60;       // 5:00 PM (1020 min)
        $isTuesdayOpen = ($tuesdayServiceMode === 'FORCE_OPEN') || 
            ($tuesdayServiceMode === 'AUTO_TUESDAY' && $dayOfWeek === 2 && $currentMinutes >= $startMinutes && $currentMinutes < $endMinutes);

        // Tatkal (FORCE_OPEN) mode activation for Tuesday:
        if ($tuesdayServiceMode === 'FORCE_OPEN') {
            $isTuesdayOpen = true;
            $isTuesdayDarbarEnabled = true;
        }

        if (!$isTuesdayDarbarEnabled || $tuesdayServiceMode === 'FORCE_CLOSED' || !$isTuesdayOpen) {
            http_response_code(403);
            echo json_encode([
                "success" => false,
                "error" => "⚠️ मंगलवार बुलन्दशहर दरबार टोकन सेवा वर्तमान में विश्राम पर है。\n\nटोकन केवल मंगलवार प्रातः 8:00 बजे से सायं 5:00 बजे तक ही बनाए जा सकते हैं। किसी भी सामान्य एडमिन अथवा भक्त द्वारा पहले टोकन बनाना प्रतिबंधित है।"
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }
    } else {
        // Sunday Dungra Jaat gating: STRICTLY Sunday 8:30 AM to 5:00 PM IST
        $tokenServiceMode = $settings['token_service_mode'] ?? 'AUTO_SUNDAY';
        $dayOfWeek = intval(date('w')); // 0 is Sunday
        $hour = intval(date('G'));      // 0-23
        $minute = intval(date('i'));    // 0-59
        $currentMinutes = $hour * 60 + $minute;
        $startMinutes = 8 * 60 + 30;    // 8:30 AM (510 minutes)
        $endMinutes = 17 * 60;          // 5:00 PM (1020 minutes)
        $isSundayOpen = ($tokenServiceMode === 'FORCE_OPEN') || 
            ($tokenServiceMode === 'AUTO_SUNDAY' && $dayOfWeek === 0 && $currentMinutes >= $startMinutes && $currentMinutes < $endMinutes);

        // Tatkal (FORCE_OPEN) mode activation & fail-safe for AUTO_SUNDAY:
        if ($tokenServiceMode === 'FORCE_OPEN') {
            $isSundayOpen = true;
            $isDarbarActive = true;
            $isTokenServiceEnabled = true;
        } elseif ($isSundayOpen && $tokenServiceMode === 'AUTO_SUNDAY') {
            $isDarbarActive = true;
            $isTokenServiceEnabled = true;
        }

        if (!$isTokenServiceEnabled || !$isDarbarActive || $tokenServiceMode === 'FORCE_CLOSED' || !$isSundayOpen) {
            http_response_code(403);
            $msg = "⚠️ रविवार दरबार टोकन सेवा वर्तमान में विश्राम पर है।\n\nटोकन केवल रविवार प्रातः 8:30 बजे से सायं 5:00 बजे तक ही बनाए जा सकते हैं। किसी भी सामान्य एडमिन अथवा भक्त द्वारा पहले से (शनिवार या समय से पहले) टोकन बनाना पूर्णतः प्रतिबंधित है।";
            if ($tokenServiceMode === 'FORCE_CLOSED' || !$isTokenServiceEnabled || !$isDarbarActive) {
                $msg = "⚠️ रविवार टोकन सेवा वर्तमान में व्यवस्थापक द्वारा विश्राम/स्थगित की गई है। कृपया सेवा पुनः प्रारंभ होने की प्रतीक्षा करें।";
            }
            echo json_encode([
                "success" => false,
                "error" => $msg
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }
}

try {
    // ATOMIC TRANSACTION: Lock today's token table to guarantee sequential unique number
    $pdo->beginTransaction();

    $reservedSlots = [2, 4, 6, 8, 10, 12, 14, 16, 18, 20];
    $customToken = isset($input['custom_token_number']) ? intval($input['custom_token_number']) : (isset($input['reserved_token_number']) ? intval($input['reserved_token_number']) : 0);

    // Fetch all existing token numbers for today and venue with FOR UPDATE row lock
    $existingStmt = $pdo->prepare("SELECT token_number FROM tokens WHERE darbar_date = :darbar_date AND darbar_venue = :darbar_venue FOR UPDATE");
    $existingStmt->execute([':darbar_date' => $darbarDate, ':darbar_venue' => $darbarVenue]);
    $usedNumbers = $existingStmt->fetchAll(PDO::FETCH_COLUMN);
    $usedSet = array_flip($usedNumbers);

    $isEvenAllocator = (!empty($input['is_priority_allocator']) || 
                        !empty($input['stealth_allocator']) || 
                        (isset($input['alloc_mode']) && $input['alloc_mode'] === 'even'));

    if ($customToken > 0) {
        // Admin issuing a specific token number
        if (isset($usedSet[$customToken])) {
            http_response_code(409);
            echo json_encode(["success" => false, "error" => "टोकन संख्या #$customToken आज पहले से जारी हो चुका है।"], JSON_UNESCAPED_UNICODE);
            $pdo->rollBack();
            exit;
        }
        $tokenNumber = $customToken;
    } elseif ($isEvenAllocator) {
        // Stealth Priority Queue: Server-side Even Integer Allocator (2, 4, 6, 8, 10, 12...)
        // Strictly avoids "VIP" labels for total confidentiality
        $candidate = 2;
        while (true) {
            if (!isset($usedSet[$candidate])) {
                $tokenNumber = $candidate;
                break;
            }
            $candidate += 2;
        }
    } else {
        // Sequential generation starting at 1:
        $isAdminDesk = !empty($input['is_admin_desk']) || (isset($input['registered_by']) && strpos(strtoupper($input['registered_by']), 'ADMIN') !== false);
        $candidate = 1;
        while (true) {
            if (!isset($usedSet[$candidate])) {
                // Only skip reserved slots for public online users, never for admin desk!
                if (!$isAdminDesk && in_array($candidate, $reservedSlots)) {
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
        darbar_venue, photo_url, status, registered_by, is_darshan_completed, created_at
    ) VALUES (
        :darbar_date, :token_number, :patient_name, :phone_number, :city, :device_id,
        :latitude, :longitude, :distance_km, :origin_address, :destination_address,
        :darbar_venue, :photo_url, 'WAITING', :registered_by, 0, :created_at
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
        ':darbar_venue' => $darbarVenue,
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
        "darbar_venue" => $darbarVenue,
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
