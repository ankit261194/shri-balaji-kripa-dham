<?php
// ==============================================================================
// ANKIT ELEVEX PRO - SPARE PARTS CATALOG, REQUISITION & QUOTATION API
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

function ensureSparePartsTables($pdo) {
    static $done = false;
    if ($done) return;

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_spare_parts (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        part_number VARCHAR(64) UNIQUE NOT NULL,
        part_name VARCHAR(191) NOT NULL,
        oem_brand VARCHAR(64) NOT NULL DEFAULT 'UNIVERSAL',
        category ENUM('Safety', 'Door_Operator', 'Traction_Drive', 'Electrical_PCB', 'Sensor_Switch', 'Mechanical_Hardware') NOT NULL DEFAULT 'Mechanical_Hardware',
        description TEXT NULL,
        cost_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
        selling_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
        stock_quantity INT NOT NULL DEFAULT 0,
        min_reorder_level INT NOT NULL DEFAULT 2,
        unit VARCHAR(32) NOT NULL DEFAULT 'Piece',
        is_active TINYINT(1) NOT NULL DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_tenant_brand (tenant_id, oem_brand)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_part_requisitions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        requisition_no VARCHAR(64) UNIQUE NOT NULL,
        ticket_id INT NULL,
        lift_id INT NULL,
        technician_id INT NOT NULL,
        technician_name VARCHAR(128) NOT NULL,
        technician_phone VARCHAR(20) NOT NULL,
        part_id INT NOT NULL,
        part_name VARCHAR(191) NOT NULL,
        requested_qty INT NOT NULL DEFAULT 1,
        urgency_level ENUM('CRITICAL_STOPPAGE', 'HIGH', 'ROUTINE') NOT NULL DEFAULT 'ROUTINE',
        site_fault_photo_url TEXT NULL,
        fault_description TEXT NOT NULL,
        status ENUM('pending_supervisor', 'approved', 'rejected', 'quoted_to_client', 'dispatched', 'installed') NOT NULL DEFAULT 'pending_supervisor',
        supervisor_id INT NULL,
        supervisor_notes TEXT NULL,
        approved_at DATETIME NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_tenant_status (tenant_id, status),
        INDEX idx_ticket (ticket_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_part_quotations (
        id INT AUTO_INCREMENT PRIMARY KEY,
        tenant_id VARCHAR(64) NOT NULL,
        quotation_no VARCHAR(64) UNIQUE NOT NULL,
        requisition_id INT NULL,
        ticket_id INT NULL,
        building_name VARCHAR(191) NOT NULL,
        customer_name VARCHAR(128) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        items_json LONGTEXT NOT NULL,
        subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        gst_rate_pct DECIMAL(5,2) NOT NULL DEFAULT 18.00,
        gst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        grand_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
        markup_pct DECIMAL(5,2) NOT NULL DEFAULT 20.00,
        approval_status ENUM('draft', 'sent_to_client', 'client_approved', 'client_rejected', 'invoiced') NOT NULL DEFAULT 'draft',
        client_approved_at DATETIME NULL,
        client_ip VARCHAR(45) NULL,
        client_notes TEXT NULL,
        whatsapp_message TEXT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        INDEX idx_tenant_quo (tenant_id, quotation_no),
        INDEX idx_status (approval_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    // Populate foundational enterprise elevator spare parts if table is empty
    $count = $pdo->query("SELECT COUNT(*) FROM elevex_spare_parts")->fetchColumn();
    if ($count == 0) {
        $seeds = [
            ['tenant_master_elevex', 'PART-MON-01', 'Monarch NICE3000+ Main Motherboard', 'MONARCH', 'Electrical_PCB', 'Original Monarch NICE3000+ Integrated Elevator Controller PCB', 18500.00, 24500.00, 5, 2, 'Piece'],
            ['tenant_master_elevex', 'PART-MON-02', 'Monarch MC-CTB Car Top Inspection Board', 'MONARCH', 'Electrical_PCB', 'Car Top Junction and Inspection Control Board', 4200.00, 5800.00, 10, 3, 'Piece'],
            ['tenant_master_elevex', 'PART-DOR-01', 'Fermator VVVF Door Operator Drive Inverter', 'UNIVERSAL', 'Door_Operator', 'Complete VVVF4+ / VVVF5 Automatic Car Door Drive', 14000.00, 18500.00, 4, 2, 'Set'],
            ['tenant_master_elevex', 'PART-DOR-02', 'Landing Door Interlock Lock Assembly with Contact', 'UNIVERSAL', 'Door_Operator', 'Heavy-Duty EN-81 Certified Landing Door Lock & Beak Mechanism', 1650.00, 2400.00, 25, 5, 'Piece'],
            ['tenant_master_elevex', 'PART-SAF-01', 'Bi-Directional Overspeed Governor Assembly (1.0 m/s)', 'UNIVERSAL', 'Safety', 'Centrifugal Overspeed Governor with Electrical Safety Switch', 9500.00, 13500.00, 3, 1, 'Set'],
            ['tenant_master_elevex', 'PART-SAF-02', 'Progressive Safety Gear Block Set (T75/T89)', 'UNIVERSAL', 'Safety', 'High-Speed Progressive Car Frame Safety Gear Wedge Block Set', 12500.00, 17000.00, 2, 1, 'Pair'],
            ['tenant_master_elevex', 'PART-SNS-01', 'Infrared Multi-Beam Door Safety Light Curtain (128 Beam)', 'UNIVERSAL', 'Sensor_Switch', 'Infrared Photoelectric Full-Height Elevator Door Sensor', 3200.00, 4800.00, 12, 4, 'Set'],
            ['tenant_master_elevex', 'PART-BAT-01', 'ARD Heavy-Duty Sealed Lead-Acid Battery Pack (48V 7Ah)', 'UNIVERSAL', 'Electrical_PCB', 'Automatic Rescue Device Emergency Power Backup Pack', 5200.00, 7200.00, 8, 2, 'Set'],
            ['tenant_master_elevex', 'PART-ROP-01', 'Traction Wire Rope 10mm (8x19S + FC Dual Strength)', 'UNIVERSAL', 'Traction_Drive', 'High-Grade Elevator Steel Hoist Rope (Per Meter)', 180.00, 280.00, 250, 50, 'Meter'],
            ['tenant_master_elevex', 'PART-ROL-01', 'High-Speed Polyurethane Roller Guide Shoes 150mm', 'UNIVERSAL', 'Mechanical_Hardware', 'Low-Vibration Spring-Loaded Guide Roller Set', 2800.00, 4000.00, 16, 4, 'Set'],
        ];

        $ins = $pdo->prepare("INSERT INTO elevex_spare_parts (tenant_id, part_number, part_name, oem_brand, category, description, cost_price, selling_price, stock_quantity, min_reorder_level, unit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        foreach ($seeds as $s) {
            $ins->execute($s);
        }
    }

    $done = true;
}
ensureSparePartsTables($pdo);

$input = getJsonInput();
$action = $_GET['action'] ?? $_POST['action'] ?? ($input['action'] ?? 'list_catalog');
$tenantId = trim($_GET['tenant_id'] ?? $_POST['tenant_id'] ?? ($input['tenant_id'] ?? ''));

// =============================================================================
// 1. LIST SPARE PARTS CATALOG & INVENTORY
// =============================================================================
if ($action === 'list_catalog') {
    try {
        $sql = "SELECT p.*,
                       CASE WHEN p.stock_quantity <= p.min_reorder_level THEN 1 ELSE 0 END AS is_low_stock
                FROM elevex_spare_parts p";
        $params = [];
        $conditions = ["p.is_active = 1"];

        if (!empty($tenantId) && $tenantId !== 'all') {
            $conditions[] = "(p.tenant_id = :tenant_id OR p.tenant_id = 'tenant_master_elevex')";
            $params[':tenant_id'] = $tenantId;
        }

        $oem = trim($_GET['oem_brand'] ?? $input['oem_brand'] ?? '');
        if (!empty($oem) && $oem !== 'ALL') {
            $conditions[] = "p.oem_brand = :oem";
            $params[':oem'] = $oem;
        }

        $cat = trim($_GET['category'] ?? $input['category'] ?? '');
        if (!empty($cat) && $cat !== 'ALL') {
            $conditions[] = "p.category = :cat";
            $params[':cat'] = $cat;
        }

        if (!empty($conditions)) {
            $sql .= " WHERE " . implode(" AND ", $conditions);
        }

        $sql .= " ORDER BY is_low_stock DESC, p.part_name ASC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $parts = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $lowStockCount = 0;
        $totalInventoryValue = 0.0;
        foreach ($parts as &$pt) {
            $cost = floatval($pt['cost_price']);
            $qty = intval($pt['stock_quantity']);
            $totalInventoryValue += ($cost * $qty);
            if ($pt['is_low_stock'] == 1) {
                $lowStockCount++;
            }
        }

        sendJsonResponse([
            "success" => true,
            "total_parts" => count($parts),
            "low_stock_count" => $lowStockCount,
            "total_inventory_value" => $totalInventoryValue,
            "parts" => $parts
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 2. ADD OR UPDATE SPARE PART
// =============================================================================
if ($action === 'add_or_update_part') {
    try {
        $tId = trim($input['tenant_id'] ?? $tenantId);
        if (empty($tId)) $tId = 'tenant_master_elevex';

        $id = intval($input['id'] ?? 0);
        $partNumber = trim($input['part_number'] ?? '');
        $partName = trim($input['part_name'] ?? '');
        $oemBrand = trim($input['oem_brand'] ?? 'UNIVERSAL');
        $category = trim($input['category'] ?? 'Mechanical_Hardware');
        $description = trim($input['description'] ?? '');
        $costPrice = floatval($input['cost_price'] ?? 0.00);
        $sellingPrice = floatval($input['selling_price'] ?? 0.00);
        $stockQty = intval($input['stock_quantity'] ?? 0);
        $minReorder = intval($input['min_reorder_level'] ?? 2);
        $unit = trim($input['unit'] ?? 'Piece');

        if (empty($partName)) {
            sendJsonResponse(["success" => false, "error" => "पार्ट का नाम अनिवार्य है!"], 400);
        }

        if (empty($partNumber)) {
            $partNumber = 'PART-' . strtoupper(substr($oemBrand, 0, 3)) . '-' . rand(1000, 9999);
        }

        if ($id > 0) {
            $stmt = $pdo->prepare("UPDATE elevex_spare_parts SET 
                part_name = :pname, oem_brand = :oem, category = :cat, description = :desc,
                cost_price = :cost, selling_price = :sell, stock_quantity = :stock,
                min_reorder_level = :min_reorder, unit = :unit
                WHERE id = :id");
            $stmt->execute([
                ':pname' => $partName,
                ':oem' => $oemBrand,
                ':cat' => $category,
                ':desc' => $description,
                ':cost' => $costPrice,
                ':sell' => $sellingPrice,
                ':stock' => $stockQty,
                ':min_reorder' => $minReorder,
                ':unit' => $unit,
                ':id' => $id
            ]);
            sendJsonResponse(["success" => true, "message" => "पार्ट सफलतापूर्वक अपडेट हुआ!", "part_id" => $id]);
        } else {
            $stmt = $pdo->prepare("INSERT INTO elevex_spare_parts 
                (tenant_id, part_number, part_name, oem_brand, category, description, cost_price, selling_price, stock_quantity, min_reorder_level, unit)
                VALUES (:tenant, :pno, :pname, :oem, :cat, :desc, :cost, :sell, :stock, :min_reorder, :unit)");
            $stmt->execute([
                ':tenant' => $tId,
                ':pno' => $partNumber,
                ':pname' => $partName,
                ':oem' => $oemBrand,
                ':cat' => $category,
                ':desc' => $description,
                ':cost' => $costPrice,
                ':sell' => $sellingPrice,
                ':stock' => $stockQty,
                ':min_reorder' => $minReorder,
                ':unit' => $unit
            ]);
            $newId = $pdo->lastInsertId();
            sendJsonResponse(["success" => true, "message" => "नया पार्ट सफलतापूर्वक कैटलॉग में दर्ज हुआ!", "part_id" => $newId, "part_number" => $partNumber]);
        }
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 3. CREATE TECHNICIAN PART REQUISITION (SITE REQUISITION)
// =============================================================================
if ($action === 'create_requisition') {
    try {
        $tId = trim($input['tenant_id'] ?? $tenantId);
        if (empty($tId)) $tId = 'tenant_master_elevex';

        $ticketId = !empty($input['ticket_id']) ? intval($input['ticket_id']) : null;
        $liftId = !empty($input['lift_id']) ? intval($input['lift_id']) : null;
        $techId = intval($input['technician_id'] ?? 1);
        $techName = trim($input['technician_name'] ?? 'Senior Technician');
        $techPhone = trim($input['technician_phone'] ?? '98XXXXXXXX');
        $partId = intval($input['part_id'] ?? 0);
        $requestedQty = max(1, intval($input['requested_qty'] ?? 1));
        $urgency = trim($input['urgency_level'] ?? 'ROUTINE');
        $faultPhotoUrl = trim($input['site_fault_photo_url'] ?? '');
        $faultDesc = trim($input['fault_description'] ?? '');

        if ($partId <= 0) {
            sendJsonResponse(["success" => false, "error" => "कृपया पार्ट का चयन करें!"], 400);
        }
        if (empty($faultDesc)) {
            sendJsonResponse(["success" => false, "error" => "खराबी का कारण व विवरण अनिवार्य है!"], 400);
        }

        // Fetch part name
        $pStmt = $pdo->prepare("SELECT part_name FROM elevex_spare_parts WHERE id = :id");
        $pStmt->execute([':id' => $partId]);
        $part = $pStmt->fetch(PDO::FETCH_ASSOC);
        if (!$part) {
            sendJsonResponse(["success" => false, "error" => "चयनित पार्ट डेटाबेस में नहीं मिला!"], 404);
        }
        $partName = $part['part_name'];

        $reqNo = 'REQ-' . date('Ymd') . '-' . rand(1000, 9999);

        $stmt = $pdo->prepare("INSERT INTO elevex_part_requisitions 
            (tenant_id, requisition_no, ticket_id, lift_id, technician_id, technician_name, technician_phone, part_id, part_name, requested_qty, urgency_level, site_fault_photo_url, fault_description, status)
            VALUES (:tenant, :rno, :ticket, :lift, :tech_id, :tech_name, :tech_phone, :part_id, :part_name, :qty, :urgency, :photo, :desc, 'pending_supervisor')");
        $stmt->execute([
            ':tenant' => $tId,
            ':rno' => $reqNo,
            ':ticket' => $ticketId,
            ':lift' => $liftId,
            ':tech_id' => $techId,
            ':tech_name' => $techName,
            ':tech_phone' => $techPhone,
            ':part_id' => $partId,
            ':part_name' => $partName,
            ':qty' => $requestedQty,
            ':urgency' => $urgency,
            ':photo' => $faultPhotoUrl,
            ':desc' => $faultDesc
        ]);

        $newReqId = $pdo->lastInsertId();
        sendJsonResponse([
            "success" => true,
            "message" => "पार्ट मांग पत्र (Requisition) सुपरवाइज़र अनुमोदन हेतु प्रेषित हुआ!",
            "requisition_id" => $newReqId,
            "requisition_no" => $reqNo
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 4. LIST PART REQUISITIONS
// =============================================================================
if ($action === 'list_requisitions') {
    try {
        $sql = "SELECT r.*, p.stock_quantity AS current_stock, p.unit, p.selling_price
                FROM elevex_part_requisitions r
                LEFT JOIN elevex_spare_parts p ON r.part_id = p.id";
        $params = [];
        $conditions = [];

        if (!empty($tenantId) && $tenantId !== 'all') {
            $conditions[] = "(r.tenant_id = :tenant_id OR r.tenant_id = 'tenant_master_elevex')";
            $params[':tenant_id'] = $tenantId;
        }

        $status = trim($_GET['status'] ?? $input['status'] ?? '');
        if (!empty($status) && $status !== 'all') {
            $conditions[] = "r.status = :status";
            $params[':status'] = $status;
        }

        if (!empty($conditions)) {
            $sql .= " WHERE " . implode(" AND ", $conditions);
        }

        $sql .= " ORDER BY r.id DESC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $requisitions = $stmt->fetchAll(PDO::FETCH_ASSOC);

        sendJsonResponse([
            "success" => true,
            "count" => count($requisitions),
            "requisitions" => $requisitions
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 5. APPROVE OR REJECT REQUISITION (SUPERVISOR ACTION)
// =============================================================================
if ($action === 'approve_requisition') {
    try {
        $reqId = intval($input['requisition_id'] ?? 0);
        $status = trim($input['status'] ?? 'approved'); // approved or rejected
        $notes = trim($input['supervisor_notes'] ?? '');
        $supId = intval($input['supervisor_id'] ?? 1);

        if ($reqId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Requisition ID is required"], 400);
        }

        $stmt = $pdo->prepare("UPDATE elevex_part_requisitions SET
            status = :status,
            supervisor_id = :sup_id,
            supervisor_notes = :notes,
            approved_at = NOW()
            WHERE id = :id");
        $stmt->execute([
            ':status' => $status,
            ':sup_id' => $supId,
            ':notes' => $notes,
            ':id' => $reqId
        ]);

        sendJsonResponse([
            "success" => true,
            "message" => ($status === 'approved') ? "पार्ट मांग पत्र अनुमोदित हुआ!" : "पार्ट मांग पत्र अस्वीकार किया गया।"
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 6. CREATE CLIENT QUOTATION WITH AUTOMATED MARKUP & GST
// =============================================================================
if ($action === 'create_quotation') {
    try {
        $tId = trim($input['tenant_id'] ?? $tenantId);
        if (empty($tId)) $tId = 'tenant_master_elevex';

        $reqId = !empty($input['requisition_id']) ? intval($input['requisition_id']) : null;
        $ticketId = !empty($input['ticket_id']) ? intval($input['ticket_id']) : null;
        $buildingName = trim($input['building_name'] ?? '');
        $customerName = trim($input['customer_name'] ?? '');
        $customerPhone = trim($input['customer_phone'] ?? '');
        $items = $input['items'] ?? [];
        $markupPct = floatval($input['markup_pct'] ?? 20.00);
        $gstRate = floatval($input['gst_rate_pct'] ?? 18.00);

        if (empty($buildingName) || empty($customerPhone)) {
            sendJsonResponse(["success" => false, "error" => "बिल्डिंग का नाम और ग्राहक का फोन नंबर अनिवार्य है!"], 400);
        }
        if (empty($items) || !is_array($items)) {
            sendJsonResponse(["success" => false, "error" => "कोटेशन में कम से कम 1 पार्ट होना आवश्यक है!"], 400);
        }

        // Calculate subtotal
        $subtotal = 0.0;
        $processedItems = [];
        foreach ($items as $item) {
            $pName = trim($item['part_name'] ?? 'Elevator Spare Part');
            $qty = max(1, intval($item['qty'] ?? 1));
            $unitCost = floatval($item['unit_price'] ?? 0.0);
            $sellingPrice = round($unitCost * (1 + ($markupPct / 100)), 2);
            $lineTotal = round($sellingPrice * $qty, 2);
            $subtotal += $lineTotal;

            $processedItems[] = [
                'part_id' => $item['part_id'] ?? null,
                'part_name' => $pName,
                'qty' => $qty,
                'unit_price' => $sellingPrice,
                'line_total' => $lineTotal
            ];
        }

        $gstAmount = round($subtotal * ($gstRate / 100), 2);
        $grandTotal = round($subtotal + $gstAmount, 2);
        $quoNo = 'QUO-' . date('Ymd') . '-' . rand(1000, 9999);

        // Pre-compose WhatsApp Quotation Message
        $waMsg = "🏢 *{$buildingName}* - स्पेयर पार्ट्स कोटेशन ({$quoNo})\n";
        $waMsg .= "नमस्ते {$customerName} जी,\nआपके लिफ्ट के रखरखाव हेतु निम्नलिखित पार्ट्स की आवश्यकता है:\n\n";
        foreach ($processedItems as $pi) {
            $waMsg .= "• {$pi['part_name']} x {$pi['qty']} = ₹{$pi['line_total']}\n";
        }
        $waMsg .= "\nउप-योग: ₹{$subtotal}\nGST ({$gstRate}%): ₹{$gstAmount}\n*कुल देय राशि: ₹{$grandTotal}*\n\n";
        $waMsg .= "✅ 1-क्लिक ऑनलाइन अनुमोदन:\nhttps://shribalajikripadham.online/portal.php?quote={$quoNo}\n";
        $waMsg .= "━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        $stmt = $pdo->prepare("INSERT INTO elevex_part_quotations 
            (tenant_id, quotation_no, requisition_id, ticket_id, building_name, customer_name, customer_phone, items_json, subtotal, gst_rate_pct, gst_amount, grand_total, markup_pct, approval_status, whatsapp_message)
            VALUES (:tenant, :qno, :req_id, :ticket, :bname, :cname, :cphone, :items, :subtotal, :gst_rate, :gst_amt, :grand, :markup, 'sent_to_client', :wamsg)");
        $stmt->execute([
            ':tenant' => $tId,
            ':qno' => $quoNo,
            ':req_id' => $reqId,
            ':ticket' => $ticketId,
            ':bname' => $buildingName,
            ':cname' => $customerName,
            ':cphone' => $customerPhone,
            ':items' => json_encode($processedItems),
            ':subtotal' => $subtotal,
            ':gst_rate' => $gstRate,
            ':gst_amt' => $gstAmount,
            ':grand' => $grandTotal,
            ':markup' => $markupPct,
            ':wamsg' => $waMsg
        ]);

        $quoId = $pdo->lastInsertId();

        // If linked to requisition, update requisition status
        if ($reqId) {
            $pdo->exec("UPDATE elevex_part_requisitions SET status = 'quoted_to_client' WHERE id = $reqId");
        }

        sendJsonResponse([
            "success" => true,
            "message" => "कोटेशन तैयार हुआ व WhatsApp अनुमोदन संदेश सृजित किया गया!",
            "quotation_id" => $quoId,
            "quotation_no" => $quoNo,
            "subtotal" => $subtotal,
            "gst_amount" => $gstAmount,
            "grand_total" => $grandTotal,
            "whatsapp_message" => $waMsg,
            "whatsapp_url" => "https://wa.me/91" . preg_replace('/[^0-9]/', '', $customerPhone) . "?text=" . urlencode($waMsg)
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 7. LIST QUOTATIONS
// =============================================================================
if ($action === 'list_quotations') {
    try {
        $sql = "SELECT q.* FROM elevex_part_quotations q";
        $params = [];
        $conditions = [];

        if (!empty($tenantId) && $tenantId !== 'all') {
            $conditions[] = "(q.tenant_id = :tenant_id OR q.tenant_id = 'tenant_master_elevex')";
            $params[':tenant_id'] = $tenantId;
        }

        $status = trim($_GET['status'] ?? $input['status'] ?? '');
        if (!empty($status) && $status !== 'all') {
            $conditions[] = "q.approval_status = :status";
            $params[':status'] = $status;
        }

        if (!empty($conditions)) {
            $sql .= " WHERE " . implode(" AND ", $conditions);
        }

        $sql .= " ORDER BY q.id DESC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $quotations = $stmt->fetchAll(PDO::FETCH_ASSOC);

        sendJsonResponse([
            "success" => true,
            "count" => count($quotations),
            "quotations" => $quotations
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 8. CLIENT RESPOND QUOTATION (1-CLICK CLIENT APPROVAL VIA PORTAL / WHATSAPP)
// =============================================================================
if ($action === 'client_respond_quote') {
    try {
        $quoNo = trim($input['quotation_no'] ?? $_GET['quotation_no'] ?? '');
        $status = trim($input['approval_status'] ?? 'client_approved'); // client_approved or client_rejected
        $notes = trim($input['client_notes'] ?? '');
        $ip = $_SERVER['REMOTE_ADDR'] ?? '';

        if (empty($quoNo)) {
            sendJsonResponse(["success" => false, "error" => "Quotation number is required"], 400);
        }

        $stmt = $pdo->prepare("UPDATE elevex_part_quotations SET
            approval_status = :status,
            client_approved_at = NOW(),
            client_ip = :ip,
            client_notes = :notes
            WHERE quotation_no = :qno");
        $stmt->execute([
            ':status' => $status,
            ':ip' => $ip,
            ':notes' => $notes,
            ':qno' => $quoNo
        ]);

        sendJsonResponse([
            "success" => true,
            "message" => ($status === 'client_approved') ? "कोटेशन ग्राहक द्वारा सफलतापूर्वक स्वीकृत (Approved) हुआ!" : "कोटेशन ग्राहक द्वारा अस्वीकार (Rejected) किया गया।"
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

// =============================================================================
// 9. DELETE SPARE PART (SUPERADMIN ACTION)
// =============================================================================
if ($action === 'delete_part') {
    try {
        $partId = intval($input['part_id'] ?? ($_GET['part_id'] ?? 0));
        if ($partId <= 0) {
            sendJsonResponse(["success" => false, "error" => "Part ID required"], 400);
        }

        $stmt = $pdo->prepare("DELETE FROM elevex_spare_parts WHERE id = :id");
        $stmt->execute([':id' => $partId]);

        sendJsonResponse([
            "success" => true,
            "message" => "स्पेयर पार्ट कैटलॉग से सफलतापूर्वक हटा दिया गया!"
        ]);
    } catch (Exception $e) {
        sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
    }
}

sendJsonResponse(["success" => false, "error" => "Unknown spare parts action specified"], 400);
