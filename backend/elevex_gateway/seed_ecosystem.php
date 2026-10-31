<?php
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE ECOSYSTEM SEEDER & VERIFIER
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// Hostinger NVMe SSD Cluster: https://shribalajikripadham.online
// ==============================================================================

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");

require_once __DIR__ . '/config.php';

$pdo = getDbConnection();
if (!$pdo) {
    echo json_encode(["success" => false, "error" => "Database connection unavailable"], JSON_UNESCAPED_UNICODE);
    exit;
}

$results = [
    "companies" => [],
    "users" => [],
    "lifts" => [],
    "telemetry" => [],
    "attendance" => [],
    "breakdowns" => []
];

// 1. Companies Dataset
$companiesData = [
    [
        'tenant_id' => 'tenant_master_elevex',
        'company_name' => 'Ankit EleveX Supreme Command',
        'brand_display_name' => 'EleveX Pro International',
        'support_phone' => '8533955333',
        'office_address' => 'Sector Alpha 1 / Pari Chowk, Greater Noida, UP 201310',
        'office_lat' => 28.4744000,
        'office_lng' => 77.5040000,
        'primary_color_hex' => '#8B5CF6',
        'features_config' => [
            "door_drive_specialist" => true,
            "offline_diagnostics" => true,
            "anti_spoof_gps" => true,
            "auto_dispatch" => true,
            "unlimited_free_tier" => true,
            "anti_mock_gps" => true,
            "kiosk_lock" => true,
            "power_telemetry" => true,
            "max_technicians" => 100
        ]
    ],
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'company_name' => 'Apex Elevators NCR',
        'brand_display_name' => 'Apex EleveX Pro',
        'support_phone' => '9811001122',
        'office_address' => 'Commercial Belt, Near Pari Chowk, Greater Noida, UP 201310',
        'office_lat' => 28.4744000,
        'office_lng' => 77.5040000,
        'primary_color_hex' => '#0284C7',
        'features_config' => [
            "door_drive_specialist" => true,
            "offline_diagnostics" => true,
            "anti_spoof_gps" => true,
            "auto_dispatch" => true,
            "unlimited_free_tier" => true,
            "anti_mock_gps" => true,
            "kiosk_lock" => true,
            "power_telemetry" => true,
            "max_technicians" => 25
        ]
    ],
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'company_name' => 'Horizon Vertical Mobility',
        'brand_display_name' => 'Horizon Elevators Delhi NCR',
        'support_phone' => '9812001122',
        'office_address' => 'Statesman House, Barakhamba Road, Connaught Place, New Delhi 110001',
        'office_lat' => 28.6304000,
        'office_lng' => 77.2177000,
        'primary_color_hex' => '#0EA5E9',
        'features_config' => [
            "door_drive_specialist" => true,
            "offline_diagnostics" => true,
            "anti_spoof_gps" => true,
            "auto_dispatch" => true,
            "unlimited_free_tier" => true,
            "anti_mock_gps" => true,
            "kiosk_lock" => true,
            "power_telemetry" => true,
            "max_technicians" => 50
        ]
    ],
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'company_name' => 'Shree Balaji Lift Engineering Services',
        'brand_display_name' => 'Shree Balaji Lift Services Meerut',
        'support_phone' => '9813001122',
        'office_address' => 'Delhi Road, Near Transport Nagar, Meerut, UP 250002',
        'office_lat' => 28.9845000,
        'office_lng' => 77.7064000,
        'primary_color_hex' => '#F59E0B',
        'features_config' => [
            "door_drive_specialist" => true,
            "offline_diagnostics" => true,
            "anti_spoof_gps" => true,
            "auto_dispatch" => true,
            "unlimited_free_tier" => true,
            "anti_mock_gps" => true,
            "kiosk_lock" => true,
            "power_telemetry" => true,
            "max_technicians" => 30
        ]
    ]
];

foreach ($companiesData as $c) {
    try {
        $check = $pdo->prepare("SELECT id FROM elevex_companies WHERE tenant_id = :t LIMIT 1");
        $check->execute([':t' => $c['tenant_id']]);
        $row = $check->fetch();

        if ($row) {
            $stmtUp = $pdo->prepare("UPDATE elevex_companies SET 
                company_name = :cname,
                brand_display_name = :bname,
                support_phone = :phone,
                office_address = :addr,
                office_lat = :lat,
                office_lng = :lng,
                primary_color_hex = :col,
                features_config = :feat,
                is_active = 1,
                updated_at = NOW()
                WHERE tenant_id = :t");
            $stmtUp->execute([
                ':cname' => $c['company_name'],
                ':bname' => $c['brand_display_name'],
                ':phone' => $c['support_phone'],
                ':addr' => $c['office_address'],
                ':lat' => $c['office_lat'],
                ':lng' => $c['office_lng'],
                ':col' => $c['primary_color_hex'],
                ':feat' => json_encode($c['features_config']),
                ':t' => $c['tenant_id']
            ]);
            $results['companies'][$c['tenant_id']] = "UPDATED (ID: " . $row['id'] . ")";
        } else {
            $stmtIns = $pdo->prepare("INSERT INTO elevex_companies 
                (tenant_id, company_name, brand_display_name, support_phone, office_address, office_lat, office_lng, primary_color_hex, features_config, is_active)
                VALUES (:t, :cname, :bname, :phone, :addr, :lat, :lng, :col, :feat, 1)");
            $stmtIns->execute([
                ':t' => $c['tenant_id'],
                ':cname' => $c['company_name'],
                ':bname' => $c['brand_display_name'],
                ':phone' => $c['support_phone'],
                ':addr' => $c['office_address'],
                ':lat' => $c['office_lat'],
                ':lng' => $c['office_lng'],
                ':col' => $c['primary_color_hex'],
                ':feat' => json_encode($c['features_config'])
            ]);
            $results['companies'][$c['tenant_id']] = "CREATED (ID: " . $pdo->lastInsertId() . ")";
        }
    } catch (Exception $e) {
        $results['companies'][$c['tenant_id']] = "ERROR: " . $e->getMessage();
    }
}

// 2. Users Dataset
$usersData = [
    // SuperAdmin
    [
        'tenant_id' => 'tenant_master_elevex',
        'role' => 'superadmin',
        'full_name' => 'अंकित चौधरी (Super Admin)',
        'phone' => '8533955333',
        'mpin' => '1234',
        'device_id' => 'DEV_ANKIT_SUPREME_01'
    ],
    // Apex Elevators NCR
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'role' => 'company_admin',
        'full_name' => 'Rajesh Kumar',
        'phone' => '9811001122',
        'mpin' => '1234',
        'device_id' => 'DEV_APEX_ADMIN_01'
    ],
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'role' => 'supervisor',
        'full_name' => 'Vikram Singh (Supervisor)',
        'phone' => '9811002233',
        'mpin' => '1234',
        'device_id' => 'DEV_APEX_SUP_01'
    ],
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'role' => 'technician',
        'full_name' => 'Suresh Sharma (Senior Tech)',
        'phone' => '9811004455',
        'mpin' => '1234',
        'device_id' => 'DEV_APEX_TECH_01'
    ],
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'role' => 'technician',
        'full_name' => 'Amit Kumar (Field Tech)',
        'phone' => '9811004456',
        'mpin' => '1234',
        'device_id' => 'DEV_APEX_TECH_02'
    ],
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'role' => 'owner',
        'full_name' => 'Horizon Grand Tower RWA',
        'phone' => '9811006677',
        'mpin' => '1234',
        'device_id' => 'DEV_APEX_OWNER_01'
    ],
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'role' => 'owner',
        'full_name' => 'Alpha Plaza Management',
        'phone' => '9811006688',
        'mpin' => '1234',
        'device_id' => 'DEV_APEX_OWNER_02'
    ],
    // Horizon Vertical Mobility
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'role' => 'company_admin',
        'full_name' => 'Arun Mehra',
        'phone' => '9812001122',
        'mpin' => '1234',
        'device_id' => 'DEV_HORIZON_ADMIN_01'
    ],
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'role' => 'supervisor',
        'full_name' => 'Ramanathan Iyer (Supervisor)',
        'phone' => '9812002233',
        'mpin' => '1234',
        'device_id' => 'DEV_HORIZON_SUP_01'
    ],
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'role' => 'technician',
        'full_name' => 'Deepak Verma (Senior Tech)',
        'phone' => '9812004455',
        'mpin' => '1234',
        'device_id' => 'DEV_HORIZON_TECH_01'
    ],
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'role' => 'technician',
        'full_name' => 'Manoj Tiwari (Field Tech)',
        'phone' => '9812004456',
        'mpin' => '1234',
        'device_id' => 'DEV_HORIZON_TECH_02'
    ],
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'role' => 'owner',
        'full_name' => 'Connaught House Facilities RWA',
        'phone' => '9812006677',
        'mpin' => '1234',
        'device_id' => 'DEV_HORIZON_OWNER_01'
    ],
    // Shree Balaji Lift Engineering Services
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'role' => 'company_admin',
        'full_name' => 'Sunil Sharma',
        'phone' => '9813001122',
        'mpin' => '1234',
        'device_id' => 'DEV_BALAJI_ADMIN_01'
    ],
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'role' => 'supervisor',
        'full_name' => 'Pradeep Tyagi (Supervisor)',
        'phone' => '9813002233',
        'mpin' => '1234',
        'device_id' => 'DEV_BALAJI_SUP_01'
    ],
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'role' => 'technician',
        'full_name' => 'Mohit Rathi (Senior Tech)',
        'phone' => '9813004455',
        'mpin' => '1234',
        'device_id' => 'DEV_BALAJI_TECH_01'
    ],
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'role' => 'technician',
        'full_name' => 'Rahul Saini (Field Tech)',
        'phone' => '9813004456',
        'mpin' => '1234',
        'device_id' => 'DEV_BALAJI_TECH_02'
    ],
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'role' => 'owner',
        'full_name' => 'Gaur City Commercial Mall RWA',
        'phone' => '9813006677',
        'mpin' => '1234',
        'device_id' => 'DEV_BALAJI_OWNER_01'
    ]
];

$userMap = []; // phone => user_id
foreach ($usersData as $u) {
    try {
        $mpinHash = password_hash($u['mpin'], PASSWORD_BCRYPT);
        $cleanPhone = preg_replace('/[^0-9]/', '', $u['phone']);
        $normPhone = strlen($cleanPhone) > 10 ? substr($cleanPhone, -10) : $cleanPhone;

        $checkU = $pdo->prepare("SELECT id FROM elevex_users WHERE phone = :p OR phone = :norm OR phone = :full LIMIT 1");
        $checkU->execute([':p' => $u['phone'], ':norm' => $normPhone, ':full' => '+91' . $normPhone]);
        $existing = $checkU->fetch();

        if ($existing) {
            $stmtUp = $pdo->prepare("UPDATE elevex_users SET 
                tenant_id = :t,
                role = :r,
                full_name = :n,
                phone = :p,
                mpin_hash = :m,
                hardware_device_id = :d,
                is_active = 1,
                updated_at = NOW()
                WHERE id = :id");
            $stmtUp->execute([
                ':t' => $u['tenant_id'],
                ':r' => $u['role'],
                ':n' => $u['full_name'],
                ':p' => $normPhone,
                ':m' => $mpinHash,
                ':d' => $u['device_id'],
                ':id' => $existing['id']
            ]);
            $userId = intval($existing['id']);
            $results['users'][$normPhone] = "UPDATED (ID: $userId, {$u['role']})";
        } else {
            $stmtIns = $pdo->prepare("INSERT INTO elevex_users 
                (tenant_id, role, full_name, phone, mobile, mpin_hash, hardware_device_id, is_active)
                VALUES (:t, :r, :n, :p, :p, :m, :d, 1)");
            $stmtIns->execute([
                ':t' => $u['tenant_id'],
                ':r' => $u['role'],
                ':n' => $u['full_name'],
                ':p' => $normPhone,
                ':m' => $mpinHash,
                ':d' => $u['device_id']
            ]);
            $userId = intval($pdo->lastInsertId());
            $results['users'][$normPhone] = "CREATED (ID: $userId, {$u['role']})";
        }
        $userMap[$normPhone] = $userId;
    } catch (Exception $e) {
        $results['users'][$u['phone']] = "ERROR: " . $e->getMessage();
    }
}

// 3. AMC Lifts Dataset
$liftsData = [
    // Apex Elevators NCR
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'owner_phone' => '9811006677',
        'lift_unique_code' => 'APEX-HORIZON-01',
        'building_name' => 'Horizon Grand Tower A',
        'building_address' => 'Sector Alpha 1, Pari Chowk, Greater Noida, UP 201310',
        'landmark_name' => 'Pari Chowk Metro Pillar 42',
        'lat' => 28.4744000,
        'lng' => 77.5040000,
        'geofence_radius_meters' => 150,
        'controller_brand' => 'Monarch NICE3000+',
        'amc_type' => 'Comprehensive Platinum',
        'amc_valid_until' => '2027-10-31'
    ],
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'owner_phone' => '9811006688',
        'lift_unique_code' => 'APEX-ALPHA-02',
        'building_name' => 'Alpha Plaza Commercial Complex',
        'building_address' => 'Alpha 1 Commercial Belt, Greater Noida, UP 201310',
        'landmark_name' => 'Opposite Pari Chowk Roundabout',
        'lat' => 28.4735000,
        'lng' => 77.5032000,
        'geofence_radius_meters' => 150,
        'controller_brand' => 'STEP AS380',
        'amc_type' => 'Comprehensive Gold',
        'amc_valid_until' => '2027-12-31'
    ],
    // Horizon Vertical Mobility
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'owner_phone' => '9812006677',
        'lift_unique_code' => 'HORIZON-CP-01',
        'building_name' => 'Connaught House Business Tower',
        'building_address' => 'Barakhamba Road, Connaught Place, New Delhi 110001',
        'landmark_name' => 'Near Rajiv Chowk Metro Gate 7',
        'lat' => 28.6328000,
        'lng' => 77.2197000,
        'geofence_radius_meters' => 150,
        'controller_brand' => 'Otis SKY',
        'amc_type' => 'Comprehensive Diamond',
        'amc_valid_until' => '2028-03-31'
    ],
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'owner_phone' => '9812006677',
        'lift_unique_code' => 'HORIZON-GUR-02',
        'building_name' => 'Cyber Horizon Corporate Hub',
        'building_address' => 'DLF Cyber City, Phase 2, Gurugram, Haryana 122002',
        'landmark_name' => 'Cyber Hub Rapid Metro Station',
        'lat' => 28.4906000,
        'lng' => 77.0888000,
        'geofence_radius_meters' => 150,
        'controller_brand' => 'Schindler 3300',
        'amc_type' => 'Comprehensive Enterprise',
        'amc_valid_until' => '2027-09-30'
    ],
    // Shree Balaji Lift Engineering Services
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'owner_phone' => '9813006677',
        'lift_unique_code' => 'BALAJI-GAUR-01',
        'building_name' => 'Gaur City Commercial Mall',
        'building_address' => 'Gaur City 1, Greater Noida West / Char Murti, UP 201009',
        'landmark_name' => 'Kisan Chowk / Char Murti Roundabout',
        'lat' => 28.6082000,
        'lng' => 77.4244000,
        'geofence_radius_meters' => 150,
        'controller_brand' => 'KONE MonoSpace',
        'amc_type' => 'Comprehensive Gold',
        'amc_valid_until' => '2027-11-30'
    ],
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'owner_phone' => '9813006677',
        'lift_unique_code' => 'BALAJI-MEERUT-02',
        'building_name' => 'Rithani Central Commercial Arcade',
        'building_address' => 'Delhi-Meerut Road, Rithani, Meerut, UP 250103',
        'landmark_name' => 'Near Rithani RRTS Station',
        'lat' => 28.9482000,
        'lng' => 77.6712000,
        'geofence_radius_meters' => 150,
        'controller_brand' => 'Monarch NICE3000+',
        'amc_type' => 'Comprehensive Platinum',
        'amc_valid_until' => '2028-01-31'
    ]
];

$liftMap = []; // code => lift_id
foreach ($liftsData as $l) {
    try {
        $ownerId = $userMap[$l['owner_phone']] ?? null;

        $checkL = $pdo->prepare("SELECT id FROM elevex_lifts_amc WHERE lift_unique_code = :code LIMIT 1");
        $checkL->execute([':code' => $l['lift_unique_code']]);
        $existing = $checkL->fetch();

        if ($existing) {
            $stmtUp = $pdo->prepare("UPDATE elevex_lifts_amc SET 
                tenant_id = :t,
                owner_id = :oid,
                building_name = :bname,
                building_address = :baddr,
                landmark_name = :lmark,
                lat = :lat,
                lng = :lng,
                geofence_radius_meters = :geo,
                controller_brand = :brand,
                amc_type = :atype,
                amc_valid_until = :vuntil,
                is_active = 1
                WHERE id = :id");
            $stmtUp->execute([
                ':t' => $l['tenant_id'],
                ':oid' => $ownerId,
                ':bname' => $l['building_name'],
                ':baddr' => $l['building_address'],
                ':lmark' => $l['landmark_name'],
                ':lat' => $l['lat'],
                ':lng' => $l['lng'],
                ':geo' => $l['geofence_radius_meters'],
                ':brand' => $l['controller_brand'],
                ':atype' => $l['amc_type'],
                ':vuntil' => $l['amc_valid_until'],
                ':id' => $existing['id']
            ]);
            $liftId = intval($existing['id']);
            $results['lifts'][$l['lift_unique_code']] = "UPDATED (ID: $liftId)";
        } else {
            $stmtIns = $pdo->prepare("INSERT INTO elevex_lifts_amc 
                (tenant_id, owner_id, lift_unique_code, building_name, building_address, landmark_name, lat, lng, geofence_radius_meters, controller_brand, amc_type, amc_valid_until, is_active)
                VALUES (:t, :oid, :code, :bname, :baddr, :lmark, :lat, :lng, :geo, :brand, :atype, :vuntil, 1)");
            $stmtIns->execute([
                ':t' => $l['tenant_id'],
                ':oid' => $ownerId,
                ':code' => $l['lift_unique_code'],
                ':bname' => $l['building_name'],
                ':baddr' => $l['building_address'],
                ':lmark' => $l['landmark_name'],
                ':lat' => $l['lat'],
                ':lng' => $l['lng'],
                ':geo' => $l['geofence_radius_meters'],
                ':brand' => $l['controller_brand'],
                ':atype' => $l['amc_type'],
                ':vuntil' => $l['amc_valid_until']
            ]);
            $liftId = intval($pdo->lastInsertId());
            $results['lifts'][$l['lift_unique_code']] = "CREATED (ID: $liftId)";
        }
        $liftMap[$l['lift_unique_code']] = $liftId;
    } catch (Exception $e) {
        $results['lifts'][$l['lift_unique_code']] = "ERROR: " . $e->getMessage();
    }
}

// 4. Live Telemetry for Field Technicians
$telemetryData = [
    // Suresh Sharma - Apex Elevators (Greater Noida)
    [
        'phone' => '9811004455',
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'lat' => 28.4745000,
        'lng' => 77.5041000,
        'landmark' => '🏢 Horizon Grand Tower A (Pari Chowk)',
        'battery' => 92,
        'charging' => 0,
        'power_state' => 'ONLINE'
    ],
    // Amit Kumar - Apex Elevators (Alpha Plaza)
    [
        'phone' => '9811004456',
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'lat' => 28.4736000,
        'lng' => 77.5033000,
        'landmark' => '🏢 Alpha Plaza Commercial Belt (Pari Chowk)',
        'battery' => 84,
        'charging' => 1,
        'power_state' => 'ONLINE'
    ],
    // Deepak Verma - Horizon Vertical Mobility (Connaught Place)
    [
        'phone' => '9812004455',
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'lat' => 28.6329000,
        'lng' => 77.2198000,
        'landmark' => '🏢 Connaught House Business Tower (CP)',
        'battery' => 88,
        'charging' => 0,
        'power_state' => 'ONLINE'
    ],
    // Manoj Tiwari - Horizon Vertical Mobility (Gurugram Cyber City)
    [
        'phone' => '9812004456',
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'lat' => 28.4907000,
        'lng' => 77.0889000,
        'landmark' => '🏢 Cyber Horizon Corporate Hub (DLF Cyber City)',
        'battery' => 76,
        'charging' => 0,
        'power_state' => 'ONLINE'
    ],
    // Mohit Rathi - Shree Balaji Lift (Gaur City)
    [
        'phone' => '9813004455',
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'lat' => 28.6083000,
        'lng' => 77.4245000,
        'landmark' => '🏢 Gaur City Commercial Mall (Char Murti)',
        'battery' => 95,
        'charging' => 1,
        'power_state' => 'ONLINE'
    ],
    // Rahul Saini - Shree Balaji Lift (Meerut)
    [
        'phone' => '9813004456',
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'lat' => 28.9483000,
        'lng' => 77.6713000,
        'landmark' => '🏢 Rithani Central Arcade (Delhi-Meerut Road)',
        'battery' => 71,
        'charging' => 0,
        'power_state' => 'ONLINE'
    ]
];

foreach ($telemetryData as $t) {
    $techId = $userMap[$t['phone']] ?? 0;
    if ($techId > 0) {
        try {
            $stmtTel = $pdo->prepare("INSERT INTO elevex_technician_telemetry 
                (tenant_id, technician_id, current_lat, current_lng, location_landmark_text, is_mock_location, battery_percentage, is_charging, device_power_state, last_heartbeat)
                VALUES (:t, :tid, :lat, :lng, :lmark, 0, :bat, :charge, :pstate, NOW())
                ON DUPLICATE KEY UPDATE 
                current_lat = VALUES(current_lat),
                current_lng = VALUES(current_lng),
                location_landmark_text = VALUES(location_landmark_text),
                is_mock_location = 0,
                battery_percentage = VALUES(battery_percentage),
                is_charging = VALUES(is_charging),
                device_power_state = VALUES(device_power_state),
                last_heartbeat = NOW()");
            $stmtTel->execute([
                ':t' => $t['tenant_id'],
                ':tid' => $techId,
                ':lat' => $t['lat'],
                ':lng' => $t['lng'],
                ':lmark' => $t['landmark'],
                ':bat' => $t['battery'],
                ':charge' => $t['charging'],
                ':pstate' => $t['power_state']
            ]);
            $results['telemetry'][$t['phone']] = "UPDATED (Tech ID: $techId, {$t['landmark']})";
        } catch (Exception $e) {
            $results['telemetry'][$t['phone']] = "ERROR: " . $e->getMessage();
        }
    }
}

// 5. Attendance Ledger Records
$today = date("Y-m-d");
$attendanceData = [
    // Suresh Sharma - On-duty today
    [
        'phone' => '9811004455',
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'punch_in_time' => "$today 08:45:00",
        'lat' => 28.4744000,
        'lng' => 77.5040000,
        'location' => '🏢 Horizon Grand Tower A, Pari Chowk, Greater Noida',
        'is_mock' => 0
    ],
    // Deepak Verma - On-duty today
    [
        'phone' => '9812004455',
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'punch_in_time' => "$today 09:00:15",
        'lat' => 28.6328000,
        'lng' => 77.2197000,
        'location' => '🏢 Connaught House, Barakhamba Road, Connaught Place',
        'is_mock' => 0
    ],
    // Mohit Rathi - On-duty today with SuperAdmin Verified Audit Seal
    [
        'phone' => '9813004455',
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'punch_in_time' => "$today 08:30:00",
        'lat' => 28.6082000,
        'lng' => 77.4244000,
        'location' => '🏢 Gaur City Commercial Mall, Greater Noida West',
        'is_mock' => 0,
        'superadmin_audit' => 'SuperAdmin Ankit Chaudhary certified on-site emergency maintenance audit'
    ]
];

foreach ($attendanceData as $att) {
    $techId = $userMap[$att['phone']] ?? 0;
    if ($techId > 0) {
        try {
            $stmtAtt = $pdo->prepare("INSERT INTO elevex_attendance_ledger 
                (tenant_id, technician_id, duty_date, punch_in_time, punch_in_lat, punch_in_lng, punch_in_location_text, is_mock_detected, is_locked_immutable, superadmin_edit_reason, created_at)
                VALUES (:t, :tid, :d, :pi, :lat, :lng, :ltext, :mock, 1, :audit, NOW())
                ON DUPLICATE KEY UPDATE 
                punch_in_time = VALUES(punch_in_time),
                punch_in_location_text = VALUES(punch_in_location_text),
                superadmin_edit_reason = VALUES(superadmin_edit_reason)");
            $stmtAtt->execute([
                ':t' => $att['tenant_id'],
                ':tid' => $techId,
                ':d' => $today,
                ':pi' => $att['punch_in_time'],
                ':lat' => $att['lat'],
                ':lng' => $att['lng'],
                ':ltext' => $att['location'],
                ':mock' => $att['is_mock'],
                ':audit' => $att['superadmin_audit'] ?? null
            ]);
            $results['attendance'][$att['phone']] = "LOGGED (Tech ID: $techId, $today)";
        } catch (Exception $e) {
            $results['attendance'][$att['phone']] = "ERROR: " . $e->getMessage();
        }
    }
}

// 6. Real-Time Breakdown Tickets (Kanban Flow: Pending, Assigned, On-Site, Resolved)
$breakdownData = [
    // Apex Elevators - On-Site
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'ticket_number' => 'BD-20261031-1001',
        'lift_code' => 'APEX-HORIZON-01',
        'reporter_phone' => '9811006677',
        'tech_phone' => '9811004455',
        'status' => 'on_site',
        'dispatch_mode' => 'auto_nearest',
        'issue' => 'Monarch NICE3000+ Error Err02: Acceleration Overcurrent during peak morning rush',
        'otp' => '482915',
        'rating' => null,
        'feedback' => null
    ],
    // Apex Elevators - Pending
    [
        'tenant_id' => 'tenant_apexelevatorsncr_5779',
        'ticket_number' => 'BD-20261031-1002',
        'lift_code' => 'APEX-ALPHA-02',
        'reporter_phone' => '9811006688',
        'tech_phone' => null,
        'status' => 'pending',
        'dispatch_mode' => 'manual',
        'issue' => 'STEP AS380 Error 23: Drive Overheating in Tower B passenger elevator',
        'otp' => '591823',
        'rating' => null,
        'feedback' => null
    ],
    // Horizon Vertical Mobility - Assigned
    [
        'tenant_id' => 'tenant_horizon_vertical_mobility',
        'ticket_number' => 'BD-20261031-2001',
        'lift_code' => 'HORIZON-CP-01',
        'reporter_phone' => '9812006677',
        'tech_phone' => '9812004455',
        'status' => 'assigned',
        'dispatch_mode' => 'auto_nearest',
        'issue' => 'Otis SKY Error 0100: Drive Failure & Door Lock Sensor Calibration Warning',
        'otp' => '726481',
        'rating' => null,
        'feedback' => null
    ],
    // Shree Balaji Lift - Resolved
    [
        'tenant_id' => 'tenant_shree_balaji_lift_eng',
        'ticket_number' => 'BD-20261031-3001',
        'lift_code' => 'BALAJI-GAUR-01',
        'reporter_phone' => '9813006677',
        'tech_phone' => '9813004455',
        'status' => 'resolved',
        'dispatch_mode' => 'manual',
        'issue' => 'KONE MonoSpace Error 0021: Safety Chain Disconnect during maintenance cycle',
        'otp' => '839201',
        'rating' => 5,
        'feedback' => 'Exceptional response speed and permanent resolution within 22 minutes!'
    ]
];

foreach ($breakdownData as $bd) {
    try {
        $liftId = $liftMap[$bd['lift_code']] ?? null;
        if (!$liftId) {
            $stmtF = $pdo->prepare("SELECT id FROM elevex_lifts_amc WHERE lift_unique_code = :c LIMIT 1");
            $stmtF->execute([':c' => $bd['lift_code']]);
            $r = $stmtF->fetch();
            $liftId = $r ? intval($r['id']) : null;
        }

        $reporterId = $userMap[$bd['reporter_phone']] ?? null;
        $techId = $bd['tech_phone'] ? ($userMap[$bd['tech_phone']] ?? null) : null;

        if ($liftId && $reporterId) {
            $stmtBd = $pdo->prepare("INSERT INTO elevex_breakdowns 
                (tenant_id, ticket_number, lift_id, reported_by_id, assigned_technician_id, dispatch_mode, status, issue_description, completion_otp, customer_rating, customer_feedback, created_at, assigned_at, resolved_at)
                VALUES (:t, :tno, :lid, :rid, :tid, :dmode, :status, :issue, :otp, :rating, :fb, NOW(), IF(:tid IS NOT NULL, NOW(), NULL), IF(:status = 'resolved', NOW(), NULL))
                ON DUPLICATE KEY UPDATE 
                status = VALUES(status),
                assigned_technician_id = VALUES(assigned_technician_id),
                issue_description = VALUES(issue_description),
                customer_rating = VALUES(customer_rating),
                customer_feedback = VALUES(customer_feedback)");
            $stmtBd->execute([
                ':t' => $bd['tenant_id'],
                ':tno' => $bd['ticket_number'],
                ':lid' => $liftId,
                ':rid' => $reporterId,
                ':tid' => $techId,
                ':dmode' => $bd['dispatch_mode'],
                ':status' => $bd['status'],
                ':issue' => $bd['issue'],
                ':otp' => $bd['otp'],
                ':rating' => $bd['rating'],
                ':fb' => $bd['feedback']
            ]);
            $results['breakdowns'][$bd['ticket_number']] = "UPSERTED ({$bd['status']})";
        }
    } catch (Exception $e) {
        $results['breakdowns'][$bd['ticket_number']] = "ERROR: " . $e->getMessage();
    }
}

echo json_encode([
    "success" => true,
    "status" => "AUTHENTIC_ECOSYSTEM_SEEDED",
    "timestamp" => date("Y-m-d H:i:s"),
    "results" => $results
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
