<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - ENTERPRISE CUSTOMER SUPPORT & HUMAN CRM ENGINE
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// ==============================================================================

require_once __DIR__ . '/config.php';

$input = getJsonInput();
$action = trim($input['action'] ?? $_GET['action'] ?? 'list');

$db = getDbConnection();

// Auto-initialize Support CRM Tables (Zero Manual SQL Setup Needed)
try {
    $db->exec("
        CREATE TABLE IF NOT EXISTS `elevex_support_tickets` (
            `id` INT AUTO_INCREMENT PRIMARY KEY,
            `ticket_number` VARCHAR(32) NOT NULL UNIQUE,
            `user_id` INT DEFAULT 0,
            `mobile_number` VARCHAR(15) NOT NULL,
            `user_name` VARCHAR(100) NOT NULL,
            `user_role` VARCHAR(50) DEFAULT 'technician',
            `category` VARCHAR(100) NOT NULL,
            `priority` ENUM('emergency', 'high', 'medium', 'low') DEFAULT 'medium',
            `status` ENUM('open', 'in_progress', 'waiting_user', 'resolved', 'closed') DEFAULT 'open',
            `subject` VARCHAR(255) NOT NULL,
            `description` TEXT NOT NULL,
            `assigned_to` VARCHAR(100) DEFAULT 'इंजीनियर अंकित चौधरी (Lead Systems Engineer)',
            `attachment_url` VARCHAR(500) DEFAULT '',
            `csat_rating` TINYINT DEFAULT NULL,
            `feedback` TEXT DEFAULT NULL,
            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
            `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            `resolved_at` DATETIME NULL,
            INDEX idx_mobile (`mobile_number`),
            INDEX idx_status (`status`),
            INDEX idx_priority (`priority`),
            INDEX idx_created (`created_at`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ");

    $db->exec("
        CREATE TABLE IF NOT EXISTS `elevex_ticket_messages` (
            `id` INT AUTO_INCREMENT PRIMARY KEY,
            `ticket_id` INT NOT NULL,
            `sender_type` ENUM('user', 'agent', 'system') NOT NULL,
            `sender_name` VARCHAR(100) NOT NULL,
            `sender_mobile` VARCHAR(15) DEFAULT '',
            `message` TEXT NOT NULL,
            `attachment_url` VARCHAR(500) DEFAULT '',
            `is_read` TINYINT(1) DEFAULT 0,
            `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_ticket (`ticket_id`),
            INDEX idx_created (`created_at`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ");
} catch (Exception $e) {
    // Tables already initialized or permission notice
}

// ------------------------------------------------------------------------------
// 1. ACTION: Create New Support Ticket
// ------------------------------------------------------------------------------
if ($action === 'create_ticket') {
    $mobile = preg_replace('/[^0-9]/', '', $input['mobile'] ?? '');
    $name = trim($input['name'] ?? 'इंजीनियर / यूज़र');
    $role = trim($input['role'] ?? 'technician');
    $category = trim($input['category'] ?? 'General Breakdown');
    $priority = strtolower(trim($input['priority'] ?? 'medium'));
    if (!in_array($priority, ['emergency', 'high', 'medium', 'low'])) {
        $priority = 'medium';
    }
    $subject = trim($input['subject'] ?? '');
    $description = trim($input['description'] ?? '');
    $attachmentUrl = trim($input['attachment_url'] ?? '');

    if (strlen($mobile) < 10) {
        sendJsonResponse(['status' => 'error', 'message' => 'मान्य 10-अंकीय मोबाइल नंबर अनिवार्य है'], 400);
    }
    if (empty($subject) || empty($description)) {
        sendJsonResponse(['status' => 'error', 'message' => 'विषय और समस्या का विवरण अनिवार्य है'], 400);
    }

    // Generate unique MNC Ticket Number: e.g. TKT-2026-8492
    $ticketNumber = 'TKT-' . date('Y') . '-' . random_int(10000, 99999);

    $stmt = $db->prepare("
        INSERT INTO elevex_support_tickets 
        (ticket_number, mobile_number, user_name, user_role, category, priority, status, subject, description, assigned_to, attachment_url)
        VALUES (?, ?, ?, ?, ?, ?, 'open', ?, ?, 'इंजीनियर अंकित चौधरी (Lead Systems Engineer)', ?)
    ");
    $stmt->execute([$ticketNumber, $mobile, $name, $role, $category, $priority, $subject, $description, $attachmentUrl]);
    $ticketId = (int)$db->lastInsertId();

    // Initial system auto-acknowledgment message
    $systemGreeting = "नमस्ते $name! आपका सपोर्ट टिकट #$ticketNumber सफलतापूर्वक दर्ज कर लिया गया है।\n\nहमारे लीड सिस्टम इंजीनियर अंकित चौधरी व टेक्निकल हेल्पडेस्क द्वारा आपकी समस्या का शीघ्र निवारण किया जाएगा।\n\nआपातकालीन सहायता हेतु आप सीधे 8533955333 पर भी संपर्क कर सकते हैं।";
    
    $stmtMsg = $db->prepare("
        INSERT INTO elevex_ticket_messages (ticket_id, sender_type, sender_name, sender_mobile, message)
        VALUES (?, 'system', 'Ankit EleveX Helpdesk Engine', '8533955333', ?)
    ");
    $stmtMsg->execute([$ticketId, $systemGreeting]);

    // Send OneSignal Admin Alert if emergency
    if ($priority === 'emergency' || $priority === 'high') {
        try {
            sendOneSignalPushNotification(
                "🚨 आपातकालीन ब्रेकडाउन टिकट: #$ticketNumber",
                "$name ($mobile) ने $category में तत्काल सहायता मांगी है: $subject",
                ['ticket_id' => $ticketId, 'ticket_number' => $ticketNumber, 'type' => 'support_ticket'],
                'admin'
            );
        } catch (Exception $e) {}
    }

    sendJsonResponse([
        'status' => 'success',
        'message' => 'सपोर्ट टिकट सफलतापूर्वक दर्ज कर लिया गया!',
        'ticket_id' => $ticketId,
        'ticket_number' => $ticketNumber,
        'assigned_to' => 'इंजीनियर अंकित चौधरी (Lead Systems Engineer)',
        'priority' => $priority
    ], 201);
}

// ------------------------------------------------------------------------------
// 2. ACTION: Get All Tickets for a Specific User
// ------------------------------------------------------------------------------
if ($action === 'get_user_tickets') {
    $mobile = preg_replace('/[^0-9]/', '', $input['mobile'] ?? $_GET['mobile'] ?? '');
    if (empty($mobile)) {
        sendJsonResponse(['status' => 'error', 'message' => 'मोबाइल नंबर अनिवार्य है'], 400);
    }

    $stmt = $db->prepare("
        SELECT t.*, 
               (SELECT message FROM elevex_ticket_messages WHERE ticket_id = t.id ORDER BY id DESC LIMIT 1) as last_message,
               (SELECT created_at FROM elevex_ticket_messages WHERE ticket_id = t.id ORDER BY id DESC LIMIT 1) as last_message_at,
               (SELECT COUNT(*) FROM elevex_ticket_messages WHERE ticket_id = t.id AND sender_type = 'agent' AND is_read = 0) as unread_count
        FROM elevex_support_tickets t
        WHERE t.mobile_number = ?
        ORDER BY t.updated_at DESC
        LIMIT 50
    ");
    $stmt->execute([$mobile]);
    $tickets = $stmt->fetchAll();

    sendJsonResponse([
        'status' => 'success',
        'count' => count($tickets),
        'tickets' => $tickets
    ]);
}

// ------------------------------------------------------------------------------
// 3. ACTION: Get Ticket Thread / Messages
// ------------------------------------------------------------------------------
if ($action === 'get_ticket_details') {
    $ticketId = intval($input['ticket_id'] ?? $_GET['ticket_id'] ?? 0);
    $ticketNumber = trim($input['ticket_number'] ?? $_GET['ticket_number'] ?? '');

    if ($ticketId <= 0 && empty($ticketNumber)) {
        sendJsonResponse(['status' => 'error', 'message' => 'ticket_id अथवा ticket_number अनिवार्य है'], 400);
    }

    if ($ticketId > 0) {
        $stmtTkt = $db->prepare("SELECT * FROM elevex_support_tickets WHERE id = ? LIMIT 1");
        $stmtTkt->execute([$ticketId]);
    } else {
        $stmtTkt = $db->prepare("SELECT * FROM elevex_support_tickets WHERE ticket_number = ? LIMIT 1");
        $stmtTkt->execute([$ticketNumber]);
    }
    $ticket = $stmtTkt->fetch();

    if (!$ticket) {
        sendJsonResponse(['status' => 'error', 'message' => 'टिकट नहीं मिला'], 404);
    }

    // Fetch messages thread
    $stmtMsg = $db->prepare("
        SELECT * FROM elevex_ticket_messages 
        WHERE ticket_id = ? 
        ORDER BY id ASC
    ");
    $stmtMsg->execute([$ticket['id']]);
    $messages = $stmtMsg->fetchAll();

    // Mark messages as read by user
    $db->prepare("UPDATE elevex_ticket_messages SET is_read = 1 WHERE ticket_id = ? AND sender_type = 'agent'")->execute([$ticket['id']]);

    sendJsonResponse([
        'status' => 'success',
        'ticket' => $ticket,
        'messages' => $messages
    ]);
}

// ------------------------------------------------------------------------------
// 4. ACTION: Send Message in Ticket
// ------------------------------------------------------------------------------
if ($action === 'send_message') {
    $ticketId = intval($input['ticket_id'] ?? 0);
    $senderType = trim($input['sender_type'] ?? 'user'); // 'user' or 'agent'
    $senderName = trim($input['sender_name'] ?? 'यूज़र');
    $senderMobile = preg_replace('/[^0-9]/', '', $input['sender_mobile'] ?? '');
    $message = trim($input['message'] ?? '');
    $attachmentUrl = trim($input['attachment_url'] ?? '');

    if ($ticketId <= 0 || empty($message)) {
        sendJsonResponse(['status' => 'error', 'message' => 'ticket_id और संदेश अनिवार्य है'], 400);
    }

    $stmtInsert = $db->prepare("
        INSERT INTO elevex_ticket_messages (ticket_id, sender_type, sender_name, sender_mobile, message, attachment_url)
        VALUES (?, ?, ?, ?, ?, ?)
    ");
    $stmtInsert->execute([$ticketId, $senderType, $senderName, $senderMobile, $message, $attachmentUrl]);
    $messageId = (int)$db->lastInsertId();

    // Update ticket updated_at and status if needed
    $newStatus = ($senderType === 'agent') ? 'waiting_user' : 'in_progress';
    $db->prepare("UPDATE elevex_support_tickets SET updated_at = NOW(), status = ? WHERE id = ? AND status != 'resolved'")->execute([$newStatus, $ticketId]);

    // Send push notification if agent replied
    if ($senderType === 'agent') {
        try {
            $stmtTkt = $db->prepare("SELECT mobile_number, ticket_number FROM elevex_support_tickets WHERE id = ?");
            $stmtTkt->execute([$ticketId]);
            $tkt = $stmtTkt->fetch();
            if ($tkt) {
                sendOneSignalPushNotification(
                    "💬 नया उत्तर: #{$tkt['ticket_number']}",
                    "इंजीनियर अंकित चौधरी: " . substr($message, 0, 90),
                    ['ticket_id' => $ticketId, 'type' => 'ticket_reply'],
                    $tkt['mobile_number']
                );
            }
        } catch (Exception $e) {}
    }

    sendJsonResponse([
        'status' => 'success',
        'message_id' => $messageId,
        'created_at' => date('Y-m-d H:i:s')
    ], 201);
}

// ------------------------------------------------------------------------------
// 5. ACTION: Resolve / Close Ticket with CSAT Rating
// ------------------------------------------------------------------------------
if ($action === 'resolve_ticket') {
    $ticketId = intval($input['ticket_id'] ?? 0);
    $rating = intval($input['rating'] ?? 5);
    $feedback = trim($input['feedback'] ?? '');

    if ($ticketId <= 0) {
        sendJsonResponse(['status' => 'error', 'message' => 'ticket_id अनिवार्य है'], 400);
    }

    $stmt = $db->prepare("
        UPDATE elevex_support_tickets 
        SET status = 'resolved', csat_rating = ?, feedback = ?, resolved_at = NOW(), updated_at = NOW() 
        WHERE id = ?
    ");
    $stmt->execute([$rating, $feedback, $ticketId]);

    // System resolution acknowledgment
    $db->prepare("
        INSERT INTO elevex_ticket_messages (ticket_id, sender_type, sender_name, message)
        VALUES (?, 'system', 'Ankit EleveX Helpdesk Engine', '✓ यह सपोर्ट टिकट सफलतापूर्वक हल (Resolved) घोषित कर दिया गया है। रेटिंग देने के लिए धन्यवाद!')
    ")->execute([$ticketId]);

    sendJsonResponse([
        'status' => 'success',
        'message' => 'टिकट सफलतापूर्वक हल कर दिया गया!'
    ]);
}

// ------------------------------------------------------------------------------
// 6. ACTION: Super Admin List All Tickets across PAN India
// ------------------------------------------------------------------------------
if ($action === 'admin_list_tickets') {
    $filterStatus = trim($input['filter_status'] ?? $_GET['filter_status'] ?? 'all');
    $filterPriority = trim($input['filter_priority'] ?? $_GET['filter_priority'] ?? 'all');

    $sql = "
        SELECT t.*, 
               (SELECT message FROM elevex_ticket_messages WHERE ticket_id = t.id ORDER BY id DESC LIMIT 1) as last_message,
               (SELECT created_at FROM elevex_ticket_messages WHERE ticket_id = t.id ORDER BY id DESC LIMIT 1) as last_message_at,
               (SELECT COUNT(*) FROM elevex_ticket_messages WHERE ticket_id = t.id AND sender_type = 'user' AND is_read = 0) as user_unread_count
        FROM elevex_support_tickets t
        WHERE 1=1
    ";
    $params = [];

    if ($filterStatus !== 'all') {
        $sql .= " AND t.status = ?";
        $params[] = $filterStatus;
    }
    if ($filterPriority !== 'all') {
        $sql .= " AND t.priority = ?";
        $params[] = $filterPriority;
    }

    $sql .= " ORDER BY FIELD(t.priority, 'emergency', 'high', 'medium', 'low'), t.updated_at DESC LIMIT 100";

    $stmt = $db->prepare($sql);
    $stmt->execute($params);
    $tickets = $stmt->fetchAll();

    sendJsonResponse([
        'status' => 'success',
        'count' => count($tickets),
        'tickets' => $tickets
    ]);
}

// Fallback
sendJsonResponse(['status' => 'error', 'message' => 'अमान्य कार्रवाई (Invalid Action)'], 400);
?>
