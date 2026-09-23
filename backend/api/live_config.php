<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - हाई-स्पीड लाइव कॉन्फ़िग व स्टेट API
// Ultra-Fast Central Live Configuration & State API with Zero-Crash Architecture
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-API-KEY, x-sbkd-api-key");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} elseif (file_exists(__DIR__ . '/config/db.php')) {
    require_once __DIR__ . '/config/db.php';
} else {
    if (!defined('DB_HOST')) define('DB_HOST', 'localhost');
    if (!defined('DB_NAME')) define('DB_NAME', 'u237101617_balaji');
    if (!defined('DB_USER')) define('DB_USER', 'u237101617_ankitantim0');
    if (!defined('DB_PASS')) define('DB_PASS', 'Aa@8006518960');
    function getDB($exitOnError = false) {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4";
        try {
            return new PDO($dsn, DB_USER, DB_PASS, [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_TIMEOUT => 3
            ]);
        } catch (Throwable $e) {
            return null;
        }
    }
}

// 1. Safe Fallback Default Config (Guarantees 200 OK Even Under Database Outages)
function getFallbackConfig() {
    $now = time();
    $data = [
        "ashram_name" => "श्री बालाजी कृपा धाम",
        "latitude" => 28.3972915,
        "longitude" => 78.1460410,
        "allowed_radius_meters" => 200.0,
        "is_geofence_enforced" => true,
        "is_outstation_advance_allowed" => true,
        "outstation_min_distance_km" => 30.0,
        "running_token_number" => 0,
        "current_serving_token" => 0,
        "daily_token_limit" => 1000,
        "is_token_service_enabled" => true,
        "is_bus_booking_live" => false,
        "is_live_counter_visible" => true,
        "is_payment_feature_live" => false,
        "is_arzi_ledger_live" => true,
        "badi_arzi_rate" => 100.0,
        "chhoti_arzi_rate" => 50.0,
        "is_darbar_active" => true,
        "darbar_date" => date('Y-m-d'),
        "darbar_timings" => "प्रत्येक रविवार प्रातःकाल 8:00 बजे से",
        "emergency_notice" => "",
        "is_emergency_notice_visible" => false,
        "banner_title" => "🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट",
        "banner_subtitle" => "परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार",
        "is_banner_visible" => true,
        "guruji_photo_url" => "",
        "can_admin_issue_reserved_tokens" => false,
        "allow_admin_reserved_tokens" => false,
        "contact_phone" => "+91 97206 91090",
        "whatsapp_number" => "+91 97206 91090",
        "upi_id" => "shribalajikripadham@upi",
        "upi_name" => "श्री बालाजी कृपा धाम",
        "aarti_timings" => "",
        "is_darbar_live_now" => false,
        "live_stream_title" => "श्री बालाजी कृपा धाम दिव्य दरबार लाइव",
        "live_stream_url" => "",
        "youtube_live_url" => "",
        "facebook_live_url" => "",
        "config_version" => 1,
        "server_time" => $now,
        "sevadars" => [],
        "donors" => []
    ];
    return $data;
}

$cacheDir = __DIR__ . '/../cache';
$cacheFile = $cacheDir . '/live_config_cache.json';

// Target columns for schema self-healing (only run on demand or during mutations)
$targetCols = [
    "ashram_name" => "VARCHAR(255) NOT NULL DEFAULT 'श्री बालाजी कृपा धाम'",
    "ashram_latitude" => "DECIMAL(11, 8) NOT NULL DEFAULT 28.3972915",
    "ashram_longitude" => "DECIMAL(11, 8) NOT NULL DEFAULT 78.1460410",
    "allowed_radius_meters" => "DECIMAL(8, 2) NOT NULL DEFAULT 200.0",
    "is_geofence_enforced" => "TINYINT(1) NOT NULL DEFAULT 1",
    "is_outstation_advance_allowed" => "TINYINT(1) NOT NULL DEFAULT 1",
    "outstation_min_distance_km" => "DECIMAL(6, 2) NOT NULL DEFAULT 30.0",
    "current_serving_token" => "INT NOT NULL DEFAULT 0",
    "daily_token_limit" => "INT NOT NULL DEFAULT 1000",
    "is_token_service_enabled" => "TINYINT(1) NOT NULL DEFAULT 1",
    "is_bus_booking_live" => "TINYINT(1) NOT NULL DEFAULT 0",
    "is_live_counter_visible" => "TINYINT(1) NOT NULL DEFAULT 1",
    "is_payment_feature_live" => "TINYINT(1) NOT NULL DEFAULT 0",
    "is_arzi_ledger_live" => "TINYINT(1) NOT NULL DEFAULT 1",
    "is_darbar_active" => "TINYINT(1) NOT NULL DEFAULT 1",
    "darbar_date" => "VARCHAR(50) NOT NULL DEFAULT ''",
    "darbar_timings" => "VARCHAR(255) NOT NULL DEFAULT 'प्रत्येक रविवार प्रातःकाल 8:00 बजे से'",
    "emergency_notice" => "TEXT",
    "is_emergency_notice_visible" => "TINYINT(1) NOT NULL DEFAULT 0",
    "banner_title" => "VARCHAR(255) NOT NULL DEFAULT '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट'",
    "banner_subtitle" => "VARCHAR(255) NOT NULL DEFAULT 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार'",
    "is_banner_visible" => "TINYINT(1) NOT NULL DEFAULT 1",
    "guruji_photo_url" => "VARCHAR(500) DEFAULT ''",
    "can_admin_issue_reserved_tokens" => "TINYINT(1) NOT NULL DEFAULT 0",
    "allow_admin_reserved_tokens" => "TINYINT(1) NOT NULL DEFAULT 0",
    "badi_arzi_rate" => "DECIMAL(10, 2) NOT NULL DEFAULT 100.0",
    "chhoti_arzi_rate" => "DECIMAL(10, 2) NOT NULL DEFAULT 50.0",
    "contact_phone" => "VARCHAR(50) NOT NULL DEFAULT '+91 97206 91090'",
    "whatsapp_number" => "VARCHAR(50) NOT NULL DEFAULT '+91 97206 91090'",
    "upi_id" => "VARCHAR(100) NOT NULL DEFAULT 'shribalajikripadham@upi'",
    "upi_name" => "VARCHAR(150) NOT NULL DEFAULT 'श्री बालाजी कृपा धाम'",
    "aarti_timings" => "TEXT",
    "is_darbar_live_now" => "TINYINT(1) NOT NULL DEFAULT 0",
    "live_stream_title" => "VARCHAR(255) NOT NULL DEFAULT 'श्री बालाजी कृपा धाम दिव्य दरबार लाइव'",
    "live_stream_url" => "VARCHAR(500) DEFAULT ''",
    "youtube_live_url" => "VARCHAR(500) DEFAULT ''",
    "facebook_live_url" => "VARCHAR(500) DEFAULT ''",
    "config_version" => "INT NOT NULL DEFAULT 1"
];

function runSchemaMigrations($pdo, $targetCols) {
    if (!$pdo) return;
    try {
        $pdo->exec("CREATE TABLE IF NOT EXISTS ashram_settings (id INT PRIMARY KEY DEFAULT 1) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        $pdo->exec("INSERT IGNORE INTO ashram_settings (id) VALUES (1)");

        $colStmt = $pdo->query("SHOW COLUMNS FROM ashram_settings");
        $existingCols = $colStmt ? $colStmt->fetchAll(PDO::FETCH_COLUMN) : [];
        if (is_array($existingCols)) {
            $existingColMap = array_flip($existingCols);
            foreach ($targetCols as $col => $definition) {
                if (!isset($existingColMap[$col])) {
                    try {
                        $pdo->exec("ALTER TABLE ashram_settings ADD COLUMN `{$col}` {$definition}");
                    } catch (Throwable $t) {}
                }
            }
        }

        // Ensure secondary tables exist
        $pdo->exec("CREATE TABLE IF NOT EXISTS sevadars (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(150) NOT NULL,
            role VARCHAR(150) NOT NULL DEFAULT 'सेवादार',
            phone VARCHAR(20) NOT NULL DEFAULT '',
            photo_url VARCHAR(500) DEFAULT '',
            bio TEXT,
            display_order INT NOT NULL DEFAULT 0,
            is_active TINYINT(1) NOT NULL DEFAULT 1,
            created_at BIGINT NOT NULL,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX idx_sevadar_order (display_order),
            INDEX idx_sevadar_active (is_active)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        $pdo->exec("CREATE TABLE IF NOT EXISTS donors (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(150) NOT NULL,
            city_address VARCHAR(200) NOT NULL DEFAULT 'ग्राम डूँगरा जाट',
            title VARCHAR(200) NOT NULL DEFAULT 'मंदिर निर्माण सहयोगी',
            photo_url VARCHAR(500) DEFAULT '',
            phone VARCHAR(20) DEFAULT '',
            notes TEXT,
            display_order INT NOT NULL DEFAULT 0,
            is_active TINYINT(1) NOT NULL DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX idx_donor_order (display_order),
            INDEX idx_donor_active (is_active)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    } catch (Throwable $e) {
        error_log("runSchemaMigrations warning: " . $e->getMessage());
    }
}

// -----------------------------------------------------------------------------
// POST REQUEST: Admin Setting Mutation
// -----------------------------------------------------------------------------
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (function_exists('verifyApiAuth')) {
        verifyApiAuth();
    }

    $pdo = getDB();
    if (!$pdo) {
        http_response_code(503);
        echo json_encode(["success" => false, "error" => "डेटाबेस कनेक्शन उपलब्ध नहीं है (Database unavailable)"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Always run schema migration on POST to ensure any new columns exist
    runSchemaMigrations($pdo, $targetCols);

    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;

    // Fetch existing settings
    $current = [];
    try {
        $stmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
        $current = ($stmt) ? ($stmt->fetch() ?: []) : [];
    } catch (Throwable $e) {}

    // Check available columns
    $colStmt = $pdo->query("SHOW COLUMNS FROM ashram_settings");
    $cols = $colStmt ? $colStmt->fetchAll(PDO::FETCH_COLUMN) : [];
    $colSet = is_array($cols) ? array_flip($cols) : [];

    $fields = [
        'ashram_name' => trim($input['ashram_name'] ?? ($current['ashram_name'] ?? 'श्री बालाजी कृपा धाम')),
        'ashram_latitude' => floatval($input['latitude'] ?? ($current['ashram_latitude'] ?? 28.3972915)),
        'ashram_longitude' => floatval($input['longitude'] ?? ($current['ashram_longitude'] ?? 78.1460410)),
        'allowed_radius_meters' => floatval($input['allowed_radius_meters'] ?? ($current['allowed_radius_meters'] ?? 200.0)),
        'is_geofence_enforced' => isset($input['is_geofence_enforced']) ? intval($input['is_geofence_enforced']) : intval($current['is_geofence_enforced'] ?? 1),
        'is_outstation_advance_allowed' => isset($input['is_outstation_advance_allowed']) ? intval($input['is_outstation_advance_allowed']) : intval($current['is_outstation_advance_allowed'] ?? 1),
        'outstation_min_distance_km' => floatval($input['outstation_min_distance_km'] ?? ($current['outstation_min_distance_km'] ?? 30.0)),
        'current_serving_token' => isset($input['current_serving_token']) ? intval($input['current_serving_token']) : intval($current['current_serving_token'] ?? 0),
        'daily_token_limit' => isset($input['daily_token_limit']) ? intval($input['daily_token_limit']) : intval($current['daily_token_limit'] ?? 1000),
        'is_token_service_enabled' => isset($input['is_token_service_enabled']) ? intval($input['is_token_service_enabled']) : intval($current['is_token_service_enabled'] ?? 1),
        'is_bus_booking_live' => isset($input['is_bus_booking_live']) ? intval($input['is_bus_booking_live']) : intval($current['is_bus_booking_live'] ?? 0),
        'is_live_counter_visible' => isset($input['is_live_counter_visible']) ? intval($input['is_live_counter_visible']) : intval($current['is_live_counter_visible'] ?? 1),
        'is_payment_feature_live' => isset($input['is_payment_feature_live']) ? intval($input['is_payment_feature_live']) : intval($current['is_payment_feature_live'] ?? 0),
        'is_arzi_ledger_live' => isset($input['is_arzi_ledger_live']) ? intval($input['is_arzi_ledger_live']) : intval($current['is_arzi_ledger_live'] ?? 1),
        'is_darbar_active' => isset($input['is_darbar_active']) ? intval($input['is_darbar_active']) : intval($current['is_darbar_active'] ?? 1),
        'darbar_date' => trim($input['darbar_date'] ?? ($current['darbar_date'] ?? date('Y-m-d'))),
        'darbar_timings' => trim($input['darbar_timings'] ?? ($current['darbar_timings'] ?? 'प्रत्येक रविवार प्रातःकाल 8:00 बजे से')),
        'emergency_notice' => trim($input['emergency_notice'] ?? ($current['emergency_notice'] ?? '')),
        'is_emergency_notice_visible' => isset($input['is_emergency_notice_visible']) ? intval($input['is_emergency_notice_visible']) : intval($current['is_emergency_notice_visible'] ?? 0),
        'banner_title' => trim($input['banner_title'] ?? ($current['banner_title'] ?? '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट')),
        'banner_subtitle' => trim($input['banner_subtitle'] ?? ($current['banner_subtitle'] ?? 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार')),
        'is_banner_visible' => isset($input['is_banner_visible']) ? intval($input['is_banner_visible']) : intval($current['is_banner_visible'] ?? 1),
        'guruji_photo_url' => trim($input['guruji_photo_url'] ?? ($current['guruji_photo_url'] ?? '')),
        'can_admin_issue_reserved_tokens' => isset($input['can_admin_issue_reserved_tokens']) ? intval($input['can_admin_issue_reserved_tokens']) : (isset($input['allow_admin_reserved_tokens']) ? intval($input['allow_admin_reserved_tokens']) : intval($current['can_admin_issue_reserved_tokens'] ?? 0)),
        'allow_admin_reserved_tokens' => isset($input['allow_admin_reserved_tokens']) ? intval($input['allow_admin_reserved_tokens']) : (isset($input['can_admin_issue_reserved_tokens']) ? intval($input['can_admin_issue_reserved_tokens']) : intval($current['allow_admin_reserved_tokens'] ?? 0)),
        'badi_arzi_rate' => isset($input['badi_arzi_rate']) ? floatval($input['badi_arzi_rate']) : floatval($current['badi_arzi_rate'] ?? 100.0),
        'chhoti_arzi_rate' => isset($input['chhoti_arzi_rate']) ? floatval($input['chhoti_arzi_rate']) : floatval($current['chhoti_arzi_rate'] ?? 50.0),
        'contact_phone' => trim($input['contact_phone'] ?? ($current['contact_phone'] ?? '+91 97206 91090')),
        'whatsapp_number' => trim($input['whatsapp_number'] ?? ($current['whatsapp_number'] ?? '+91 97206 91090')),
        'upi_id' => trim($input['upi_id'] ?? ($current['upi_id'] ?? 'shribalajikripadham@upi')),
        'upi_name' => trim($input['upi_name'] ?? ($current['upi_name'] ?? 'श्री बालाजी कृपा धाम')),
        'aarti_timings' => trim($input['aarti_timings'] ?? ($current['aarti_timings'] ?? '')),
        'is_darbar_live_now' => isset($input['is_darbar_live_now']) ? intval($input['is_darbar_live_now']) : intval($current['is_darbar_live_now'] ?? 0),
        'live_stream_title' => trim($input['live_stream_title'] ?? ($current['live_stream_title'] ?? 'श्री बालाजी कृपा धाम दिव्य दरबार लाइव')),
        'live_stream_url' => trim($input['live_stream_url'] ?? ($current['live_stream_url'] ?? '')),
        'youtube_live_url' => trim($input['youtube_live_url'] ?? ($current['youtube_live_url'] ?? '')),
        'facebook_live_url' => trim($input['facebook_live_url'] ?? ($current['facebook_live_url'] ?? ''))
    ];

    $updatePairs = [];
    $bindings = [];
    foreach ($fields as $colName => $val) {
        if (isset($colSet[$colName])) {
            $paramName = ":p_" . $colName;
            $updatePairs[] = "`$colName` = $paramName";
            $bindings[$paramName] = $val;
        }
    }

    if (isset($colSet['config_version'])) {
        $updatePairs[] = "`config_version` = COALESCE(`config_version`, 1) + 1";
    }

    if (!empty($updatePairs)) {
        try {
            $sql = "UPDATE ashram_settings SET " . implode(", ", $updatePairs) . " WHERE id = 1";
            $stmt = $pdo->prepare($sql);
            $stmt->execute($bindings);
        } catch (Throwable $e) {
            error_log("Update ashram_settings error: " . $e->getMessage());
        }
    }

    // Synchronize full Sevadars list if provided in payload
    if (isset($input['sevadars']) && is_array($input['sevadars'])) {
        try {
            $inSevList = $input['sevadars'];
            $activeIds = [];
            foreach ($inSevList as $s) {
                $sName = trim($s['name'] ?? '');
                if (empty($sName)) continue;
                $sId = intval($s['id'] ?? 0);
                $sRole = trim($s['role'] ?? ($s['roleTitleHindi'] ?? 'आश्रम सेवादार'));
                $sPhone = trim($s['phone'] ?? ($s['phoneNumber'] ?? ''));
                $sPhoto = trim($s['photo_url'] ?? ($s['photoUri'] ?? ''));
                $sOrder = intval($s['display_order'] ?? ($s['displayOrder'] ?? 0));
                $sActive = isset($s['is_active']) ? intval($s['is_active']) : (isset($s['isActive']) ? ($s['isActive'] ? 1 : 0) : 1);
                $sCreated = intval($s['created_at'] ?? (time() * 1000));

                if ($sId > 0) {
                    $uStmt = $pdo->prepare("INSERT INTO sevadars (id, name, role, phone, photo_url, display_order, is_active, created_at)
                        VALUES (:id, :name, :role, :phone, :photo_url, :display_order, :is_active, :created_at)
                        ON DUPLICATE KEY UPDATE name = VALUES(name), role = VALUES(role), phone = VALUES(phone), photo_url = VALUES(photo_url), display_order = VALUES(display_order), is_active = VALUES(is_active)");
                    $uStmt->execute([
                        ':id' => $sId,
                        ':name' => $sName,
                        ':role' => $sRole,
                        ':phone' => $sPhone,
                        ':photo_url' => $sPhoto,
                        ':display_order' => $sOrder,
                        ':is_active' => $sActive,
                        ':created_at' => $sCreated
                    ]);
                    $activeIds[] = $sId;
                } else {
                    $iStmt = $pdo->prepare("INSERT INTO sevadars (name, role, phone, photo_url, display_order, is_active, created_at)
                        VALUES (:name, :role, :phone, :photo_url, :display_order, :is_active, :created_at)");
                    $iStmt->execute([
                        ':name' => $sName,
                        ':role' => $sRole,
                        ':phone' => $sPhone,
                        ':photo_url' => $sPhoto,
                        ':display_order' => $sOrder,
                        ':is_active' => $sActive,
                        ':created_at' => $sCreated
                    ]);
                    $activeIds[] = intval($pdo->lastInsertId());
                }
            }

            if (!empty($activeIds)) {
                $inClause = implode(',', array_map('intval', $activeIds));
                $pdo->exec("UPDATE sevadars SET is_active = 0 WHERE id NOT IN ($inClause)");
            }
        } catch (Throwable $sevEx) {}
    }

    // Invalidate Cache Immediately
    if (file_exists($cacheFile)) {
        @unlink($cacheFile);
    }

    echo json_encode([
        "success" => true,
        "status" => "SUCCESS",
        "message" => "सुपरएडमिन सेटिंग्स सफलतापूर्वक अपडेट व लाइव प्रसारित हुईं!",
        "current_serving_token" => $fields['current_serving_token'],
        "badi_arzi_rate" => $fields['badi_arzi_rate'],
        "chhoti_arzi_rate" => $fields['chhoti_arzi_rate'],
        "guruji_photo_url" => $fields['guruji_photo_url'],
        "is_token_service_enabled" => boolval($fields['is_token_service_enabled']),
        "is_bus_booking_live" => boolval($fields['is_bus_booking_live']),
        "timestamp" => time()
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// GET REQUEST: High-Speed Cached Live Status
// -----------------------------------------------------------------------------

// Optional manual trigger for schema migration: ?run_migration=1
if (isset($_GET['run_migration']) && $_GET['run_migration'] == '1') {
    $pdo = getDB();
    runSchemaMigrations($pdo, $targetCols);
    if (file_exists($cacheFile)) @unlink($cacheFile);
}

// Step 1: Check File Cache (10s TTL) for lightning-fast reads
$now = time();
if (file_exists($cacheFile) && ($now - filemtime($cacheFile) < 10)) {
    $cached = @file_get_contents($cacheFile);
    if ($cached && strlen($cached) > 50) {
        $etag = '"sbkd_c_' . filemtime($cacheFile) . '"';
        header("ETag: $etag");
        if (isset($_SERVER['HTTP_IF_NONE_MATCH']) && trim($_SERVER['HTTP_IF_NONE_MATCH']) === $etag) {
            http_response_code(304);
            exit;
        }
        echo $cached;
        exit;
    }
}

// Step 2: Query Live Database
$pdo = getDB();
$fb = getFallbackConfig();

if (!$pdo) {
    // Database connection down or busy: return safe fallback immediately
    $response = array_merge(["success" => true, "status" => "OFFLINE_CACHE_ACTIVE"], $fb);
    $response["config"] = $fb;
    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

try {
    $row = [];
    try {
        $stmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
        $row = $stmt ? ($stmt->fetch() ?: []) : [];
    } catch (Throwable $ex) {
        // Table may be missing; trigger migration once
        runSchemaMigrations($pdo, $targetCols);
    }

    $sevadars = [];
    try {
        $sevStmt = $pdo->query("SELECT id, name, role, phone, photo_url, bio, display_order FROM sevadars WHERE is_active = 1 ORDER BY display_order ASC, id ASC");
        $sevadars = $sevStmt ? ($sevStmt->fetchAll() ?: []) : [];
    } catch (Throwable $e) {}

    $donors = [];
    try {
        $donStmt = $pdo->query("SELECT id, name, city_address, title, photo_url, notes, display_order FROM donors WHERE is_active = 1 ORDER BY display_order ASC, id ASC");
        $donors = $donStmt ? ($donStmt->fetchAll() ?: []) : [];
    } catch (Throwable $e) {}

    $servingNum = isset($row['current_serving_token']) ? intval($row['current_serving_token']) : $fb['current_serving_token'];

    $configData = [
        "ashram_name" => !empty($row['ashram_name']) ? $row['ashram_name'] : $fb['ashram_name'],
        "latitude" => isset($row['ashram_latitude']) ? floatval($row['ashram_latitude']) : $fb['latitude'],
        "longitude" => isset($row['ashram_longitude']) ? floatval($row['ashram_longitude']) : $fb['longitude'],
        "allowed_radius_meters" => isset($row['allowed_radius_meters']) ? floatval($row['allowed_radius_meters']) : $fb['allowed_radius_meters'],
        "is_geofence_enforced" => isset($row['is_geofence_enforced']) ? boolval($row['is_geofence_enforced']) : $fb['is_geofence_enforced'],
        "is_outstation_advance_allowed" => isset($row['is_outstation_advance_allowed']) ? boolval($row['is_outstation_advance_allowed']) : $fb['is_outstation_advance_allowed'],
        "outstation_min_distance_km" => isset($row['outstation_min_distance_km']) ? floatval($row['outstation_min_distance_km']) : $fb['outstation_min_distance_km'],
        "running_token_number" => $servingNum,
        "current_serving_token" => $servingNum,
        "daily_token_limit" => isset($row['daily_token_limit']) ? intval($row['daily_token_limit']) : $fb['daily_token_limit'],
        "is_token_service_enabled" => isset($row['is_token_service_enabled']) ? boolval($row['is_token_service_enabled']) : $fb['is_token_service_enabled'],
        "is_bus_booking_live" => isset($row['is_bus_booking_live']) ? boolval($row['is_bus_booking_live']) : $fb['is_bus_booking_live'],
        "is_live_counter_visible" => isset($row['is_live_counter_visible']) ? boolval($row['is_live_counter_visible']) : $fb['is_live_counter_visible'],
        "is_payment_feature_live" => isset($row['is_payment_feature_live']) ? boolval($row['is_payment_feature_live']) : $fb['is_payment_feature_live'],
        "is_arzi_ledger_live" => isset($row['is_arzi_ledger_live']) ? boolval($row['is_arzi_ledger_live']) : $fb['is_arzi_ledger_live'],
        "badi_arzi_rate" => isset($row['badi_arzi_rate']) ? floatval($row['badi_arzi_rate']) : $fb['badi_arzi_rate'],
        "chhoti_arzi_rate" => isset($row['chhoti_arzi_rate']) ? floatval($row['chhoti_arzi_rate']) : $fb['chhoti_arzi_rate'],
        "is_darbar_active" => isset($row['is_darbar_active']) ? boolval($row['is_darbar_active']) : $fb['is_darbar_active'],
        "darbar_date" => !empty($row['darbar_date']) ? $row['darbar_date'] : $fb['darbar_date'],
        "darbar_timings" => !empty($row['darbar_timings']) ? $row['darbar_timings'] : $fb['darbar_timings'],
        "emergency_notice" => $row['emergency_notice'] ?? $fb['emergency_notice'],
        "is_emergency_notice_visible" => isset($row['is_emergency_notice_visible']) ? boolval($row['is_emergency_notice_visible']) : $fb['is_emergency_notice_visible'],
        "banner_title" => !empty($row['banner_title']) ? $row['banner_title'] : $fb['banner_title'],
        "banner_subtitle" => !empty($row['banner_subtitle']) ? $row['banner_subtitle'] : $fb['banner_subtitle'],
        "is_banner_visible" => isset($row['is_banner_visible']) ? boolval($row['is_banner_visible']) : $fb['is_banner_visible'],
        "guruji_photo_url" => $row['guruji_photo_url'] ?? $fb['guruji_photo_url'],
        "can_admin_issue_reserved_tokens" => isset($row['can_admin_issue_reserved_tokens']) ? boolval($row['can_admin_issue_reserved_tokens']) : $fb['can_admin_issue_reserved_tokens'],
        "allow_admin_reserved_tokens" => isset($row['allow_admin_reserved_tokens']) ? boolval($row['allow_admin_reserved_tokens']) : $fb['allow_admin_reserved_tokens'],
        "contact_phone" => !empty($row['contact_phone']) ? $row['contact_phone'] : $fb['contact_phone'],
        "whatsapp_number" => !empty($row['whatsapp_number']) ? $row['whatsapp_number'] : $fb['whatsapp_number'],
        "upi_id" => !empty($row['upi_id']) ? $row['upi_id'] : $fb['upi_id'],
        "upi_name" => !empty($row['upi_name']) ? $row['upi_name'] : $fb['upi_name'],
        "aarti_timings" => $row['aarti_timings'] ?? $fb['aarti_timings'],
        "is_darbar_live_now" => isset($row['is_darbar_live_now']) ? boolval($row['is_darbar_live_now']) : $fb['is_darbar_live_now'],
        "live_stream_title" => !empty($row['live_stream_title']) ? $row['live_stream_title'] : $fb['live_stream_title'],
        "live_stream_url" => $row['live_stream_url'] ?? $fb['live_stream_url'],
        "youtube_live_url" => $row['youtube_live_url'] ?? $fb['youtube_live_url'],
        "facebook_live_url" => $row['facebook_live_url'] ?? $fb['facebook_live_url'],
        "config_version" => isset($row['config_version']) ? intval($row['config_version']) : $fb['config_version'],
        "server_time" => time(),
        "sevadars" => $sevadars,
        "donors" => $donors
    ];

    $response = array_merge([
        "success" => true,
        "status" => "SUCCESS"
    ], $configData);
    $response["config"] = $configData;

    $jsonOutput = json_encode($response, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

    // Save to Cache
    if (!is_dir($cacheDir)) {
        @mkdir($cacheDir, 0755, true);
    }
    @file_put_contents($cacheFile, $jsonOutput, LOCK_EX);

    $etag = '"sbkd_' . $configData['config_version'] . '_' . $configData['current_serving_token'] . '"';
    header("ETag: $etag");
    echo $jsonOutput;

} catch (Throwable $fatal) {
    error_log("live_config fatal error: " . $fatal->getMessage());
    $fb = getFallbackConfig();
    $response = array_merge(["success" => true, "status" => "FALLBACK_RECOVERY_ACTIVE"], $fb);
    $response["config"] = $fb;
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
}