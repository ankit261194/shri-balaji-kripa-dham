<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE 5-BRAND FAULT CODE & DELTA SYNC ENGINE
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// Brands: Monarch, STEP, Otis, Schindler, KONE
// ==============================================================================

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

require_once __DIR__ . '/config.php';

try {
    $pdo = getDbConnection();
    if (!$pdo) {
        sendJsonResponse(["success" => false, "error" => "Database connection unavailable"], 500);
    }

    // Auto-ensure schema tables on first run
    function ensureFaultCodeSchema($pdo) {
        $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_master_fault_codes (
            id INT AUTO_INCREMENT PRIMARY KEY,
            error_id VARCHAR(64) NOT NULL UNIQUE,
            brand_controller VARCHAR(64) NOT NULL,
            model_series VARCHAR(128) NOT NULL,
            error_code VARCHAR(64) NOT NULL,
            error_title_en VARCHAR(255) NOT NULL,
            error_title_hi VARCHAR(255) NOT NULL,
            root_causes TEXT NOT NULL,
            troubleshooting_steps TEXT NOT NULL,
            safety_precaution TEXT NOT NULL,
            is_safety_critical TINYINT(1) DEFAULT 0,
            version INT NOT NULL DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX idx_fc_brand (brand_controller),
            INDEX idx_fc_code (error_code),
            INDEX idx_fc_version (version),
            INDEX idx_fc_updated (updated_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

        $pdo->exec("CREATE TABLE IF NOT EXISTS elevex_technician_field_remarks (
            id INT AUTO_INCREMENT PRIMARY KEY,
            tenant_id VARCHAR(64) NOT NULL,
            technician_id INT NOT NULL,
            technician_name VARCHAR(128) NOT NULL,
            brand_controller VARCHAR(64) NOT NULL,
            error_code VARCHAR(64) NOT NULL,
            lift_unique_code VARCHAR(64) NULL,
            site_name VARCHAR(191) NULL,
            field_remark TEXT NOT NULL,
            parts_replaced TEXT NULL,
            offline_created_at DATETIME NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_fr_brand_code (brand_controller, error_code),
            INDEX idx_fr_tenant (tenant_id),
            INDEX idx_fr_tech (technician_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

        // Seed core OEM default records if master codes table is empty
        $stmt = $pdo->query("SELECT COUNT(*) FROM elevex_master_fault_codes");
        if ($stmt->fetchColumn() == 0) {
            $seeds = [
                // 1. Monarch
                [
                    'error_id' => 'monarch_err02',
                    'brand_controller' => 'Monarch',
                    'model_series' => 'NICE 3000+ / NICE 1000+',
                    'error_code' => 'Err02',
                    'error_title_en' => 'Acceleration Overcurrent (Motor Stuck)',
                    'error_title_hi' => 'एक्सेलेरेशन ओवरकरंट (मोटर जाम)',
                    'root_causes' => "1. Main motor winding short circuit.\n2. Mechanical brake does not open or delayed opening.\n3. Counterweight imbalance.\n4. Encoder pulse direction inverted.",
                    'troubleshooting_steps' => "1. Check brake microswitch contact air gap (0.2-0.3mm).\n2. Measure U-V-W resistance with digital multimeter.\n3. Verify F1-12 motor tuning parameters.\n4. Check mechanical brake lever release manually.",
                    'safety_precaution' => 'DANGER: Isolate main 415V power before checking motor windings or brake contacts.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],
                [
                    'error_id' => 'monarch_err10',
                    'brand_controller' => 'Monarch',
                    'model_series' => 'NICE 3000+',
                    'error_code' => 'Err10',
                    'error_title_en' => 'Brake Switch Feedback Failure',
                    'error_title_hi' => 'ब्रेक स्विच फीडबैक विफलता',
                    'root_causes' => "1. Brake microswitch auxiliary contact dirty or carbon deposit.\n2. Brake contact wiring loose on MCB board.\n3. Brake coil rectifier diode burnt.",
                    'troubleshooting_steps' => "1. Clean brake microswitch contacts with contact cleaner.\n2. Check X6 / X7 input LED status on NICE 3000 main board.\n3. Replace brake auxiliary switch if resistance > 0.5 ohm.",
                    'safety_precaution' => 'WARNING: Do not bypass brake microswitches; risk of uncontrolled car drift.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],
                [
                    'error_id' => 'monarch_err20',
                    'brand_controller' => 'Monarch',
                    'model_series' => 'NICE 3000+',
                    'error_code' => 'Err20',
                    'error_title_en' => 'Rotary Encoder Speed Feedback Lost',
                    'error_title_hi' => 'रोटरी एनकोडर स्पीड फीडबैक गुम',
                    'root_causes' => "1. PG card connection loose.\n2. Encoder coupling loose on motor shaft.\n3. Encoder cable interference from motor power cables.",
                    'troubleshooting_steps' => "1. Check PG card seating and 5V/12V encoder power supply.\n2. Verify shielded earth ground on PG board.\n3. Inspect rubber coupling alignment on traction motor shaft.",
                    'safety_precaution' => 'CAUTION: Ensure car is in inspection mode before inspecting shaft encoder.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],
                [
                    'error_id' => 'monarch_err41',
                    'brand_controller' => 'Monarch',
                    'model_series' => 'NICE 3000+',
                    'error_code' => 'Err41',
                    'error_title_en' => 'Landing Door Lock Disconnected During Run',
                    'error_title_hi' => 'रन के दौरान लैंडिंग डोर लॉक सर्किट टूटा',
                    'root_causes' => "1. Landing door lock contact bounced or loose roller.\n2. Door vane skate rubbing door lock beak.\n3. Car gate switch contact weak.",
                    'troubleshooting_steps' => "1. Check 110V safety string voltage drop.\n2. Inspect door lock hook gap (min 7mm engagement).\n3. Check door skate clearance (6-8mm) across all floors.",
                    'safety_precaution' => 'CRITICAL SAFETY: Never jump door lock circuit with wire jumper during passenger service.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],

                // 2. STEP Electric
                [
                    'error_id' => 'step_e01',
                    'brand_controller' => 'Step Electric',
                    'model_series' => 'AS380 / AS320',
                    'error_code' => 'E01',
                    'error_title_en' => 'Main Drive Inverter Fault / Overcurrent',
                    'error_title_hi' => 'ड्राइव इन्वर्टर ओवरकरंट प्रोटेक्शन ट्रिप',
                    'root_causes' => "1. Motor phase-to-ground insulation breakdown.\n2. Acceleration rate set too fast (F09 parameter).\n3. IGBT drive module damaged.",
                    'troubleshooting_steps' => "1. Disconnect motor wires and perform inverter self-test.\n2. Megger test motor windings at 500V DC (> 5 MOhm).\n3. Reduce acceleration slope in F09 parameter.",
                    'safety_precaution' => 'HIGH VOLTAGE: Wait 10 minutes after power off for DC bus capacitors to discharge < 30V.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],
                [
                    'error_id' => 'step_e41',
                    'brand_controller' => 'Step Electric',
                    'model_series' => 'AS380',
                    'error_code' => 'E41',
                    'error_title_en' => 'Door Lock Circuit Disconnected During Travel',
                    'error_title_hi' => 'चलते समय डोर लॉक सर्किट खुला',
                    'root_causes' => "1. Door lock interlock switch gap improper.\n2. Travelling cable 110V door lock core intermittent.\n3. Landing door lock hook contact worn.",
                    'troubleshooting_steps' => "1. Check AS380 X25 (Door Lock) input LED on main CPU.\n2. Inspect door lock contact bridge for carbonization.\n3. Measure continuity of travelling cable core 11 and 12.",
                    'safety_precaution' => 'DANGER: Always set elevator to Inspection (T-Drive) mode before opening door locks.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],

                // 3. OTIS
                [
                    'error_id' => 'otis_e01',
                    'brand_controller' => 'OTIS',
                    'model_series' => 'GeN2 / MCS 2200 / GCS',
                    'error_code' => 'E01',
                    'error_title_en' => 'Drive Inverter Overcurrent Trip',
                    'error_title_hi' => 'ड्राइव इन्वर्टर ओवरकरंट ट्रिप',
                    'root_causes' => "1. Traction motor winding resistance low.\n2. Brake not lifting completely.\n3. Coated steel belt (CSB) pulse slip.",
                    'troubleshooting_steps' => "1. Check brake coil DC voltage on pick (110V) and hold (55V).\n2. Inspect CSB pulse monitoring sensors.\n3. Verify inverter IGBT module health using Service Tool (M-2-1-1).",
                    'safety_precaution' => 'DANGER: 415V AC bus and 650V DC bus present. Follow LOTO procedures.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],
                [
                    'error_id' => 'otis_m211',
                    'brand_controller' => 'OTIS',
                    'model_series' => 'GeN2 / MCS 2200',
                    'error_code' => 'M-2-1-1',
                    'error_title_en' => 'Service Tool Drive Fault Diagnostic Menu',
                    'error_title_hi' => 'सर्विस टूल ड्राइव फॉल्ट डायग्नोस्टिक मेनू',
                    'root_causes' => "Drive subsystem fault logged in internal EEPROM ring buffer.",
                    'troubleshooting_steps' => "1. Plug OTIS blue service tool into OCSS / MCSS port.\n2. Navigate: M -> 2 (DRIVE) -> 1 (FAULT LOG) -> 1 (VIEW).\n3. Clear fault history after solving underlying physical cause.",
                    'safety_precaution' => 'NOTE: Only use authorized OTIS service tool with valid protocol handshake.',
                    'is_safety_critical' => 0,
                    'version' => 1
                ],

                // 4. KONE
                [
                    'error_id' => 'kone_0021',
                    'brand_controller' => 'KONE',
                    'model_series' => 'MonoSpace / MiniSpace / KCM831',
                    'error_code' => '0021',
                    'error_title_en' => 'Safety Chain Interruption at Start',
                    'error_title_hi' => 'स्टार्ट पर सेफ्टी चेन रुकावट',
                    'root_causes' => "1. Landing door contact intermittent.\n2. Car top stop switch tripped.\n3. Governor switch open.",
                    'troubleshooting_steps' => "1. Check LEDs on LCECPU board (50, 51, 52, 53 safety loop).\n2. Inspect pit stop switch and buffer switches.\n3. Measure 230VAC / 110VAC safety line at connector XM1.",
                    'safety_precaution' => 'CRITICAL: Never short safety inputs on LCECPU board.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],
                [
                    'error_id' => 'kone_0026',
                    'brand_controller' => 'KONE',
                    'model_series' => 'MonoSpace LCE',
                    'error_code' => '0026',
                    'error_title_en' => 'Tachometer / Encoder Sync Discrepancy',
                    'error_title_hi' => 'टैकोमीटर / एनकोडर सिंक विसंगति',
                    'root_causes' => "1. Tachometer feedback cable damaged.\n2. V3F16L / V3F25 drive tachometer calibration lost.\n3. Motor pole position error.",
                    'troubleshooting_steps' => "1. Check EcoDisc motor optical encoder alignment.\n2. Perform shaft learning drive with LCE User Interface menu 5-1.\n3. Inspect shielded tachometer ground connection on drive terminal.",
                    'safety_precaution' => 'CAUTION: Ensure elevator is on inspection before executing shaft learning drive.',
                    'is_safety_critical' => 0,
                    'version' => 1
                ],

                // 5. Schindler
                [
                    'error_id' => 'schindler_0004',
                    'brand_controller' => 'SCHINDLER',
                    'model_series' => '3300 / 5500 / Bionic',
                    'error_code' => '0004',
                    'error_title_en' => 'Bionic Controller Watchdog Timeout',
                    'error_title_hi' => 'बायोनिक कंट्रोलर वॉचडॉग टाइमआउट',
                    'root_causes' => "1. 24V DC auxiliary power supply ripple voltage > 100mV.\n2. Main CPU board memory parity exception.\n3. CAN bus communication noise from VFD.",
                    'troubleshooting_steps' => "1. Power down elevator for 60 seconds and restart.\n2. Measure 24VDC SMPS with oscilloscope or AC multimeter range.\n3. Check CAN bus termination resistor (120 Ohm) between CAN_H and CAN_L.",
                    'safety_precaution' => 'WARNING: Do not disconnect Bionic CPU ribbon cables while power is active.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ],
                [
                    'error_id' => 'schindler_0025',
                    'brand_controller' => 'SCHINDLER',
                    'model_series' => '3300 / 5500',
                    'error_code' => '0025',
                    'error_title_en' => 'Door Lock Contact Bounce / Timeout',
                    'error_title_hi' => 'डोर लॉक कॉन्टैक्ट बाउंस / टाइमआउट',
                    'root_causes' => "1. Landing door lock contact gap < 4mm.\n2. Door vane skate worn out.\n3. Debris or cement dust in landing door sill groove.",
                    'troubleshooting_steps' => "1. Vacuum dust from bottom door sill groove.\n2. Set door skate clearance to exactly 6mm on both sides.\n3. Clean contacts on Bionic Door Interface Board (BIO2).",
                    'safety_precaution' => 'SAFETY: Wear safety harness on car top while adjusting door operator.',
                    'is_safety_critical' => 1,
                    'version' => 1
                ]
            ];

            $insertStmt = $pdo->prepare("INSERT INTO elevex_master_fault_codes 
                (error_id, brand_controller, model_series, error_code, error_title_en, error_title_hi, root_causes, troubleshooting_steps, safety_precaution, is_safety_critical, version)
                VALUES (:error_id, :brand_controller, :model_series, :error_code, :error_title_en, :error_title_hi, :root_causes, :troubleshooting_steps, :safety_precaution, :is_safety_critical, :version)
                ON DUPLICATE KEY UPDATE version = VALUES(version)");

            foreach ($seeds as $s) {
                $insertStmt->execute($s);
            }
        }
    }

    ensureFaultCodeSchema($pdo);

    $action = $_GET['action'] ?? $_POST['action'] ?? '';
    $input = getJsonInput();

    // =========================================================================
    // 1. DELTA CLOUD SYNCHRONIZATION
    // =========================================================================
    if ($action === 'delta_sync') {
        $sinceTimestamp = trim($_GET['since_timestamp'] ?? $input['since_timestamp'] ?? '');
        $lastVersion = intval($_GET['last_version'] ?? $input['last_version'] ?? 0);
        $brandFilter = trim($_GET['brand'] ?? $input['brand'] ?? '');
        $limit = min(500, max(1, intval($_GET['limit'] ?? $input['limit'] ?? 100)));

        $where = ["1=1"];
        $params = [];

        if (!empty($sinceTimestamp)) {
            $where[] = "updated_at > :since_ts";
            $params[':since_ts'] = $sinceTimestamp;
        } elseif ($lastVersion > 0) {
            $where[] = "version > :last_ver";
            $params[':last_ver'] = $lastVersion;
        }

        if (!empty($brandFilter) && strtoupper($brandFilter) !== 'ALL') {
            $where[] = "UPPER(brand_controller) = :brand";
            $params[':brand'] = strtoupper($brandFilter);
        }

        $whereClause = implode(" AND ", $where);
        $sql = "SELECT error_id, brand_controller, model_series, error_code, 
                       error_title_en, error_title_hi, root_causes, 
                       troubleshooting_steps, safety_precaution, is_safety_critical, 
                       version, updated_at
                FROM elevex_master_fault_codes
                WHERE $whereClause
                ORDER BY version ASC, updated_at ASC
                LIMIT $limit";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $codes = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Fetch server max version and total active count
        $metaStmt = $pdo->query("SELECT MAX(version) as max_v, COUNT(*) as total_cnt FROM elevex_master_fault_codes");
        $meta = $metaStmt->fetch(PDO::FETCH_ASSOC);

        sendJsonResponse([
            "success" => true,
            "codes" => $codes,
            "server_timestamp" => date("Y-m-d H:i:s"),
            "latest_version" => intval($meta['max_v'] ?? 1),
            "total_active_codes" => intval($meta['total_cnt'] ?? 0),
            "delta_count" => count($codes)
        ]);
    }

    // =========================================================================
    // 2. SUBMIT TECHNICIAN FIELD REMARK (OFFLINE QUEUE FLUSH SUPPORT)
    // =========================================================================
    if ($action === 'submit_field_remark') {
        // Can be a single remark or array of remarks queued offline
        $remarks = $input['remarks'] ?? [$input];
        if (!is_array($remarks) || empty($remarks)) {
            sendJsonResponse(["success" => false, "error" => "रिमार्क डेटा अनिवार्य है"], 400);
        }

        $stmt = $pdo->prepare("INSERT INTO elevex_technician_field_remarks
            (tenant_id, technician_id, technician_name, brand_controller, error_code, lift_unique_code, site_name, field_remark, parts_replaced, offline_created_at)
            VALUES (:tenant_id, :technician_id, :technician_name, :brand_controller, :error_code, :lift_unique_code, :site_name, :field_remark, :parts_replaced, :offline_created_at)");

        $savedCount = 0;
        foreach ($remarks as $r) {
            $tenantId = trim($r['tenant_id'] ?? '');
            $techId = intval($r['technician_id'] ?? 0);
            $techName = trim($r['technician_name'] ?? 'Senior Technician');
            $brand = trim($r['brand'] ?? $r['brand_controller'] ?? 'MONARCH');
            $code = trim($r['code'] ?? $r['error_code'] ?? '');
            $liftCode = trim($r['lift_unique_code'] ?? '');
            $siteName = trim($r['site_name'] ?? '');
            $remark = trim($r['field_remark'] ?? $r['remark'] ?? '');
            $parts = trim($r['parts_replaced'] ?? '');
            $offlineCreatedAt = trim($r['offline_created_at'] ?? date("Y-m-d H:i:s"));

            if (!empty($tenantId) && !empty($code) && !empty($remark)) {
                $stmt->execute([
                    ':tenant_id' => $tenantId,
                    ':technician_id' => $techId,
                    ':technician_name' => $techName,
                    ':brand_controller' => $brand,
                    ':error_code' => $code,
                    ':lift_unique_code' => $liftCode ?: null,
                    ':site_name' => $siteName ?: null,
                    ':field_remark' => $remark,
                    ':parts_replaced' => $parts ?: null,
                    ':offline_created_at' => $offlineCreatedAt
                ]);
                $savedCount++;
            }
        }

        sendJsonResponse([
            "success" => true,
            "message" => "फील्ड अनुभव सफलतापूर्वक क्लाउड पर दर्ज किया गया ($savedCount रिमार्क सिंक)",
            "saved_count" => $savedCount
        ]);
    }

    // =========================================================================
    // 3. GET TECHNICIAN FIELD REMARKS FOR A FAULT CODE
    // =========================================================================
    if ($action === 'get_field_remarks') {
        $brand = trim($_GET['brand'] ?? $input['brand'] ?? '');
        $code = trim($_GET['code'] ?? $input['code'] ?? '');
        $tenantId = trim($_GET['tenant_id'] ?? $input['tenant_id'] ?? '');

        if (empty($code)) {
            sendJsonResponse(["success" => false, "error" => "एरर कोड अनिवार्य है"], 400);
        }

        $sql = "SELECT id, tenant_id, technician_id, technician_name, brand_controller, 
                       error_code, lift_unique_code, site_name, field_remark, parts_replaced, 
                       offline_created_at, created_at
                FROM elevex_technician_field_remarks
                WHERE error_code = :code";
        $params = [':code' => $code];

        if (!empty($brand) && strtoupper($brand) !== 'ALL') {
            $sql .= " AND UPPER(brand_controller) = :brand";
            $params[':brand'] = strtoupper($brand);
        }

        $sql .= " ORDER BY created_at DESC LIMIT 20";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $remarks = $stmt->fetchAll(PDO::FETCH_ASSOC);

        sendJsonResponse([
            "success" => true,
            "error_code" => $code,
            "remarks" => $remarks,
            "total_remarks" => count($remarks)
        ]);
    }

    // =========================================================================
    // 4. UPSERT MASTER FAULT CODE (SUPER ADMIN / ENTERPRISE MASTER ENGINE)
    // =========================================================================
    if ($action === 'upsert_code') {
        $errorId = trim($input['error_id'] ?? '');
        $brand = trim($input['brand_controller'] ?? '');
        $model = trim($input['model_series'] ?? 'Universal');
        $code = trim($input['error_code'] ?? '');
        $titleEn = trim($input['error_title_en'] ?? '');
        $titleHi = trim($input['error_title_hi'] ?? '');
        $causes = trim($input['root_causes'] ?? '');
        $steps = trim($input['troubleshooting_steps'] ?? '');
        $safety = trim($input['safety_precaution'] ?? '');
        $isCritical = !empty($input['is_safety_critical']) ? 1 : 0;

        if (empty($errorId) || empty($brand) || empty($code) || empty($titleEn)) {
            sendJsonResponse(["success" => false, "error" => "आईडी, ब्रांड, कोड और शीर्षक अनिवार्य हैं"], 400);
        }

        // Increment version
        $vStmt = $pdo->query("SELECT IFNULL(MAX(version), 0) + 1 FROM elevex_master_fault_codes");
        $newVersion = intval($vStmt->fetchColumn());

        $stmt = $pdo->prepare("INSERT INTO elevex_master_fault_codes
            (error_id, brand_controller, model_series, error_code, error_title_en, error_title_hi, root_causes, troubleshooting_steps, safety_precaution, is_safety_critical, version)
            VALUES (:error_id, :brand_controller, :model_series, :error_code, :error_title_en, :error_title_hi, :root_causes, :troubleshooting_steps, :safety_precaution, :is_safety_critical, :version)
            ON DUPLICATE KEY UPDATE
                model_series = VALUES(model_series),
                error_code = VALUES(error_code),
                error_title_en = VALUES(error_title_en),
                error_title_hi = VALUES(error_title_hi),
                root_causes = VALUES(root_causes),
                troubleshooting_steps = VALUES(troubleshooting_steps),
                safety_precaution = VALUES(safety_precaution),
                is_safety_critical = VALUES(is_safety_critical),
                version = VALUES(version)");

        $stmt->execute([
            ':error_id' => $errorId,
            ':brand_controller' => $brand,
            ':model_series' => $model,
            ':error_code' => $code,
            ':error_title_en' => $titleEn,
            ':error_title_hi' => $titleHi ?: $titleEn,
            ':root_causes' => $causes,
            ':troubleshooting_steps' => $steps,
            ':safety_precaution' => $safety,
            ':is_safety_critical' => $isCritical,
            ':version' => $newVersion
        ]);

        sendJsonResponse([
            "success" => true,
            "message" => "फॉल्ट कोड सफलतापूर्वक सहेजा गया (वर्जन $newVersion)",
            "version" => $newVersion,
            "error_id" => $errorId
        ]);
    }

    sendJsonResponse(["success" => false, "error" => "अमान्य एक्शन अनुरोध (Invalid Action Request)"], 400);

} catch (Exception $e) {
    sendJsonResponse(["success" => false, "error" => $e->getMessage()], 500);
}
