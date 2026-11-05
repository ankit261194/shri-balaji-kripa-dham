<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE ZERO-TOLERANCE DATABASE PURGE & SCHEMA MIGRATOR
// Hostinger NVMe SSD Cluster: https://shribalajikripadham.online
// Sole Authorized SuperAdmin: 910010025123343 / 910010025123343
// ==============================================================================

ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Cache-Control: no-cache, no-store, must-revalidate");

require_once __DIR__ . '/config.php';

$pdo = getDbConnection();
if (!$pdo) {
    echo json_encode(["success" => false, "error" => "Database connection unavailable"]);
    exit;
}

$results = [];

try {
    // 1. Alter elevex_amc_contracts to support exact GPS Coordinates & Specs
    $cols = $pdo->query("SHOW COLUMNS FROM elevex_amc_contracts")->fetchAll(PDO::FETCH_COLUMN);
    
    if (!in_array('latitude', $cols)) {
        $pdo->exec("ALTER TABLE elevex_amc_contracts ADD COLUMN latitude DECIMAL(10,7) NULL AFTER building_address");
        $results[] = "Added latitude to elevex_amc_contracts";
    }
    if (!in_array('longitude', $cols)) {
        $pdo->exec("ALTER TABLE elevex_amc_contracts ADD COLUMN longitude DECIMAL(10,7) NULL AFTER latitude");
        $results[] = "Added longitude to elevex_amc_contracts";
    }
    if (!in_array('elevator_type', $cols)) {
        $pdo->exec("ALTER TABLE elevex_amc_contracts ADD COLUMN elevator_type VARCHAR(64) NOT NULL DEFAULT 'PASSENGER_MRL' AFTER lift_count");
        $results[] = "Added elevator_type to elevex_amc_contracts";
    }
    if (!in_array('floors_count', $cols)) {
        $pdo->exec("ALTER TABLE elevex_amc_contracts ADD COLUMN floors_count INT NOT NULL DEFAULT 4 AFTER elevator_type");
        $results[] = "Added floors_count to elevex_amc_contracts";
    }

    // 2. PURGE ALL FAKE/DUMMY/TEST AMC CONTRACTS & SCHEDULES
    $pdo->exec("DELETE FROM elevex_amc_schedules");
    $pdo->exec("DELETE FROM elevex_amc_contracts");
    $results[] = "Purged all fake AMC contracts and schedules (100% Clean Slate)";

    // 3. PURGE ALL FAKE/STALE BREAKDOWN SLA TICKETS
    $stmt3 = $pdo->exec("DELETE FROM elevex_breakdowns WHERE issue_description LIKE '%SOS%' 
                         OR issue_description LIKE '%Ghost%' 
                         OR issue_description LIKE '%सायरन%' 
                         OR issue_description LIKE '%Otis SKY%' 
                         OR issue_description LIKE '%Test%'
                         OR status IN ('pending', 'assigned')");
    $results[] = "Purged fake/stale breakdown tickets ($stmt3 deleted)";

    // 4. PURGE FAKE ERECTION PROJECTS
    try {
        $pdo->exec("DELETE FROM elevex_erection_stages");
        $pdo->exec("DELETE FROM elevex_erection_projects WHERE customer_name LIKE '%Singhania%' OR customer_phone = '9810123456'");
        $results[] = "Purged fake erection projects and stages";
    } catch (Exception $e) {
        $results[] = "Erection purge skipped or table empty: " . $e->getMessage();
    }

    // 5. PURGE FAKE TRAVEL CLAIMS
    try {
        $pdo->exec("DELETE FROM elevex_technician_travel_claims");
        $results[] = "Purged fake travel claims";
    } catch (Exception $e) {
        $results[] = "Travel claims purge skipped: " . $e->getMessage();
    }

    // 6. PURGE ALL FAKE/TEST COMPANIES (Keep ONLY real ones)
    $stmtPurge = $pdo->exec("DELETE FROM elevex_companies WHERE tenant_id LIKE 'tenant_surakshaelevator_%' 
        OR tenant_id LIKE 'tenant_apexotisvertical_%' 
        OR tenant_id LIKE 'tenant_testverification_%'
        OR tenant_id LIKE 'tenant_kripaliftspvtltd_%'
        OR tenant_id LIKE 'tenant_apexhightechelev_%'
        OR tenant_id LIKE 'tenant_apexelevatorsncr_%'
        OR tenant_id LIKE 'tenant_kripaelevatorsno_%'
        OR tenant_id LIKE 'tenant_horizon_%'");
    $results[] = 'Purged ' . intval($stmtPurge) . ' fake/test companies from elevex_companies';

    // 7. PURGE ALL OTHER USERS FROM elevex_users (ONLY 910010025123343 ALLOWED!)
    $superUsername = '910010025123343';
    $superPassword = '910010025123343';
    $superPasswordHash = password_hash($superPassword, PASSWORD_BCRYPT);

    $stmtDeleteUsers = $pdo->prepare("DELETE FROM elevex_users WHERE phone != :p");
    $stmtDeleteUsers->execute([':p' => $superUsername]);
    $deletedUsersCount = $stmtDeleteUsers->rowCount();
    $results[] = "Purged $deletedUsersCount unauthorized/other registered users from elevex_users";

    // 8. ENSURE SOLE SUPERADMIN 910010025123343 EXISTS WITH EXACT BCRYPT PASSWORD
    $stmtCheckSuper = $pdo->prepare("SELECT id FROM elevex_users WHERE phone = :p LIMIT 1");
    $stmtCheckSuper->execute([':p' => $superUsername]);
    $superUserRow = $stmtCheckSuper->fetch(PDO::FETCH_ASSOC);

    if ($superUserRow) {
        $stmtUp = $pdo->prepare("UPDATE elevex_users SET 
            tenant_id = 'tenant_master_elevex',
            role = 'superadmin',
            full_name = 'Super Admin',
            mpin_hash = :hash,
            is_active = 1,
            is_first_login = 0,
            mpin_changed_at = NOW(),
            session_token = NULL
            WHERE phone = :p");
        $stmtUp->execute([':hash' => $superPasswordHash, ':p' => $superUsername]);
        $results[] = "Updated SuperAdmin user ($superUsername) with fresh bcrypt password hash and role 'superadmin'";
    } else {
        $stmtIns = $pdo->prepare("INSERT INTO elevex_users 
            (tenant_id, role, full_name, phone, mpin_hash, is_active, is_first_login, mpin_changed_at) 
            VALUES ('tenant_master_elevex', 'superadmin', 'Super Admin', :p, :hash, 1, 0, NOW())");
        $stmtIns->execute([':p' => $superUsername, ':hash' => $superPasswordHash]);
        $results[] = "Created Sole SuperAdmin user ($superUsername) with bcrypt password hash and role 'superadmin'";
    }

    // 9. PURGE ATTENDANCE LEDGER OF NON-EXISTENT USERS
    try {
        $pdo->exec("DELETE FROM elevex_attendance_ledger WHERE phone != '910010025123343'");
        $results[] = "Cleaned attendance ledger";
    } catch (Exception $e) {}

    // 10. VERIFY SOLE USER STATUS IN DATABASE
    $allUsers = $pdo->query("SELECT id, tenant_id, role, full_name, phone, is_active FROM elevex_users")->fetchAll(PDO::FETCH_ASSOC);
    $pwdCheck = false;
    if (!empty($allUsers)) {
        $checkStmt = $pdo->prepare("SELECT mpin_hash FROM elevex_users WHERE phone = :p LIMIT 1");
        $checkStmt->execute([':p' => $superUsername]);
        $hashInDb = $checkStmt->fetchColumn();
        $pwdCheck = password_verify($superPassword, $hashInDb);
    }

    echo json_encode([
        "success" => true,
        "message" => "Database successfully purged! Exactly ONE user registered in entire app: 910010025123343",
        "total_registered_users" => count($allUsers),
        "superadmin_password_verified" => $pwdCheck,
        "registered_users" => $allUsers,
        "details" => $results
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "error" => $e->getMessage()
    ]);
}
