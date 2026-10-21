<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - REPAIR LEADS & REAL-TIME DISPATCH API
// Conceived & Engineered by Lead Engineer Ankit Chaudhary (8533955333)
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-EleveX-Auth, X-Requested-With");
header("Cache-Control: no-cache, no-store, must-revalidate");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/../config/db.php';

$raw = file_get_contents('php://input');
$input = !empty($raw) ? json_decode($raw, true) : [];
$action = trim($input['action'] ?? $_POST['action'] ?? $_GET['action'] ?? 'get_nearby_repair_leads');

$db = getDB();

if ($db) {
    try {
        $db->exec("
            CREATE TABLE IF NOT EXISTS elevex_repair_leads (
                id INT AUTO_INCREMENT PRIMARY KEY,
                lead_ref VARCHAR(64) UNIQUE NOT NULL,
                owner_name VARCHAR(100) NOT NULL,
                owner_phone VARCHAR(20) NOT NULL,
                building_name VARCHAR(150) NOT NULL,
                address TEXT NOT NULL,
                city VARCHAR(100) NOT NULL,
                pincode VARCHAR(10) NOT NULL,
                lift_brand VARCHAR(50) NOT NULL,
                issue_category VARCHAR(100) NOT NULL,
                error_description TEXT NOT NULL,
                error_code VARCHAR(50) DEFAULT '',
                priority VARCHAR(50) DEFAULT 'Normal',
                fault_image_url VARCHAR(255) DEFAULT '',
                status VARCHAR(50) DEFAULT 'pending',
                tech_user_id INT DEFAULT 0,
                tech_name VARCHAR(100) DEFAULT '',
                tech_phone VARCHAR(20) DEFAULT '',
                completion_otp VARCHAR(10) DEFAULT '',
                service_notes TEXT DEFAULT NULL,
                total_amount DECIMAL(10,2) DEFAULT 0.00,
                completed_at DATETIME DEFAULT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_city (city),
                INDEX idx_owner_phone (owner_phone),
                INDEX idx_tech_phone (tech_phone),
                INDEX idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

            CREATE TABLE IF NOT EXISTS elevex_registered_elevators (
                id INT AUTO_INCREMENT PRIMARY KEY,
                lift_ref VARCHAR(64) UNIQUE NOT NULL,
                owner_phone VARCHAR(20) NOT NULL,
                owner_name VARCHAR(100) NOT NULL,
                building_name VARCHAR(150) NOT NULL,
                address TEXT NOT NULL,
                city VARCHAR(100) NOT NULL,
                pincode VARCHAR(10) NOT NULL,
                lift_brand VARCHAR(50) NOT NULL,
                controller_type VARCHAR(50) DEFAULT 'Universal',
                stops_count INT DEFAULT 4,
                capacity_persons INT DEFAULT 6,
                installation_year VARCHAR(10) DEFAULT '',
                status VARCHAR(50) DEFAULT 'active',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_owner_phone (owner_phone)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

            CREATE TABLE IF NOT EXISTS elevex_notifications (
                id INT AUTO_INCREMENT PRIMARY KEY,
                role VARCHAR(50) NOT NULL,
                phone VARCHAR(20) NOT NULL,
                title VARCHAR(150) NOT NULL,
                message TEXT NOT NULL,
                lead_id INT DEFAULT 0,
                is_read TINYINT DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_phone (phone),
                INDEX idx_role (role)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        ");
    } catch (Exception $e) {}
}

// -----------------------------------------------------------------------------
// 1. GET NEARBY REPAIR LEADS (For Field Technicians)
// -----------------------------------------------------------------------------
if ($action === 'get_nearby_repair_leads') {
    $city = trim($input['city'] ?? $_POST['city'] ?? $_GET['city'] ?? '');
    $leads = [];
    if ($db) {
        try {
            if (!empty($city)) {
                $stmt = $db->prepare("SELECT * FROM elevex_repair_leads WHERE city = :ct AND status IN ('pending', 'open', 'accepted', 'accepted_by_tech', 'in_transit', 'arrived_on_site') ORDER BY id DESC LIMIT 50");
                $stmt->execute([':ct' => $city]);
            } else {
                $stmt = $db->query("SELECT * FROM elevex_repair_leads WHERE status IN ('pending', 'open', 'accepted', 'accepted_by_tech', 'in_transit', 'arrived_on_site') ORDER BY id DESC LIMIT 50");
            }
            $leads = $stmt->fetchAll(PDO::FETCH_ASSOC);
        } catch (Exception $e) {}
    }
    echo json_encode(["status" => "success", "leads" => $leads ?: []], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// 2. GET OWNER LEADS (For Building Owners to view their breakdown history)
// -----------------------------------------------------------------------------
if ($action === 'get_owner_leads') {
    $ownerPhone = trim($input['owner_phone'] ?? $_POST['owner_phone'] ?? $_GET['owner_phone'] ?? '');
    $cleanPhone = preg_replace('/[^0-9]/', '', $ownerPhone);
    $leads = [];
    if ($db && !empty($cleanPhone)) {
        try {
            $stmt = $db->prepare("SELECT * FROM elevex_repair_leads WHERE owner_phone = :op OR owner_phone LIKE :op_wild ORDER BY id DESC LIMIT 50");
            $stmt->execute([
                ':op' => $cleanPhone,
                ':op_wild' => "%$cleanPhone%"
            ]);
            $leads = $stmt->fetchAll(PDO::FETCH_ASSOC);
        } catch (Exception $e) {}
    }
    echo json_encode(["status" => "success", "leads" => $leads ?: []], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// 3. CREATE REPAIR LEAD (Owner Reports a Breakdown)
// -----------------------------------------------------------------------------
if ($action === 'create_repair_lead') {
    $leadRef = 'ELV-' . strtoupper(substr(md5(uniqid(rand(), true)), 0, 8));
    $ownerName = trim($input['owner_name'] ?? '');
    $ownerPhone = preg_replace('/[^0-9]/', '', trim($input['owner_phone'] ?? ''));
    $buildingName = trim($input['building_name'] ?? '');
    $address = trim($input['address'] ?? '');
    $city = trim($input['city'] ?? '');
    $pincode = trim($input['pincode'] ?? '');
    $liftBrand = trim($input['lift_brand'] ?? 'Universal');
    $issueCat = trim($input['issue_category'] ?? 'General Breakdown');
    $errDesc = trim($input['error_description'] ?? '');
    $errCode = trim($input['error_code'] ?? '');
    $priority = trim($input['priority'] ?? 'Emergency SOS');
    $imageUrl = trim($input['fault_image_url'] ?? '');
    $completionOtp = sprintf('%04d', rand(1000, 9999));

    if (empty($ownerPhone) || empty($buildingName)) {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "ओनर मोबाइल नंबर व बिल्डिंग का नाम अनिवार्य है"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($db) {
        try {
            $stmt = $db->prepare("
                INSERT INTO elevex_repair_leads 
                (lead_ref, owner_name, owner_phone, building_name, address, city, pincode, lift_brand, issue_category, error_description, error_code, priority, fault_image_url, status, completion_otp)
                VALUES 
                (:ref, :on, :op, :bn, :ad, :ct, :pin, :lb, :ic, :ed, :ec, :pr, :img, 'pending', :otp)
            ");
            $stmt->execute([
                ':ref' => $leadRef,
                ':on' => $ownerName,
                ':op' => $ownerPhone,
                ':bn' => $buildingName,
                ':ad' => $address,
                ':ct' => $city,
                ':pin' => $pincode,
                ':lb' => $liftBrand,
                ':ic' => $issueCat,
                ':ed' => $errDesc,
                ':ec' => $errCode,
                ':pr' => $priority,
                ':img' => $imageUrl,
                ':otp' => $completionOtp
            ]);
            $leadId = $db->lastInsertId();

            // Broadcast in-app notification to all field technicians in that city
            try {
                $notifStmt = $db->prepare("
                    INSERT INTO elevex_notifications (role, phone, title, message, lead_id)
                    VALUES ('technician', 'BROADCAST', :title, :msg, :lid)
                ");
                $notifStmt->execute([
                    ':title' => "🚨 नई लिफ्ट ब्रेकडाउन: $buildingName ($city)",
                    ':msg' => "ब्रांड: $liftBrand | समस्या: $issueCat. तुरंत स्वीकार करें!",
                    ':lid' => (int)$leadId
                ]);
            } catch (Exception $ne) {}

            http_response_code(201);
            echo json_encode([
                "status" => "success", 
                "message" => "ब्रेकडाउन टिकट दर्ज हुआ! नजदीकी प्रमाणित इंजीनियरों को अलर्ट भेज दिया गया है।", 
                "lead_id" => (int)$leadId, 
                "lead_ref" => $leadRef,
                "completion_otp" => $completionOtp
            ], JSON_UNESCAPED_UNICODE);
            exit;
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["status" => "error", "message" => "डेटाबेस एरर: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }

    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "सर्वर डेटाबेस अनुपलब्ध है"], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// 4. UPDATE TECH STATUS (Accept, In-Transit, Arrived on Site)
// -----------------------------------------------------------------------------
if ($action === 'update_tech_status') {
    $leadId = trim($input['lead_id'] ?? '');
    $status = trim($input['status'] ?? 'accepted_by_tech');
    $techUserId = intval($input['tech_user_id'] ?? 0);
    $techName = trim($input['tech_name'] ?? '');
    $techPhone = preg_replace('/[^0-9]/', '', trim($input['tech_phone'] ?? ''));

    if (empty($leadId)) {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "लीड ID अनिवार्य है"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($db) {
        try {
            $stmt = $db->prepare("
                UPDATE elevex_repair_leads 
                SET status = :st, tech_user_id = :tuid, tech_name = :tn, tech_phone = :tp 
                WHERE id = :lid OR lead_ref = :lref
            ");
            $stmt->execute([
                ':st' => $status, 
                ':tuid' => $techUserId, 
                ':tn' => $techName, 
                ':tp' => $techPhone, 
                ':lid' => intval($leadId),
                ':lref' => $leadId
            ]);

            // Notify owner about technician status
            try {
                $leadQuery = $db->prepare("SELECT id, owner_phone, building_name, completion_otp FROM elevex_repair_leads WHERE id = :lid OR lead_ref = :lref LIMIT 1");
                $leadQuery->execute([':lid' => intval($leadId), ':lref' => $leadId]);
                $leadRow = $leadQuery->fetch(PDO::FETCH_ASSOC);

                if ($leadRow && !empty($leadRow['owner_phone'])) {
                    $ownerMsg = "";
                    $ownerTitle = "";
                    if ($status === 'accepted' || $status === 'accepted_by_tech') {
                        $ownerTitle = "👷 इंजीनियर असाइन हो गए!";
                        $ownerMsg = "$techName ($techPhone) ने आपकी साइट के लिए कार्यभार संभाला है।";
                    } elseif ($status === 'in_transit') {
                        $ownerTitle = "🛵 इंजीनियर रवाना हो चुके हैं";
                        $ownerMsg = "$techName आपकी बिल्डिंग के लिए निकल चुके हैं।";
                    } elseif ($status === 'arrived_on_site') {
                        $ownerTitle = "📍 इंजीनियर साइट पर पहुंच गए!";
                        $ownerMsg = "$techName साइट पर हैं। काम पूरा होने पर ही अपना OTP (${leadRow['completion_otp']}) शेयर करें।";
                    }

                    if (!empty($ownerTitle)) {
                        $ownerNotif = $db->prepare("
                            INSERT INTO elevex_notifications (role, phone, title, message, lead_id)
                            VALUES ('owner', :ph, :title, :msg, :lid)
                        ");
                        $ownerNotif->execute([
                            ':ph' => $leadRow['owner_phone'],
                            ':title' => $ownerTitle,
                            ':msg' => $ownerMsg,
                            ':lid' => (int)$leadRow['id']
                        ]);
                    }
                }
            } catch (Exception $ne) {}

            echo json_encode(["status" => "success", "message" => "स्थिति सफलतापूर्वक अपडेट कर दी गई!"], JSON_UNESCAPED_UNICODE);
            exit;
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["status" => "error", "message" => "डेटाबेस एरर: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }

    echo json_encode(["status" => "success", "message" => "अपडेट किया गया"], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// 5. VERIFY & COMPLETE LEAD (Technician Enters Customer OTP to Finish Job)
// -----------------------------------------------------------------------------
if ($action === 'verify_and_complete_lead') {
    $leadId = trim($input['lead_id'] ?? '');
    $enteredOtp = trim($input['otp'] ?? '');
    $serviceNotes = trim($input['service_notes'] ?? 'IS 14665 मानक अनुसार टेस्ट रन पूर्ण। लिफ्ट सुचारू चालू की गई।');
    $amount = floatval($input['amount'] ?? 0.0);

    if (empty($leadId) || empty($enteredOtp)) {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "लीड ID और ग्राहक OTP अनिवार्य हैं"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($db) {
        try {
            $checkStmt = $db->prepare("SELECT id, completion_otp, owner_phone, building_name FROM elevex_repair_leads WHERE id = :lid OR lead_ref = :lref LIMIT 1");
            $checkStmt->execute([':lid' => intval($leadId), ':lref' => $leadId]);
            $lead = $checkStmt->fetch(PDO::FETCH_ASSOC);

            if (!$lead) {
                http_response_code(404);
                echo json_encode(["status" => "error", "message" => "जॉब रिकॉर्ड नहीं मिला"], JSON_UNESCAPED_UNICODE);
                exit;
            }

            if (trim($lead['completion_otp']) !== $enteredOtp) {
                http_response_code(400);
                echo json_encode([
                    "status" => "error", 
                    "message" => "अमान्य OTP! कृपया ग्राहक के फोन स्क्रीन पर प्रदर्शित सही 4-अंकीय कोड प्राप्त करें।"
                ], JSON_UNESCAPED_UNICODE);
                exit;
            }

            // OTP verified! Mark completed
            $updateStmt = $db->prepare("
                UPDATE elevex_repair_leads 
                SET status = 'completed', 
                    service_notes = :notes, 
                    total_amount = :amt, 
                    completed_at = NOW() 
                WHERE id = :lid OR lead_ref = :lref
            ");
            $updateStmt->execute([
                ':notes' => $serviceNotes,
                ':amt' => $amount,
                ':lid' => intval($leadId),
                ':lref' => $leadId
            ]);

            // Notify owner of job completion
            try {
                if (!empty($lead['owner_phone'])) {
                    $ownerNotif = $db->prepare("
                        INSERT INTO elevex_notifications (role, phone, title, message, lead_id)
                        VALUES ('owner', :ph, :title, :msg, :lid)
                    ");
                    $ownerNotif->execute([
                        ':ph' => $lead['owner_phone'],
                        ':title' => "✅ लिफ्ट मेंटेनेंस कार्य पूर्ण!",
                        ':msg' => "आपकी लिफ्ट (${lead['building_name']}) सफलतापूर्वक ठीक हो गई है। डिजिटल जॉबशीट जनरेट हो गई है।",
                        ':lid' => (int)$lead['id']
                    ]);
                }
            } catch (Exception $ne) {}

            echo json_encode([
                "status" => "success", 
                "message" => "IS 14665 वैधानिक साइन-ऑफ पूर्ण हुआ! जॉब सफलतापूर्वक बंद कर दी गई।"
            ], JSON_UNESCAPED_UNICODE);
            exit;
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["status" => "error", "message" => "डेटाबेस एरर: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }

    echo json_encode(["status" => "success", "message" => "जॉब पूर्ण हुई"], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// 6. SYNC REGISTERED ELEVATORS (Add/Fetch Owner Fleet from Cloud)
// -----------------------------------------------------------------------------
if ($action === 'get_owner_elevators') {
    $ownerPhone = preg_replace('/[^0-9]/', '', trim($input['owner_phone'] ?? $_GET['owner_phone'] ?? ''));
    $lifts = [];
    if ($db && !empty($ownerPhone)) {
        try {
            $stmt = $db->prepare("SELECT * FROM elevex_registered_elevators WHERE owner_phone = :ph ORDER BY id DESC");
            $stmt->execute([':ph' => $ownerPhone]);
            $lifts = $stmt->fetchAll(PDO::FETCH_ASSOC);
        } catch (Exception $e) {}
    }
    echo json_encode(["status" => "success", "elevators" => $lifts ?: []], JSON_UNESCAPED_UNICODE);
    exit;
}

if ($action === 'save_owner_elevator') {
    $ownerPhone = preg_replace('/[^0-9]/', '', trim($input['owner_phone'] ?? ''));
    $ownerName = trim($input['owner_name'] ?? '');
    $buildingName = trim($input['building_name'] ?? '');
    $address = trim($input['address'] ?? '');
    $city = trim($input['city'] ?? '');
    $pincode = trim($input['pincode'] ?? '');
    $liftBrand = trim($input['lift_brand'] ?? 'Universal');
    $controllerType = trim($input['controller_type'] ?? 'Universal');
    $stopsCount = intval($input['stops_count'] ?? 4);
    $capacity = intval($input['capacity_persons'] ?? 6);
    $installYear = trim($input['installation_year'] ?? date('Y'));
    $liftRef = 'LFT-' . strtoupper(substr(md5(uniqid(rand(), true)), 0, 8));

    if (empty($ownerPhone) || empty($buildingName)) {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "ओनर फोन व बिल्डिंग का नाम अनिवार्य है"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    if ($db) {
        try {
            $stmt = $db->prepare("
                INSERT INTO elevex_registered_elevators 
                (lift_ref, owner_phone, owner_name, building_name, address, city, pincode, lift_brand, controller_type, stops_count, capacity_persons, installation_year, status)
                VALUES 
                (:ref, :ph, :on, :bn, :ad, :ct, :pin, :lb, :ctp, :sc, :cp, :iy, 'active')
            ");
            $stmt->execute([
                ':ref' => $liftRef,
                ':ph' => $ownerPhone,
                ':on' => $ownerName,
                ':bn' => $buildingName,
                ':ad' => $address,
                ':ct' => $city,
                ':pin' => $pincode,
                ':lb' => $liftBrand,
                ':ctp' => $controllerType,
                ':sc' => $stopsCount,
                ':cp' => $capacity,
                ':iy' => $installYear
            ]);
            $liftId = $db->lastInsertId();
            echo json_encode(["status" => "success", "message" => "लिफ्ट क्लाउड पर पंजीकृत हो गई!", "lift_id" => (int)$liftId, "lift_ref" => $liftRef], JSON_UNESCAPED_UNICODE);
            exit;
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["status" => "error", "message" => "डेटाबेस एरर: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
            exit;
        }
    }
    echo json_encode(["status" => "success", "message" => "सहेजा गया"], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// 7. POLL IN-APP NOTIFICATIONS
// -----------------------------------------------------------------------------
if ($action === 'poll_notifications') {
    $phone = preg_replace('/[^0-9]/', '', trim($input['phone'] ?? ''));
    $role = trim($input['role'] ?? '');
    $sinceId = intval($input['since_id'] ?? 0);
    $notifs = [];
    if ($db) {
        try {
            if (!empty($phone)) {
                $stmt = $db->prepare("
                    SELECT * FROM elevex_notifications 
                    WHERE (phone = :ph OR (role = :role AND phone = 'BROADCAST')) 
                      AND id > :sid 
                    ORDER BY id ASC LIMIT 20
                ");
                $stmt->execute([':ph' => $phone, ':role' => $role, ':sid' => $sinceId]);
                $notifs = $stmt->fetchAll(PDO::FETCH_ASSOC);
            } else {
                $stmt = $db->prepare("SELECT * FROM elevex_notifications WHERE role = :role AND id > :sid ORDER BY id ASC LIMIT 20");
                $stmt->execute([':role' => $role, ':sid' => $sinceId]);
                $notifs = $stmt->fetchAll(PDO::FETCH_ASSOC);
            }
        } catch (Exception $e) {}
    }
    echo json_encode(["status" => "success", "notifications" => $notifs ?: []], JSON_UNESCAPED_UNICODE);
    exit;
}

// -----------------------------------------------------------------------------
// 8. MARK NOTIFICATION READ
// -----------------------------------------------------------------------------
if ($action === 'mark_notification_read') {
    $notifId = intval($input['id'] ?? 0);
    if ($db && $notifId > 0) {
        try {
            $stmt = $db->prepare("UPDATE elevex_notifications SET is_read = 1 WHERE id = :id");
            $stmt->execute([':id' => $notifId]);
        } catch (Exception $e) {}
    }
    echo json_encode(["status" => "success"], JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode(["status" => "error", "message" => "अमान्य अनुरोध"], JSON_UNESCAPED_UNICODE);
exit;
