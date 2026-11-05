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

        sync_version INT NOT NULL DEFAULT 1,

        last_modified_by INT NULL,

        last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

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

        is_charging TINYINT(1) DEFAULT 0,

        device_power_state ENUM('ONLINE', 'SHUTTING_DOWN_USER', 'SHUTTING_DOWN_LOW_BATTERY', 'OFFLINE_TIMEOUT') DEFAULT 'ONLINE',

        shutdown_at DATETIME NULL,

        last_heartbeat DATETIME NOT NULL,

        INDEX idx_tenant_telemetry (tenant_id),

        INDEX idx_power_state (device_power_state)

    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",



    // 7. Dynamic OpenStreetMap Reverse Geocoding Cache

    "elevex_geocode_cache" => "CREATE TABLE IF NOT EXISTS elevex_geocode_cache (

        id INT AUTO_INCREMENT PRIMARY KEY,

        lat_round DECIMAL(8, 4) NOT NULL,

        lng_round DECIMAL(8, 4) NOT NULL,

        formatted_landmark VARCHAR(255) NOT NULL,

        road_name VARCHAR(128) NULL,

        suburb_name VARCHAR(128) NULL,

        city_name VARCHAR(128) NULL,

        state_name VARCHAR(128) NULL,

        source VARCHAR(32) DEFAULT 'nominatim',

        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

        UNIQUE KEY uq_lat_lng_grid (lat_round, lng_round)

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



// Standardize character set and collation across all tables to prevent collation mismatch errors

$tableNames = ['elevex_companies', 'elevex_users', 'elevex_lifts_amc', 'elevex_breakdowns', 'elevex_attendance_ledger', 'elevex_technician_telemetry', 'elevex_geocode_cache'];

foreach ($tableNames as $tName) {

    try {

        $pdo->exec("ALTER TABLE $tName CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

    } catch (Exception $e) {

        $results[$tName . '_collation_error'] = $e->getMessage();

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



    if (in_array('mobile', $existingCols)) {

        try {

            $pdo->exec("ALTER TABLE elevex_users MODIFY COLUMN mobile VARCHAR(20) NULL DEFAULT NULL");

            $idxStmt = $pdo->query("SHOW INDEX FROM elevex_users");

            while ($idx = $idxStmt->fetch()) {

                if (($idx['Column_name'] === 'mobile' || $idx['Key_name'] === 'mobile') && $idx['Key_name'] !== 'PRIMARY') {

                    $pdo->exec("ALTER TABLE elevex_users DROP INDEX " . $idx['Key_name']);

                }

            }

        } catch (Exception $e) {}

    }

    $results['elevex_users_migration'] = "COLUMNS_VERIFIED_AND_UPDATED";

} catch (Exception $e) {

    $results['elevex_users_migration_error'] = $e->getMessage();

}



// Ensure columns exist on elevex_technician_telemetry

try {

    $colsStmt = $pdo->query("SHOW COLUMNS FROM elevex_technician_telemetry");

    $existingCols = $colsStmt->fetchAll(PDO::FETCH_COLUMN);

    if (!in_array('device_power_state', $existingCols)) {

        $pdo->exec("ALTER TABLE elevex_technician_telemetry ADD COLUMN device_power_state ENUM('ONLINE', 'SHUTTING_DOWN_USER', 'SHUTTING_DOWN_LOW_BATTERY', 'OFFLINE_TIMEOUT') DEFAULT 'ONLINE'");

    }

    if (!in_array('shutdown_at', $existingCols)) {

        $pdo->exec("ALTER TABLE elevex_technician_telemetry ADD COLUMN shutdown_at DATETIME NULL");

    }

    if (!in_array('is_charging', $existingCols)) {

        $pdo->exec("ALTER TABLE elevex_technician_telemetry ADD COLUMN is_charging TINYINT(1) DEFAULT 0");

    }

    $results['elevex_telemetry_migration'] = "COLUMNS_VERIFIED_AND_UPDATED";

} catch (Exception $e) {

    $results['elevex_telemetry_migration_error'] = $e->getMessage();

}



// -----------------------------------------------------------------------------

// ENTERPRISE SCHEMA UPGRADE: 12-POINT CORPORATE PROFORMA & EXTENDED LIFT INVENTORY

// -----------------------------------------------------------------------------



// 1. Upgrade elevex_companies with Corporate Dossier (GST, CIN, Bank UPI, SEO Domain)

try {

    $compColsStmt = $pdo->query("SHOW COLUMNS FROM elevex_companies");

    $existingCompCols = $compColsStmt->fetchAll(PDO::FETCH_COLUMN);

    $companyDossier = [

        "gst_number" => "VARCHAR(15) NULL",

        "cin_number" => "VARCHAR(21) NULL",

        "msme_number" => "VARCHAR(30) NULL",

        "registered_address" => "TEXT NULL",

        "city" => "VARCHAR(100) NULL",

        "state" => "VARCHAR(100) NULL",

        "pincode" => "VARCHAR(10) NULL",

        "official_email" => "VARCHAR(150) NULL",

        "tollfree_phone" => "VARCHAR(20) NULL",

        "website_domain" => "VARCHAR(200) NULL",

        "seo_keywords" => "TEXT NULL",

        "google_maps_link" => "VARCHAR(300) NULL",

        "bank_name" => "VARCHAR(100) NULL",

        "bank_account_no" => "VARCHAR(50) NULL",

        "bank_ifsc" => "VARCHAR(20) NULL",

        "bank_upi_id" => "VARCHAR(100) NULL",

        "services_catalog" => "TEXT NULL"

    ];

    foreach ($companyDossier as $col => $def) {

        if (!in_array($col, $existingCompCols)) {

            $pdo->exec("ALTER TABLE elevex_companies ADD COLUMN $col $def");

        }

    }

    $results['elevex_companies_dossier'] = "CORPORATE_PROFORMA_COLUMNS_ADDED";

} catch (Exception $e) {

    $results['elevex_companies_dossier_error'] = $e->getMessage();

}



// 2. Upgrade elevex_lifts_amc with Full Inventory Attributes (AMC vs Non-AMC, Tech Specs, Green Audit)

try {

    $liftColsStmt = $pdo->query("SHOW COLUMNS FROM elevex_lifts_amc");

    $existingLiftCols = $liftColsStmt->fetchAll(PDO::FETCH_COLUMN);

    $liftAttributes = [

        "is_amc" => "TINYINT(1) DEFAULT 1",

        "contract_type" => "VARCHAR(30) DEFAULT 'COMPREHENSIVE'",

        "amc_start_date" => "DATE NULL",

        "amc_end_date" => "DATE NULL",

        "per_visit_charge" => "DECIMAL(10,2) DEFAULT 0.00",

        "controller_model" => "VARCHAR(100) NULL",

        "machine_type" => "VARCHAR(50) NULL",

        "door_type" => "VARCHAR(50) NULL",

        "stops_count" => "VARCHAR(20) NULL",

        "capacity_kg" => "VARCHAR(30) NULL",

        "last_serviced_date" => "DATETIME NULL",

        "last_serviced_by_technician_id" => "INT NULL",

        "last_serviced_by_technician_name" => "VARCHAR(100) NULL",

        "next_service_due_date" => "DATE NULL"

    ];

    foreach ($liftAttributes as $col => $def) {

        if (!in_array($col, $existingLiftCols)) {

            $pdo->exec("ALTER TABLE elevex_lifts_amc ADD COLUMN $col $def");

        }

    }

    $results['elevex_lifts_inventory'] = "LIFT_INVENTORY_ATTRIBUTES_ADDED";

} catch (Exception $e) {

    $results['elevex_lifts_inventory_error'] = $e->getMessage();

}



// 3. Upgrade elevex_users with Skills, Aadhar, Tool Kit, and Creator

try {

    $userColsStmt = $pdo->query("SHOW COLUMNS FROM elevex_users");

    $existingUserCols = $userColsStmt->fetchAll(PDO::FETCH_COLUMN);

    $userAttributes = [

        "device_id" => "VARCHAR(100) NULL",

        "skill_grade" => "VARCHAR(50) NULL",

        "aadhar_no" => "VARCHAR(20) NULL",

        "safety_kit_status" => "VARCHAR(50) NULL",

        "created_by_technician_id" => "INT NULL",

        "created_by_supervisor_id" => "INT NULL"

    ];

    foreach ($userAttributes as $col => $def) {

        if (!in_array($col, $existingUserCols)) {

            $pdo->exec("ALTER TABLE elevex_users ADD COLUMN $col $def");

        }

    }

    $results['elevex_users_attributes'] = "USER_SKILLS_AND_ID_ADDED";

} catch (Exception $e) {

    $results['elevex_users_attributes_error'] = $e->getMessage();

}



// 3.5 Create Platform Config Table

try {

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_config (

        setting_key VARCHAR(100) PRIMARY KEY,

        setting_value TEXT NULL,

        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    $results['elevex_config'] = "TABLE_READY";

} catch (Exception $e) {

    $results['elevex_config_error'] = $e->getMessage();

}



// 4. Create Company-Branded OTP Table

try {

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_sms_logs (

        id INT AUTO_INCREMENT PRIMARY KEY,

        tenant_id VARCHAR(64) NOT NULL,

        brand_name VARCHAR(128) NOT NULL,

        phone VARCHAR(20) NOT NULL,

        message_type VARCHAR(64) NOT NULL,

        sms_text TEXT NOT NULL,

        status VARCHAR(32) DEFAULT 'SENT',

        gateway_response TEXT NULL,

        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

        INDEX idx_tenant_phone (tenant_id, phone),

        INDEX idx_created (created_at)

    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    $results['elevex_sms_logs'] = "TABLE_READY";

} catch (Exception $e) {

    $results['elevex_sms_logs_error'] = $e->getMessage();

}



try {

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_company_otps (

        id INT AUTO_INCREMENT PRIMARY KEY,

        tenant_id VARCHAR(64) NOT NULL,

        phone VARCHAR(20) NOT NULL,

        otp_code VARCHAR(6) NOT NULL,

        purpose VARCHAR(50) NOT NULL,

        reference_id VARCHAR(64) NULL,

        is_verified TINYINT(1) DEFAULT 0,

        expires_at DATETIME NOT NULL,

        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

        INDEX idx_tenant_phone (tenant_id, phone),

        INDEX idx_verify (phone, otp_code, is_verified)

    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    $results['elevex_company_otps'] = "TABLE_READY";

} catch (Exception $e) {

    $results['elevex_company_otps_error'] = $e->getMessage();

}



// 4.5 Create Enterprise Asynchronous Outbox Queue

try {

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_outbox_queue (

        id BIGINT AUTO_INCREMENT PRIMARY KEY,

        tenant_id VARCHAR(64) NOT NULL DEFAULT 'tenant_master_elevex',

        payload_type VARCHAR(32) NOT NULL,

        recipient VARCHAR(128) NOT NULL,

        payload_json LONGTEXT NOT NULL,

        status VARCHAR(32) NOT NULL DEFAULT 'PENDING',

        retry_count INT NOT NULL DEFAULT 0,

        max_retries INT NOT NULL DEFAULT 3,

        last_error TEXT NULL,

        processed_at DATETIME NULL,

        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

        INDEX idx_status_created (status, created_at),

        INDEX idx_tenant (tenant_id)

    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

    $results['elevex_outbox_queue'] = "TABLE_READY";

} catch (Exception $e) {

    $results['elevex_outbox_queue_error'] = $e->getMessage();

}



// 5. PURGE ALL OLD TEST SEED ACCOUNTS (Zero Dummy Data Enforcement)

try {

    $purgeStmt = $pdo->prepare("DELETE FROM elevex_users WHERE phone IN ('9811001122', '9811004455') OR (role = 'technician' AND full_name LIKE '%Demo%')");

    $purgeStmt->execute();

    $results['test_seed_purge'] = "DEMO_USERS_PURGED";

} catch (Exception $e) {

    $results['test_seed_purge_error'] = $e->getMessage();

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





// Ensure Optimistic Locking & Conflict Resolution Columns in elevex_breakdowns

try {

    $colsStmt = $pdo->query("SHOW COLUMNS FROM elevex_breakdowns");

    $existingBreakdownCols = $colsStmt->fetchAll(PDO::FETCH_COLUMN);

    if (!in_array('sync_version', $existingBreakdownCols)) {

        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN sync_version INT NOT NULL DEFAULT 1");

    }

    if (!in_array('last_modified_by', $existingBreakdownCols)) {

        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN last_modified_by INT NULL");

    }

    if (!in_array('last_modified_at', $existingBreakdownCols)) {

        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

    }

    $results['elevex_breakdowns_optimistic_locking'] = "COLUMNS_VERIFIED_AND_UPDATED";

} catch (Exception $e) {

    $results['elevex_breakdowns_optimistic_locking_error'] = $e->getMessage();

}




// =============================================================================
// PHASE 1: CENTRAL AMC MASTER & REAL-TIME BREAKDOWN SLA TABLES
// =============================================================================
try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_amc_contracts (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        contract_no VARCHAR(64) UNIQUE NOT NULL,
        building_name VARCHAR(191) NOT NULL,
        building_address TEXT NOT NULL,
        city VARCHAR(64) NOT NULL DEFAULT 'Delhi NCR',
        customer_name VARCHAR(128) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        customer_email VARCHAR(128) NULL,
        lift_count INT NOT NULL DEFAULT 1,
        amc_type ENUM('Comprehensive', 'Non-Comprehensive', 'Semi-Comprehensive') NOT NULL DEFAULT 'Non-Comprehensive',
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        annual_amount DECIMAL(12,2) NOT NULL DEFAULT 15000.00,
        payment_status ENUM('paid', 'partial', 'pending', 'overdue') NOT NULL DEFAULT 'pending',
        paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        service_frequency ENUM('monthly', 'bi_monthly', 'quarterly') NOT NULL DEFAULT 'monthly',
        services_completed INT NOT NULL DEFAULT 0,
        total_services_scheduled INT NOT NULL DEFAULT 12,
        status ENUM('active', 'expiring_soon', 'expired', 'renewed', 'terminated') NOT NULL DEFAULT 'active',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_tenant_status (tenant_id, status),
        INDEX idx_dates (start_date, end_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_amc_schedules (
        id INT AUTO_INCREMENT PRIMARY KEY,
        contract_id INT NOT NULL,
        tenant_id VARCHAR(64) NOT NULL,
        service_month VARCHAR(20) NOT NULL,
        scheduled_date DATE NOT NULL,
        assigned_technician_id INT NULL,
        assigned_technician_name VARCHAR(128) NULL,
        completed_date DATETIME NULL,
        service_status ENUM('scheduled', 'in_progress', 'completed', 'missed', 'rescheduled') DEFAULT 'scheduled',
        checklist_score INT DEFAULT 0,
        technician_notes TEXT NULL,
        customer_signature_url TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_contract (contract_id),
        INDEX idx_tenant_sched (tenant_id, scheduled_date),
        INDEX idx_status (service_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    // Upgrade elevex_breakdowns with real-time SLA and severity columns
    $colsStmt = $pdo->query("SHOW COLUMNS FROM elevex_breakdowns");
    $bCols = $colsStmt->fetchAll(PDO::FETCH_COLUMN);

    if (!in_array('severity_level', $bCols)) {
        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN severity_level ENUM('P1_TRAP', 'P2_STOPPAGE', 'P3_MINOR') NOT NULL DEFAULT 'P2_STOPPAGE'");
    }
    if (!in_array('sla_minutes', $bCols)) {
        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN sla_minutes INT NOT NULL DEFAULT 120");
    }
    if (!in_array('sla_breached', $bCols)) {
        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN sla_breached TINYINT(1) DEFAULT 0");
    }
    if (!in_array('root_cause_category', $bCols)) {
        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN root_cause_category VARCHAR(64) DEFAULT 'PENDING_DIAGNOSIS'");
    }
    if (!in_array('dispatched_at', $bCols)) {
        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN dispatched_at DATETIME NULL");
    }
    if (!in_array('arrived_at', $bCols)) {
        $pdo->exec("ALTER TABLE elevex_breakdowns ADD COLUMN arrived_at DATETIME NULL");
    }

    
    // Ensure all required columns in elevex_amc_schedules
    $schedColsStmt = $pdo->query("SHOW COLUMNS FROM elevex_amc_schedules");
    $existingSchedCols = $schedColsStmt->fetchAll(PDO::FETCH_COLUMN);

    $neededSchedCols = [
        "contract_id" => "INT NOT NULL",
        "tenant_id" => "VARCHAR(64) NOT NULL",
        "service_month" => "VARCHAR(20) NOT NULL",
        "scheduled_date" => "DATE NOT NULL",
        "assigned_technician_id" => "INT NULL",
        "assigned_technician_name" => "VARCHAR(128) NULL",
        "completed_date" => "DATETIME NULL",
        "service_status" => "ENUM('scheduled', 'in_progress', 'completed', 'missed', 'rescheduled') DEFAULT 'scheduled'",
        "checklist_score" => "INT DEFAULT 0",
        "technician_notes" => "TEXT NULL",
        "customer_signature_url" => "TEXT NULL"
    ];

    foreach ($neededSchedCols as $col => $def) {
        if (!in_array($col, $existingSchedCols)) {
            $pdo->exec("ALTER TABLE elevex_amc_schedules ADD COLUMN $col $def");
        }
    }

    // Ensure all required columns in elevex_amc_contracts
    $contrColsStmt = $pdo->query("SHOW COLUMNS FROM elevex_amc_contracts");
    $existingContrCols = $contrColsStmt->fetchAll(PDO::FETCH_COLUMN);

    $neededContrCols = [
        "tenant_id" => "VARCHAR(64) NOT NULL",
        "contract_no" => "VARCHAR(64) UNIQUE NOT NULL",
        "building_name" => "VARCHAR(191) NOT NULL",
        "building_address" => "TEXT NOT NULL",
        "city" => "VARCHAR(64) NOT NULL DEFAULT 'Delhi NCR'",
        "customer_name" => "VARCHAR(128) NOT NULL",
        "customer_phone" => "VARCHAR(20) NOT NULL",
        "customer_email" => "VARCHAR(128) NULL",
        "lift_count" => "INT NOT NULL DEFAULT 1",
        "amc_type" => "ENUM('Comprehensive', 'Non-Comprehensive', 'Semi-Comprehensive') NOT NULL DEFAULT 'Non-Comprehensive'",
        "start_date" => "DATE NOT NULL",
        "end_date" => "DATE NOT NULL",
        "annual_amount" => "DECIMAL(12,2) NOT NULL DEFAULT 15000.00",
        "payment_status" => "ENUM('paid', 'partial', 'pending', 'overdue') NOT NULL DEFAULT 'pending'",
        "paid_amount" => "DECIMAL(12,2) NOT NULL DEFAULT 0.00",
        "service_frequency" => "ENUM('monthly', 'bi_monthly', 'quarterly') NOT NULL DEFAULT 'monthly'",
        "services_completed" => "INT NOT NULL DEFAULT 0",
        "total_services_scheduled" => "INT NOT NULL DEFAULT 12",
        "status" => "ENUM('active', 'expiring_soon', 'expired', 'renewed', 'terminated') NOT NULL DEFAULT 'active'"
    ];

    foreach ($neededContrCols as $col => $def) {
        if (!in_array($col, $existingContrCols)) {
            $pdo->exec("ALTER TABLE elevex_amc_contracts ADD COLUMN $col $def");
        }
    }

    $results['elevex_phase1_amc_sla_tables'] = "VERIFIED_AND_CREATED_OK";
} catch (Exception $e) {
    $results['elevex_phase1_error'] = $e->getMessage();
}

// =============================================================================
// PHASE 2: SPARE PARTS CATALOG, TECHNICIAN REQUISITION & QUOTATION TABLES
// =============================================================================
try {
    // 1. elevex_spare_parts
    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_spare_parts (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        part_number VARCHAR(64) UNIQUE NOT NULL,
        part_name VARCHAR(191) NOT NULL,
        oem_brand VARCHAR(64) NOT NULL DEFAULT 'UNIVERSAL',
        category ENUM('Safety', 'Door_Operator', 'Traction_Drive', 'Electrical_PCB', 'Sensor_Switch', 'Mechanical_Hardware') NOT NULL DEFAULT 'Mechanical_Hardware',
        description TEXT NULL,
        cost_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
        selling_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
        stock_quantity INT NOT NULL DEFAULT 0,
        min_reorder_level INT NOT NULL DEFAULT 2,
        unit VARCHAR(32) NOT NULL DEFAULT 'Piece',
        is_active TINYINT(1) NOT NULL DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_tenant_brand (tenant_id, oem_brand)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    // 2. elevex_part_requisitions
    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_part_requisitions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        requisition_no VARCHAR(64) UNIQUE NOT NULL,
        ticket_id INT NULL,
        lift_id INT NULL,
        technician_id INT NOT NULL,
        technician_name VARCHAR(128) NOT NULL,
        technician_phone VARCHAR(20) NOT NULL,
        part_id INT NOT NULL,
        part_name VARCHAR(191) NOT NULL,
        requested_qty INT NOT NULL DEFAULT 1,
        urgency_level ENUM('CRITICAL_STOPPAGE', 'HIGH', 'ROUTINE') NOT NULL DEFAULT 'ROUTINE',
        site_fault_photo_url TEXT NULL,
        fault_description TEXT NOT NULL,
        status ENUM('pending_supervisor', 'approved', 'rejected', 'quoted_to_client', 'dispatched', 'installed') NOT NULL DEFAULT 'pending_supervisor',
        supervisor_id INT NULL,
        supervisor_notes TEXT NULL,
        approved_at DATETIME NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_tenant_status (tenant_id, status),
        INDEX idx_ticket (ticket_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    // 3. elevex_part_quotations
    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_part_quotations (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        quotation_no VARCHAR(64) UNIQUE NOT NULL,
        requisition_id INT NULL,
        ticket_id INT NULL,
        building_name VARCHAR(191) NOT NULL,
        customer_name VARCHAR(128) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        items_json LONGTEXT NOT NULL,
        subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        gst_rate_pct DECIMAL(5,2) NOT NULL DEFAULT 18.00,
        gst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        grand_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        markup_pct DECIMAL(5,2) NOT NULL DEFAULT 20.00,
        approval_status ENUM('draft', 'sent_to_client', 'client_approved', 'client_rejected', 'invoiced') NOT NULL DEFAULT 'draft',
        client_approved_at DATETIME NULL,
        client_ip VARCHAR(45) NULL,
        client_notes TEXT NULL,
        whatsapp_message TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_tenant_quo (tenant_id, quotation_no),
        INDEX idx_status (approval_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $results['elevex_phase2_spare_parts_tables'] = "VERIFIED_AND_CREATED_OK";

    // =========================================================================
    // PHASE 3: 10-STAGE NEW LIFT ERECTION PROJECT TRACKER & STATUTORY AUDITS
    // =========================================================================
    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_erection_projects (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        project_code VARCHAR(64) UNIQUE NOT NULL,
        customer_name VARCHAR(128) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        customer_email VARCHAR(128) NULL,
        building_name VARCHAR(191) NOT NULL,
        site_address TEXT NOT NULL,
        elevator_type ENUM('PASSENGER_MR', 'PASSENGER_MRL', 'HOSPITAL_BED', 'FREIGHT_CARGO', 'HYDRAULIC', 'HOME_VILLA') NOT NULL DEFAULT 'PASSENGER_MRL',
        capacity_kg INT NOT NULL DEFAULT 544,
        speed_mps DECIMAL(4,2) NOT NULL DEFAULT 1.00,
        stops_count INT NOT NULL DEFAULT 6,
        current_stage INT NOT NULL DEFAULT 1,
        progress_pct INT NOT NULL DEFAULT 0,
        assigned_supervisor_id INT NULL,
        assigned_technician_id INT NULL,
        target_handover_date DATE NULL,
        actual_handover_date DATE NULL,
        status ENUM('planning', 'civil_readiness', 'under_erection', 'testing_commissioning', 'statutory_inspection', 'handed_over', 'on_hold') NOT NULL DEFAULT 'under_erection',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_tenant_proj (tenant_id, project_code),
        INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_erection_stages (
        id INT AUTO_INCREMENT PRIMARY KEY,
        project_id INT NOT NULL,
        stage_number INT NOT NULL,
        stage_name VARCHAR(191) NOT NULL,
        stage_name_hi VARCHAR(191) NOT NULL,
        description TEXT NULL,
        status ENUM('pending', 'in_progress', 'completed', 'verified_by_supervisor') NOT NULL DEFAULT 'pending',
        site_photo_url VARCHAR(500) NULL,
        technician_id INT NULL,
        technician_name VARCHAR(128) NULL,
        technician_notes TEXT NULL,
        supervisor_id INT NULL,
        supervisor_remarks TEXT NULL,
        completed_at DATETIME NULL,
        verified_at DATETIME NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY uq_proj_stage (project_id, stage_number),
        INDEX idx_stage_status (project_id, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_statutory_audits (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        audit_no VARCHAR(64) UNIQUE NOT NULL,
        lift_id INT NULL,
        project_id INT NULL,
        building_name VARCHAR(191) NOT NULL,
        site_address TEXT NOT NULL,
        audit_type ENUM('FORM_D_INITIAL_COMMISSIONING', 'ANNUAL_SAFETY_INSPECTION', 'SPECIAL_INVESTIGATION') NOT NULL DEFAULT 'ANNUAL_SAFETY_INSPECTION',
        inspector_name VARCHAR(128) NOT NULL,
        inspector_license_no VARCHAR(64) NOT NULL,
        inspection_date DATE NOT NULL,
        valid_until DATE NOT NULL,
        checklist_json LONGTEXT NOT NULL,
        total_checkpoints INT NOT NULL DEFAULT 10,
        passed_checkpoints INT NOT NULL DEFAULT 10,
        failed_checkpoints INT NOT NULL DEFAULT 0,
        safety_score_pct INT NOT NULL DEFAULT 100,
        certificate_status ENUM('PASSED_CERTIFIED', 'FAILED_CORRECTION_REQUIRED', 'SUSPENDED') NOT NULL DEFAULT 'PASSED_CERTIFIED',
        certificate_hash VARCHAR(64) NOT NULL,
        inspector_signature_url VARCHAR(500) NULL,
        pdf_certificate_url VARCHAR(500) NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_tenant_audit (tenant_id, audit_no),
        INDEX idx_audit_status (certificate_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $results['elevex_phase3_erection_and_audit_tables'] = "VERIFIED_AND_CREATED_OK";

    // =========================================================================
    // PHASE 4: ANTI-CHEATING 50M GPS GEOFENCING & DAILY BIKE TRAVEL (TA-DA)
    // =========================================================================
    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_technician_travel_logs (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        technician_id INT NOT NULL,
        duty_date DATE NOT NULL,
        start_lat DECIMAL(10, 7) NULL,
        start_lng DECIMAL(10, 7) NULL,
        end_lat DECIMAL(10, 7) NULL,
        end_lng DECIMAL(10, 7) NULL,
        waypoints_json LONGTEXT NULL,
        total_km DECIMAL(8, 2) NOT NULL DEFAULT 0.00,
        ta_rate_per_km DECIMAL(6, 2) NOT NULL DEFAULT 3.50,
        ta_claim_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
        geofence_violations_count INT NOT NULL DEFAULT 0,
        status ENUM('active_tracking', 'submitted_for_approval', 'approved', 'reimbursed', 'rejected') NOT NULL DEFAULT 'active_tracking',
        approved_by_supervisor_id INT NULL,
        supervisor_remarks TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uq_tech_duty_date (technician_id, duty_date),
        INDEX idx_tenant_duty (tenant_id, duty_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $results['elevex_phase4_geofence_and_travel_tables'] = "VERIFIED_AND_CREATED_OK";
} catch (Exception $e) {
    $results['elevex_phase2_error'] = $e->getMessage();
}

// Production Seeder Permanently Disabled - Zero Dummy Data Policy



echo json_encode([

    "success" => true,

    "status" => "SCHEMA_V4_INITIALIZED",

    "tables" => $results,

    "timestamp" => date("Y-m-d H:i:s")

], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);



