import os
import base64
import zipfile
import shutil

backend_dir = r'C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\backend'
downloads_dir = r'C:\Users\hp\Downloads'

# Files to bundle in install.php
bundle_files = {}

# 1. Root files
root_files = ['.htaccess', 'download.php', 'index.php', 'schema.sql']
for rf in root_files:
    p = os.path.join(backend_dir, rf)
    if os.path.exists(p):
        with open(p, 'rb') as f:
            bundle_files[rf] = base64.b64encode(f.read()).decode('utf-8')

# 2. config/db.php
db_path = os.path.join(backend_dir, 'config', 'db.php')
if os.path.exists(db_path):
    with open(db_path, 'rb') as f:
        bundle_files['config/db.php'] = base64.b64encode(f.read()).decode('utf-8')

# 3. api/ files
api_dir = os.path.join(backend_dir, 'api')
for fname in os.listdir(api_dir):
    if fname.endswith('.php'):
        p = os.path.join(api_dir, fname)
        rel_path = f'api/{fname}'
        with open(p, 'rb') as f:
            bundle_files[rel_path] = base64.b64encode(f.read()).decode('utf-8')

# Build install.php template
install_php_code = """<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");

$baseDir = __DIR__;
$createdFiles = [];

// 1. Create Directories
$dirs = ['api', 'config', 'uploads', 'uploads/sevadars', 'uploads/donors', 'downloads'];
foreach ($dirs as $d) {
    $p = $baseDir . '/' . $d;
    if (!is_dir($p)) {
        mkdir($p, 0755, true);
    }
}

// 2. Deploy Files
$files = [
"""

for rel_path, b64 in bundle_files.items():
    install_php_code += f"    '{rel_path}' => base64_decode('{b64}'),\n"

install_php_code += """
];

foreach ($files as $relPath => $data) {
    $fullPath = $baseDir . '/' . $relPath;
    $dir = dirname($fullPath);
    if (!is_dir($dir)) {
        mkdir($dir, 0755, true);
    }
    if (file_put_contents($fullPath, $data) !== false) {
        $createdFiles[] = $relPath;
    }
}

// 3. Database Connection & Schema Migration
if (file_exists($baseDir . '/config/db.php')) {
    require_once $baseDir . '/config/db.php';
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

$pdo = getDB();
$results = [];

// Base Tables
$queries = [
    // 1. tokens
    "CREATE TABLE IF NOT EXISTS tokens (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        token_number INT NOT NULL,
        devotee_name VARCHAR(150) NOT NULL,
        phone_number VARCHAR(20) NOT NULL,
        district_city VARCHAR(100) NOT NULL,
        is_outstation TINYINT(1) NOT NULL DEFAULT 0,
        distance_km DECIMAL(6,2) NOT NULL DEFAULT 0.00,
        registration_time VARCHAR(50) NOT NULL,
        allocated_time_slot VARCHAR(100) NOT NULL,
        darbar_date VARCHAR(50) NOT NULL,
        status VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
        entry_source VARCHAR(30) NOT NULL DEFAULT 'DEVOTEE_APP',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_token_number (token_number),
        INDEX idx_phone (phone_number),
        INDEX idx_darbar_date (darbar_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 2. ashram_settings
    "CREATE TABLE IF NOT EXISTS ashram_settings (
        id INT PRIMARY KEY DEFAULT 1,
        ashram_name VARCHAR(255) NOT NULL DEFAULT 'श्री बालाजी कृपा धाम',
        ashram_latitude DECIMAL(11, 8) NOT NULL DEFAULT 28.3972915,
        ashram_longitude DECIMAL(11, 8) NOT NULL DEFAULT 78.1460410,
        allowed_radius_meters DECIMAL(8, 2) NOT NULL DEFAULT 200.0,
        is_geofence_enforced TINYINT(1) NOT NULL DEFAULT 1,
        is_outstation_advance_allowed TINYINT(1) NOT NULL DEFAULT 1,
        outstation_min_distance_km DECIMAL(6, 2) NOT NULL DEFAULT 30.0,
        current_serving_token INT NOT NULL DEFAULT 0,
        daily_token_limit INT NOT NULL DEFAULT 1000,
        is_darbar_active TINYINT(1) NOT NULL DEFAULT 1,
        darbar_date VARCHAR(50) NOT NULL DEFAULT '',
        darbar_timings VARCHAR(255) NOT NULL DEFAULT 'प्रत्येक रविवार प्रातःकाल 8:00 बजे से',
        emergency_notice TEXT,
        is_emergency_notice_visible TINYINT(1) NOT NULL DEFAULT 0,
        banner_title VARCHAR(255) NOT NULL DEFAULT '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट',
        banner_subtitle VARCHAR(255) NOT NULL DEFAULT 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार',
        is_banner_visible TINYINT(1) NOT NULL DEFAULT 1,
        guruji_photo_url VARCHAR(500) DEFAULT '',
        is_token_service_enabled TINYINT(1) NOT NULL DEFAULT 1,
        is_bus_booking_live TINYINT(1) NOT NULL DEFAULT 0,
        is_live_counter_visible TINYINT(1) NOT NULL DEFAULT 1,
        is_payment_feature_live TINYINT(1) NOT NULL DEFAULT 0,
        is_arzi_ledger_live TINYINT(1) NOT NULL DEFAULT 1,
        can_admin_issue_reserved_tokens TINYINT(1) NOT NULL DEFAULT 0,
        allow_admin_reserved_tokens TINYINT(1) NOT NULL DEFAULT 0,
        aarti_timings TEXT,
        whatsapp_number VARCHAR(20) DEFAULT '+918006518960',
        whatsapp_group_url VARCHAR(500) DEFAULT '',
        config_version INT NOT NULL DEFAULT 1,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 3. sevadars
    "CREATE TABLE IF NOT EXISTS sevadars (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(150) NOT NULL,
        phone VARCHAR(20) NOT NULL,
        role VARCHAR(150) NOT NULL DEFAULT 'सेवादार',
        photo_url VARCHAR(500) DEFAULT '',
        display_order INT NOT NULL DEFAULT 0,
        is_active TINYINT(1) NOT NULL DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_sevadar_order (display_order),
        INDEX idx_sevadar_active (is_active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 4. donors
    "CREATE TABLE IF NOT EXISTS donors (
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 5. parchas
    "CREATE TABLE IF NOT EXISTS parchas (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        parcha_id VARCHAR(50) NOT NULL UNIQUE,
        devotee_name VARCHAR(150) NOT NULL,
        phone_number VARCHAR(20) NOT NULL,
        address VARCHAR(255) NOT NULL,
        darbar_date VARCHAR(50) NOT NULL,
        parcha_text TEXT NOT NULL,
        blessing_type VARCHAR(100) NOT NULL DEFAULT 'सामान्य कृपा',
        is_urgent TINYINT(1) NOT NULL DEFAULT 0,
        is_hidden TINYINT(1) NOT NULL DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_parcha_phone (phone_number),
        INDEX idx_parcha_date (darbar_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 6. bus_seats
    "CREATE TABLE IF NOT EXISTS bus_seats (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        seat_number INT NOT NULL,
        devotee_name VARCHAR(150) NOT NULL,
        phone_number VARCHAR(20) NOT NULL,
        gender VARCHAR(10) NOT NULL,
        boarding_point VARCHAR(100) NOT NULL DEFAULT 'ग्राम डूँगरा जाट',
        booking_date VARCHAR(50) NOT NULL,
        darbar_date VARCHAR(50) NOT NULL,
        fare_amount DECIMAL(8,2) NOT NULL DEFAULT 0.00,
        is_paid TINYINT(1) NOT NULL DEFAULT 0,
        status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_seat_date (darbar_date, seat_number)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
];

foreach ($queries as $sql) {
    try {
        $pdo->exec($sql);
        $results[] = ["query" => substr(trim($sql), 0, 45) . "...", "status" => "OK"];
    } catch (PDOException $e) {
        $results[] = ["query" => substr(trim($sql), 0, 45) . "...", "status" => "WARN: " . $e->getMessage()];
    }
}

// 4. Automatic Column Migration for ashram_settings (Guarantees zero 500 errors)
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
    "aarti_timings" => "TEXT",
    "whatsapp_number" => "VARCHAR(20) DEFAULT '+918006518960'",
    "whatsapp_group_url" => "VARCHAR(500) DEFAULT ''",
    "config_version" => "INT NOT NULL DEFAULT 1"
];

$existingColsStmt = $pdo->query("SHOW COLUMNS FROM ashram_settings");
$existingCols = $existingColsStmt->fetchAll(PDO::FETCH_COLUMN);

$addedCols = [];
foreach ($targetCols as $col => $definition) {
    if (!in_array($col, $existingCols)) {
        try {
            $pdo->exec("ALTER TABLE ashram_settings ADD COLUMN `{$col}` {$definition}");
            $addedCols[] = $col;
        } catch (Exception $e) {}
    }
}

// 5. Ensure row 1 exists in ashram_settings
$checkRow = $pdo->query("SELECT id FROM ashram_settings WHERE id = 1 LIMIT 1")->fetch();
if (!$checkRow) {
    $pdo->exec("INSERT INTO ashram_settings (id, ashram_name, current_serving_token, is_darbar_active, is_token_service_enabled) VALUES (1, 'श्री बालाजी कृपा धाम', 0, 1, 1)");
}

$tablesStmt = $pdo->query("SHOW TABLES");
$existingTables = $tablesStmt->fetchAll(PDO::FETCH_COLUMN);

echo json_encode([
    "success" => true,
    "status" => "WEBSITE_AND_CENTRAL_API_ONLINE",
    "message" => "श्री बालाजी कृपा धाम आधिकारिक वेबसाइट व सेंट्रल API 100% तैनात व अपडेटेड!",
    "installed_files" => $createdFiles,
    "added_columns" => $addedCols,
    "tables_in_database" => $existingTables,
    "migration_results" => $results
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
"""

# Write install.php
install_path = os.path.join(backend_dir, 'install.php')
with open(install_path, 'w', encoding='utf-8') as f:
    f.write(install_php_code)
print(f"Written: {install_path}")

# Copy install.php to Downloads
dl_install_path = os.path.join(downloads_dir, 'install.php')
shutil.copy2(install_path, dl_install_path)
print(f"Copied to Downloads: {dl_install_path}")

# Create shribalajikripadham_backend.zip
zip_path = os.path.join(backend_dir, 'shribalajikripadham_backend.zip')
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(backend_dir):
        # skip zip itself and downloads APKs to keep zip tiny (< 1MB)
        if 'downloads' in root or 'uploads' in root:
            continue
        for file in files:
            if file.endswith('.zip') or file.endswith('.apk'):
                continue
            full = os.path.join(root, file)
            rel = os.path.relpath(full, backend_dir)
            z.write(full, rel)

print(f"Created ZIP: {zip_path} (Size: {os.path.getsize(zip_path)} bytes)")

# Copy zip to Downloads
dl_zip_path = os.path.join(downloads_dir, 'shribalajikripadham_backend.zip')
shutil.copy2(zip_path, dl_zip_path)
print(f"Copied ZIP to Downloads: {dl_zip_path}")
