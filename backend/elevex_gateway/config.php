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
