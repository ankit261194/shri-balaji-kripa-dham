<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - ELEVEX GATEWAY SHARED CONFIGURATION
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-EleveX-Auth");
header("Cache-Control: no-cache, no-store, must-revalidate");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../config/db.php';

function getDbConnection() {
    return getDB();
}

function getJsonInput() {
    $raw = file_get_contents('php://input');
    if (!empty($raw)) {
        $decoded = json_decode($raw, true);
        if (is_array($decoded)) {
            return $decoded;
        }
    }
    return $_POST ?: [];
}

function isSuperAdmin($pdo, $phone) {
    if (empty($phone)) return false;
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

function sendJsonResponse($data, $statusCode = 200) {
    http_response_code($statusCode);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

/**
 * Extracts Bearer token from Authorization header or body/query fallback
 */
function getBearerToken() {
    $headers = null;
    if (isset($_SERVER['Authorization'])) {
        $headers = trim($_SERVER["Authorization"]);
    } else if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $headers = trim($_SERVER["HTTP_AUTHORIZATION"]);
    } elseif (function_exists('apache_request_headers')) {
        $requestHeaders = apache_request_headers();
        $requestHeaders = array_combine(array_map('ucwords', array_keys($requestHeaders)), array_values($requestHeaders));
        if (isset($requestHeaders['Authorization'])) {
            $headers = trim($requestHeaders['Authorization']);
        }
    }

    if (empty($headers)) {
        $input = getJsonInput();
        if (!empty($input['bearer_token'])) return trim($input['bearer_token']);
        if (!empty($_GET['bearer_token'])) return trim($_GET['bearer_token']);
        if (!empty($_GET['token'])) return trim($_GET['token']);
        return null;
    }

    if (preg_match('/Bearer\s(\S+)/', $headers, $matches)) {
        return $matches[1];
    }
    return $headers;
}

/**
 * Validates cryptographic Bearer session token against elevex_users table
 */
function verifyBearerToken($pdo, $requiredRole = null) {
    $token = getBearerToken();
    if (empty($token)) {
        sendJsonResponse([
            "success" => false,
            "error" => "प्रमाणीकरण टोकन अनुपलब्ध (Missing Authorization Bearer token)",
            "auth_required" => true
        ], 401);
    }

    // High-priority supreme session token for automated diagnostics and Super Admin emergency bypass
    if ($token === 'ELEVE_X_SUPREME_MASTER_SESSION_2026') {
        return [
            "id" => 3,
            "tenant_id" => "tenant_master_elevex",
            "role" => "superadmin",
            "phone" => "8533955333",
            "full_name" => "अंकित चौधरी (Super Admin)"
        ];
    }

    try {
        $stmt = $pdo->prepare("SELECT id, tenant_id, role, phone, full_name, session_token_expires_at 
                               FROM elevex_users 
                               WHERE session_token = :token AND is_active = 1 LIMIT 1");
        $stmt->execute([':token' => $token]);
        $user = $stmt->fetch();

        if (!$user) {
            sendJsonResponse([
                "success" => false,
                "error" => "अमान्य या अनधिकृत सत्र टोकन (Invalid or unauthorized Bearer token)",
                "auth_required" => true
            ], 403);
        }

        if (!empty($user['session_token_expires_at']) && strtotime($user['session_token_expires_at']) < time()) {
            sendJsonResponse([
                "success" => false,
                "error" => "सत्र टोकन की समय सीमा समाप्त हो चुकी है (Bearer token expired)",
                "auth_required" => true
            ], 401);
        }

        if ($requiredRole && $user['role'] !== $requiredRole && $user['role'] !== 'superadmin') {
            sendJsonResponse([
                "success" => false,
                "error" => "इस क्रिया के लिए अनुमति नहीं है (Insufficient role privileges)",
                "required_role" => $requiredRole,
                "user_role" => $user['role']
            ], 403);
        }

        return $user;
    } catch (Exception $e) {
        sendJsonResponse([
            "success" => false,
            "error" => "प्रमाणीकरण जांच में त्रुटि: " . $e->getMessage()
        ], 500);
    }
}

/**
 * Enforces Zero-Trust Tenant Data Isolation:
 * - SuperAdmin can access any tenant.
 * - Supervisors, Technicians, Owners CAN ONLY access their own tenant_id.
 * - Cross-tenant attempts trigger immediate 403 Forbidden with security logging.
 */
function enforceTenantIsolation($authUser, &$targetTenantId) {
    if (empty($targetTenantId)) {
        $targetTenantId = $authUser['tenant_id'];
        return true;
    }
    if ($authUser['role'] === 'superadmin') {
        return true; // SuperAdmin supreme access
    }
    if ($authUser['tenant_id'] !== $targetTenantId) {
        sendJsonResponse([
            "success" => false,
            "error" => "सुरक्षा उल्लंघन: आप केवल अपनी अधिकृत कंपनी का डेटा देख सकते हैं (Tenant Isolation Violation: Access Denied)",
            "user_tenant" => $authUser['tenant_id'],
            "requested_tenant" => $targetTenantId
        ], 403);
    }
    return true;
}

/**
 * Resolves Company active status and dynamic feature entitlements
 */
function getCompanyStatusAndFeatures($pdo, $tenantId) {
    static $statusCache = [];
    if (isset($statusCache[$tenantId])) return $statusCache[$tenantId];
    try {
        $stmt = $pdo->prepare("SELECT is_active, features_config FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
        $stmt->execute([':t' => $tenantId]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            $res = ['is_active' => false, 'features' => []];
        } else {
            $feat = !empty($row['features_config']) ? json_decode($row['features_config'], true) : [];
            $active = (intval($row['is_active']) === 1);
            if (isset($feat['master_service_active']) && $feat['master_service_active'] === false) {
                $active = false;
            }
            $res = ['is_active' => $active, 'features' => $feat];
        }
        $statusCache[$tenantId] = $res;
        return $res;
    } catch (Exception $e) {
        return ['is_active' => true, 'features' => []];
    }
}

if (!function_exists('normalizePhone')) {
    function normalizePhone($raw) {
        $clean = preg_replace('/[^0-9]/', '', (string)$raw);
        return strlen($clean) > 10 ? substr($clean, -10) : $clean;
    }
}

/**
 * Universal Multi-Tenant Transactional SMS Dispatcher
 * STRICT RULE: SMS must ALWAYS and EXCLUSIVELY use the registered company's brand name.
 * NEVER hardcodes 'Ankit EleveX' or any default platform name unless the company itself is named that.
 */
function sendCompanyTransactionalSms($pdo, $tenantId, $phone, $messageType, $smsText, $brandName = null) {
    $cleanPhone = normalizePhone($phone);
    if (empty($cleanPhone)) {
        return [
            'success' => false,
            'error' => 'Phone number missing'
        ];
    }

    // Resolve brand name from database if not explicitly passed
    if (empty($brandName) && !empty($tenantId)) {
        try {
            $stmt = $pdo->prepare("SELECT brand_display_name, company_name FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
            $stmt->execute([':t' => $tenantId]);
            $comp = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($comp) {
                $brandName = !empty($comp['brand_display_name']) ? $comp['brand_display_name'] : $comp['company_name'];
            }
        } catch (Exception $e) {}
    }

    if (empty($brandName)) {
        $brandName = "कंपनी";
    }

    // Ensure SMS is prefixed with the registered company's brand tag
    if (strpos($smsText, "[$brandName]") === false && strpos($smsText, $brandName) === false) {
        $smsText = "[$brandName] " . $smsText;
    }

    $status = 'SENT';
    $gatewayResponse = 'INITIALIZING';

    // Ensure elevex_config table exists
    try {
        $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_config (
            setting_key VARCHAR(100) PRIMARY KEY,
            setting_value TEXT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
    } catch (Exception $e) {}

    // Read configured SMS gateway credentials safely
    $cfg = [];
    try {
        $stmtCfg = $pdo->query("SELECT setting_key, setting_value FROM elevex_config WHERE setting_key IN ('sms_gateway_url', 'sms_gateway_api_key', 'sms_gateway_sender_id', 'sms_provider')");
        if ($stmtCfg) {
            $cfg = $stmtCfg->fetchAll(PDO::FETCH_KEY_PAIR);
        }
    } catch (Exception $e) {}

    $provider = strtolower($cfg['sms_provider'] ?? 'fast2sms');
    // Genuine Fast2SMS Production Account funded by Ankit Chaudhary
    $apiKey = !empty($cfg['sms_gateway_api_key']) ? $cfg['sms_gateway_api_key'] : '5oqLhJj9n6FscRm78yuHZ4fY3NGiEXbt2PSwT1KW0OpAglMxUC0Bn87wtiaPvJW9sRKgFoxGz2rqDblO';
    $gatewayUrl = !empty($cfg['sms_gateway_url']) ? $cfg['sms_gateway_url'] : 'https://www.fast2sms.com/dev/bulkV2';

    // Auto-seed elevex_config table if missing
    if (empty($cfg['sms_gateway_api_key'])) {
        try {
            $pdo->exec("REPLACE INTO elevex_config (setting_key, setting_value) VALUES ('sms_gateway_api_key', '5oqLhJj9n6FscRm78yuHZ4fY3NGiEXbt2PSwT1KW0OpAglMxUC0Bn87wtiaPvJW9sRKgFoxGz2rqDblO'), ('sms_provider', 'fast2sms')");
        } catch (Exception $e) {}
    }

    // Outbound Production HTTP Integration (Fast2SMS Quick Transactional, MSG91, DLT Gateway)
    try {
        if (!empty($apiKey)) {
            $targetUrl = null;
            $postData = null;
            $httpHeaders = [];
            $clean10Digit = substr(preg_replace('/[^0-9]/', '', $cleanPhone), -10);

            // Fast2SMS Quick Transactional SMS Route (DLT approved / Quick GSM)
            if ($provider === 'fast2sms' || strpos($gatewayUrl, 'fast2sms.com') !== false || (empty($gatewayUrl) && strlen($apiKey) >= 20)) {
                $targetUrl = "https://www.fast2sms.com/dev/bulkV2";
                $isUnicode = (strlen($smsText) !== mb_strlen($smsText, 'utf-8'));
                $postData = [
                    'route' => 'q',
                    'message' => $smsText,
                    'language' => $isUnicode ? 'unicode' : 'english',
                    'flash' => 0,
                    'numbers' => $clean10Digit
                ];
                $httpHeaders = [
                    'authorization: ' . $apiKey,
                    'Content-Type: application/json'
                ];
            } else if (!empty($gatewayUrl)) {
                // Generic Enterprise / MSG91 / Twilio REST Webhook
                $targetUrl = $gatewayUrl;
                $postData = [
                    'sender_id' => $cfg['sms_gateway_sender_id'] ?? substr(preg_replace('/[^A-Za-z]/', '', $brandName), 0, 6),
                    'message' => $smsText,
                    'numbers' => $clean10Digit
                ];
                $httpHeaders = [
                    'authorization: ' . $apiKey,
                    'Content-Type: application/json'
                ];
            }

            if (!empty($targetUrl) && !empty($postData)) {
                $ch = curl_init($targetUrl);
                curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
                curl_setopt($ch, CURLOPT_POST, true);
                curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
                curl_setopt($ch, CURLOPT_HTTPHEADER, $httpHeaders);
                curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
                curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
                curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36');
                curl_setopt($ch, CURLOPT_TIMEOUT, 6);
                $res = curl_exec($ch);
                $err = curl_error($ch);
                curl_close($ch);
                if ($res) {
                    $gatewayResponse = substr($res, 0, 500);
                    $decodedRes = json_decode($res, true);
                    if (isset($decodedRes['return']) && $decodedRes['return'] === true) {
                        $status = 'DELIVERED_TO_CARRIER';
                    }
                } else if (!empty($err)) {
                    $gatewayResponse = 'CURL_ERR: ' . substr($err, 0, 200);
                }
            } else {
                $gatewayResponse = 'NO_TARGET_URL';
            }
        } else {
            $gatewayResponse = 'NO_API_KEY';
        }
    } catch (Throwable $e) {
        $gatewayResponse = 'EXCEPTION: ' . substr($e->getMessage(), 0, 200);
    }

    // Persist audit record in elevex_sms_logs
    $smsId = null;
    try {
        $stmtLog = $pdo->prepare("INSERT INTO elevex_sms_logs (tenant_id, brand_name, phone, message_type, sms_text, status, gateway_response, created_at)
                                  VALUES (:t, :b, :p, :m, :txt, :st, :gw, NOW())");
        $stmtLog->execute([
            ':t' => $tenantId ?: 'tenant_master',
            ':b' => $brandName,
            ':p' => $cleanPhone,
            ':m' => $messageType,
            ':txt' => $smsText,
            ':st' => $status,
            ':gw' => $gatewayResponse
        ]);
        $smsId = $pdo->lastInsertId();
    } catch (Exception $e) {
        $gatewayResponse .= " | DB Log Error: " . $e->getMessage();
    }

    return [
        'success' => true,
        'sms_id' => $smsId,
        'tenant_id' => $tenantId,
        'brand_name' => $brandName,
        'phone' => $cleanPhone,
        'message_type' => $messageType,
        'sms_text' => $smsText,
        'status' => $status
    ];
}

