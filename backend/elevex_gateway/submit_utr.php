<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - VIP RECHARGE & UTR VERIFICATION ENDPOINT
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// ==============================================================================

require_once __DIR__ . '/config.php';

$input = getJsonInput();
$action = trim($input['action'] ?? 'submit');

$db = getDbConnection();

// Auto-create table if missing
try {
    $db->exec("
        CREATE TABLE IF NOT EXISTS elevex_vip_recharges (
            id VARCHAR(64) PRIMARY KEY,
            user_id INT DEFAULT 0,
            mobile_number VARCHAR(15) NOT NULL,
            user_name VARCHAR(100) NOT NULL,
            plan_id VARCHAR(50) NOT NULL,
            plan_name VARCHAR(100) NOT NULL,
            amount DECIMAL(10,2) NOT NULL,
            utr_number VARCHAR(64) NOT NULL,
            status VARCHAR(20) DEFAULT 'pending',
            submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            verified_at DATETIME NULL,
            verified_by VARCHAR(50) NULL,
            INDEX idx_mobile (mobile_number),
            INDEX idx_utr (utr_number),
            INDEX idx_status (status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    ");
} catch (Exception $e) {}

// ACTION 1: Submit new UTR
if ($action === 'submit') {
    $utr = strtoupper(trim($input['utr_number'] ?? ''));
    $mobile = preg_replace('/[^0-9]/', '', $input['mobile'] ?? '');
    $name = trim($input['user_name'] ?? 'फील्ड टेक्नीशियन (VIP)');
    $planId = trim($input['plan_id'] ?? 'technician_monthly');
    $planName = trim($input['plan_name'] ?? '₹199 VIP प्रो (30 दिन)');
    $amount = floatval($input['amount'] ?? 199.0);
    $userId = intval($input['user_id'] ?? 0);

    if (strlen($utr) < 8) {
        sendJsonResponse([
            'status' => 'error',
            'code' => 'INVALID_UTR',
            'message' => 'कृपया कम से कम 8-12 अंकों का वैध UTR / UPI Ref No दर्ज करें।'
        ], 400);
    }

    if (strlen($mobile) < 10) {
        sendJsonResponse([
            'status' => 'error',
            'code' => 'INVALID_MOBILE',
            'message' => 'कृपया वैध 10-अंकीय मोबाइल नंबर दर्ज करें।'
        ], 400);
    }

    // Check if UTR already submitted
    $stmtCheck = $db->prepare("SELECT id, status, submitted_at FROM elevex_vip_recharges WHERE utr_number = ? LIMIT 1");
    $stmtCheck->execute([$utr]);
    $existing = $stmtCheck->fetch();

    if ($existing) {
        sendJsonResponse([
            'status' => 'success',
            'message' => 'यह UTR पहले से दर्ज है। सत्यापन की स्थिति: ' . strtoupper($existing['status']),
            'recharge_id' => $existing['id'],
            'current_status' => $existing['status'],
            'already_exists' => true
        ], 200);
    }

    $rechargeId = 'PAY-' . strtoupper(substr(uniqid(), -6));

    $stmtInsert = $db->prepare("
        INSERT INTO elevex_vip_recharges (
            id, user_id, mobile_number, user_name, plan_id, plan_name, amount, utr_number, status, submitted_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', NOW())
    ");
    $stmtInsert->execute([$rechargeId, $userId, $mobile, $name, $planId, $planName, $amount, $utr]);

    // Send Alert Notification to Admin (Ankit Chaudhary)
    $notifTitle = "💰 नया VIP रिचार्ज UTR प्राप्त!";
    $notifMsg   = "$name ($mobile) ने ₹$amount ($planName) हेतु UTR: $utr दर्ज किया।";
    
    try {
        recordNotification($db, 'admin', '8533955333', $notifTitle, $notifMsg, $rechargeId, [
            'type' => 'vip_recharge',
            'amount' => $amount,
            'utr' => $utr,
            'mobile' => $mobile
        ]);
        sendOneSignalPushNotification($notifTitle, $notifMsg, ['recharge_id' => $rechargeId, 'type' => 'vip_recharge'], 'admin');
    } catch (Exception $e) {}

    sendJsonResponse([
        'status' => 'success',
        'message' => 'UTR सफलतापूर्वक क्लाउड सर्वर पर दर्ज किया गया! सत्यापन के बाद VIP पास तुरंत सक्रिय हो जाएगा।',
        'recharge_id' => $rechargeId,
        'utr' => $utr,
        'plan_name' => $planName,
        'amount' => $amount
    ], 201);
}

// ACTION 2: Get user's recharge status
if ($action === 'get_status') {
    $mobile = preg_replace('/[^0-9]/', '', $input['mobile'] ?? '');
    if (empty($mobile)) {
        sendJsonResponse(['status' => 'error', 'message' => 'मोबाइल नंबर अनिवार्य है'], 400);
    }

    $stmt = $db->prepare("SELECT * FROM elevex_vip_recharges WHERE mobile_number = ? ORDER BY submitted_at DESC LIMIT 5");
    $stmt->execute([$mobile]);
    $records = $stmt->fetchAll();

    $isVipActive = false;
    foreach ($records as $r) {
        if ($r['status'] === 'approved') {
            $isVipActive = true;
            break;
        }
    }

    sendJsonResponse([
        'status' => 'success',
        'is_vip_active' => $isVipActive,
        'recharges' => $records
    ]);
}

// ACTION 3: List all pending UTRs for Super Admin
if ($action === 'list_all') {
    $status = trim($input['filter_status'] ?? 'pending');
    if ($status === 'all') {
        $stmt = $db->query("SELECT * FROM elevex_vip_recharges ORDER BY submitted_at DESC LIMIT 100");
    } else {
        $stmt = $db->prepare("SELECT * FROM elevex_vip_recharges WHERE status = ? ORDER BY submitted_at DESC LIMIT 100");
        $stmt->execute([$status]);
    }
    $rows = $stmt->fetchAll();
    sendJsonResponse([
        'status' => 'success',
        'count' => count($rows),
        'recharges' => $rows
    ]);
}

// ACTION 4: Approve or Reject UTR (Super Admin)
if ($action === 'verify_utr') {
    $rechargeId = trim($input['recharge_id'] ?? '');
    $newStatus = trim($input['status'] ?? 'approved'); // 'approved' or 'rejected'
    $adminName = trim($input['admin_name'] ?? 'Ankit Chaudhary');

    if (empty($rechargeId)) {
        sendJsonResponse(['status' => 'error', 'message' => 'recharge_id अनिवार्य है'], 400);
    }

    $stmt = $db->prepare("
        UPDATE elevex_vip_recharges 
        SET status = ?, verified_at = NOW(), verified_by = ? 
        WHERE id = ?
    ");
    $stmt->execute([$newStatus, $adminName, $rechargeId]);

    // Also update user profile to VIP if approved
    if ($newStatus === 'approved') {
        $stmtFetch = $db->prepare("SELECT mobile_number, plan_name, amount FROM elevex_vip_recharges WHERE id = ?");
        $stmtFetch->execute([$rechargeId]);
        $rec = $stmtFetch->fetch();
        if ($rec) {
            $uMobile = $rec['mobile_number'];
            try {
                $db->exec("ALTER TABLE elevex_users ADD COLUMN IF NOT EXISTS is_vip TINYINT(1) DEFAULT 0");
                $db->exec("ALTER TABLE elevex_users ADD COLUMN IF NOT EXISTS vip_expires_at DATETIME DEFAULT NULL");
            } catch (Exception $e) {}

            $stmtUser = $db->prepare("UPDATE elevex_users SET is_vip = 1, vip_expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE mobile_number = ?");
            $stmtUser->execute([$uMobile]);

            // Notify user
            recordNotification($db, 'technician', $uMobile, "🎉 VIP सर्विस पास सक्रिय!", "आपका ₹{$rec['amount']} ({$rec['plan_name']}) पास सक्रिय हो गया है!", $rechargeId, ['type' => 'vip_activated']);
            sendOneSignalPushNotification("🎉 VIP सर्विस पास सक्रिय!", "अंकित चौधरी द्वारा आपका VIP पास स्वीकृत व सक्रिय कर दिया गया है!", ['recharge_id' => $rechargeId], 'technician');
        }
    }

    sendJsonResponse([
        'status' => 'success',
        'message' => "UTR स्थिति को $newStatus में अपडेट कर दिया गया!",
        'recharge_id' => $rechargeId,
        'new_status' => $newStatus
    ]);
}

sendJsonResponse(['status' => 'error', 'message' => 'अमान्य एक्शन'], 400);
