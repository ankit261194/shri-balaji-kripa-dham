<?php
// ==============================================================================
// ANKIT ELEVEX PRO - CENTRAL AMC MASTER & ROUTINE SERVICE API
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

function ensureAmcTables($pdo) {
    static $done = false;
    if ($done) return;
    
    // Check elevex_amc_contracts
    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_amc_contracts (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        contract_no VARCHAR(64) UNIQUE NOT NULL,
        building_name VARCHAR(191) NOT NULL,
        building_address TEXT NOT NULL,
        city VARCHAR(64) NOT NULL DEFAULT 'Delhi NCR',
        customer_name VARCHAR(128) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        customer_email VARCHAR(128) NULL,
        lift_count INT NOT NULL DEFAULT 1,
        amc_type ENUM('Comprehensive', 'Non-Comprehensive', 'Semi-Comprehensive') NOT NULL DEFAULT 'Non-Comprehensive',
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        annual_amount DECIMAL(12,2) NOT NULL DEFAULT 15000.00,
        payment_status ENUM('paid', 'partial', 'pending', 'overdue') NOT NULL DEFAULT 'pending',
        paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        service_frequency ENUM('monthly', 'bi_monthly', 'quarterly') NOT NULL DEFAULT 'monthly',
        services_completed INT NOT NULL DEFAULT 0,
        total_services_scheduled INT NOT NULL DEFAULT 12,
        status ENUM('active', 'expiring_soon', 'expired', 'renewed', 'terminated') NOT NULL DEFAULT 'active',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_tenant_status (tenant_id, status),
        INDEX idx_dates (start_date, end_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    // Recreate elevex_amc_schedules if contract_id is missing
    try {
        $checkStmt = $pdo->query("SHOW COLUMNS FROM elevex_amc_schedules LIKE 'contract_id'");
        $cols = $checkStmt ? $checkStmt->fetchAll(PDO::FETCH_ASSOC) : [];
        if (empty($cols)) {
            $pdo->exec("DROP TABLE IF EXISTS elevex_amc_schedules");
        }
    } catch (Exception $e) {}

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_amc_schedules (
        id INT AUTO_INCREMENT PRIMARY KEY,
        contract_id INT NOT NULL,
        tenant_id VARCHAR(64) NOT NULL,
        service_month VARCHAR(20) NOT NULL,
        scheduled_date DATE NOT NULL,
        assigned_technician_id INT NULL,
        assigned_technician_name VARCHAR(128) NULL,
        completed_date DATETIME NULL,
        service_status ENUM('scheduled', 'in_progress', 'completed', 'missed', 'rescheduled') DEFAULT 'scheduled',
        checklist_score INT DEFAULT 0,
        technician_notes TEXT NULL,
        customer_signature_url TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_contract (contract_id),
        INDEX idx_tenant_sched (tenant_id, scheduled_date),
        INDEX idx_status (service_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $done = true;
}
ensureAmcTables($pdo);

$input = getJsonInput();
$action = $_GET['action'] ?? $_POST['action'] ?? ($input['action'] ?? 'list_contracts');
$tenantId = trim($_GET['tenant_id'] ?? $_POST['tenant_id'] ?? ($input['tenant_id'] ?? ''));

// =============================================================================
// 1. LIST AMC CONTRACTS
// =============================================================================
if ($action === 'list_contracts') {
    try {
        $sql = "SELECT c.*, 
                       DATEDIFF(c.end_date, CURDATE()) AS days_left,
                       ROUND((c.services_completed / GREATEST(c.total_services_scheduled, 1)) * 100) AS progress_pct
                FROM elevex_amc_contracts c";
        $params = [];

        if (!empty($tenantId) && $tenantId !== 'all') {
            $sql .= " WHERE c.tenant_id = :tenant_id";
            $params[':tenant_id'] = $tenantId;
        }

        $sql .= " ORDER BY days_left ASC, c.id DESC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $contracts = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Compute summary metrics
        $totalContracts = count($contracts);
        $activeCount = 0;
        $expiringCount = 0;
        $expiredCount = 0;
        $totalRevenue = 0.0;
        $collectedRevenue = 0.0;

        foreach ($contracts as &$c) {
            $days = intval($c['days_left']);
            if ($days < 0) {
                $c['computed_status'] = 'expired';
                $expiredCount++;
            } elseif ($days <= 30) {
                $c['computed_status'] = 'expiring_soon';
                $expiringCount++;
            } else {
                $c['computed_status'] = 'active';
                $activeCount++;
            }
            $totalRevenue += floatval($c['annual_amount']);
            $collectedRevenue += floatval($c['paid_amount']);
        }

        sendJsonResponse([
            "success" => true,
            "total_contracts" => $totalContracts,
            "summary" => [
                "active_contracts" => $activeCount,
                "expiring_soon_30d" => $expiringCount,
                "expired_contracts" => $expiredCount,
                "total_annual_revenue" => $totalRevenue,
                "collected_revenue" => $collectedRevenue,
                "pending_revenue" => ($totalRevenue - $collectedRevenue)
            ],
            "contracts" => $contracts
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 2. EXPIRY RADAR (30 / 15 / 7 DAYS & AT-RISK CONTRACTS)
// =============================================================================
if ($action === 'get_expiry_radar') {
    try {
        $sql = "SELECT c.*, 
                       DATEDIFF(c.end_date, CURDATE()) AS days_left,
                       ROUND((c.services_completed / GREATEST(c.total_services_scheduled, 1)) * 100) AS progress_pct
                FROM elevex_amc_contracts c";
        $params = [];

        if (!empty($tenantId) && $tenantId !== 'all') {
            $sql .= " WHERE c.tenant_id = :tenant_id";
            $params[':tenant_id'] = $tenantId;
        }

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $all = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $critical7 = [];
        $warning15 = [];
        $upcoming30 = [];
        $expired = [];
        $totalAtRisk = 0.0;

        foreach ($all as $c) {
            $days = intval($c['days_left']);
            
            // Build dynamic WhatsApp Renewal Message
            $clientName = $c['customer_name'];
            $bldg = $c['building_name'];
            $cNo = $c['contract_no'];
            $endDate = date('d-m-Y', strtotime($c['end_date']));
            $amt = number_format($c['annual_amount'], 2);

            $waMsg = "नमस्कार {$clientName} जी,\n\nआपकी बिल्डिंग *{$bldg}* की लिफ्ट AMC (कॉन्ट्रैक्ट: *{$cNo}*) दिनांक *{$endDate}* को समाप्त हो रही है।\n\nनिर्बाध, सुरक्षित व सुचारू लिफ्ट संचालन हेतु कृपया तुरंत रिन्यूअल कोटेशन (₹{$amt}) स्वीकृत करें।\n\nधन्यवाद,\nअंकित एलेवेक्स लिफ्ट इंजीनियरिंग टीम\nहेल्पलाइन: 8533955333";
            $c['whatsapp_renewal_text'] = $waMsg;

            if ($days < 0) {
                $c['urgency_badge'] = 'EXPIRED';
                $expired[] = $c;
                $totalAtRisk += floatval($c['annual_amount']);
            } elseif ($days <= 7) {
                $c['urgency_badge'] = 'CRITICAL_7_DAYS';
                $critical7[] = $c;
                $totalAtRisk += floatval($c['annual_amount']);
            } elseif ($days <= 15) {
                $c['urgency_badge'] = 'WARNING_15_DAYS';
                $warning15[] = $c;
                $totalAtRisk += floatval($c['annual_amount']);
            } elseif ($days <= 30) {
                $c['urgency_badge'] = 'UPCOMING_30_DAYS';
                $upcoming30[] = $c;
                $totalAtRisk += floatval($c['annual_amount']);
            }
        }

        sendJsonResponse([
            "success" => true,
            "total_at_risk_amount" => $totalAtRisk,
            "counts" => [
                "critical_7_days" => count($critical7),
                "warning_15_days" => count($warning15),
                "upcoming_30_days" => count($upcoming30),
                "expired" => count($expired),
                "total_attention_required" => (count($critical7) + count($warning15) + count($upcoming30) + count($expired))
            ],
            "critical_7_days" => $critical7,
            "warning_15_days" => $warning15,
            "upcoming_30_days" => $upcoming30,
            "expired" => $expired
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 3. CREATE AMC CONTRACT & AUTO-GENERATE 12-MONTH VISIT CALENDAR
// =============================================================================
if ($action === 'create_contract') {
    try {
        $tId = trim($input['tenant_id'] ?? $tenantId);
        if (empty($tId)) $tId = 'tenant_master_elevex';

        $buildingName = trim($input['building_name'] ?? '');
        $buildingAddress = trim($input['building_address'] ?? '');
        $city = trim($input['city'] ?? 'Delhi NCR');
        $lat = !empty($input['latitude']) ? floatval($input['latitude']) : null;
        $lng = !empty($input['longitude']) ? floatval($input['longitude']) : null;
        $elevatorType = trim($input['elevator_type'] ?? 'PASSENGER_MRL');
        $floorsCount = max(1, intval($input['floors_count'] ?? 4));
        $customerName = trim($input['customer_name'] ?? '');
        $customerPhone = trim($input['customer_phone'] ?? '');
        $customerEmail = trim($input['customer_email'] ?? '');
        $liftCount = max(1, intval($input['lift_count'] ?? 1));
        $amcType = trim($input['amc_type'] ?? 'Non-Comprehensive');
        $startDate = trim($input['start_date'] ?? date('Y-m-d'));
        $annualAmount = floatval($input['annual_amount'] ?? 15000.00);
        $paidAmount = floatval($input['paid_amount'] ?? 0.00);
        $serviceFrequency = trim($input['service_frequency'] ?? 'monthly');

        if (empty($buildingName) || empty($customerPhone)) {
            sendJsonResponse(["success" => false, "error" => "बिल्डिंग का नाम और ग्राहक का मोबाइल नंबर अनिवार्य है!"], 400);
        }

        // Calculate end date (1 year later)
        $endDate = trim($input['end_date'] ?? date('Y-m-d', strtotime("$startDate +1 year -1 day")));

        // Generate unique Contract Number
        $randHex = strtoupper(substr(md5(uniqid(mt_rand(), true)), 0, 4));
        $contractNo = "AMC-" . date('Y', strtotime($startDate)) . "-" . $randHex;

        // Payment status
        $paymentStatus = 'pending';
        if ($paidAmount >= $annualAmount && $annualAmount > 0) {
            $paymentStatus = 'paid';
        } elseif ($paidAmount > 0) {
            $paymentStatus = 'partial';
        }

        // Calculate number of service schedules (Monthly = 12, Bi-Monthly = 6, Quarterly = 4)
        $numServices = ($serviceFrequency === 'quarterly') ? 4 : (($serviceFrequency === 'bi_monthly') ? 6 : 12);

        $stmt = $pdo->prepare("INSERT INTO elevex_amc_contracts (
            tenant_id, contract_no, building_name, building_address, city, latitude, longitude,
            elevator_type, floors_count, customer_name, customer_phone,
            customer_email, lift_count, amc_type, start_date, end_date, annual_amount, payment_status,
            paid_amount, service_frequency, services_completed, total_services_scheduled, status
        ) VALUES (
            :tenant_id, :contract_no, :building_name, :building_address, :city, :latitude, :longitude,
            :elevator_type, :floors_count, :customer_name, :customer_phone,
            :customer_email, :lift_count, :amc_type, :start_date, :end_date, :annual_amount, :payment_status,
            :paid_amount, :service_frequency, 0, :total_services, 'active'
        )");

        $stmt->execute([
            ':tenant_id' => $tId,
            ':contract_no' => $contractNo,
            ':building_name' => $buildingName,
            ':building_address' => $buildingAddress,
            ':city' => $city,
            ':latitude' => $lat,
            ':longitude' => $lng,
            ':elevator_type' => $elevatorType,
            ':floors_count' => $floorsCount,
            ':customer_name' => $customerName,
            ':customer_phone' => $customerPhone,
            ':customer_email' => $customerEmail,
            ':lift_count' => $liftCount,
            ':amc_type' => $amcType,
            ':start_date' => $startDate,
            ':end_date' => $endDate,
            ':annual_amount' => $annualAmount,
            ':payment_status' => $paymentStatus,
            ':paid_amount' => $paidAmount,
            ':service_frequency' => $serviceFrequency,
            ':total_services' => $numServices
        ]);

        $contractId = $pdo->lastInsertId();

        // Automatically populate 12 scheduled service visits
        $monthStep = ($serviceFrequency === 'quarterly') ? 3 : (($serviceFrequency === 'bi_monthly') ? 2 : 1);
        $schedStmt = $pdo->prepare("INSERT INTO elevex_amc_schedules (
            contract_id, tenant_id, service_month, scheduled_date, service_status
        ) VALUES (
            :contract_id, :tenant_id, :service_month, :scheduled_date, 'scheduled'
        )");

        for ($i = 0; $i < $numServices; $i++) {
            $offset = $i * $monthStep;
            $schedDate = date('Y-m-d', strtotime("$startDate +$offset month"));
            $monthLabel = date('M Y', strtotime($schedDate));

            $schedStmt->execute([
                ':contract_id' => $contractId,
                ':tenant_id' => $tId,
                ':service_month' => $monthLabel,
                ':scheduled_date' => $schedDate
            ]);
        }

        sendJsonResponse([
            "success" => true,
            "message" => "नया AMC कॉन्ट्रैक्ट व 12 नियमित सर्विस शेड्यूल्स सफलतापूर्वक दर्ज हो गए!",
            "contract_id" => $contractId,
            "contract_no" => $contractNo,
            "services_scheduled" => $numServices
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 4. GET AMC ROUTINE SCHEDULES (12 MONTHLY VISIT CARDS)
// =============================================================================
if ($action === 'get_schedules') {
    try {
        $contractId = intval($_GET['contract_id'] ?? ($input['contract_id'] ?? 0));
        if ($contractId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Valid contract_id required"], 400);
        }

        $stmt = $pdo->prepare("SELECT * FROM elevex_amc_schedules WHERE contract_id = :cid ORDER BY scheduled_date ASC");
        $stmt->execute([':cid' => $contractId]);
        $schedules = $stmt->fetchAll(PDO::FETCH_ASSOC);

        sendJsonResponse([
            "success" => true,
            "contract_id" => $contractId,
            "total_visits" => count($schedules),
            "schedules" => $schedules
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 5. COMPLETE ROUTINE SERVICE VISIT
// =============================================================================
if ($action === 'complete_service_visit') {
    try {
        $scheduleId = intval($input['schedule_id'] ?? 0);
        $techId = intval($input['technician_id'] ?? 0);
        $techName = trim($input['technician_name'] ?? 'अंकित एलेवेक्स तकनीशियन');
        $notes = trim($input['technician_notes'] ?? 'रूटीन मासिक सर्विस पूर्ण। सभी 28 सेफ्टी पॉइंट्स व गेट लॉक जाँचे गए।');
        $score = min(100, max(0, intval($input['checklist_score'] ?? 100)));
        $sigUrl = trim($input['customer_signature_url'] ?? '');

        if ($scheduleId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Valid schedule_id required"], 400);
        }

        $stmtFetch = $pdo->prepare("SELECT contract_id, service_status FROM elevex_amc_schedules WHERE id = :id");
        $stmtFetch->execute([':id' => $scheduleId]);
        $curr = $stmtFetch->fetch();

        if (!$curr) {
            sendJsonResponse(["success" => false, "error" => "Schedule not found"], 404);
        }

        $pdo->beginTransaction();

        $stmtUp = $pdo->prepare("UPDATE elevex_amc_schedules SET 
            service_status = 'completed',
            assigned_technician_id = :tid,
            assigned_technician_name = :tname,
            completed_date = NOW(),
            checklist_score = :score,
            technician_notes = :notes,
            customer_signature_url = :sig
            WHERE id = :id");

        $stmtUp->execute([
            ':tid' => $techId > 0 ? $techId : null,
            ':tname' => $techName,
            ':score' => $score,
            ':notes' => $notes,
            ':sig' => $sigUrl,
            ':id' => $scheduleId
        ]);

        // Increment services_completed in parent contract
        $stmtInc = $pdo->prepare("UPDATE elevex_amc_contracts SET 
            services_completed = services_completed + 1 
            WHERE id = :cid");
        $stmtInc->execute([':cid' => $curr['contract_id']]);

        $pdo->commit();

        sendJsonResponse([
            "success" => true,
            "message" => "मासिक सर्विस विजिट सफलतापूर्वक पूर्ण दर्ज हुई!"
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 6. RENEW AMC CONTRACT
// =============================================================================
if ($action === 'renew_contract') {
    try {
        $contractId = intval($input['contract_id'] ?? 0);
        $newAmount = floatval($input['new_annual_amount'] ?? 0);

        if ($contractId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Valid contract_id required"], 400);
        }

        $stmtFetch = $pdo->prepare("SELECT * FROM elevex_amc_contracts WHERE id = :id");
        $stmtFetch->execute([':id' => $contractId]);
        $c = $stmtFetch->fetch(PDO::FETCH_ASSOC);

        if (!$c) {
            sendJsonResponse(["success" => false, "error" => "Contract not found"], 404);
        }

        $oldEnd = $c['end_date'];
        $newStart = date('Y-m-d', strtotime("$oldEnd +1 day"));
        $newEnd = date('Y-m-d', strtotime("$newStart +1 year -1 day"));
        $renewalAmount = $newAmount > 0 ? $newAmount : floatval($c['annual_amount']);

        $pdo->beginTransaction();

        $stmtUp = $pdo->prepare("UPDATE elevex_amc_contracts SET 
            start_date = :s,
            end_date = :e,
            annual_amount = :amt,
            services_completed = 0,
            status = 'active',
            payment_status = 'pending',
            paid_amount = 0.00
            WHERE id = :id");

        $stmtUp->execute([
            ':s' => $newStart,
            ':e' => $newEnd,
            ':amt' => $renewalAmount,
            ':id' => $contractId
        ]);

        // Generate next 12 routine visits
        $numServices = intval($c['total_services_scheduled'] ?? 12);
        $schedStmt = $pdo->prepare("INSERT INTO elevex_amc_schedules (
            contract_id, tenant_id, service_month, scheduled_date, service_status
        ) VALUES (
            :contract_id, :tenant_id, :service_month, :scheduled_date, 'scheduled'
        )");

        for ($i = 0; $i < $numServices; $i++) {
            $schedDate = date('Y-m-d', strtotime("$newStart +$i month"));
            $monthLabel = date('M Y', strtotime($schedDate));

            $schedStmt->execute([
                ':contract_id' => $contractId,
                ':tenant_id' => $c['tenant_id'],
                ':service_month' => $monthLabel,
                ':scheduled_date' => $schedDate
            ]);
        }

        $pdo->commit();

        sendJsonResponse([
            "success" => true,
            "message" => "AMC कॉन्ट्रैक्ट 1 वर्ष के लिए सफलतापूर्वक रिन्यू हो गया!",
            "new_start_date" => $newStart,
            "new_end_date" => $newEnd
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 7. DELETE AMC CONTRACT (SUPERADMIN FULL PURGE)
// =============================================================================
if ($action === 'delete_contract') {
    try {
        $contractId = intval($input['contract_id'] ?? ($_GET['contract_id'] ?? 0));
        if ($contractId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Contract ID required"], 400);
        }

        $pdo->beginTransaction();
        $stmtSched = $pdo->prepare("DELETE FROM elevex_amc_schedules WHERE contract_id = :id");
        $stmtSched->execute([':id' => $contractId]);

        $stmtCont = $pdo->prepare("DELETE FROM elevex_amc_contracts WHERE id = :id");
        $stmtCont->execute([':id' => $contractId]);
        $pdo->commit();

        sendJsonResponse([
            "success" => true,
            "message" => "AMC अनुबंध व उसके सभी 12 सर्विस शेड्यूल सफलतापूर्वक हटा दिए गए!"
        ]);
    } catch (Exception $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

sendJsonResponse(["success" => false, "error" => "Unknown AMC action specified"], 400);
