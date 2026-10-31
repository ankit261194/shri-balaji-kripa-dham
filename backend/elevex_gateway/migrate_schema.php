<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE MULTI-TENANT SCHEMA MIGRATION ENGINE
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// ==============================================================================

ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");

require_once __DIR__ . '/config.php';

$pdo = getDbConnection();
if (!$pdo) {
    echo json_encode(["success" => false, "error" => "Database connection unavailable"], JSON_UNESCAPED_UNICODE);
    exit;
}

$tables = [
    // 1. Companies Table
    "elevex_companies" => "CREATE TABLE IF NOT EXISTS elevex_companies (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL UNIQUE,
        company_name VARCHAR(191) NOT NULL,
        brand_display_name VARCHAR(191) NOT NULL,
        logo_url TEXT NULL,
        primary_color_hex VARCHAR(16) DEFAULT '#0284C7',
        support_phone VARCHAR(32) NOT NULL,
        office_address TEXT NOT NULL,
        office_lat DECIMAL(10, 7) NULL,
        office_lng DECIMAL(10, 7) NULL,
        features_config JSON NULL,
        default_language VARCHAR(8) DEFAULT 'hi',
        is_active TINYINT(1) DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 2. Users Table
    "elevex_users" => "CREATE TABLE IF NOT EXISTS elevex_users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        role VARCHAR(50) NOT NULL DEFAULT 'technician',
        full_name VARCHAR(128) NOT NULL,
        phone VARCHAR(20) NOT NULL,
        mpin_hash VARCHAR(255) NULL,
        hardware_device_id VARCHAR(128) NULL,
        fcm_token TEXT NULL,
        preferred_language VARCHAR(8) DEFAULT 'hi',
        is_active TINYINT(1) DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_tenant_phone (tenant_id, phone),
        INDEX idx_tenant_role (tenant_id, role)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 3. AMC Lifts Table
    "elevex_lifts_amc" => "CREATE TABLE IF NOT EXISTS elevex_lifts_amc (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        owner_id INT NULL,
        lift_unique_code VARCHAR(64) NOT NULL,
        building_name VARCHAR(191) NOT NULL,
        building_address TEXT NOT NULL,
        landmark_name VARCHAR(128) NOT NULL,
        lat DECIMAL(10, 7) NOT NULL,
        lng DECIMAL(10, 7) NOT NULL,
        geofence_radius_meters INT DEFAULT 100,
        controller_brand VARCHAR(64) DEFAULT 'MONARCH',
        amc_type VARCHAR(64) DEFAULT 'Comprehensive',
        amc_valid_until DATE NOT NULL,
        last_service_date DATE NULL,
        is_active TINYINT(1) DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_tenant_lifts (tenant_id, is_active),
        INDEX idx_owner (owner_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 4. Breakdowns Queue Table
    "elevex_breakdowns" => "CREATE TABLE IF NOT EXISTS elevex_breakdowns (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        ticket_number VARCHAR(64) NOT NULL UNIQUE,
        lift_id INT NOT NULL,
        reported_by_id INT NOT NULL,
        assigned_technician_id INT NULL,
        assigned_by_id INT NULL,
        is_ghost_override TINYINT(1) DEFAULT 0,
        dispatch_mode ENUM('manual', 'auto_nearest', 'superadmin_override') DEFAULT 'manual',
        status ENUM('pending', 'assigned', 'technician_enroute', 'on_site', 'resolved', 'cancelled') DEFAULT 'pending',
        issue_description TEXT NULL,
        completion_otp VARCHAR(6) NULL,
        customer_rating TINYINT(1) NULL,
        customer_feedback TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        assigned_at DATETIME NULL,
        resolved_at DATETIME NULL,
        INDEX idx_tenant_status (tenant_id, status),
        INDEX idx_tech_status (assigned_technician_id, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 5. Immutable Attendance Ledger Table
    "elevex_attendance_ledger" => "CREATE TABLE IF NOT EXISTS elevex_attendance_ledger (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        technician_id INT NOT NULL,
        duty_date DATE NOT NULL,
        punch_in_time DATETIME NOT NULL,
        punch_in_lat DECIMAL(10, 7) NOT NULL,
        punch_in_lng DECIMAL(10, 7) NOT NULL,
        punch_in_location_text VARCHAR(255) NOT NULL,
        punch_out_time DATETIME NULL,
        punch_out_lat DECIMAL(10, 7) NULL,
        punch_out_lng DECIMAL(10, 7) NULL,
        punch_out_location_text VARCHAR(255) NULL,
        is_mock_detected TINYINT(1) DEFAULT 0,
        is_locked_immutable TINYINT(1) DEFAULT 1,
        edited_by_superadmin_at DATETIME NULL,
        superadmin_edit_reason TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uq_tech_duty_date (technician_id, duty_date),
        INDEX idx_tenant_duty (tenant_id, duty_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",

    // 6. Real-time Telemetry Table
    "elevex_technician_telemetry" => "CREATE TABLE IF NOT EXISTS elevex_technician_telemetry (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        technician_id INT NOT NULL UNIQUE,
        current_lat DECIMAL(10, 7) NOT NULL,
        current_lng DECIMAL(10, 7) NOT NULL,
        location_landmark_text VARCHAR(255) NOT NULL,
        current_site_id INT NULL,
        is_mock_location TINYINT(1) DEFAULT 0,
        battery_percentage INT DEFAULT 100,
        last_heartbeat DATETIME NOT NULL,
        INDEX idx_tenant_telemetry (tenant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
];

$results = [];
foreach ($tables as $name => $ddl) {
    try {
        $pdo->exec($ddl);
        $results[$name] = "CREATED / VERIFIED OK";
    } catch (Exception $e) {
        $results[$name] = "ERROR: " . $e->getMessage();
    }
}

// Seamless migration of elevex_users table for existing databases
try {
    $existingCols = [];
    $colStmt = $pdo->query("SHOW COLUMNS FROM elevex_users");
    while ($col = $colStmt->fetch()) {
        $existingCols[] = $col['Field'];
    }

    if (!in_array('tenant_id', $existingCols)) {
        $pdo->exec("ALTER TABLE elevex_users ADD COLUMN tenant_id VARCHAR(64) NOT NULL DEFAULT 'tenant_master_elevex'");
    }
    if (!in_array('phone', $existingCols)) {
        $pdo->exec("ALTER TABLE elevex_users ADD COLUMN phone VARCHAR(20) NULL");
        if (in_array('mobile', $existingCols)) {
            $pdo->exec("UPDATE elevex_users SET phone = mobile WHERE phone IS NULL OR phone = ''");
        }
    }
    if (!in_array('hardware_device_id', $existingCols)) {
        $pdo->exec("ALTER TABLE elevex_users ADD COLUMN hardware_device_id VARCHAR(128) NULL");
    }
    if (!in_array('fcm_token', $existingCols)) {
        $pdo->exec("ALTER TABLE elevex_users ADD COLUMN fcm_token TEXT NULL");
    }
    if (!in_array('preferred_language', $existingCols)) {
        $pdo->exec("ALTER TABLE elevex_users ADD COLUMN preferred_language VARCHAR(8) DEFAULT 'hi'");
    }
    if (!in_array('is_active', $existingCols)) {
        $pdo->exec("ALTER TABLE elevex_users ADD COLUMN is_active TINYINT(1) DEFAULT 1");
    }
    $results['elevex_users_migration'] = "COLUMNS_VERIFIED_AND_UPDATED";
} catch (Exception $e) {
    $results['elevex_users_migration_error'] = $e->getMessage();
}

// Seed Master SuperAdmin Ankit Chaudhary
try {
    $superTenant = 'tenant_master_elevex';
    $superCheck = $pdo->prepare("SELECT id FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $superCheck->execute([':t' => $superTenant]);
    if (!$superCheck->fetch()) {
        $stmtC = $pdo->prepare("INSERT INTO elevex_companies (tenant_id, company_name, brand_display_name, support_phone, office_address, features_config, is_active) VALUES (:tid, :cname, :bname, :phone, :addr, :feat, 1)");
        $stmtC->execute([
            ':tid' => $superTenant,
            ':cname' => 'Ankit EleveX Supreme Command',
            ':bname' => 'EleveX Pro International',
            ':phone' => '8533955333',
            ':addr' => 'Sector Alpha 1 / Pari Chowk, Greater Noida',
            ':feat' => json_encode([
                "door_drive_specialist" => true,
                "offline_diagnostics" => true,
                "anti_spoof_gps" => true,
                "unlimited_free_tier" => true
            ])
        ]);
        $results['seed_master_company'] = "INSERTED";
    } else {
        $results['seed_master_company'] = "EXISTS";
    }

    $userCheck = $pdo->prepare("SELECT id FROM elevex_users WHERE (phone = :p OR phone = :p2) LIMIT 1");
    $userCheck->execute([':p' => '8533955333', ':p2' => '+918533955333']);
    $existingUser = $userCheck->fetch();

    if (!$existingUser) {
        $mpinHash = password_hash('1234', PASSWORD_BCRYPT);
        $stmtU = $pdo->prepare("INSERT INTO elevex_users (tenant_id, role, full_name, phone, mpin_hash, is_active) VALUES (:t, 'superadmin', :n, :p, :m, 1)");
        $stmtU->execute([
            ':t' => $superTenant,
            ':n' => 'अंकित चौधरी (Super Admin)',
            ':p' => '8533955333',
            ':m' => $mpinHash
        ]);
        $results['seed_super_user'] = "INSERTED";
    } else {
        // Ensure role is superadmin and tenant_id is set
        $stmtUp = $pdo->prepare("UPDATE elevex_users SET role = 'superadmin', tenant_id = :t, is_active = 1 WHERE id = :id");
        $stmtUp->execute([':t' => $superTenant, ':id' => $existingUser['id']]);
        $results['seed_super_user'] = "EXISTS_AND_PROMOTED_TO_SUPERADMIN";
    }
} catch (Exception $e) {
    $results['seed_error'] = $e->getMessage();
}

echo json_encode([
    "success" => true,
    "status" => "SCHEMA_V4_INITIALIZED",
    "tables" => $results,
    "timestamp" => date("Y-m-d H:i:s")
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
