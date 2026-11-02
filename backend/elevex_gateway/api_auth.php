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

$input = getJsonInput();
$action = $_GET['action'] ?? $_POST['action'] ?? ($input['action'] ?? '');

function ensureAuthColumns($pdo) {
    static $ensured = false;
    if ($ensured) return;
    $cols = [
        "session_token" => "VARCHAR(64) NULL",
        "session_token_expires_at" => "DATETIME NULL",
        "onesignal_player_id" => "VARCHAR(64) NULL",
        "mpin_changed_at" => "DATETIME NULL",
        "is_first_login" => "TINYINT(1) DEFAULT 1"
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

if (!function_exists('normalizePhone')) {
    function normalizePhone($raw) {
        $clean = preg_replace('/[^0-9]/', '', (string)$raw);
        return strlen($clean) > 10 ? substr($clean, -10) : $clean;
    }
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
    $phoneRaw = trim($input['phone'] ?? $input['mobile'] ?? $_POST['phone'] ?? $_POST['mobile'] ?? $_GET['phone'] ?? $_GET['mobile'] ?? '');
    $mpin = trim($input['mpin'] ?? $input['pin'] ?? $_POST['mpin'] ?? $_POST['pin'] ?? '');
    $deviceId = trim($input['device_id'] ?? $_POST['device_id'] ?? $_GET['device_id'] ?? '');

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
    if (!empty($user['mpin_hash'])) {
        if (empty($mpin)) {
            sendJsonResponse(["success" => false, "error" => "सुरक्षा पिन अनिवार्य है (Security PIN required)"], 400);
        }
        $isValidMpin = password_verify($mpin, $user['mpin_hash']);
        if (!$isValidMpin) {
            sendJsonResponse(["success" => false, "error" => "गलत सुरक्षा पिन (Invalid Security MPIN)"], 401);
        }
    }

    // Issue Cryptographic SHA-256 Bearer Session Token (30-day validity)
    $sessionToken = bin2hex(random_bytes(32));
    $expiresAt = date('Y-m-d H:i:s', strtotime('+30 days'));
    $playerParam = trim($input['onesignal_player_id'] ?? $input['player_id'] ?? '');

    $updateSql = "UPDATE elevex_users SET session_token = :st, session_token_expires_at = :exp";
    $updateParams = [':st' => $sessionToken, ':exp' => $expiresAt, ':uid' => $user['id']];
    if (!empty($playerParam)) {
        $updateSql .= ", onesignal_player_id = :pid";
        $updateParams[':pid'] = $playerParam;
    }
    if (!empty($deviceId)) {
        $updateSql .= ", hardware_device_id = :did";
        $updateParams[':did'] = $deviceId;
    }
    $updateSql .= ", updated_at = NOW() WHERE id = :uid";
    $stmtDev = $pdo->prepare($updateSql);
    $stmtDev->execute($updateParams);

    // Return complete dynamic branding + user role + Bearer session token
    sendJsonResponse([
        "success" => true,
        "message" => "लॉगिन सफल (Login successful)",
        "session_token" => $sessionToken,
        "token_type" => "Bearer",
        "expires_at" => $expiresAt,
        "must_change_pin" => (!empty($user['is_first_login']) || empty($user['mpin_changed_at']) || $mpin === '1234'),
        "is_first_login" => (!empty($user['is_first_login']) || empty($user['mpin_changed_at'])),
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
    $supervisorName = trim($input['supervisor_name'] ?? $adminName);
    $supervisorPhone = trim($input['supervisor_phone'] ?? $adminPhone);
    $supervisorMpin = trim($input['supervisor_mpin'] ?? $input['default_mpin'] ?? '');
    if (empty($supervisorMpin)) {
        $supervisorMpin = strval(rand(1000, 9999));
    }

    // Extended 12-Point Corporate Proforma Fields
    $gstNumber = strtoupper(trim($input['gst_number'] ?? ''));
    $cinNumber = strtoupper(trim($input['cin_number'] ?? ''));
    $msmeNumber = trim($input['msme_number'] ?? '');
    $city = trim($input['city'] ?? '');
    $state = trim($input['state'] ?? '');
    $pincode = trim($input['pincode'] ?? '');
    $officialEmail = trim($input['official_email'] ?? '');
    $tollfreePhone = trim($input['tollfree_phone'] ?? '');
    $websiteDomain = trim($input['website_domain'] ?? '');
    $seoKeywords = trim($input['seo_keywords'] ?? '');
    $googleMapsLink = trim($input['google_maps_link'] ?? '');
    $bankName = trim($input['bank_name'] ?? '');
    $bankAccountNo = trim($input['bank_account_no'] ?? '');
    $bankIfsc = strtoupper(trim($input['bank_ifsc'] ?? ''));
    $bankUpiId = trim($input['bank_upi_id'] ?? '');
    $servicesCatalog = $input['services_catalog'] ?? [];
    $servicesCatalogJson = is_array($servicesCatalog) ? json_encode($servicesCatalog) : (is_string($servicesCatalog) ? $servicesCatalog : '[]');

    if (empty($cname) || empty($phone)) {
        sendJsonResponse(["success" => false, "error" => "कंपनी का नाम व फोन नंबर अनिवार्य है"], 400);
    }

    // 15-digit GSTIN format validation (if provided)
    if (!empty($gstNumber)) {
        $gstRegex = '/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/';
        if (!preg_match($gstRegex, $gstNumber)) {
            sendJsonResponse(["success" => false, "error" => "अमान्य 15-अंकीय GSTIN संख्या (Invalid 15-digit GSTIN format)"], 400);
        }
    }

    // Generate unique tenant_id
    $cleanSlug = preg_replace('/[^a-z0-9]/', '', strtolower($cname));
    $tenantId = "tenant_" . substr($cleanSlug, 0, 16) . "_" . rand(1000, 9999);

    try {
        $pdo->beginTransaction();

        $stmtComp = $pdo->prepare("INSERT INTO elevex_companies 
            (tenant_id, company_name, brand_display_name, logo_url, primary_color_hex, support_phone, office_address,
             gst_number, cin_number, msme_number, registered_address, city, state, pincode, official_email, tollfree_phone,
             website_domain, seo_keywords, google_maps_link, bank_name, bank_account_no, bank_ifsc, bank_upi_id, services_catalog,
             features_config, is_active) 
            VALUES (:tid, :cname, :bname, :logo, :col, :ph, :addr,
                    :gst, :cin, :msme, :reg_addr, :city, :state, :pin, :email, :tf,
                    :web, :seo, :gmaps, :bname_bank, :acc, :ifsc, :upi, :services,
                    :feat, 1)");
        
        $features = json_encode([
            "master_service_active" => true,
            "spare_parts_quotation" => false, // STRICT: Locked by SuperAdmin by default
            "whatsapp_pdf_jobsheet" => true,
            "offline_outbox_sync" => true,
            "emergency_siren_wakeup" => true,
            "amc_routine_scheduler" => true,
            "anti_spoof_gps" => true,
            "engineering_tools_47" => true,
            "customer_feedback_rating" => true,
            "door_drive_specialist" => true,
            "offline_diagnostics" => true,
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
            ':gst' => $gstNumber,
            ':cin' => $cinNumber,
            ':msme' => $msmeNumber,
            ':reg_addr' => $address,
            ':city' => $city,
            ':state' => $state,
            ':pin' => $pincode,
            ':email' => $officialEmail,
            ':tf' => $tollfreePhone,
            ':web' => $websiteDomain,
            ':seo' => $seoKeywords,
            ':gmaps' => $googleMapsLink,
            ':bname_bank' => $bankName,
            ':acc' => $bankAccountNo,
            ':ifsc' => $bankIfsc,
            ':upi' => $bankUpiId,
            ':services' => $servicesCatalogJson,
            ':feat' => $features
        ]);

        // Create or Update Head Supervisor user with role = 'supervisor'
        $defaultMpin = password_hash($supervisorMpin, PASSWORD_BCRYPT);
        $cleanSupervisorPhone = normalizePhone($supervisorPhone);

        $stmtCheck = $pdo->prepare("SELECT id FROM elevex_users WHERE phone = :p OR phone = :norm OR phone = :full LIMIT 1");
        $stmtCheck->execute([
            ':p' => $supervisorPhone,
            ':norm' => $cleanSupervisorPhone,
            ':full' => '+91' . $cleanSupervisorPhone
        ]);
        $existing = $stmtCheck->fetch();

        if ($existing) {
            $stmtUp = $pdo->prepare("UPDATE elevex_users SET tenant_id = :tid, role = 'supervisor', full_name = :name, phone = :phone, mpin_hash = :mpin, is_active = 1 WHERE id = :id");
            $stmtUp->execute([
                ':tid' => $tenantId,
                ':name' => $supervisorName,
                ':phone' => $cleanSupervisorPhone,
                ':mpin' => $defaultMpin,
                ':id' => $existing['id']
            ]);
        } else {
            $colCheck = $pdo->query("SHOW COLUMNS FROM elevex_users LIKE 'mobile'")->fetch();
            if ($colCheck) {
                $stmtUsr = $pdo->prepare("INSERT INTO elevex_users (tenant_id, role, full_name, phone, mobile, mpin_hash, is_active) 
                                          VALUES (:tid, 'supervisor', :name, :phone, :mob, :mpin, 1)");
                $stmtUsr->execute([
                    ':tid' => $tenantId,
                    ':name' => $supervisorName,
                    ':phone' => $cleanSupervisorPhone,
                    ':mob' => $cleanSupervisorPhone,
                    ':mpin' => $defaultMpin
                ]);
            } else {
                $stmtUsr = $pdo->prepare("INSERT INTO elevex_users (tenant_id, role, full_name, phone, mpin_hash, is_active) 
                                          VALUES (:tid, 'supervisor', :name, :phone, :mpin, 1)");
                $stmtUsr->execute([
                    ':tid' => $tenantId,
                    ':name' => $supervisorName,
                    ':phone' => $cleanSupervisorPhone,
                    ':mpin' => $defaultMpin
                ]);
            }
        }

        $pdo->commit();

        // 100% White-Label Company Branded WhatsApp Onboarding Message & Link
        $supportLine = !empty($tollfreePhone) ? $tollfreePhone : $phone;
        $waMsg = "🏢 *" . $brandName . "* 🏢

"
               . "नमस्ते " . $supervisorName . " जी,

"
               . "आपको *" . $brandName . "* का हेड सुपरवाइज़र (Operations Head) नियुक्त किया गया है।

"
               . "📲 *Supervisor Portal App डाउनलोड लिंक:*
"
               . "https://shribalajikripadham.online/download.php?app=supervisor

"
               . "🔐 *आपके लॉगिन क्रेडेंशियल्स:*
"
               . "• कंपनी कोड (Tenant ID): `" . $tenantId . "`
"
               . "• यूजर आईडी / मोबाइल: `" . $cleanSupervisorPhone . "`
"
               . "• सुरक्षा पिन: `" . $supervisorMpin . "`

"
               . (!empty($websiteDomain) ? "🌐 आधिकारिक वेबसाइट: " . $websiteDomain . "
" : "")
               . "📞 सपोर्ट हेल्पलाइन: " . $supportLine . "
"
               . (!empty($address) ? "📍 ऑफिस: " . $address . "
" : "")
               . "━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        $waUrl = "https://wa.me/91" . $cleanSupervisorPhone . "?text=" . rawurlencode($waMsg);

        // 100% White-Label Company Branded Automated Server SMS Dispatch
        $brandTitle = !empty($brandName) ? $brandName : $cname;
        $smsText = "[{$brandTitle}] नमस्ते {$supervisorName} जी! {$brandTitle} में आपका हेड सुपरवाइज़र खाता सक्रिय हुआ। ऐप: https://shribalajikripadham.online/download.php?app=supervisor | कंपनी कोड: {$tenantId} | यूजर: {$cleanSupervisorPhone} | सुरक्षा पिन: {$supervisorMpin} - टीम {$brandTitle}";
        $smsResult = sendCompanyTransactionalSms($pdo, $tenantId, $cleanSupervisorPhone, 'SUPERVISOR_ONBOARDING', $smsText, $brandTitle);

        sendJsonResponse([
            "success" => true,
            "message" => "नई कंपनी व सुपरवाइज़र सफलतापूर्वक सक्रिय हुए!",
            "sms_sent" => true,
            "sms_text" => $smsText,
            "sms_brand" => $brandTitle,
            "sms_id" => $smsResult['sms_id'] ?? null,
            "tenant_id" => $tenantId,
            "company_name" => $cname,
            "brand_display_name" => $brandName,
            "supervisor_name" => $supervisorName,
            "supervisor_phone" => $cleanSupervisorPhone,
            "supervisor_mpin" => $supervisorMpin,
            "supervisor_app_url" => "https://shribalajikripadham.online/download.php?app=supervisor",
            "whatsapp_message" => $waMsg,
            "whatsapp_url" => $waUrl
        ]);
    } catch (Exception $e) {
        $pdo->rollBack();
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// 2.9 Get Company Profile & Live Features
if ($action === 'get_company_profile') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    if (empty($tenantId)) {
        sendJsonResponse(["success" => false, "error" => "Tenant ID required"], 400);
    }
    $statusInfo = getCompanyStatusAndFeatures($pdo, $tenantId);
    $stmtC = $pdo->prepare("SELECT tenant_id, company_name, brand_display_name, logo_url, primary_color_hex, support_phone, office_address, default_language, is_active, features_config FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmtC->execute([':t' => $tenantId]);
    $comp = $stmtC->fetch(PDO::FETCH_ASSOC);
    if (!$comp) {
        sendJsonResponse(["success" => false, "error" => "Company not found"], 404);
    }
    sendJsonResponse([
        "success" => true,
        "company" => [
            "tenant_id" => $comp['tenant_id'],
            "company_name" => $comp['company_name'],
            "brand_display_name" => $comp['brand_display_name'],
            "logo_url" => $comp['logo_url'],
            "primary_color" => $comp['primary_color_hex'] ?: '#0284C7',
            "support_phone" => $comp['support_phone'],
            "office_address" => $comp['office_address'],
            "is_active" => $statusInfo['is_active'],
            "features" => $statusInfo['features'],
            "default_language" => $comp['default_language'] ?: 'hi'
        ]
    ]);
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

    $stmtC = $pdo->prepare("SELECT features_config, is_active FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmtC->execute([':t' => $targetTenant]);
    $row = $stmtC->fetch();
    if (!$row) {
        sendJsonResponse(["success" => false, "error" => "Company not found"], 404);
    }

    $config = !empty($row['features_config']) ? json_decode($row['features_config'], true) : [];
    $config[$featureKey] = $enabled;

    if ($featureKey === 'master_service_active') {
        $stmtUp = $pdo->prepare("UPDATE elevex_companies SET features_config = :fc, is_active = :act, updated_at = NOW() WHERE tenant_id = :t");
        $stmtUp->execute([':fc' => json_encode($config), ':act' => $enabled ? 1 : 0, ':t' => $targetTenant]);
    } else {
        $stmtUp = $pdo->prepare("UPDATE elevex_companies SET features_config = :fc, updated_at = NOW() WHERE tenant_id = :t");
        $stmtUp->execute([':fc' => json_encode($config), ':t' => $targetTenant]);
    }

    sendJsonResponse([
        "success" => true,
        "message" => "Feature toggle updated successfully",
        "tenant_id" => $targetTenant,
        "feature_key" => $featureKey,
        "enabled" => $enabled,
        "features" => $config,
        "is_active" => ($featureKey === 'master_service_active') ? ($enabled ? 1 : 0) : intval($row['is_active'] ?? 1)
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
                         (SELECT COUNT(*) FROM elevex_breakdowns WHERE tenant_id = c.tenant_id AND status != 'resolved') as active_breakdowns,
                         (SELECT full_name FROM elevex_users WHERE tenant_id = c.tenant_id AND role IN ('supervisor', 'company_admin') ORDER BY id ASC LIMIT 1) as supervisor_name,
                         (SELECT phone FROM elevex_users WHERE tenant_id = c.tenant_id AND role IN ('supervisor', 'company_admin') ORDER BY id ASC LIMIT 1) as supervisor_phone
                         FROM elevex_companies c ORDER BY c.id DESC");
    $companies = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $defaultFeatures = [
        "master_service_active" => true,
        "spare_parts_quotation" => false, // STRICT: Default LOCKED by SuperAdmin
        "whatsapp_pdf_jobsheet" => true,
        "offline_outbox_sync" => true,
        "emergency_siren_wakeup" => true,
        "amc_routine_scheduler" => true,
        "anti_spoof_gps" => true,
        "engineering_tools_47" => true,
        "customer_feedback_rating" => true,
        "door_drive_specialist" => true,
        "offline_diagnostics" => true,
        "auto_dispatch" => true,
        "unlimited_free_tier" => true
    ];
    foreach ($companies as &$comp) {
        $cFeat = !empty($comp['features_config']) ? json_decode($comp['features_config'], true) : [];
        if (!is_array($cFeat)) $cFeat = [];
        $mergedFeat = array_merge($defaultFeatures, $cFeat);
        $comp['features_config'] = json_encode($mergedFeat);
        $sName = !empty($comp['supervisor_name']) ? $comp['supervisor_name'] : 'हेड सुपरवाइज़र';
        $sPhone = normalizePhone(!empty($comp['supervisor_phone']) ? $comp['supervisor_phone'] : $comp['support_phone']);
        $tId = $comp['tenant_id'];
        $cName = $comp['company_name'];
        $waMsg = "🏢 *" . $cName . "* 🏢

"
               . "नमस्ते " . $sName . " जी,

"
               . "आपको *" . $cName . "* का सुपरवाइज़र नियुक्त किया गया है।

"
               . "📲 *Supervisor Portal App डाउनलोड:*
"
               . "https://shribalajikripadham.online/download.php?app=supervisor

"
               . "🔐 *लॉगिन क्रेडेंशियल्स:*
"
               . "• कंपनी कोड: `" . $tId . "`
"
               . "• यूजर आईडी: `" . $sPhone . "`
"
               . "• सुरक्षा पिन: `1234`

"
               . "📞 सपोर्ट हेल्पलाइन: " . (!empty($comp['support_phone']) ? $comp['support_phone'] : '9811001122') . "
"
               . "━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        $comp['whatsapp_message'] = $waMsg;
        $comp['whatsapp_url'] = "https://wa.me/91" . $sPhone . "?text=" . rawurlencode($waMsg);
    }
    unset($comp);

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

    $stmtC = $pdo->prepare("SELECT features_config, is_active FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
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
    $mpin = trim($input['mpin'] ?? '');
    if (empty($mpin)) {
        $mpin = strval(rand(1000, 9999));
    }
    $preferredLang = trim($input['preferred_language'] ?? 'hi');
    $liftId = intval($input['lift_id'] ?? 0);
    $liftCode = trim($input['lift_unique_code'] ?? $input['lift_code'] ?? '');
    $skillGrade = trim($input['skill_grade'] ?? 'Field Specialist');
    $aadharNo = trim($input['aadhar_no'] ?? '');
    $safetyKit = trim($input['safety_kit_status'] ?? 'VERIFIED_ACTIVE');

    // Token Auth & Tenant Isolation Wall
    $token = getBearerToken();
    if (!empty($token)) {
        $authUser = verifyBearerToken($pdo);
        enforceTenantIsolation($authUser, $tenantId);
        // Restrict non-SuperAdmins: Supervisor can only create technicians or lift owners
        if ($authUser['role'] !== 'superadmin' && !in_array($role, ['technician', 'senior_technician', 'lift_owner', 'owner', 'client'])) {
            sendJsonResponse(["success" => false, "error" => "सुपरवाइज़र केवल अपनी कंपनी के टेक्नीशियन या लिफ्ट ओनर ही जोड़ सकते हैं"], 403);
        }
    }

    if (empty($tenantId) || empty($fullName) || empty($phone)) {
        sendJsonResponse(["success" => false, "error" => "कंपनी, नाम व फोन अनिवार्य है"], 400);
    }

    $mpinHash = password_hash($mpin, PASSWORD_BCRYPT);

    $stmtCheck = $pdo->prepare("SELECT id FROM elevex_users WHERE phone = :p LIMIT 1");
    $stmtCheck->execute([':p' => $phone]);
    $existing = $stmtCheck->fetch();

    if ($existing) {
        $stmtUp = $pdo->prepare("UPDATE elevex_users SET tenant_id = :t, role = :r, full_name = :n, mpin_hash = :m, preferred_language = :lang, skill_grade = :sg, aadhar_no = :aadh, safety_kit_status = :sk, is_first_login = 1, mpin_changed_at = NULL, is_active = 1 WHERE id = :id");
        $stmtUp->execute([
            ':t' => $tenantId,
            ':r' => $role,
            ':n' => $fullName,
            ':m' => $mpinHash,
            ':lang' => $preferredLang,
            ':sg' => $skillGrade,
            ':aadh' => $aadharNo,
            ':sk' => $safetyKit,
            ':id' => $existing['id']
        ]);
        $userId = intval($existing['id']);
    } else {
        $stmtIns = $pdo->prepare("INSERT INTO elevex_users (tenant_id, role, full_name, phone, mobile, mpin_hash, preferred_language, skill_grade, aadhar_no, safety_kit_status, is_first_login, is_active) 
                                  VALUES (:t, :r, :n, :p, :mob, :m, :lang, :sg, :aadh, :sk, 1, 1)");
        $stmtIns->execute([
            ':t' => $tenantId,
            ':r' => $role,
            ':n' => $fullName,
            ':p' => $phone,
            ':mob' => $phone,
            ':m' => $mpinHash,
            ':lang' => $preferredLang,
            ':sg' => $skillGrade,
            ':aadh' => $aadharNo,
            ':sk' => $safetyKit
        ]);
        $userId = intval($pdo->lastInsertId());
    }

    // Link Lift Owner if lift specified
    if (in_array($role, ['lift_owner', 'owner', 'client'])) {
        if ($liftId > 0) {
            $stmtLiftOwner = $pdo->prepare("UPDATE elevex_lifts_amc SET owner_id = :oid WHERE id = :lid AND tenant_id = :t");
            $stmtLiftOwner->execute([':oid' => $userId, ':lid' => $liftId, ':t' => $tenantId]);
        } else if (!empty($liftCode)) {
            $stmtLiftOwner = $pdo->prepare("UPDATE elevex_lifts_amc SET owner_id = :oid WHERE lift_unique_code = :lc AND tenant_id = :t");
            $stmtLiftOwner->execute([':oid' => $userId, ':lc' => $liftCode, ':t' => $tenantId]);
        }
    }

    // Fetch company display name
    $stmtC = $pdo->prepare("SELECT company_name, brand_display_name, support_phone FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmtC->execute([':t' => $tenantId]);
    $comp = $stmtC->fetch(PDO::FETCH_ASSOC);
    $cName = $comp['company_name'] ?? 'EleveX Network';

    // 100% White-Label Company Branded WhatsApp Onboarding Message & Link
    $compSupport = !empty($comp['support_phone']) ? $comp['support_phone'] : '';
    $brandTitle = !empty($comp['brand_display_name']) ? $comp['brand_display_name'] : $cName;

    if (in_array($role, ['technician', 'senior_technician'])) {
        $roleTitle = ($role === 'senior_technician') ? "सीनियर फील्ड टेक्नीशियन (Senior Technician)" : "फील्ड टेक्नीशियन (Field Technician)";
        $appUrl = "https://shribalajikripadham.online/download.php?app=technician";
        $waMsg = "🛠️ *" . $brandTitle . "* 🛠️

"
               . "नमस्ते *" . $fullName . "* जी,
"
               . "आपको *" . $brandTitle . "* में *" . $roleTitle . "* के रूप में पंजीकृत किया गया है।

"
               . "📲 *Technician App डाउनलोड लिंक:*
"
               . $appUrl . "

"
               . "🔐 *आपके लॉगिन क्रेडेंशियल्स:*
"
               . "• कंपनी कोड (Tenant ID): `" . $tenantId . "`
"
               . "• यूजर आईडी / मोबाइल: `" . $phone . "`
"
               . "• सुरक्षा पिन: `" . $mpin . "`

"
               . "⚡ *निर्देश:* ऐप डाउनलोड करके लॉगिन करें और फील्ड कार्य शुरू करें।
"
               . (!empty($compSupport) ? "📞 हेल्पलाइन: " . $compSupport . "
" : "")
               . "━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    } else {
        $appUrl = "https://shribalajikripadham.online/download.php?app=owner";
        $waMsg = "🏢 *" . $brandTitle . "* 🏢

"
               . "नमस्ते *" . $fullName . "* जी,
"
               . "आपकी लिफ्ट की सुरक्षा और रियल-टाइम मॉनिटरिंग हेतु *" . $brandTitle . "* द्वारा आपका ओनर खाता सक्रिय कर दिया गया है।

"
               . "📲 *Customer App डाउनलोड लिंक:*
"
               . $appUrl . "

"
               . "🔐 *आपके लॉगिन क्रेडेंशियल्स:*
"
               . "• कंपनी कोड: `" . $tenantId . "`
"
               . (!empty($liftCode) ? "• लिफ्ट कोड: `" . $liftCode . "`
" : "")
               . "• यूजर आईडी / मोबाइल: `" . $phone . "`
"
               . "• सुरक्षा पिन: `" . $mpin . "`

"
               . "✨ *सुविधाएं:* 1-टैप इमरजेंसी ब्रेकडाउन, डिजिटल सर्विस जॉब शीट्स, व AMC स्थिति।
"
               . (!empty($compSupport) ? "📞 24x7 हेल्पलाइन: " . $compSupport . "
" : "")
               . "━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    }
    $waUrl = "https://wa.me/91" . $phone . "?text=" . rawurlencode($waMsg);

    // Automated Server SMS under strict company brand
    if (in_array($role, ['technician', 'senior_technician'])) {
        $smsText = "[{$brandTitle}] नमस्ते {$fullName} जी! {$brandTitle} में आपका {$roleTitle} खाता सक्रिय है। ऐप लिंक: {$appUrl} | कंपनी कोड: {$tenantId} | यूजर: {$phone} | पिन: {$mpin} - टीम {$brandTitle}";
        $smsResult = sendCompanyTransactionalSms($pdo, $tenantId, $phone, 'TECHNICIAN_ONBOARDING', $smsText, $brandTitle);
    } else {
        $smsText = "[{$brandTitle}] नमस्ते {$fullName} जी! {$brandTitle} द्वारा आपकी लिफ्ट का कस्टमर खाता सक्रिय है। ऐप लिंक: {$appUrl} | कंपनी कोड: {$tenantId} | यूजर: {$phone} | पिन: {$mpin} | हेल्पलाइन: {$compSupport} - टीम {$brandTitle}";
        $smsResult = sendCompanyTransactionalSms($pdo, $tenantId, $phone, 'OWNER_ONBOARDING', $smsText, $brandTitle);
    }

    sendJsonResponse([
        "sms_sent" => true,
        "sms_text" => $smsText,
        "sms_brand" => $brandTitle,
        "sms_id" => $smsResult['sms_id'] ?? null,
        "success" => true,
        "message" => in_array($role, ['lift_owner', 'owner', 'client']) ? "लिफ्ट ओनर सफलतापूर्वक पंजीकृत हुआ!" : "टेक्नीशियन सफलतापूर्वक पंजीकृत हुआ!",
        "user_id" => $userId,
        "role" => $role,
        "full_name" => $fullName,
        "phone" => $phone,
        "mpin" => $mpin,
        "tenant_id" => $tenantId,
        "app_download_url" => $appUrl,
        "whatsapp_message" => $waMsg,
        "whatsapp_url" => $waUrl
    ]);
}

// 5.5 Change Security MPIN / PIN
if ($action === 'change_mpin') {
    $authUser = verifyBearerToken($pdo);
    $oldMpin = trim($input['old_mpin'] ?? $input['current_mpin'] ?? '');
    $newMpin = trim($input['new_mpin'] ?? $input['pin'] ?? '');

    if (strlen($newMpin) !== 4 || !ctype_digit($newMpin)) {
        sendJsonResponse(["success" => false, "error" => "सुरक्षा पिन अनिवार्य रूप से 4 अंकों का होना चाहिए (MPIN must be exactly 4 digits)"], 400);
    }

    $stmtU = $pdo->prepare("SELECT id, mpin_hash FROM elevex_users WHERE id = :id LIMIT 1");
    $stmtU->execute([':id' => $authUser['id']]);
    $curr = $stmtU->fetch();

    if (!empty($curr['mpin_hash']) && !empty($oldMpin)) {
        $isValidOld = password_verify($oldMpin, $curr['mpin_hash']);
        if (!$isValidOld && empty($curr['mpin_changed_at']) && $oldMpin === '1234') {
            $isValidOld = true;
        }
        if (!$isValidOld) {
            sendJsonResponse(["success" => false, "error" => "वर्तमान सुरक्षा पिन गलत है (Invalid current MPIN)"], 401);
        }
    }

    $newHash = password_hash($newMpin, PASSWORD_BCRYPT);
    $stmtUp = $pdo->prepare("UPDATE elevex_users SET mpin_hash = :m, mpin_changed_at = NOW(), is_first_login = 0, updated_at = NOW() WHERE id = :id");
    $stmtUp->execute([':m' => $newHash, ':id' => $authUser['id']]);

    sendJsonResponse([
        "success" => true,
        "message" => "सुरक्षा पिन (MPIN) सफलतापूर्वक बदला गया!",
        "user_id" => $authUser['id']
    ]);
}

// 6. List Users for Company
if ($action === 'list_users') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    $token = getBearerToken();
    if (!empty($token)) {
        $authUser = verifyBearerToken($pdo);
        enforceTenantIsolation($authUser, $tenantId);
    }
    $roleFilter = trim($input['role'] ?? $_GET['role'] ?? '');

    $sql = "SELECT u.id, u.tenant_id, u.role, u.full_name, u.phone, u.hardware_device_id, u.preferred_language, u.is_active, u.created_at,
                   c.company_name, c.brand_display_name,
                   (CASE WHEN t.last_heartbeat > NOW() - INTERVAL 10 MINUTE THEN 1 ELSE 0 END) as is_online,
                   t.location_landmark_text, t.last_heartbeat
            FROM elevex_users u
            LEFT JOIN elevex_companies c ON u.tenant_id COLLATE utf8mb4_unicode_ci = c.tenant_id COLLATE utf8mb4_unicode_ci
            LEFT JOIN elevex_technician_telemetry t ON u.id = t.technician_id
            WHERE u.tenant_id = :t AND u.is_active = 1";
    $params = [':t' => $tenantId];

    if (!empty($roleFilter)) {
        if ($roleFilter === 'technician') {
            $sql .= " AND u.role IN ('technician', 'senior_technician')";
        } else if ($roleFilter === 'owner' || $roleFilter === 'lift_owner') {
            $sql .= " AND u.role IN ('lift_owner', 'owner', 'client')";
        } else {
            $sql .= " AND u.role = :r";
            $params[':r'] = $roleFilter;
        }
    }

    $sql .= " ORDER BY u.id DESC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);

    foreach ($users as &$usr) {
        $uRole = $usr['role'];
        $uName = $usr['full_name'];
        $uPhone = normalizePhone($usr['phone']);
        $cName = $usr['company_name'] ?: 'EleveX Network';
        $tId = $usr['tenant_id'];

        if (in_array($uRole, ['technician', 'senior_technician'])) {
            $roleLabel = ($uRole === 'senior_technician') ? "सीनियर फील्ड तकनीशियन" : "फील्ड तकनीशियन";
            $appLink = "https://shribalajikripadham.online/download.php?app=technician";
            $msg = "🛠️ *Ankit EleveX Pro - फील्ड तकनीशियन आमंत्रण* 🛠️\n\n"
                 . "प्रिय *" . $uName . "* जी,\n"
                 . "आपको *" . $cName . "* में *" . $roleLabel . "* नियुक्त किया गया है।\n\n"
                 . "📲 *Technician App डाउनलोड लिंक:*\n"
                 . $appLink . "\n\n"
                 . "🔐 *लॉगिन क्रेडेंशियल्स:*\n"
                 . "• कंपनी कोड (Tenant ID): `" . $tId . "`\n"
                 . "• यूजरनेम / मोबाइल: `" . $uPhone . "`\n"
                 . "• डिफॉल्ट पिन: `1234`\n\n"
                 . "👉 कृपया ऐप इंस्टॉल करके तुरंत लॉगिन करें और अपने असाइन ब्रेकडाउन टिकट देखें।\n\n"
                 . "हेल्पलाइन: 8533955333 (अंकित चौधरी)";
            $usr['app_download_url'] = $appLink;
            $usr['whatsapp_message'] = $msg;
            $usr['whatsapp_url'] = "https://wa.me/91" . $uPhone . "?text=" . rawurlencode($msg);
        } else if (in_array($uRole, ['lift_owner', 'owner', 'client'])) {
            $appLink = "https://shribalajikripadham.online/download.php?app=owner";
            $msg = "🏢 *Ankit EleveX Pro - लिफ्ट ओनर पोर्टल आमंत्रण* 🏢\n\n"
                 . "प्रिय *" . $uName . "* जी,\n"
                 . "आपकी लिफ्ट की रियल-टाइम मॉनिटरिंग और सुरक्षा हेतु *" . $cName . "* द्वारा आपका ओनर पोर्टल अकाउंट सक्रिय कर दिया गया है।\n\n"
                 . "📲 *Owner App डाउनलोड लिंक:*\n"
                 . $appLink . "\n\n"
                 . "🔐 *लॉगिन क्रेडेंशियल्स:*\n"
                 . "• कंपनी कोड (Tenant ID): `" . $tId . "`\n"
                 . "• यूजरनेम / मोबाइल: `" . $uPhone . "`\n"
                 . "• डिफॉल्ट पिन: `1234`\n\n"
                 . "👉 ऐप से आप 1-टैप में ब्रेकडाउन रिपोर्ट कर सकते हैं और तकनीशियन की लाइव लोकेशन ट्रैक कर सकते हैं।\n\n"
                 . "कस्टमर केयर: 8533955333 (अंकित चौधरी)";
            $usr['app_download_url'] = $appLink;
            $usr['whatsapp_message'] = $msg;
            $usr['whatsapp_url'] = "https://wa.me/91" . $uPhone . "?text=" . rawurlencode($msg);
        }
    }
    unset($usr);

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


// 6. Request Company-Branded OTP
if ($action === 'request_company_otp') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    $phone = normalizePhone($input['phone'] ?? $_GET['phone'] ?? '');
    $purpose = trim($input['purpose'] ?? $_GET['purpose'] ?? 'OWNER_VERIFICATION');
    $refId = trim($input['reference_id'] ?? $_GET['reference_id'] ?? '');

    if (empty($phone)) {
        sendJsonResponse(["success" => false, "error" => "फोन नंबर अनिवार्य है"], 400);
    }

    // Lookup company branding
    $brandName = "EleveX";
    if (!empty($tenantId)) {
        $stmtC = $pdo->prepare("SELECT brand_display_name, company_name FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
        $stmtC->execute([':t' => $tenantId]);
        $cRow = $stmtC->fetch();
        if ($cRow) {
            $brandName = !empty($cRow['brand_display_name']) ? $cRow['brand_display_name'] : $cRow['company_name'];
        }
    }

    $otpCode = strval(rand(1000, 9999));
    $expiresAt = date('Y-m-d H:i:s', strtotime('+5 minutes'));

    $stmtOtp = $pdo->prepare("INSERT INTO elevex_company_otps (tenant_id, phone, otp_code, purpose, reference_id, is_verified, expires_at) 
                             VALUES (:t, :p, :code, :purp, :ref, 0, :exp)");
    $stmtOtp->execute([
        ':t' => $tenantId ?: 'tenant_master_elevex',
        ':p' => $phone,
        ':code' => $otpCode,
        ':purp' => $purpose,
        ':ref' => $refId,
        ':exp' => $expiresAt
    ]);

    $waMsg = "🔐 *" . $brandName . " सत्यापन कोड*\n\n"
           . "नमस्ते,\n"
           . "आपका *" . $brandName . "* सुरक्षा कोड है: *" . $otpCode . "*\n\n"
           . "यह कोड केवल 5 मिनट के लिए मान्य है। कृपया अपनी सुरक्षा के लिए इसे किसी से साझा न करें।\n\n"
           . "- टीम *" . $brandName . "*";

    $waUrl = "https://wa.me/91" . $phone . "?text=" . rawurlencode($waMsg);

    // Automated Server SMS under strict company brand
    $smsText = "[{$brandName}] आपका {$brandName} सुरक्षा कोड: {$otpCode}. यह 5 मिनट के लिए मान्य है। किसी से शेयर न करें। - टीम {$brandName}";
    $smsResult = sendCompanyTransactionalSms($pdo, $tenantId, $phone, 'SECURITY_OTP', $smsText, $brandName);

    sendJsonResponse([
        "success" => true,
        "message" => "सत्यापन कोड सफलतापूर्वक तैयार हुआ",
        "otp_code" => $otpCode,
        "phone" => $phone,
        "brand_name" => $brandName,
        "purpose" => $purpose,
        "expires_at" => $expiresAt,
        "sms_sent" => true,
        "sms_text" => $smsText,
        "sms_brand" => $brandName,
        "sms_id" => $smsResult['sms_id'] ?? null,
        "whatsapp_message" => $waMsg,
        "whatsapp_url" => $waUrl
    ]);
}

// 7. Verify Company-Branded OTP
if ($action === 'verify_company_otp') {
    $phone = normalizePhone($input['phone'] ?? $_POST['phone'] ?? '');
    $otpCode = trim($input['otp_code'] ?? $input['otp'] ?? $_POST['otp_code'] ?? '');
    $purpose = trim($input['purpose'] ?? $_POST['purpose'] ?? '');

    if (empty($phone) || empty($otpCode)) {
        sendJsonResponse(["success" => false, "error" => "फोन नंबर व ओटीपी अनिवार्य है"], 400);
    }

    $stmtV = $pdo->prepare("SELECT id, tenant_id FROM elevex_company_otps 
                            WHERE phone = :p AND otp_code = :c AND is_verified = 0 AND expires_at > NOW() 
                            ORDER BY id DESC LIMIT 1");
    $stmtV->execute([':p' => $phone, ':c' => $otpCode]);
    $otpRecord = $stmtV->fetch();

    if (!$otpRecord) {
        sendJsonResponse(["success" => false, "error" => "अमान्य अथवा समय-समाप्त (Expired) ओटीपी कोड"], 401);
    }

    $stmtUp = $pdo->prepare("UPDATE elevex_company_otps SET is_verified = 1 WHERE id = :id");
    $stmtUp->execute([':id' => $otpRecord['id']]);

    sendJsonResponse([
        "success" => true,
        "message" => "ओटीपी सफलतापूर्वक सत्यापित हुआ (OTP Verified)",
        "phone" => $phone,
        "tenant_id" => $otpRecord['tenant_id']
    ]);
}

// 8. Onboard Lift Owner On-Site (Called by Field Technician or Supervisor)
if ($action === 'onboard_owner_on_site') {
    $tenantId = trim($input['tenant_id'] ?? '');
    $ownerName = trim($input['owner_name'] ?? $input['full_name'] ?? '');
    $phone = normalizePhone($input['owner_phone'] ?? $input['phone'] ?? '');
    $liftId = intval($input['lift_id'] ?? 0);
    $liftCode = trim($input['lift_code'] ?? '');
    $contractType = trim($input['contract_type'] ?? 'AMC'); // AMC or NON_AMC
    $buildingName = trim($input['building_name'] ?? '');
    $flatOffice = trim($input['flat_office'] ?? '');
    $technicianId = intval($input['technician_id'] ?? 0);
    $technicianName = trim($input['technician_name'] ?? '');

    if (empty($tenantId) || empty($ownerName) || empty($phone)) {
        sendJsonResponse(["success" => false, "error" => "कंपनी, ग्राहक नाम व फोन नंबर अनिवार्य है"], 400);
    }

    // Lookup company branding
    $stmtC = $pdo->prepare("SELECT company_name, brand_display_name, support_phone, office_address, website_domain FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmtC->execute([':t' => $tenantId]);
    $comp = $stmtC->fetch(PDO::FETCH_ASSOC);
    $brandName = !empty($comp['brand_display_name']) ? $comp['brand_display_name'] : ($comp['company_name'] ?? 'Elevators');
    $supportPhone = !empty($comp['support_phone']) ? $comp['support_phone'] : '';

    $mpin = strval(rand(1000, 9999));
    $mpinHash = password_hash($mpin, PASSWORD_BCRYPT);

    $stmtCheck = $pdo->prepare("SELECT id FROM elevex_users WHERE phone = :p LIMIT 1");
    $stmtCheck->execute([':p' => $phone]);
    $existing = $stmtCheck->fetch();

    if ($existing) {
        $ownerId = intval($existing['id']);
        $stmtUp = $pdo->prepare("UPDATE elevex_users SET tenant_id = :t, role = 'lift_owner', full_name = :n, mpin_hash = :m, created_by_technician_id = :tid, is_active = 1 WHERE id = :id");
        $stmtUp->execute([
            ':t' => $tenantId,
            ':n' => $ownerName,
            ':m' => $mpinHash,
            ':tid' => $technicianId,
            ':id' => $ownerId
        ]);
    } else {
        $stmtIns = $pdo->prepare("INSERT INTO elevex_users (tenant_id, role, full_name, phone, mobile, mpin_hash, created_by_technician_id, is_active) 
                                  VALUES (:t, 'lift_owner', :n, :p, :mob, :m, :tid, 1)");
        $stmtIns->execute([
            ':t' => $tenantId,
            ':n' => $ownerName,
            ':p' => $phone,
            ':mob' => $phone,
            ':m' => $mpinHash,
            ':tid' => $technicianId
        ]);
        $ownerId = intval($pdo->lastInsertId());
    }

    // Link or Create Lift for Owner
    if ($liftId > 0) {
        $stmtL = $pdo->prepare("UPDATE elevex_lifts_amc SET owner_id = :oid WHERE id = :lid AND tenant_id = :t");
        $stmtL->execute([':oid' => $ownerId, ':lid' => $liftId, ':t' => $tenantId]);
    } else if (!empty($buildingName) || !empty($input['lift_unique_code']) || !empty($liftCode)) {
        $lCode = !empty($liftCode) ? $liftCode : (!empty($input['lift_unique_code']) ? trim($input['lift_unique_code']) : ('LIFT-' . rand(100, 999)));
        $isAmc = (strtoupper($contractType) === 'NON_AMC') ? 0 : 1;
        $stmtNewLift = $pdo->prepare("INSERT INTO elevex_lifts_amc 
            (tenant_id, building_name, lift_unique_code, building_address, contract_type, is_amc, owner_id, controller_model, stops_count, capacity_kg, next_service_due_date, created_at)
            VALUES 
            (:t, :bname, :code, :addr, :ctype, :isamc, :oid, :ctrl, :stops, :cap, DATE_ADD(CURDATE(), INTERVAL 30 DAY), NOW())");
        $stmtNewLift->execute([
            ':t' => $tenantId,
            ':bname' => $buildingName,
            ':code' => $lCode,
            ':addr' => trim($input['building_address'] ?? $input['address'] ?? ''),
            ':ctype' => $contractType,
            ':isamc' => $isAmc,
            ':oid' => $ownerId,
            ':ctrl' => trim($input['controller_model'] ?? 'MONARCH NICE3000+'),
            ':stops' => intval($input['stops_count'] ?? 10),
            ':cap' => intval($input['capacity_kg'] ?? 680)
        ]);
        $liftId = intval($pdo->lastInsertId());
    }

    // White-Label WhatsApp Welcome Invitation
    $waMsg = "🏢 *" . $brandName . "* 🏢\n\n"
           . "नमस्ते *" . $ownerName . "* जी,\n\n"
           . "आपकी बिल्डिंग *" . (!empty($buildingName) ? $buildingName : "साइट") . "* की लिफ्ट के डिजिटल प्रबंधन हेतु *" . $brandName . "* द्वारा आपका ग्राहक पोर्टल सक्रिय कर दिया गया है।\n\n"
           . "📲 *Customer App डाउनलोड लिंक:*\n"
           . "https://shribalajikripadham.online/download.php?app=owner\n\n"
           . "🔐 *लॉगिन क्रेडेंशियल्स:*\n"
           . "• कंपनी कोड (Tenant ID): `" . $tenantId . "`\n"
           . "• यूजर आईडी / मोबाइल: `" . $phone . "`\n"
           . "• सुरक्षा पिन: `" . $mpin . "`\n\n"
           . "✨ *सुविधाएं:*\n"
           . "• 1-टैप में ब्रेकडाउन शिकायत दर्ज करें\n"
           . "• लिफ्ट सर्विस की डिजिटल जॉब शीट्स देखें\n"
           . "• AMC स्थिति व अगला सर्विस शेड्यूल देखें\n\n"
           . (!empty($technicianName) ? "👷 फील्ड तकनीशियन: " . $technicianName . "\n" : "")
           . (!empty($supportPhone) ? "📞 24x7 हेल्पलाइन: " . $supportPhone . "\n" : "")
           . "━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    $waUrl = "https://wa.me/91" . $phone . "?text=" . rawurlencode($waMsg);

    // Automated Server SMS under strict company brand
    if (in_array($role, ['technician', 'senior_technician'])) {
        $smsText = "[{$brandTitle}] नमस्ते {$fullName} जी! {$brandTitle} में आपका {$roleTitle} खाता सक्रिय है। ऐप लिंक: {$appUrl} | कंपनी कोड: {$tenantId} | यूजर: {$phone} | पिन: {$mpin} - टीम {$brandTitle}";
        $smsResult = sendCompanyTransactionalSms($pdo, $tenantId, $phone, 'TECHNICIAN_ONBOARDING', $smsText, $brandTitle);
    } else {
        $smsText = "[{$brandTitle}] नमस्ते {$fullName} जी! {$brandTitle} द्वारा आपकी लिफ्ट का कस्टमर खाता सक्रिय है। ऐप लिंक: {$appUrl} | कंपनी कोड: {$tenantId} | यूजर: {$phone} | पिन: {$mpin} | हेल्पलाइन: {$compSupport} - टीम {$brandTitle}";
        $smsResult = sendCompanyTransactionalSms($pdo, $tenantId, $phone, 'OWNER_ONBOARDING', $smsText, $brandTitle);
    }

    sendJsonResponse([
        "sms_sent" => true,
        "sms_text" => $smsText,
        "sms_brand" => $brandTitle,
        "sms_id" => $smsResult['sms_id'] ?? null,
        "success" => true,
        "message" => "ग्राहक (Lift Owner) सफलतापूर्वक ऑनबोर्ड किया गया!",
        "owner_id" => $ownerId,
        "lift_id" => $liftId,
        "owner_name" => $ownerName,
        "phone" => $phone,
        "mpin" => $mpin,
        "tenant_id" => $tenantId,
        "brand_name" => $brandName,
        "whatsapp_message" => $waMsg,
        "whatsapp_url" => $waUrl
    ]);
}

// 9. Get Full Corporate Proforma Dossier
if ($action === 'get_company_proforma') {
    $tenantId = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? '');
    if (empty($tenantId)) {
        sendJsonResponse(["success" => false, "error" => "Tenant ID required"], 400);
    }

    $stmt = $pdo->prepare("SELECT * FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
    $stmt->execute([':t' => $tenantId]);
    $comp = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$comp) {
        sendJsonResponse(["success" => false, "error" => "Company not found"], 404);
    }

    sendJsonResponse([
        "success" => true,
        "proforma" => [
            "tenant_id" => $comp['tenant_id'],
            "company_name" => $comp['company_name'],
            "brand_display_name" => $comp['brand_display_name'],
            "logo_url" => $comp['logo_url'],
            "primary_color" => $comp['primary_color_hex'] ?: '#0284C7',
            "support_phone" => $comp['support_phone'],
            "office_address" => $comp['office_address'],
            "gst_number" => $comp['gst_number'] ?? '',
            "cin_number" => $comp['cin_number'] ?? '',
            "msme_number" => $comp['msme_number'] ?? '',
            "registered_address" => $comp['registered_address'] ?? $comp['office_address'],
            "city" => $comp['city'] ?? '',
            "state" => $comp['state'] ?? '',
            "pincode" => $comp['pincode'] ?? '',
            "official_email" => $comp['official_email'] ?? '',
            "tollfree_phone" => $comp['tollfree_phone'] ?? '',
            "website_domain" => $comp['website_domain'] ?? '',
            "seo_keywords" => $comp['seo_keywords'] ?? '',
            "google_maps_link" => $comp['google_maps_link'] ?? '',
            "bank_name" => $comp['bank_name'] ?? '',
            "bank_account_no" => $comp['bank_account_no'] ?? '',
            "bank_ifsc" => $comp['bank_ifsc'] ?? '',
            "bank_upi_id" => $comp['bank_upi_id'] ?? '',
            "services_catalog" => !empty($comp['services_catalog']) ? json_decode($comp['services_catalog'], true) : [],
            "features" => !empty($comp['features_config']) ? json_decode($comp['features_config'], true) : []
        ]
    ]);
}

// 9. Get Company Transactional SMS Logs (Audit Trail)
if ($action === 'get_company_sms_logs') {
    $authUser = verifyBearerToken($pdo);
    $targetTenant = trim($input['tenant_id'] ?? $_GET['tenant_id'] ?? $authUser['tenant_id'] ?? '');
    enforceTenantIsolation($authUser, $targetTenant);

    $limit = intval($input['limit'] ?? $_GET['limit'] ?? 50);
    $stmtSms = $pdo->prepare("SELECT id, tenant_id, brand_name, phone, message_type, sms_text, status, gateway_response, created_at 
                             FROM elevex_sms_logs 
                             WHERE tenant_id = :t 
                             ORDER BY id DESC LIMIT :lim");
    $stmtSms->bindValue(':t', $targetTenant, PDO::PARAM_STR);
    $stmtSms->bindValue(':lim', $limit, PDO::PARAM_INT);
    $stmtSms->execute();
    $logs = $stmtSms->fetchAll(PDO::FETCH_ASSOC);

    sendJsonResponse([
        "success" => true,
        "tenant_id" => $targetTenant,
        "count" => count($logs),
        "sms_logs" => $logs
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

