<?php
if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} else {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$pdo = getDB();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true) ?: $_POST;
    
    // Fetch existing settings first
    $stmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
    $current = $stmt->fetch() ?: [];

    $ashramName = trim($input['ashram_name'] ?? ($current['ashram_name'] ?? 'श्री बालाजी कृपा धाम'));
    $lat = floatval($input['latitude'] ?? ($current['ashram_latitude'] ?? 28.3972915));
    $long = floatval($input['longitude'] ?? ($current['ashram_longitude'] ?? 78.1460410));
    $radius = floatval($input['allowed_radius_meters'] ?? ($current['allowed_radius_meters'] ?? 200.0));
    $enforced = isset($input['is_geofence_enforced']) ? intval($input['is_geofence_enforced']) : intval($current['is_geofence_enforced'] ?? 1);
    $outstationAllowed = isset($input['is_outstation_advance_allowed']) ? intval($input['is_outstation_advance_allowed']) : intval($current['is_outstation_advance_allowed'] ?? 1);
    $outstationKm = floatval($input['outstation_min_distance_km'] ?? ($current['outstation_min_distance_km'] ?? 30.0));
    $servingToken = isset($input['current_serving_token']) ? intval($input['current_serving_token']) : intval($current['current_serving_token'] ?? 0);
    $dailyLimit = isset($input['daily_token_limit']) ? intval($input['daily_token_limit']) : intval($current['daily_token_limit'] ?? 1000);

    // Dynamic Live Service Toggles
    $isTokenServiceEnabled = isset($input['is_token_service_enabled']) ? intval($input['is_token_service_enabled']) : intval($current['is_token_service_enabled'] ?? 1);
    $isBusBookingLive = isset($input['is_bus_booking_live']) ? intval($input['is_bus_booking_live']) : intval($current['is_bus_booking_live'] ?? 0);
    $isLiveCounterVisible = isset($input['is_live_counter_visible']) ? intval($input['is_live_counter_visible']) : intval($current['is_live_counter_visible'] ?? 1);
    $isPaymentFeatureLive = isset($input['is_payment_feature_live']) ? intval($input['is_payment_feature_live']) : intval($current['is_payment_feature_live'] ?? 0);
    $isArziLedgerLive = isset($input['is_arzi_ledger_live']) ? intval($input['is_arzi_ledger_live']) : intval($current['is_arzi_ledger_live'] ?? 1);
    $isDarbarActive = isset($input['is_darbar_active']) ? intval($input['is_darbar_active']) : intval($current['is_darbar_active'] ?? 1);

    $darbarDate = trim($input['darbar_date'] ?? ($current['darbar_date'] ?? date('Y-m-d')));
    $darbarTimings = trim($input['darbar_timings'] ?? ($current['darbar_timings'] ?? 'प्रत्येक रविवार प्रातःकाल 8:00 बजे से'));
    $emergencyNotice = trim($input['emergency_notice'] ?? ($current['emergency_notice'] ?? ''));
    $isEmergencyNoticeVisible = isset($input['is_emergency_notice_visible']) ? intval($input['is_emergency_notice_visible']) : intval($current['is_emergency_notice_visible'] ?? 0);
    $bannerTitle = trim($input['banner_title'] ?? ($current['banner_title'] ?? '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट'));
    $bannerSubtitle = trim($input['banner_subtitle'] ?? ($current['banner_subtitle'] ?? 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार'));
    $isBannerVisible = isset($input['is_banner_visible']) ? intval($input['is_banner_visible']) : intval($current['is_banner_visible'] ?? 1);

    $stmt = $pdo->prepare("UPDATE ashram_settings SET
        ashram_name = :ashram_name,
        ashram_latitude = :lat,
        ashram_longitude = :long,
        allowed_radius_meters = :rad,
        is_geofence_enforced = :enf,
        is_outstation_advance_allowed = :out_allow,
        outstation_min_distance_km = :out_km,
        current_serving_token = :serving,
        daily_token_limit = :daily_limit,
        is_token_service_enabled = :tok_enabled,
        is_bus_booking_live = :bus_live,
        is_live_counter_visible = :cnt_vis,
        is_payment_feature_live = :pay_live,
        is_arzi_ledger_live = :arzi_live,
        is_darbar_active = :darbar_active,
        darbar_date = :darbar_date,
        darbar_timings = :darbar_timings,
        emergency_notice = :emergency_notice,
        is_emergency_notice_visible = :em_vis,
        banner_title = :banner_title,
        banner_subtitle = :banner_subtitle,
        is_banner_visible = :ban_vis,
        config_version = COALESCE(config_version, 1) + 1
        WHERE id = 1");

    $stmt->execute([
        ':ashram_name' => $ashramName,
        ':lat' => $lat,
        ':long' => $long,
        ':rad' => $radius,
        ':enf' => $enforced,
        ':out_allow' => $outstationAllowed,
        ':out_km' => $outstationKm,
        ':serving' => $servingToken,
        ':daily_limit' => $dailyLimit,
        ':tok_enabled' => $isTokenServiceEnabled,
        ':bus_live' => $isBusBookingLive,
        ':cnt_vis' => $isLiveCounterVisible,
        ':pay_live' => $isPaymentFeatureLive,
        ':arzi_live' => $isArziLedgerLive,
        ':darbar_active' => $isDarbarActive,
        ':darbar_date' => $darbarDate,
        ':darbar_timings' => $darbarTimings,
        ':emergency_notice' => $emergencyNotice,
        ':em_vis' => $isEmergencyNoticeVisible,
        ':banner_title' => $bannerTitle,
        ':banner_subtitle' => $bannerSubtitle,
        ':ban_vis' => $isBannerVisible
    ]);

    echo json_encode([
        "success" => true,
        "message" => "सुपरएडमिन सेटिंग्स सफलतापूर्वक अपडेट व लाइव प्रसारित हुईं!",
        "current_serving_token" => $servingToken,
        "is_token_service_enabled" => boolval($isTokenServiceEnabled),
        "is_bus_booking_live" => boolval($isBusBookingLive),
        "timestamp" => time()
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// GET Request: Return full live settings
$stmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
$row = $stmt->fetch() ?: [];

echo json_encode([
    "success" => true,
    "ashram_name" => $row['ashram_name'] ?? 'श्री बालाजी कृपा धाम',
    "latitude" => floatval($row['ashram_latitude'] ?? 28.3972915),
    "longitude" => floatval($row['ashram_longitude'] ?? 78.1460410),
    "allowed_radius_meters" => floatval($row['allowed_radius_meters'] ?? 200.0),
    "is_geofence_enforced" => boolval($row['is_geofence_enforced'] ?? true),
    "is_outstation_advance_allowed" => boolval($row['is_outstation_advance_allowed'] ?? true),
    "outstation_min_distance_km" => floatval($row['outstation_min_distance_km'] ?? 30.0),
    "current_serving_token" => intval($row['current_serving_token'] ?? 0),
    "daily_token_limit" => intval($row['daily_token_limit'] ?? 1000),
    "is_token_service_enabled" => boolval($row['is_token_service_enabled'] ?? true),
    "is_bus_booking_live" => boolval($row['is_bus_booking_live'] ?? false),
    "is_live_counter_visible" => boolval($row['is_live_counter_visible'] ?? true),
    "is_payment_feature_live" => boolval($row['is_payment_feature_live'] ?? false),
    "is_arzi_ledger_live" => boolval($row['is_arzi_ledger_live'] ?? true),
    "is_darbar_active" => boolval($row['is_darbar_active'] ?? true),
    "darbar_date" => $row['darbar_date'] ?? date('Y-m-d'),
    "darbar_timings" => $row['darbar_timings'] ?? 'प्रत्येक रविवार प्रातःकाल 8:00 बजे से',
    "emergency_notice" => $row['emergency_notice'] ?? '',
    "is_emergency_notice_visible" => boolval($row['is_emergency_notice_visible'] ?? false),
    "banner_title" => $row['banner_title'] ?? '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट',
    "banner_subtitle" => $row['banner_subtitle'] ?? 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार',
    "is_banner_visible" => boolval($row['is_banner_visible'] ?? true),
    "config_version" => intval($row['config_version'] ?? 1),
    "server_time" => time()
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
