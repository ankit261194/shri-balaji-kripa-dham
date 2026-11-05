<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE ZERO-TOLERANCE 100% DATABASE PURGE ENGINE
// Hostinger NVMe SSD Cluster: https://shribalajikripadham.online
// Sole SuperAdmin: 910010025123343 / 910010025123343 (6-Digit alias: 910010)
// Authentic Company: श्री बालाजी कृपा धाम लिफ्ट्स एंड इंजीनियर्स (TENANT_DEFAULT_ANKIT)
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
    // 1. TRUNCATE ALL DUMMY LIFTS & TECHNICAL ASSETS
    $pdo->exec("TRUNCATE TABLE elevex_lifts_amc");
    $results[] = "Purged all dummy lifts from elevex_lifts_amc (Count: 0)";

    // 2. TRUNCATE ALL DUMMY AMC CONTRACTS & SCHEDULES
    $pdo->exec("TRUNCATE TABLE elevex_amc_contracts");
    $pdo->exec("TRUNCATE TABLE elevex_amc_schedules");
    $results[] = "Purged all fake AMC contracts and schedules (Count: 0)";

    // 3. TRUNCATE ALL DUMMY / SIMULATION BREAKDOWN SLA TICKETS
    $pdo->exec("TRUNCATE TABLE elevex_breakdowns");
    $results[] = "Purged all fake/simulation breakdown tickets from elevex_breakdowns (Count: 0)";

    // 4. TRUNCATE ATTENDANCE LEDGER & TELEMETRY
    $pdo->exec("TRUNCATE TABLE elevex_attendance_ledger");
    try { $pdo->exec("TRUNCATE TABLE elevex_technician_telemetry"); } catch (Exception $e) {}
    $results[] = "Purged all fake attendance and telemetry logs (Count: 0)";

    // 5. TRUNCATE ALL DUMMY ERECTION PROJECTS & STAGES
    try {
        $pdo->exec("TRUNCATE TABLE elevex_erection_stages");
        $pdo->exec("TRUNCATE TABLE elevex_erection_projects");
        $results[] = "Purged all dummy erection projects and stages (Count: 0)";
    } catch (Exception $e) {}

    // 6. TRUNCATE DUMMY SPARE PARTS & QUOTATIONS
    try {
        $pdo->exec("TRUNCATE TABLE elevex_part_requisitions");
        $pdo->exec("TRUNCATE TABLE elevex_part_quotations");
        $pdo->exec("TRUNCATE TABLE elevex_spare_parts");
        $results[] = "Purged dummy spare parts, requisitions and quotations (Count: 0)";
    } catch (Exception $e) {}

    // 7. PURGE ALL FAKE COMPANIES EXCEPT THE GENUINE COMPANY
    // Keep ONLY 'TENANT_DEFAULT_ANKIT' (श्री बालाजी कृपा धाम लिफ्ट्स एंड इंजीनियर्स)
    $stmtPurgeComp = $pdo->exec("DELETE FROM elevex_companies WHERE tenant_id != 'TENANT_DEFAULT_ANKIT'");
    $results[] = "Purged " . intval($stmtPurgeComp) . " fake/unneeded companies from elevex_companies";

    // Ensure authentic company details are accurate
    $stmtEnsureComp = $pdo->prepare("SELECT id FROM elevex_companies WHERE tenant_id = 'TENANT_DEFAULT_ANKIT' LIMIT 1");
    $stmtEnsureComp->execute();
    if (!$stmtEnsureComp->fetch()) {
        $pdo->prepare("INSERT INTO elevex_companies 
            (tenant_id, company_name, brand_display_name, support_phone, office_address, primary_color_hex, is_active)
            VALUES ('TENANT_DEFAULT_ANKIT', 'श्री बालाजी कृपा धाम लिफ्ट्स एंड इंजीनियर्स', 'India Premier Elevator Automation & Diagnostic Systems', '8533955333', 'Plot 44, Okhla Industrial Area Phase-III, New Delhi', '#0284C7', 1)")
            ->execute();
        $results[] = "Ensured authentic company 'श्री बालाजी कृपा धाम लिफ्ट्स एंड इंजीनियर्स' exists";
    }


    // 8. PURGE ALL USERS EXCEPT SOLE SUPERADMIN 910010025123343
    $superUsername = '910010025123343';
    $superPassword = '910010025123343';
    $superPasswordHash = password_hash($superPassword, PASSWORD_BCRYPT);

    $stmtDeleteUsers = $pdo->prepare("DELETE FROM elevex_users WHERE phone != :p");
    $stmtDeleteUsers->execute([':p' => $superUsername]);
    $deletedUsersCount = $stmtDeleteUsers->rowCount();
    $results[] = "Purged $deletedUsersCount unauthorized users from elevex_users";

    // Ensure sole SuperAdmin user exists with TENANT_DEFAULT_ANKIT
    $stmtCheckSuper = $pdo->prepare("SELECT id FROM elevex_users WHERE phone = :p LIMIT 1");
    $stmtCheckSuper->execute([':p' => $superUsername]);
    $superUserRow = $stmtCheckSuper->fetch(PDO::FETCH_ASSOC);

    if ($superUserRow) {
        $stmtUp = $pdo->prepare("UPDATE elevex_users SET 
            tenant_id = 'TENANT_DEFAULT_ANKIT',
            role = 'superadmin',
            full_name = 'Super Admin',
            mpin_hash = :hash,
            is_active = 1,
            is_first_login = 0,
            mpin_changed_at = NOW(),
            session_token = NULL
            WHERE phone = :p");
        $stmtUp->execute([':hash' => $superPasswordHash, ':p' => $superUsername]);
        $results[] = "Updated SuperAdmin user ($superUsername) bound to TENANT_DEFAULT_ANKIT";
    } else {
        $stmtIns = $pdo->prepare("INSERT INTO elevex_users 
            (tenant_id, role, full_name, phone, mpin_hash, is_active, is_first_login, mpin_changed_at) 
            VALUES ('TENANT_DEFAULT_ANKIT', 'superadmin', 'Super Admin', :p, :hash, 1, 0, NOW())");
        $stmtIns->execute([':p' => $superUsername, ':hash' => $superPasswordHash]);
        $results[] = "Created SuperAdmin user ($superUsername) bound to TENANT_DEFAULT_ANKIT";
    }

    // 9. AUDIT VERIFICATION OF CURRENT STATE
    $companies = $pdo->query("SELECT tenant_id, company_name, support_phone FROM elevex_companies")->fetchAll(PDO::FETCH_ASSOC);
    $users = $pdo->query("SELECT id, tenant_id, role, full_name, phone, is_active FROM elevex_users")->fetchAll(PDO::FETCH_ASSOC);
    $liftsCount = $pdo->query("SELECT COUNT(*) FROM elevex_lifts_amc")->fetchColumn();
    $breakdownsCount = $pdo->query("SELECT COUNT(*) FROM elevex_breakdowns")->fetchColumn();
    $attendanceCount = $pdo->query("SELECT COUNT(*) FROM elevex_attendance_ledger")->fetchColumn();

    echo json_encode([
        "success" => true,
        "message" => "ABSOLUTE ZERO DUMMY DATA PURGE COMPLETE! 100% CLEAN PRODUCTION STATE.",
        "audit" => [
            "total_companies" => count($companies),
            "companies" => $companies,
            "total_users" => count($users),
            "users" => $users,
            "total_lifts" => intval($liftsCount),
            "total_breakdowns" => intval($breakdownsCount),
            "total_attendance_records" => intval($attendanceCount)
        ],
        "details" => $results
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "error" => $e->getMessage()
    ]);
}
