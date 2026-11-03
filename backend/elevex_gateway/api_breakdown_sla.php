<?php
// ==============================================================================
// ANKIT ELEVEX PRO - REAL-TIME BREAKDOWN & PASSENGER TRAP SLA COMMAND API
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// ==============================================================================

ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-EleveX-Auth");
header("Cache-Control: no-cache, no-store, must-revalidate, max-age=0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/config.php';

$pdo = getDbConnection();
if (!$pdo) {
    sendJsonResponse(["success" => false, "error" => "Database connection unavailable"], 500);
}

$input = getJsonInput();
$action = $_GET['action'] ?? $_POST['action'] ?? ($input['action'] ?? 'get_live_sla_dashboard');
$tenantId = trim($_GET['tenant_id'] ?? $_POST['tenant_id'] ?? ($input['tenant_id'] ?? ''));

// =============================================================================
// 1. LIVE SLA DASHBOARD (PASSENGER TRAP & BREAKDOWN SLA RADAR)
// =============================================================================
if ($action === 'get_live_sla_dashboard') {
    try {
        $sql = "SELECT b.*,
                       u.full_name AS tech_name,
                       u.phone AS tech_phone,
                       l.building_name AS lift_building_name,
                       l.building_address AS lift_address,
                       l.controller_brand AS lift_controller,
                       TIMESTAMPDIFF(MINUTE, b.created_at, NOW()) AS elapsed_minutes
                FROM elevex_breakdowns b
                LEFT JOIN elevex_users u ON b.assigned_technician_id = u.id
                LEFT JOIN elevex_lifts_amc l ON b.lift_id = l.id
                WHERE b.status IN ('pending', 'assigned', 'technician_enroute', 'on_site')";
        
        $params = [];
        if (!empty($tenantId) && $tenantId !== 'all') {
            $sql .= " AND b.tenant_id = :tenant_id";
            $params[':tenant_id'] = $tenantId;
        }

        $sql .= " ORDER BY (b.severity_level = 'P1_TRAP') DESC, b.created_at ASC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $tickets = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $activeTrapsCount = 0;
        $slaBreachedCount = 0;
        $onScheduleCount = 0;
        $p1Count = 0;
        $p2Count = 0;
        $p3Count = 0;

        foreach ($tickets as &$t) {
            $severity = $t['severity_level'] ?? 'P2_STOPPAGE';
            $slaMinutes = intval($t['sla_minutes'] ?? ($severity === 'P1_TRAP' ? 15 : ($severity === 'P2_STOPPAGE' ? 120 : 1440)));
            $elapsed = intval($t['elapsed_minutes'] ?? 0);
            $remaining = $slaMinutes - $elapsed;

            $isBreached = ($remaining < 0) ? 1 : 0;
            $t['sla_minutes'] = $slaMinutes;
            $t['remaining_minutes'] = $remaining;
            $t['is_sla_breached'] = $isBreached;

            if ($severity === 'P1_TRAP') {
                $p1Count++;
                if (intval($t['is_passenger_trapped'] ?? 1) == 1) {
                    $activeTrapsCount++;
                }
            } elseif ($severity === 'P2_STOPPAGE') {
                $p2Count++;
            } else {
                $p3Count++;
            }

            if ($isBreached) {
                $slaBreachedCount++;
            } else {
                $onScheduleCount++;
            }
        }

        sendJsonResponse([
            "success" => true,
            "emergency_siren_active" => ($activeTrapsCount > 0),
            "summary" => [
                "active_passenger_traps" => $activeTrapsCount,
                "total_active_breakdowns" => count($tickets),
                "sla_breached_count" => $slaBreachedCount,
                "on_schedule_count" => $onScheduleCount,
                "p1_critical_count" => $p1Count,
                "p2_stoppage_count" => $p2Count,
                "p3_minor_count" => $p3Count
            ],
            "active_tickets" => $tickets
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 2. CREATE EMERGENCY / BREAKDOWN TICKET WITH REAL-TIME SLA
// =============================================================================
if ($action === 'create_ticket') {
    try {
        $tId = trim($input['tenant_id'] ?? $tenantId);
        if (empty($tId)) $tId = 'tenant_master_elevex';

        $liftId = intval($input['lift_id'] ?? 1);
        $severity = trim($input['severity_level'] ?? 'P2_STOPPAGE');
        if (!in_array($severity, ['P1_TRAP', 'P2_STOPPAGE', 'P3_MINOR'])) {
            $severity = 'P2_STOPPAGE';
        }

        $isTrap = ($severity === 'P1_TRAP' || !empty($input['is_passenger_trapped'])) ? 1 : 0;
        $trapCount = max(0, intval($input['trapped_passenger_count'] ?? ($isTrap ? 1 : 0)));
        $issue = trim($input['issue_description'] ?? 'लिफ्ट बंद पड़ी है। त्वरित जांच आवश्यक है।');
        $floor = trim($input['emergency_floor'] ?? 'ग्राउंड फ्लोर');

        // SLA Minutes: P1 Trap = 15 Mins, P2 Stoppage = 120 Mins (2 Hours), P3 Minor = 1440 Mins (24 Hours)
        $slaMinutes = ($severity === 'P1_TRAP') ? 15 : (($severity === 'P2_STOPPAGE') ? 120 : 1440);

        // Unique Ticket Number
        $ticketNo = "BD-" . date('ymd') . "-" . strtoupper(substr(md5(uniqid(mt_rand(), true)), 0, 4));

        $stmt = $pdo->prepare("INSERT INTO elevex_breakdowns (
            tenant_id, ticket_number, lift_id, reported_by_id, status,
            severity_level, sla_minutes, is_passenger_trapped, trapped_passenger_count,
            emergency_floor, is_emergency, issue_description, emergency_reported_at
        ) VALUES (
            :tenant_id, :ticket_no, :lift_id, 1, 'pending',
            :severity, :sla_minutes, :is_trap, :trap_count,
            :floor, :is_emerg, :issue, NOW()
        )");

        $stmt->execute([
            ':tenant_id' => $tId,
            ':ticket_no' => $ticketNo,
            ':lift_id' => $liftId,
            ':severity' => $severity,
            ':sla_minutes' => $slaMinutes,
            ':is_trap' => $isTrap,
            ':trap_count' => $trapCount,
            ':floor' => $floor,
            ':is_emerg' => ($isTrap || $severity === 'P1_TRAP') ? 1 : 0,
            ':issue' => $issue
        ]);

        $ticketId = $pdo->lastInsertId();

        sendJsonResponse([
            "success" => true,
            "message" => "ब्रेकडाउन टिकट सफलतापूर्वक पंजीकृत हुआ!",
            "ticket_id" => $ticketId,
            "ticket_number" => $ticketNo,
            "severity_level" => $severity,
            "sla_minutes" => $slaMinutes,
            "is_passenger_trapped" => ($isTrap === 1)
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 3. DISPATCH TECHNICIAN
// =============================================================================
if ($action === 'dispatch_technician') {
    try {
        $ticketId = intval($input['ticket_id'] ?? 0);
        $techId = intval($input['technician_id'] ?? 0);

        if ($ticketId <= 0 || $techId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Valid ticket_id and technician_id required"], 400);
        }

        $stmt = $pdo->prepare("UPDATE elevex_breakdowns SET 
            status = 'assigned',
            assigned_technician_id = :tid,
            assigned_at = NOW(),
            dispatched_at = NOW()
            WHERE id = :id");

        $stmt->execute([
            ':tid' => $techId,
            ':id' => $ticketId
        ]);

        sendJsonResponse([
            "success" => true,
            "message" => "तकनीशियन को साइट पर सफलतापूर्वक रवाना (Dispatch) किया गया!"
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 4. RECORD SITE ARRIVAL
// =============================================================================
if ($action === 'record_arrival') {
    try {
        $ticketId = intval($input['ticket_id'] ?? 0);
        if ($ticketId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Valid ticket_id required"], 400);
        }

        $stmt = $pdo->prepare("UPDATE elevex_breakdowns SET 
            status = 'on_site',
            arrived_at = NOW()
            WHERE id = :id");
        $stmt->execute([':id' => $ticketId]);

        sendJsonResponse([
            "success" => true,
            "message" => "तकनीशियन की साइट पर पहुँच (On-Site Arrival) दर्ज हुई!"
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 5. RESOLVE BREAKDOWN & AUDIT SLA COMPLIANCE
// =============================================================================
if ($action === 'resolve_ticket') {
    try {
        $ticketId = intval($input['ticket_id'] ?? 0);
        $rootCause = trim($input['root_cause_category'] ?? 'GATE_LOCK_FAILURE');
        $notes = trim($input['resolution_notes'] ?? 'समस्या का निवारण किया गया और लिफ्ट सामान्य रूप से चालू है।');
        $photoBefore = trim($input['photo_before_url'] ?? '');
        $photoAfter = trim($input['photo_after_url'] ?? '');
        $signatureUrl = trim($input['customer_signature_url'] ?? '');

        if ($ticketId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Valid ticket_id required"], 400);
        }

        // Fetch ticket details to audit SLA
        $stmtF = $pdo->prepare("SELECT created_at, sla_minutes FROM elevex_breakdowns WHERE id = :id");
        $stmtF->execute([':id' => $ticketId]);
        $ticket = $stmtF->fetch();

        if (!$ticket) {
            sendJsonResponse(["success" => false, "error" => "Ticket not found"], 404);
        }

        $slaMins = intval($ticket['sla_minutes'] ?? 120);
        $createdTime = strtotime($ticket['created_at']);
        $nowTime = time();
        $totalElapsed = round(($nowTime - $createdTime) / 60);
        $isBreached = ($totalElapsed > $slaMins) ? 1 : 0;

        $stmtUp = $pdo->prepare("UPDATE elevex_breakdowns SET 
            status = 'resolved',
            resolved_at = NOW(),
            root_cause_category = :root_cause,
            resolution_notes = :notes,
            photo_before_url = :photo_b,
            photo_after_url = :photo_a,
            customer_signature_url = :sig,
            sla_breached = :is_breached,
            is_passenger_trapped = 0
            WHERE id = :id");

        $stmtUp->execute([
            ':root_cause' => $rootCause,
            ':notes' => $notes,
            ':photo_b' => $photoBefore,
            ':photo_a' => $photoAfter,
            ':sig' => $signatureUrl,
            ':is_breached' => $isBreached,
            ':id' => $ticketId
        ]);

        sendJsonResponse([
            "success" => true,
            "message" => "ब्रेकडाउन टिकट सफलतापूर्वक हल (Resolved) हुआ!",
            "total_elapsed_minutes" => $totalElapsed,
            "sla_limit_minutes" => $slaMins,
            "sla_breached" => ($isBreached === 1)
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 6. PASSENGER TRAPS QUICK POLL (FOR TOP SIREN BEACON)
// =============================================================================
if ($action === 'active_traps') {
    try {
        $sql = "SELECT COUNT(*) FROM elevex_breakdowns 
                WHERE status IN ('pending', 'assigned', 'technician_enroute', 'on_site')
                AND (is_passenger_trapped = 1 OR severity_level = 'P1_TRAP')";
        $stmt = $pdo->query($sql);
        $count = intval($stmt->fetchColumn());

        sendJsonResponse([
            "success" => true,
            "active_traps_count" => $count,
            "emergency_siren" => ($count > 0)
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

sendJsonResponse(["success" => false, "error" => "Unknown SLA action specified"], 400);
