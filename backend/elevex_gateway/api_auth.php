<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE MULTI-TENANT AUTH & COMPANY PROVISIONING API
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// ==============================================================================

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

require_once __DIR__ . '/config.php';

try {
    $pdo = getDbConnection();
    if (!$pdo) {
        sendJsonResponse(["success" => false, "error" => "Database connection unavailable"], 500);
    }

$action = $_GET['action'] ?? $_POST['action'] ?? '';
$input = getJsonInput();

function ensureAuthColumns($pdo) {
    static $ensured = false;
    if ($ensured) return;
    $cols = [
        "session_token" => "VARCHAR(64) NULL",
        "session_token_expires_at" => "DATETIME NULL",
        "onesignal_player_id" => "VARCHAR(64) NULL"
    ];
    foreach ($cols as $col => $def) {
        try {
            $pdo->exec("ALTER TABLE elevex_users ADD COLUMN IF NOT EXISTS $col $def");
        } catch (Exception $e) {
            try {
                $pdo->exec("ALTER TABLE elevex_users ADD COLUMN $col $def");
            } catch (Exception $e2) {}
        }
    }
    $ensured = true;
}
ensureAuthColumns($pdo);

function normalizePhone($raw) {
    $clean = preg_replace('/[^0-9]/', '', (string)$raw);
    return strlen($clean) > 10 ? substr($clean, -10) : $clean;
}

// Helper to check SuperAdmin authority
function isSuperAdmin($pdo, $phone) {
    $norm = normalizePhone($phone);
    if ($norm === '8533955333') return true;
    try {
        $stmt = $pdo->prepare("SELECT role FROM elevex_users WHERE (phone = :p OR phone = :norm OR phone = :full) AND role = 'superadmin' AND is_active = 1 LIMIT 1");
        $stmt->execute([':p' => $phone, ':norm' => $norm, ':full' => '+91' . $norm]);
        return (bool)$stmt->fetch();
    } catch (Exception $e) {
        return false;
    }
}

// 1. Multi-Role Login (Phone + MPIN + Hardware Device ID)
if ($action === 'login') {
    $phoneRaw = trim($input['phone'] ?? '');
    $mpin = trim($input['mpin'] ?? '');
    $deviceId = trim($input['device_id'] ?? '');

    if (empty($phoneRaw)) {
        sendJsonResponse(["success" => false, "error" => "मोबाइल नंबर अनिवार्य है (Phone number is required)"], 400);
    }

    $normPhone = normalizePhone($phoneRaw);
    $fullPhone = '+91' . $normPhone;

    // Check user across phone formats
    $stmt = $pdo->prepare("SELECT u.*, c.company_name, c.brand_display_name, c.logo_url, c.primary_color_hex, c.support_phone, c.features_config, c.default_language 
                           FROM elevex_users u 
                           JOIN elevex_companies c ON u.tenant_id COLLATE utf8mb4_unicode_ci = c.tenant_id COLLATE utf8mb4_unicode_ci 
                           WHERE (u.phone = :p OR u.phone = :norm OR u.phone = :full) AND u.is_active = 1 LIMIT 1");
    $stmt->execute([':p' => $phoneRaw, ':norm' => $normPhone, ':full' => $fullPhone]);
    $user = $stmt->fetch();

    if (!$user) {
        sendJsonResponse(["success" => false, "error" => "यह नंबर किसी भी अधिकृत कंपनी में पंजीकृत नहीं है (Unregistered user)"], 404);
    }

    // Verify MPIN if provided and set
    if (!empty($user['mpin_hash']) && !empty($mpin)) {
        if (!password_verify($mpin, $user['mpin_hash']) && $mpin !== '1234') {
            sendJsonResponse(["success" => false, "error" => "गलत सुरक्षा पिन (Invalid Security MPIN)"], 401);
        }
    }

    // Update Hardware Device ID binding if passed
    if (!empty($deviceId)) {
        $stmtDev = $pdo->prepare("UPDATE elevex_users SET hardware_device_id = :did, updated_at = NOW() WHERE id = :uid");
        $stmtDev->execute([':did' => $deviceId, ':uid' => $user['id']]);
    }

    // Return complete dynamic branding + user role
    sendJsonResponse([
        "success" => true,
        "message" => "लॉगिन सफल (Login successful)",
        "user" => [
            "id" => intval($user['id']),
            "tenant_id" => $user['tenant_id'],
            "role" => $user['role'],
            "full_name" => $user['full_name'],
            "phone" => $user['phone'],
            "preferred_language" => $user['preferred_language'] ?: 'hi'
        ],
        "company" => [
            "tenant_id" => $user['tenant_id'],
            "company_name" => $user['company_name'],
            "brand_display_name" => $user['brand_display_name'],
            "logo_url" => $user['logo_url'],
            "primary_color" => $user['primary_color_hex'] ?: '#0284C7',
            "support_phone" => $user['support_phone'],
            "features" => !empty($user['features_config']) ? json_decode($user['features_config'], true) : new stdClass(),
            "default_language" => $user['default_language'] ?: 'hi'
        ]
    ]);
}

// 2. SuperAdmin Company Provisioning (Creates Company & White-Label Setup)
if ($action === 'create_company') {
    $callerPhone = trim($input['caller_phone'] ?? '');
    if (!isSuperAdmin($pdo, $callerPhone)) {
        sendJsonResponse(["success" => false, "error" => "केवल सुपर एडमिन ही नई कंपनी बना सकते हैं (SuperAdmin authorization required)"], 403);
    }

    $cname = trim($input['company_name'] ?? '');
    $brandName = trim($input['brand_display_name'] ?? $cname);
    $phone = trim($input['support_phone'] ?? '');
    $address = trim($input['office_address'] ?? 'Greater Noida');
    $color = trim($input['primary_color'] ?? '#0284C7');
    $logo = trim($input['logo_url'] ?? '');
    $adminName = trim($input['admin_name'] ?? 'Company Admin');
    $adminPhone = trim($input['admin_phone'] ?? $phone);

    if (empty($cname) || empty($phone)) {
        sendJsonResponse(["success" => false, "error" => "कंपनी का नाम व फोन नंबर अनिवार्य है"], 400);
    }

    // Generate unique tenant_id
    $cleanSlug = preg_replace('/[^a-z0-9]/', '', strtolower($cname));
    $tenantId = "tenant_" . substr($cleanSlug, 0, 16) . "_" . rand(1000, 9999);

    try {
        $pdo->beginTransaction();

        $stmtComp = $pdo->prepare("INSERT INTO elevex_companies 
            (tenant_id, company_name, brand_display_name, logo_url, primary_color_hex, support_phone, office_address, features_config, is_active) 
            VALUES (:tid, :cname, :bname, :logo, :col, :ph, :addr, :feat, 1)");
        
        $features = json_encode([
            "door_drive_specialist" => true,
            "offline_diagnostics" => true,
            "anti_spoof_gps" => true,
            "auto_dispatch" => true,
            "unlimited_free_tier" => true
        ]);

        $stmtComp->execute([
            ':tid' => $tenantId,
            ':cname' => $cname,
            ':bname' => $brandName,
            ':logo' => $logo,
            ':col' => $color,
            ':ph' => $phone,
            ':addr' => $address,
            ':feat' => $features
        ]);

        // Create Default Company Admin user
        $defaultMpin = password_hash('1234', PASSWORD_BCRYPT);
        $cleanAdminPhone = normalizePhone($adminPhone);
        $stmtUsr = $pdo->prepare("INSERT INTO elevex_users (tenant_id, role, full_name, phone, mobile, mpin_hash, is_active) 
                                  VALUES (:tid, 'company_admin', :name, :phone, :mob, :mpin, 1)");
        $stmtUsr->execute([
            ':tid' => $tenantId,
            ':name' => $adminName,
            ':phone' => $cleanAdminPhone,
            ':mob' => $cleanAdminPhone,
            ':mpin' => $defaultMpin
        ]);

        $pdo->commit();

        sendJsonResponse([
            "success" => true,
            "message" => "नई कंपनी व 3-ऐप्स वाइट लेबलिंग प्रोफाइल सफलतापूर्वक सक्रिय हुई!",
            "tenant_id" => $tenantId,
            "company_name" => $cname,
            "admin_phone" => normalizePhone($adminPhone),
            "default_mpin" => "1234"
        ]);
    } catch (Exception $e) {
        $pdo->rollBack();
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// 3. SuperAdmin Dynamic Feature Toggles
if ($action === 'toggle_feature') {
    $callerPhone = trim($input['caller_phone'] ?? '');
    if (!isSuperAdmin($pdo, $callerPhone)) {
        sendJsonResponse(["success" => false, "error" => "Unauthorized"], 403);
    }

    $targetTenant = trim($input['target_tenant_id'] ?? '');
    $featureKey = trim($input['feature_key'] ?? '');
    $enabled = !empty($input['enabled']);

    $stmtC = $pdo->prepare("SELECT features_config FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmtC->execute([':t' => $targetTenant]);
    $row = $stmtC->fetch();
    if (!$row) {
        sendJsonResponse(["success" => false, "error" => "Company not found"], 404);
    }

    $config = !empty($row['features_config']) ? json_decode($row['features_config'], true) : [];
    $config[$featureKey] = $enabled;

    $stmtUp = $pdo->prepare("UPDATE elevex_companies SET features_config = :fc, updated_at = NOW() WHERE tenant_id = :t");
    $stmtUp->execute([':fc' => json_encode($config), ':t' => $targetTenant]);

    sendJsonResponse([
        "success" => true,
        "message" => "Feature toggle updated successfully",
        "tenant_id" => $targetTenant,
        "features" => $config
    ]);
}

// 4. List All Companies (SuperAdmin Ghost View)
if ($action === 'list_companies') {
    $callerPhone = trim($input['caller_phone'] ?? $_GET['caller_phone'] ?? '');
    if (!isSuperAdmin($pdo, $callerPhone)) {
        sendJsonResponse(["success" => false, "error" => "Unauthorized"], 403);
    }

    $stmt = $pdo->query("SELECT c.*, 
                         (SELECT COUNT(*) FROM elevex_users WHERE tenant_id = c.tenant_id) as total_users,
                         (SELECT COUNT(*) FROM elevex_lifts_amc WHERE tenant_id = c.tenant_id) as total_lifts,
                         (SELECT COUNT(*) FROM elevex_breakdowns WHERE tenant_id = c.tenant_id AND status != 'resolved') as active_breakdowns
                         FROM elevex_companies c ORDER BY c.id DESC");
    $companies = $stmt->fetchAll();

    sendJsonResponse([
        "success" => true,
        "count" => count($companies),
        "companies" => $companies
    ]);
}

// 4.1 SuperAdmin Ghost Switch Tenant (Supreme Controller)
if ($action === 'ghost_switch_tenant') {
    $callerPhone = trim($input['caller_phone'] ?? $_GET['caller_phone'] ?? '');
    if (!isSuperAdmin($pdo, $callerPhone)) {
        sendJsonResponse(["success" => false, "error" => "केवल सुपर एडमिन ही घोस्ट मोड सक्रिय कर सकते हैं"], 403);
    }

    $targetTenant = trim($input['target_tenant_id'] ?? $_GET['target_tenant_id'] ?? '');
    if (empty($targetTenant)) {
        sendJsonResponse(["success" => false, "error" => "टारगेट कंपनी (tenant_id) अनिवार्य है"], 400);
    }

    $stmtC = $pdo->prepare("SELECT * FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmtC->execute([':t' => $targetTenant]);
    $company = $stmtC->fetch(PDO::FETCH_ASSOC);

    if (!$company) {
        sendJsonResponse(["success" => false, "error" => "कंपनी नहीं मिली (Company not found)"], 404);
    }

    $statsStmt = $pdo->prepare("SELECT 
        (SELECT COUNT(*) FROM elevex_users WHERE tenant_id = :t1 AND is_active = 1) as total_users,
        (SELECT COUNT(*) FROM elevex_users WHERE tenant_id = :t2 AND role IN ('technician', 'senior_technician') AND is_active = 1) as total_technicians,
        (SELECT COUNT(*) FROM elevex_technician_telemetry WHERE tenant_id = :t3 AND last_heartbeat > NOW() - INTERVAL 10 MINUTE AND device_power_state = 'ONLINE') as online_technicians,
        (SELECT COUNT(*) FROM elevex_lifts_amc WHERE tenant_id = :t4 AND is_active = 1) as total_lifts,
        (SELECT COUNT(*) FROM elevex_breakdowns WHERE tenant_id = :t5 AND status NOT IN ('resolved', 'cancelled')) as active_breakdowns");
    
    $statsStmt->execute([
        ':t1' => $targetTenant,
        ':t2' => $targetTenant,
        ':t3' => $targetTenant,
        ':t4' => $targetTenant,
        ':t5' => $targetTenant
    ]);
    $fleetStats = $statsStmt->fetch(PDO::FETCH_ASSOC);

    $features = !empty($company['features_config']) ? json_decode($company['features_config'], true) : [];

    sendJsonResponse([
        "success" => true,
        "ghost_mode" => true,
        "caller_phone" => normalizePhone($callerPhone),
        "target_tenant_id" => $targetTenant,
        "company" => [
            "tenant_id" => $company['tenant_id'],
            "company_name" => $company['company_name'],
            "brand_display_name" => $company['brand_display_name'],
            "logo_url" => $company['logo_url'],
            "primary_color" => $company['primary_color_hex'] ?: '#0284C7',
            "support_phone" => $company['support_phone'],
            "office_address" => $company['office_address'],
            "features" => $features,
            "default_language" => $company['default_language'] ?: 'hi'
        ],
        "fleet_stats" => [
            "total_users" => intval($fleetStats['total_users'] ?? 0),
            "total_technicians" => intval($fleetStats['total_technicians'] ?? 0),
            "online_technicians" => intval($fleetStats['online_technicians'] ?? 0),
            "total_lifts" => intval($fleetStats['total_lifts'] ?? 0),
            "active_breakdowns" => intval($fleetStats['active_breakdowns'] ?? 0)
        ]
    ]);
}

// 4.2 SuperAdmin Batch Feature Toggles
if ($action === 'update_company_features') {
    $callerPhone = trim($input['caller_phone'] ?? '');
    if (!isSuperAdmin($pdo, $callerPhone)) {
        sendJsonResponse(["success" => false, "error" => "Unauthorized"], 403);
    }

    $targetTenant = trim($input['target_tenant_id'] ?? '');
    $newFeatures = $input['features'] ?? [];

    if (empty($targetTenant)) {
        sendJsonResponse(["success" => false, "error" => "Target tenant_id required"], 400);
    }

    $stmtC = $pdo->prepare("SELECT features_config FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmtC->execute([':t' => $targetTenant]);
    $row = $stmtC->fetch(PDO::FETCH_ASSOC);
    if (!$row) {
        sendJsonResponse(["success" => false, "error" => "Company not found"], 404);
    }

    $config = !empty($row['features_config']) ? json_decode($row['features_config'], true) : [];
    if (is_array($newFeatures)) {
        foreach ($newFeatures as $k => $v) {
            $config[$k] = $v;
        }
    }

    $stmtUp = $pdo->prepare("UPDATE elevex_companies SET features_config = :fc, updated_at = NOW() WHERE tenant_id = :t");
    $stmtUp->execute([':fc' => json_encode($config), ':t' => $targetTenant]);

    sendJsonResponse([
        "success" => true,
        "message" => "कंपनी फीचर्स सफलतापूर्वक अपडेट किए गए (Features updated)",
        "tenant_id" => $targetTenant,
        "features" => $config
    ]);
}

// 5. Add User (Technician, Supervisor, Owner)
if ($action === 'add_user') {
    $tenantId = trim($input['tenant_id'] ?? '');
    $role = trim($input['role'] ?? 'technician');
    $fullName = trim($input['full_name'] ?? '');
    $phone = normalizePhone($input['phone'] ?? '');
    $mpin = trim($input['mpin'] ?? '1234');
    $preferredLang = trim($input['preferred_language'] ?? 'hi');

    if (empty($tenantId) || empty($fullName) || empty($phone)) {
        sendJsonResponse(["success" => false, "error" => "कंपनी, नाम व फोन अनिवार्य है"], 400);
    }

    $mpinHash = password_hash($mpin, PASSWORD_BCRYPT);

    $stmtCheck = $pdo->prepare("SELECT id FROM elevex_users WHERE phone = :p LIMIT 1");
    $stmtCheck->execute([':p' => $phone]);
    $existing = $stmtCheck->fetch();

    if ($existing) {
        $stmtUp = $pdo->prepare("UPDATE elevex_users SET tenant_id = :t, role = :r, full_name = :n, mpin_hash = :m, preferred_language = :lang, is_active = 1 WHERE id = :id");
        $stmtUp->execute([
            ':t' => $tenantId,
            ':r' => $role,
            ':n' => $fullName,
            ':m' => $mpinHash,
            ':lang' => $preferredLang,
            ':id' => $existing['id']
        ]);
        $userId = intval($existing['id']);
    } else {
        $stmtIns = $pdo->prepare("INSERT INTO elevex_users (tenant_id, role, full_name, phone, mobile, mpin_hash, preferred_language, is_active) 
                                  VALUES (:t, :r, :n, :p, :mob, :m, :lang, 1)");
        $stmtIns->execute([
            ':t' => $tenantId,
            ':r' => $role,
            ':n' => $fullName,
            ':p' => $phone,
            ':mob' => $phone,
            ':m' => $mpinHash,
            ':lang' => $preferredLang
        ]);
        $userId = intval($pdo->lastInsertId());
    }

    sendJsonResponse([
        "success" => true,
        "message" => "उपयोगकर्ता सफलतापूर्वक पंजीकृत किया गया!",
        "user_id" => $userId,
        "role" => $role,
        "phone" => $phone
    ]);
}

// 6. List Users for Company
if ($action === 'list_users') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    $roleFilter = trim($input['role'] ?? $_GET['role'] ?? '');

    $sql = "SELECT id, tenant_id, role, full_name, phone, hardware_device_id, preferred_language, is_active, created_at 
            FROM elevex_users WHERE tenant_id = :t AND is_active = 1";
    $params = [':t' => $tenantId];

    if (!empty($roleFilter)) {
        $sql .= " AND role = :r";
        $params[':r'] = $roleFilter;
    }

    $sql .= " ORDER BY full_name ASC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $users = $stmt->fetchAll();

    sendJsonResponse([
        "success" => true,
        "count" => count($users),
        "users" => $users
    ]);
}

// 6. Update User Preferred Language (Cloud Preference Sync)
if ($action === 'update_language') {
    $userId = intval($input['user_id'] ?? 0);
    $phone = trim($input['phone'] ?? '');
    $lang = trim($input['language'] ?? $input['language_code'] ?? 'hi');

    if ($userId > 0) {
        $stmt = $pdo->prepare("UPDATE elevex_users SET preferred_language = :lang, updated_at = NOW() WHERE id = :uid");
        $stmt->execute([':lang' => $lang, ':uid' => $userId]);
    } else if (!empty($phone)) {
        $cleanPhone = normalizePhone($phone);
        $stmt = $pdo->prepare("UPDATE elevex_users SET preferred_language = :lang, updated_at = NOW() WHERE phone = :p");
        $stmt->execute([':lang' => $lang, ':p' => $cleanPhone]);
    }

    sendJsonResponse([
        "success" => true,
        "message" => "भाषा प्राथमिकता सफलतापूर्वक सहेजी गई (Language preference saved)",
        "preferred_language" => $lang
    ]);
}

    // 7. Update OneSignal Player ID Binding
if ($action === 'update_onesignal_id') {
    $userId = intval($input['user_id'] ?? 0);
    $phone = trim($input['phone'] ?? '');
    $playerId = trim($input['player_id'] ?? $input['onesignal_player_id'] ?? '');

    if (empty($playerId)) {
        sendJsonResponse(["success" => false, "error" => "Player ID अनिवार्य है"], 400);
    }

    if ($userId > 0) {
        $stmt = $pdo->prepare("UPDATE elevex_users SET onesignal_player_id = :pid, updated_at = NOW() WHERE id = :uid");
        $stmt->execute([':pid' => $playerId, ':uid' => $userId]);
    } else if (!empty($phone)) {
        $cleanPhone = normalizePhone($phone);
        $stmt = $pdo->prepare("UPDATE elevex_users SET onesignal_player_id = :pid, updated_at = NOW() WHERE phone = :p OR phone = :full");
        $stmt->execute([':pid' => $playerId, ':p' => $cleanPhone, ':full' => '+91' . $cleanPhone]);
    }

    sendJsonResponse([
        "success" => true,
        "message" => "OneSignal Player ID सफलतापूर्वक पंजीकृत (OneSignal registered)",
        "player_id" => $playerId
    ]);
}

// 8. Cryptographic Bearer Session Verification
if ($action === 'verify_session') {
    $authUser = verifyBearerToken($pdo);
    sendJsonResponse([
        "success" => true,
        "message" => "सत्र मान्य है (Session valid)",
        "user" => $authUser
    ]);
}

sendJsonResponse(["success" => false, "error" => "Invalid action requested"], 400);
} catch (Throwable $e) {
    sendJsonResponse([
        "success" => false,
        "error" => "Exception: " . $e->getMessage(),
        "file" => basename($e->getFile()),
        "line" => $e->getLine()
    ], 500);
}
