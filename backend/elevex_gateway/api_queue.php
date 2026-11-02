<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE ASYNCHRONOUS OUTBOX QUEUE WORKER
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// ==============================================================================

ini_set('display_errors', 1);
error_reporting(E_ALL);

require_once __DIR__ . '/config.php';

try {
    $pdo = getDbConnection();
    if (!$pdo) {
        sendJsonResponse(["success" => false, "error" => "Database connection unavailable"], 500);
    }

    $input = getJsonInput();
    $action = $_GET['action'] ?? $_POST['action'] ?? ($input['action'] ?? 'status');

    // Action 1: Enqueue Outbound Notification / Task
    if ($action === 'enqueue') {
        $authUser = verifyBearerToken($pdo);
        $tenantId = trim($input['tenant_id'] ?? $authUser['tenant_id'] ?? 'tenant_master_elevex');
        $type = strtoupper(trim($input['payload_type'] ?? $input['type'] ?? 'SMS'));
        $recipient = trim($input['recipient'] ?? $input['phone'] ?? '');
        $payloadData = $input['payload'] ?? $input['data'] ?? [];

        if (empty($recipient) || empty($payloadData)) {
            sendJsonResponse(["success" => false, "error" => "Recipient and payload data required"], 400);
        }

        $stmt = $pdo->prepare("INSERT INTO elevex_outbox_queue (tenant_id, payload_type, recipient, payload_json, status, created_at)
                               VALUES (:tid, :typ, :rec, :dat, 'PENDING', NOW())");
        $stmt->execute([
            ':tid' => $tenantId,
            ':typ' => $type,
            ':rec' => $recipient,
            ':dat' => json_encode($payloadData, JSON_UNESCAPED_UNICODE)
        ]);

        $queueId = $pdo->lastInsertId();

        sendJsonResponse([
            "success" => true,
            "message" => "संदेश सफलतापूर्वक आउटबॉक्स कतार में जोड़ दिया गया (Enqueued in outbox)",
            "queue_id" => $queueId,
            "status" => "PENDING"
        ]);
    }

    // Action 2: Drain Outbox Queue (Process Pending Tasks)
    if ($action === 'drain') {
        $limit = intval($input['limit'] ?? $_GET['limit'] ?? 10);
        $stmtPending = $pdo->prepare("SELECT id, tenant_id, payload_type, recipient, payload_json, retry_count 
                                      FROM elevex_outbox_queue 
                                      WHERE status IN ('PENDING', 'FAILED') AND retry_count < max_retries 
                                      ORDER BY id ASC LIMIT :lim");
        $stmtPending->bindValue(':lim', $limit, PDO::PARAM_INT);
        $stmtPending->execute();
        $items = $stmtPending->fetchAll(PDO::FETCH_ASSOC);

        $processedCount = 0;
        $successCount = 0;
        $failCount = 0;
        $results = [];

        foreach ($items as $item) {
            $itemId = $item['id'];
            $pdo->prepare("UPDATE elevex_outbox_queue SET status = 'PROCESSING' WHERE id = :id")->execute([':id' => $itemId]);
            $payload = json_decode($item['payload_json'], true) ?: [];
            $deliverySuccess = false;
            $deliveryError = null;

            if ($item['payload_type'] === 'SMS') {
                $smsText = $payload['message'] ?? $payload['text'] ?? '';
                $brand = $payload['brand'] ?? null;
                $smsRes = sendCompanyTransactionalSms($pdo, $item['tenant_id'], $item['recipient'], 'OUTBOX_DRAIN', $smsText, $brand);
                if (!empty($smsRes['success']) && ($smsRes['status'] === 'DELIVERED_TO_CARRIER' || $smsRes['status'] === 'SENT')) {
                    $deliverySuccess = true;
                } else {
                    $deliveryError = $smsRes['error'] ?? 'SMS delivery failure';
                }
            } else if ($item['payload_type'] === 'PUSH') {
                $title = $payload['title'] ?? 'EleveX Alert';
                $body = $payload['body'] ?? '';
                $pushRes = sendUnifiedOneSignalAlert($pdo, $item['recipient'], $title, $body, $payload['data'] ?? []);
                $deliverySuccess = $pushRes['success'] ?? true;
                if (!$deliverySuccess) $deliveryError = $pushRes['error'] ?? 'Push delivery error';
            } else if ($item['payload_type'] === 'WHATSAPP_WEBHOOK') {
                $deliverySuccess = true; // Recorded for webhook sync
            }

            if ($deliverySuccess) {
                $pdo->prepare("UPDATE elevex_outbox_queue SET status = 'DELIVERED', processed_at = NOW() WHERE id = :id")->execute([':id' => $itemId]);
                $successCount++;
            } else {
                $pdo->prepare("UPDATE elevex_outbox_queue SET status = 'FAILED', retry_count = retry_count + 1, last_error = :err, processed_at = NOW() WHERE id = :id")
                    ->execute([':id' => $itemId, ':err' => substr($deliveryError, 0, 500)]);
                $failCount++;
            }

            $processedCount++;
            $results[] = [
                'id' => $itemId,
                'type' => $item['payload_type'],
                'recipient' => $item['recipient'],
                'success' => $deliverySuccess
            ];
        }

        sendJsonResponse([
            "success" => true,
            "processed_count" => $processedCount,
            "delivered_count" => $successCount,
            "failed_count" => $failCount,
            "items" => $results
        ]);
    }

    // Action 3: Queue Status & Health Metric
    if ($action === 'status') {
        $stmtStats = $pdo->query("SELECT status, COUNT(*) as count FROM elevex_outbox_queue GROUP BY status");
        $stats = $stmtStats ? $stmtStats->fetchAll(PDO::FETCH_KEY_PAIR) : [];

        $stmtTotal = $pdo->query("SELECT COUNT(*) FROM elevex_outbox_queue");
        $total = $stmtTotal ? intval($stmtTotal->fetchColumn()) : 0;

        sendJsonResponse([
            "success" => true,
            "total_items" => $total,
            "stats" => [
                "pending" => intval($stats['PENDING'] ?? 0),
                "processing" => intval($stats['PROCESSING'] ?? 0),
                "delivered" => intval($stats['DELIVERED'] ?? 0),
                "failed" => intval($stats['FAILED'] ?? 0)
            ],
            "timestamp" => date('Y-m-d H:i:s')
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
