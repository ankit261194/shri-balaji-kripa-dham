<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - DEVICE SESSION & RECHARGE LEASE ENGINE
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

$raw = file_get_contents('php://input');
$input = !empty($raw) ? json_decode($raw, true) : [];
$action = trim($_GET['action'] ?? $input['action'] ?? 'check_session');
$mobile = preg_replace('/[^0-9]/', '', $input['mobile'] ?? $_GET['mobile'] ?? '');
$deviceId = trim($input['device_id'] ?? $_GET['device_id'] ?? '');
$sessionToken = trim($input['session_token'] ?? $_GET['session_token'] ?? '');

$mobile10 = substr($mobile, -10);
$db = getDB();

// 1. Check Session Status (Heartbeat / Device Deactivation Check)
if ($action === 'check_session') {
    if (empty($mobile10) || empty($deviceId)) {
        echo json_encode(["status" => "error", "message" => "Mobile and Device ID required"]);
        exit;
    }

    $isActive = true;
    $isRevoked = false;

    if ($db) {
        try {
            $stmt = $db->prepare("SELECT is_active, session_token FROM elevex_devices WHERE user_mobile = :mob AND device_id = :did LIMIT 1");
            $stmt->execute([':mob' => $mobile10, ':did' => $deviceId]);
            $dev = $stmt->fetch();
            if ($dev) {
                if (intval($dev['is_active']) === 0) {
                    $isRevoked = true;
                    $isActive = false;
                }
            }
        } catch (Exception $e) {}
    }

    // Check subscription
    $sub = [
        "is_active" => true,
        "plan_name" => "PRO_SUBSCRIPTION",
        "days_remaining" => 30,
        "expires_at" => date('Y-m-d H:i:s', strtotime('+30 days')),
        "offline_lease_key" => hash('sha256', $mobile10 . ':' . $deviceId . ':' . date('Y-m-d', strtotime('+30 days')) . ':ELEVEX_PRO_SECRET')
    ];

    if ($db) {
        try {
            $subStmt = $db->prepare("SELECT * FROM elevex_subscriptions WHERE user_mobile = :mob AND expires_at > NOW() AND status = 'active' ORDER BY id DESC LIMIT 1");
            $subStmt->execute([':mob' => $mobile10]);
            $subRow = $subStmt->fetch();
            if ($subRow) {
                $days = max(1, (int) ceil((strtotime($subRow['expires_at']) - time()) / 86400));
                $sub = [
                    "is_active" => true,
                    "plan_name" => $subRow['plan_name'],
                    "days_remaining" => $days,
                    "expires_at" => $subRow['expires_at'],
                    "offline_lease_key" => hash('sha256', $mobile10 . ':' . $deviceId . ':' . substr($subRow['expires_at'], 0, 10) . ':ELEVEX_PRO_SECRET')
                ];
            }
        } catch (Exception $e) {}
    }

    echo json_encode([
        "status" => "success",
        "is_active" => $isActive,
        "is_revoked_by_new_device" => $isRevoked,
        "message" => $isRevoked ? "यह अकाउंट किसी अन्य नए फ़ोन पर खोला गया है। सुरक्षा कारणों से यह डिवाइस निष्क्रिय कर दिया गया है।" : "डिवाइस सत्र सक्रिय है।",
        "subscription" => $sub
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 2. Submit UPI UTR Recharge
if ($action === 'submit_recharge') {
    $utr = preg_replace('/[^0-9A-Za-z]/', '', $input['utr_number'] ?? '');
    $plan = trim($input['plan'] ?? '30_DAYS_PRO');
    $amount = intval($input['amount'] ?? 199);

    if (strlen($utr) < 8) {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "कृपया 12-अंकीय मान्य UPI UTR / Transaction ID दर्ज करें।"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    $daysToAdd = ($plan === '365_DAYS_ENTERPRISE') ? 365 : (($plan === '90_DAYS_PRO') ? 90 : 30);
    $newExpiry = date('Y-m-d H:i:s', strtotime("+{$daysToAdd} days"));

    if ($db) {
        try {
            $ins = $db->prepare("
                INSERT INTO elevex_subscriptions (user_mobile, plan_name, amount, utr_number, expires_at, status)
                VALUES (:m, :p, :a, :u, :exp, 'active')
            ");
            $ins->execute([
                ':m' => $mobile10,
                ':p' => $plan,
                ':a' => $amount,
                ':u' => $utr,
                ':exp' => $newExpiry
            ]);
        } catch (Exception $e) {}
    }

    $newLeaseKey = hash('sha256', $mobile10 . ':' . $deviceId . ':' . substr($newExpiry, 0, 10) . ':ELEVEX_PRO_SECRET');

    echo json_encode([
        "status" => "success",
        "message" => "बधाई! आपका ₹{$amount} का प्रो रिचार्ज सक्रिय हो गया है ({$daysToAdd} दिन वैध)।",
        "subscription" => [
            "is_active" => true,
            "plan_name" => $plan,
            "days_remaining" => $daysToAdd,
            "expires_at" => $newExpiry,
            "offline_lease_key" => $newLeaseKey
        ]
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode(["status" => "ready", "service" => "EleveX Device Session Engine"]);
exit;
