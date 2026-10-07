<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - पावन हवन व अनुष्ठान सेवा REST API
// Sacred Havan & Anushthan Devotee Application & Admin Management Service
// ==============================================================================

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} else {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-API-KEY, X-SBKD-ADMIN-TOKEN, x-sbkd-api-key, x-sbkd-admin-token");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$pdo = getDB();
if (!$pdo) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => "डेटाबेस कनेक्शन उपलब्ध नहीं है। कृपया कुछ समय बाद पुनः प्रयास करें।"], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// Auto Schema: Ensure havan_applications Table Exists
// -----------------------------------------------------------------------------
try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS havan_applications (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        application_no VARCHAR(50) NOT NULL UNIQUE,
        devotee_name VARCHAR(150) NOT NULL,
        phone_number VARCHAR(30) NOT NULL,
        whatsapp_number VARCHAR(30) DEFAULT '',
        preferred_date VARCHAR(50) NOT NULL,
        address TEXT NOT NULL,
        village_city VARCHAR(150) DEFAULT '',
        district VARCHAR(100) DEFAULT '',
        state VARCHAR(100) DEFAULT 'उत्तर प्रदेश',
        pincode VARCHAR(20) DEFAULT '',
        gotra VARCHAR(100) DEFAULT '',
        family_members_count INT DEFAULT 1,
        havan_purpose VARCHAR(255) NOT NULL,
        problem_details TEXT,
        estimated_cost INT DEFAULT 14000,
        cost_ack TINYINT(1) NOT NULL DEFAULT 1,
        travel_fare_ack TINYINT(1) NOT NULL DEFAULT 1,
        status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
        admin_notes TEXT,
        contacted_at BIGINT DEFAULT 0,
        contacted_by VARCHAR(100) DEFAULT '',
        created_at BIGINT NOT NULL,
        ip_address VARCHAR(50) DEFAULT '',
        user_device VARCHAR(150) DEFAULT '',
        INDEX idx_status (status),
        INDEX idx_phone (phone_number),
        INDEX idx_created (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
} catch (Exception $e) {
    // Ignore schema errors if already exists
}

// Ensure admins table has can_manage_havan column
try {
    $pdo->exec("ALTER TABLE admins ADD COLUMN can_manage_havan TINYINT(1) NOT NULL DEFAULT 0");
} catch (Exception $e) {}
try {
    $pdo->exec("UPDATE admins SET can_manage_havan = 1 WHERE role = 'SUPER_ADMIN' OR username = 'admin'");
} catch (Exception $e) {}

$rawInput = file_get_contents('php://input');
$input = json_decode($rawInput, true) ?: $_POST;
$action = strtoupper(trim($input['action'] ?? $_GET['action'] ?? ''));

$clientIp = $_SERVER['HTTP_CF_CONNECTING_IP'] ?? $_SERVER['HTTP_X_FORWARDED_FOR'] ?? $_SERVER['REMOTE_ADDR'] ?? '';
if (strpos($clientIp, ',') !== false) {
    $clientIp = trim(explode(',', $clientIp)[0]);
}

/**
 * Check if the current request is from an authorized admin.
 * Accepts:
 * 1. Admin PIN (default: 1234 or configured admin pin)
 * 2. Admin session token (header or parameter)
 * 3. Master API key
 */
function isHavanAdminAuthorized($input, $pdo) {
    $pin = trim($input['admin_pin'] ?? $_GET['admin_pin'] ?? $_SERVER['HTTP_X_SBKD_ADMIN_PIN'] ?? '');

    if (!empty($pin)) {
        try {
            $stmt = $pdo->prepare("SELECT id, name, username, phone_number, role, can_manage_havan FROM admins WHERE pin = :pin AND is_active = 1 LIMIT 1");
            $stmt->execute([':pin' => $pin]);
            $adm = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($adm) {
                $isSuper = ($adm['role'] === 'SUPER_ADMIN' || $adm['username'] === 'admin');
                $canHavan = !empty($adm['can_manage_havan']) || $isSuper;
                if ($canHavan) {
                    return [
                        'id' => $adm['id'],
                        'name' => $adm['name'],
                        'username' => $adm['username'],
                        'phone_number' => $adm['phone_number'],
                        'role' => $adm['role'],
                        'is_super' => $isSuper,
                        'can_manage_havan' => 1
                    ];
                } else {
                    return [
                        'access_denied' => true,
                        'name' => $adm['name'],
                        'reason' => 'हवन आवेदन देखने की अनुमति केवल सुपर एडमिन द्वारा स्वीकृत सेवादारों को ही है।'
                    ];
                }
            }
        } catch (Exception $e) {}
    }

    $adminToken = $input['admin_token'] ?? $_GET['admin_token'] ?? $_SERVER['HTTP_X_SBKD_ADMIN_TOKEN'] ?? '';
    if (!empty($adminToken)) {
        $sess = verifyAdminSessionToken($adminToken, $pdo);
        if ($sess) {
            $isSuper = ($sess['admin_role'] === 'SUPER_ADMIN');
            $canHavan = $isSuper;
            if (!$canHavan && !empty($sess['admin_id'])) {
                try {
                    $chk = $pdo->prepare("SELECT can_manage_havan FROM admins WHERE id = :id LIMIT 1");
                    $chk->execute([':id' => $sess['admin_id']]);
                    $canHavan = intval($chk->fetchColumn()) === 1;
                } catch (Exception $e) {}
            }
            if ($canHavan) {
                return [
                    'id' => $sess['admin_id'] ?? 0,
                    'name' => $sess['admin_name'],
                    'role' => $sess['admin_role'],
                    'is_super' => $isSuper,
                    'can_manage_havan' => 1
                ];
            } else {
                return [
                    'access_denied' => true,
                    'name' => $sess['admin_name'],
                    'reason' => 'हवन आवेदन देखने की अनुमति केवल सुपर एडमिन द्वारा स्वीकृत सेवादारों को ही है।'
                ];
            }
        }
    }

    $apiKey = $input['api_key'] ?? $_GET['api_key'] ?? $_SERVER['HTTP_X_SBKD_API_KEY'] ?? '';
    if (!empty($apiKey) && defined('SBKD_API_SECRET') && $apiKey === SBKD_API_SECRET) {
        return ['name' => 'सुपर एडमिन (API)', 'role' => 'SUPER_ADMIN', 'is_super' => true, 'can_manage_havan' => 1];
    }

    return false;
}

// -----------------------------------------------------------------------------
// 1. SUBMIT HAVAN APPLICATION (Public)
// -----------------------------------------------------------------------------
if ($action === 'SUBMIT' || $action === 'SUBMIT_APPLICATION' || ($_SERVER['REQUEST_METHOD'] === 'POST' && empty($action))) {
    $devoteeName = trim($input['devotee_name'] ?? '');
    $phoneNumber = preg_replace('/[^0-9]/', '', $input['phone_number'] ?? '');
    $whatsappNumber = preg_replace('/[^0-9]/', '', $input['whatsapp_number'] ?? $phoneNumber);
    $preferredDate = !empty(trim($input['preferred_date'] ?? '')) ? trim($input['preferred_date']) : 'आश्रम द्वारा तय होगी';
    $address = trim($input['address'] ?? '');
    $villageCity = trim($input['village_city'] ?? '');
    $district = trim($input['district'] ?? '');
    $state = trim($input['state'] ?? 'उत्तर प्रदेश');
    $pincode = trim($input['pincode'] ?? '');
    $gotra = trim($input['gotra'] ?? '');
    $familyMembers = intval($input['family_members_count'] ?? 1);
    $havanPurpose = !empty(trim($input['havan_purpose'] ?? '')) ? trim($input['havan_purpose']) : 'पावन हवन अनुष्ठान';
    $problemDetails = trim($input['problem_details'] ?? '');
    $costAck = (!empty($input['cost_ack']) || !empty($input['cost_acknowledged'])) ? 1 : 0;
    $travelFareAck = (!empty($input['travel_fare_ack']) || !empty($input['travel_fare_acknowledged'])) ? 1 : 0;
    $deviceInfo = substr(trim($_SERVER['HTTP_USER_AGENT'] ?? 'Devotee App'), 0, 150);

    // Validation
    if (empty($devoteeName) || mb_strlen($devoteeName) < 2) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "कृपया यजमान/भक्त का पूरा नाम दर्ज करें।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if (strlen($phoneNumber) < 10) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "कृपया 10 अंकों का मान्य मोबाइल नंबर दर्ज करें।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if (empty($address) || mb_strlen($address) < 6) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "कृपया हवन कराने का पूरा पता (ग्राम/मोहल्ला, तहसील, जिला) विस्तार से लिखें।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Fetch dynamic estimated cost from settings
    $dynamicCost = 14000;
    $dynamicRules = "हवन अनुष्ठान का अनुमानित खर्च लगभग ₹14,000 होता है। गाड़ी का आने-जाने का सम्पूर्ण किराया यजमान (भगत) को स्वयं वहन करना होगा।";
    try {
        $cStmt = $pdo->query("SELECT havan_estimated_cost, havan_rules_notice FROM ashram_settings WHERE id = 1 LIMIT 1");
        $cRow = $cStmt ? $cStmt->fetch(PDO::FETCH_ASSOC) : null;
        if ($cRow) {
            if (isset($cRow['havan_estimated_cost']) && intval($cRow['havan_estimated_cost']) > 0) {
                $dynamicCost = intval($cRow['havan_estimated_cost']);
            }
            if (!empty($cRow['havan_rules_notice'])) {
                $dynamicRules = $cRow['havan_rules_notice'];
            }
        }
    } catch (Throwable $e) {}

    // MANDATORY ACKNOWLEDGEMENT CHECK: Cost + Devotee must pay travel fare
    if ($costAck !== 1 || $travelFareAck !== 1) {
        $formattedCost = number_format($dynamicCost);
        http_response_code(400);
        echo json_encode([
            "success" => false, 
            "error" => "हवन आवेदन के लिए यह स्वीकार करना अनिवार्य है कि:\n1. हवन का अनुमानित खर्च लगभग ₹{$formattedCost} होगा।\n2. गाड़ी का आने-जाने का सम्पूर्ण किराया भगत को स्वयं देना होगा।"
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Anti-Spam: Check if same phone applied in last 3 minutes
    try {
        $recentStmt = $pdo->prepare("SELECT id, application_no FROM havan_applications WHERE phone_number = :ph AND created_at > :t LIMIT 1");
        $recentStmt->execute([
            ':ph' => $phoneNumber,
            ':t' => time() - 180
        ]);
        $existing = $recentStmt->fetch(PDO::FETCH_ASSOC);
        if ($existing) {
            echo json_encode([
                "success" => true,
                "is_duplicate" => true,
                "application_no" => $existing['application_no'],
                "message" => "आपका हवन आवेदन पहले ही प्राप्त हो चुका है (क्रमांक: {$existing['application_no']})। आश्रम व्यवस्थापक जल्द आपसे संपर्क करेंगे।"
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }
    } catch (Exception $e) {}

    // Generate unique Application Number: HVN-YYMMDD-XXXX
    $datePart = date('ymd');
    $randPart = str_pad(strval(rand(100, 9999)), 4, '0', STR_PAD_LEFT);
    $appNo = "HVN-{$datePart}-{$randPart}";

    // Ensure uniqueness
    for ($i = 0; $i < 5; $i++) {
        $chk = $pdo->prepare("SELECT COUNT(*) FROM havan_applications WHERE application_no = :no");
        $chk->execute([':no' => $appNo]);
        if ($chk->fetchColumn() == 0) break;
        $appNo = "HVN-{$datePart}-" . str_pad(strval(rand(100, 9999)), 4, '0', STR_PAD_LEFT);
    }

    $now = time();
    $insertStmt = $pdo->prepare("INSERT INTO havan_applications (
        application_no, devotee_name, phone_number, whatsapp_number, preferred_date,
        address, village_city, district, state, pincode,
        gotra, family_members_count, havan_purpose, problem_details,
        estimated_cost, cost_ack, travel_fare_ack, status,
        created_at, ip_address, user_device
    ) VALUES (
        :app_no, :name, :phone, :wa, :pref_date,
        :addr, :vc, :dist, :state, :pin,
        :gotra, :fam_cnt, :purpose, :prob,
        :est_cost, 1, 1, 'PENDING',
        :now, :ip, :device
    )");

    $inserted = $insertStmt->execute([
        ':app_no' => $appNo,
        ':name' => $devoteeName,
        ':phone' => $phoneNumber,
        ':wa' => !empty($whatsappNumber) ? $whatsappNumber : $phoneNumber,
        ':pref_date' => $preferredDate,
        ':addr' => $address,
        ':vc' => $villageCity,
        ':dist' => $district,
        ':state' => $state,
        ':pin' => $pincode,
        ':gotra' => $gotra,
        ':fam_cnt' => $familyMembers > 0 ? $familyMembers : 1,
        ':purpose' => $havanPurpose,
        ':prob' => $problemDetails,
        ':est_cost' => $dynamicCost,
        ':now' => $now,
        ':ip' => $clientIp,
        ':device' => $deviceInfo
    ]);

    if (!$inserted) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "आवेदन सुरक्षित करने में तकनीकी समस्या हुई। कृपया पुनः प्रयास करें।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    echo json_encode([
        "success" => true,
        "application_no" => $appNo,
        "devotee_name" => $devoteeName,
        "preferred_date" => $preferredDate,
        "estimated_cost" => $dynamicCost,
        "travel_fare_note" => "गाड़ी का आने-जाने का किराया यजमान द्वारा देय है।",
        "message" => "जय श्री बालाजी! आपका पावन हवन आवेदन सफलतापूर्वक दर्ज हो गया है। आवेदन क्रमांक: {$appNo}। पूज्य गुरुजी के मार्गदर्शन में आश्रम सेवा दल शीघ्र ही आपसे फोन/व्हाट्सएप पर संपर्क करेगा।"
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// PUBLIC: GET HAVAN CONFIG (Dynamic Cost & Rules)
// -----------------------------------------------------------------------------
if ($action === 'GET_CONFIG' || $action === 'GET_HAVAN_CONFIG') {
    $cost = 14000;
    $rules = "हवन अनुष्ठान का अनुमानित खर्च लगभग ₹14,000 होता है। गाड़ी का आने-जाने का सम्पूर्ण किराया यजमान (भगत) को स्वयं वहन करना होगा।";
    try {
        $cStmt = $pdo->query("SELECT havan_estimated_cost, havan_rules_notice FROM ashram_settings WHERE id = 1 LIMIT 1");
        $cRow = $cStmt ? $cStmt->fetch(PDO::FETCH_ASSOC) : null;
        if ($cRow) {
            if (isset($cRow['havan_estimated_cost']) && intval($cRow['havan_estimated_cost']) > 0) {
                $cost = intval($cRow['havan_estimated_cost']);
            }
            if (!empty($cRow['havan_rules_notice'])) {
                $rules = $cRow['havan_rules_notice'];
            }
        }
    } catch (Throwable $e) {}
    echo json_encode([
        "success" => true,
        "havan_estimated_cost" => $cost,
        "havan_rules_notice" => $rules
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit;
}

// -----------------------------------------------------------------------------
// 2. ADMIN AUTHENTICATION CHECK
// -----------------------------------------------------------------------------
$adminUser = isHavanAdminAuthorized($input, $pdo);

if ($action === 'ADMIN_LOGIN') {
    $pin = trim($input['pin'] ?? '');
    if ($pin === '1234') {
        echo json_encode([
            "success" => true,
            "admin_name" => "आश्रम मुख्य व्यवस्थापक",
            "role" => "SUPER_ADMIN",
            "is_super" => true,
            "can_manage_havan" => 1,
            "token" => "SBKD_HAVAN_" . md5(time() . "1234")
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

    try {
        $stmt = $pdo->prepare("SELECT id, name, username, phone_number, role, can_manage_havan FROM admins WHERE pin = :pin AND is_active = 1 LIMIT 1");
        $stmt->execute([':pin' => $pin]);
        $adm = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($adm) {
            $isSuper = ($adm['role'] === 'SUPER_ADMIN' || $adm['username'] === 'admin');
            $canHavan = !empty($adm['can_manage_havan']) || $isSuper;
            if (!$canHavan) {
                http_response_code(403);
                echo json_encode([
                    "success" => false,
                    "access_denied" => true,
                    "error" => "नमस्ते {$adm['name']} जी। हवन आवेदन देखने की अनुमति केवल सुपर एडमिन द्वारा स्वीकृत सेवादारों को ही है। कृपया सुपर एडमिन से अनुमति प्राप्त करें।"
                ], JSON_UNESCAPED_UNICODE);
                exit;
            }

            echo json_encode([
                "success" => true,
                "admin_name" => $adm['name'],
                "role" => $adm['role'],
                "is_super" => $isSuper,
                "can_manage_havan" => 1,
                "token" => "SBKD_HAVAN_" . md5(time() . $adm['name'])
            ], JSON_UNESCAPED_UNICODE);
            exit;
        }
    } catch (Exception $e) {}

    http_response_code(401);
    echo json_encode(["success" => false, "error" => "अमान्य एडमिन पिन। कृपया सही 4-अंकीय पिन दर्ज करें।"], JSON_UNESCAPED_UNICODE);
    exit;
}

// Check if user is authenticated but denied permission
if (is_array($adminUser) && !empty($adminUser['access_denied'])) {
    http_response_code(403);
    echo json_encode([
        "success" => false,
        "access_denied" => true,
        "error" => $adminUser['reason'] ?? "हवन आवेदन देखने की अनुमति केवल सुपर एडमिन द्वारा स्वीकृत सेवादारों को ही है।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// All actions below require admin authentication
if (!$adminUser) {
    http_response_code(401);
    echo json_encode([
        "success" => false,
        "error" => "इस सेवा हेतु एडमिन प्रमाणीकरण अनिवार्य है (मान्य PIN अथवा एडमिन टोकन प्रदान करें)।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// SAVE HAVAN CONFIG (Super Admin & Havan Admin CMS Action)
// -----------------------------------------------------------------------------
if ($action === 'SAVE_CONFIG' || $action === 'SAVE_HAVAN_CONFIG') {
    if (empty($adminUser['is_super']) && empty($adminUser['can_manage_havan'])) {
        http_response_code(403);
        echo json_encode(["success" => false, "error" => "यह अधिकार केवल सुपर एडमिन अथवा अधिकृत व्यवस्थापक को है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $cost = intval($input['havan_estimated_cost'] ?? $input['cost'] ?? 14000);
    $rules = trim($input['havan_rules_notice'] ?? $input['rules'] ?? '');

    try {
        try {
            $pdo->exec("ALTER TABLE ashram_settings ADD COLUMN havan_estimated_cost INT NOT NULL DEFAULT 14000");
        } catch (Throwable $t) {}
        try {
            $pdo->exec("ALTER TABLE ashram_settings ADD COLUMN havan_rules_notice TEXT");
        } catch (Throwable $t) {}

        $uStmt = $pdo->prepare("UPDATE ashram_settings SET havan_estimated_cost = :c, havan_rules_notice = :r, config_version = COALESCE(config_version, 1) + 1 WHERE id = 1");
        $uStmt->execute([':c' => $cost, ':r' => $rules]);

        // Invalidate cache
        $cacheDir = __DIR__ . '/../cache';
        $allCaches = glob($cacheDir . '/*');
        if ($allCaches) {
            @array_map('unlink', $allCaches);
        }

        echo json_encode([
            "success" => true,
            "message" => "हवन सेटिंग्स (अनुमानित खर्च ₹" . number_format($cost) . " व नियम) सफलतापूर्वक अपडेट व लाइव प्रसारित हुईं!",
            "havan_estimated_cost" => $cost,
            "havan_rules_notice" => $rules
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    } catch (Throwable $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "सेटिंग्स सुरक्षित करने में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 3. GET APPLICATIONS LIST (For Admin Dashboard & App)
// -----------------------------------------------------------------------------
if ($action === 'GET_APPLICATIONS' || $action === 'LIST') {
    $statusFilter = trim($input['status'] ?? $_GET['status'] ?? 'ALL');
    $search = trim($input['search'] ?? $_GET['search'] ?? '');
    $limit = intval($input['limit'] ?? $_GET['limit'] ?? 100);
    if ($limit <= 0 || $limit > 500) $limit = 100;

    $where = [];
    $params = [];

    if ($statusFilter !== 'ALL' && !empty($statusFilter)) {
        $where[] = "status = :status";
        $params[':status'] = $statusFilter;
    }

    if (!empty($search)) {
        $where[] = "(devotee_name LIKE :srch OR phone_number LIKE :srch OR application_no LIKE :srch OR address LIKE :srch OR havan_purpose LIKE :srch)";
        $params[':srch'] = '%' . $search . '%';
    }

    $whereSql = !empty($where) ? "WHERE " . implode(" AND ", $where) : "";

    try {
        // Summary Counts
        $countStmt = $pdo->query("SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
            SUM(CASE WHEN status = 'CONTACTED' THEN 1 ELSE 0 END) as contacted,
            SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as approved,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled
            FROM havan_applications");
        $counts = $countStmt ? $countStmt->fetch(PDO::FETCH_ASSOC) : [];

        // Application list
        $sql = "SELECT id, application_no, devotee_name, phone_number, whatsapp_number, preferred_date,
                       address, village_city, district, state, pincode, gotra, family_members_count,
                       havan_purpose, problem_details, estimated_cost, cost_ack, travel_fare_ack,
                       status, admin_notes, contacted_at, contacted_by, created_at
                FROM havan_applications
                {$whereSql}
                ORDER BY id DESC
                LIMIT {$limit}";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $applications = $stmt->fetchAll(PDO::FETCH_ASSOC) ?: [];

        // Format dates
        foreach ($applications as &$app) {
            $app['formatted_created_at'] = date('d-m-Y h:i A', intval($app['created_at']));
            $app['contacted_at_formatted'] = !empty($app['contacted_at']) ? date('d-m-Y h:i A', intval($app['contacted_at'])) : '';
        }

        echo json_encode([
            "success" => true,
            "counts" => [
                "total" => intval($counts['total'] ?? 0),
                "pending" => intval($counts['pending'] ?? 0),
                "contacted" => intval($counts['contacted'] ?? 0),
                "approved" => intval($counts['approved'] ?? 0),
                "completed" => intval($counts['completed'] ?? 0),
                "cancelled" => intval($counts['cancelled'] ?? 0)
            ],
            "applications" => $applications
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "आवेदन सूची प्राप्त करने में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 4. UPDATE APPLICATION STATUS & NOTES (For Admin)
// -----------------------------------------------------------------------------
if ($action === 'UPDATE_STATUS' || $action === 'UPDATE') {
    $id = intval($input['id'] ?? 0);
    $appNo = trim($input['application_no'] ?? '');
    $newStatus = strtoupper(trim($input['status'] ?? ''));
    $adminNotes = trim($input['admin_notes'] ?? '');
    $contactedBy = trim($input['contacted_by'] ?? ($adminUser['name'] ?? 'व्यवस्थापक'));

    $validStatuses = ['PENDING', 'CONTACTED', 'APPROVED', 'COMPLETED', 'CANCELLED'];
    if (!in_array($newStatus, $validStatuses)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "अमान्य स्थिति। मान्य स्थितियां: " . implode(', ', $validStatuses)], JSON_UNESCAPED_UNICODE);
        exit;
    }

    try {
        $now = time();
        $isContacted = in_array($newStatus, ['CONTACTED', 'APPROVED', 'COMPLETED']) ? $now : 0;

        if ($id > 0) {
            $upStmt = $pdo->prepare("UPDATE havan_applications 
                SET status = :st, 
                    admin_notes = :notes, 
                    contacted_at = CASE WHEN contacted_at = 0 AND :is_cont > 0 THEN :is_cont ELSE contacted_at END,
                    contacted_by = CASE WHEN (contacted_by IS NULL OR contacted_by = '') THEN :by ELSE contacted_by END
                WHERE id = :id");
            $upStmt->execute([
                ':st' => $newStatus,
                ':notes' => $adminNotes,
                ':is_cont' => $isContacted,
                ':by' => $contactedBy,
                ':id' => $id
            ]);
        } elseif (!empty($appNo)) {
            $upStmt = $pdo->prepare("UPDATE havan_applications 
                SET status = :st, 
                    admin_notes = :notes, 
                    contacted_at = CASE WHEN contacted_at = 0 AND :is_cont > 0 THEN :is_cont ELSE contacted_at END,
                    contacted_by = CASE WHEN (contacted_by IS NULL OR contacted_by = '') THEN :by ELSE contacted_by END
                WHERE application_no = :no");
            $upStmt->execute([
                ':st' => $newStatus,
                ':notes' => $adminNotes,
                ':is_cont' => $isContacted,
                ':by' => $contactedBy,
                ':no' => $appNo
            ]);
        } else {
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "आवेदन आईडी अथवा आवेदन क्रमांक अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
            exit;
        }

        echo json_encode([
            "success" => true,
            "message" => "हवन आवेदन स्थिति सफलतापूर्वक अपडेट कर दी गई है।",
            "new_status" => $newStatus
        ], JSON_UNESCAPED_UNICODE);
        exit;

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "अपडेट करने में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 5. DELETE APPLICATION (For Admin)
// -----------------------------------------------------------------------------
if ($action === 'DELETE' || $action === 'DELETE_APPLICATION') {
    $id = intval($input['id'] ?? 0);
    $appNo = trim($input['application_no'] ?? '');

    if ($id <= 0 && empty($appNo)) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "आईडी अथवा क्रमांक अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    try {
        if ($id > 0) {
            $delStmt = $pdo->prepare("DELETE FROM havan_applications WHERE id = :id");
            $delStmt->execute([':id' => $id]);
        } else {
            $delStmt = $pdo->prepare("DELETE FROM havan_applications WHERE application_no = :no");
            $delStmt->execute([':no' => $appNo]);
        }

        echo json_encode(["success" => true, "message" => "आवेदन सफलतापूर्वक हटा दिया गया है।"], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "हटाने में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 6. GET SEVADARS LIST & HAVAN ACCESS (Super Admin Exclusive)
// -----------------------------------------------------------------------------
if ($action === 'GET_SEVADAR_ACCESS' || $action === 'GET_SEVADARS') {
    if (empty($adminUser['is_super'])) {
        http_response_code(403);
        echo json_encode(["success" => false, "error" => "यह अधिकार केवल सुपर एडमिन को है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    try {
        $stmt = $pdo->query("SELECT id, name, username, phone_number, role, can_manage_havan, is_active FROM admins ORDER BY id ASC");
        $sevadars = $stmt->fetchAll(PDO::FETCH_ASSOC) ?: [];
        echo json_encode([
            "success" => true,
            "sevadars" => $sevadars
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "सेवादार सूची प्राप्त करने में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// -----------------------------------------------------------------------------
// 7. TOGGLE SEVADAR HAVAN ACCESS (Super Admin Exclusive)
// -----------------------------------------------------------------------------
if ($action === 'TOGGLE_SEVADAR_ACCESS') {
    if (empty($adminUser['is_super'])) {
        http_response_code(403);
        echo json_encode(["success" => false, "error" => "यह अधिकार केवल सुपर एडमिन को है।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $sevadarId = intval($input['sevadar_id'] ?? 0);
    $canManage = !empty($input['can_manage_havan']) ? 1 : 0;

    if ($sevadarId <= 0) {
        http_response_code(400);
        echo json_encode(["success" => false, "error" => "अमान्य सेवादार आईडी।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    try {
        $stmt = $pdo->prepare("UPDATE admins SET can_manage_havan = :cm WHERE id = :id");
        $stmt->execute([':cm' => $canManage, ':id' => $sevadarId]);

        // Get sevadar name
        $nameStmt = $pdo->prepare("SELECT name FROM admins WHERE id = :id LIMIT 1");
        $nameStmt->execute([':id' => $sevadarId]);
        $sevName = $nameStmt->fetchColumn() ?: "सेवादार";

        $statusMsg = $canManage ? "हवन आवेदन देखने की अनुमति प्रदान कर दी गई है।" : "हवन आवेदन देखने की अनुमति वापस ले ली गई है।";

        echo json_encode([
            "success" => true,
            "sevadar_id" => $sevadarId,
            "can_manage_havan" => $canManage,
            "message" => "✓ {$sevName} को {$statusMsg}"
        ], JSON_UNESCAPED_UNICODE);
        exit;
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "अनुमति अपडेट करने में त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// Fallback for unknown action
http_response_code(400);
echo json_encode(["success" => false, "error" => "अमान्य अनुरोध या कार्य (Action)।"], JSON_UNESCAPED_UNICODE);
