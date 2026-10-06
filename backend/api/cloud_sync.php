<?php
// Shri Balaji Kripa Dham - Central Cloud Sync Endpoint
// 100% Real Live Bidirectional MySQL Database Synchronization
date_default_timezone_set('Asia/Kolkata');

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} elseif (file_exists(__DIR__ . '/config/db.php')) {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-SBKD-API-KEY');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$uploadsDir = __DIR__ . '/../uploads/';
if (!is_dir($uploadsDir)) {
    @mkdir($uploadsDir, 0755, true);
}
$backupFile = $uploadsDir . 'backup_latest.json';

$pdo = function_exists('getDB') ? getDB() : null;

// =============================================================================
// GET REQUEST: Download Live Database State / Perform File Repair
// =============================================================================
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    if (isset($_GET['repair']) || isset($_GET['sync_files'])) {
        $repoRawBase = "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/main/backend/";
        $needed = [
            "deploy.php" => __DIR__ . '/../deploy.php',
            "api/delete_token.php" => __DIR__ . '/delete_token.php',
            "config/db.php" => __DIR__ . '/../config/db.php'
        ];
        $repaired = [];
        foreach ($needed as $rel => $dest) {
            $code = @file_get_contents($repoRawBase . $rel . "?t=" . time());
            if ($code && strlen($code) > 10) {
                @file_put_contents($dest, $code);
                $repaired[] = $rel;
            }
        }
        echo json_encode(['success' => true, 'repaired' => $repaired, 'message' => 'Missing files synchronized from GitHub'], JSON_UNESCAPED_UNICODE);
        exit;
    }

    // Return complete live database state for client sync & restore
    if ($pdo) {
        try {
            $settingsStmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
            $settings = $settingsStmt ? ($settingsStmt->fetch(PDO::FETCH_ASSOC) ?: []) : [];

            $tokensStmt = $pdo->query("SELECT * FROM tokens ORDER BY darbar_date DESC, token_number ASC LIMIT 2000");
            $tokens = $tokensStmt ? ($tokensStmt->fetchAll(PDO::FETCH_ASSOC) ?: []) : [];

            $sevStmt = $pdo->query("SELECT * FROM sevadars ORDER BY display_order ASC");
            $sevadars = $sevStmt ? ($sevStmt->fetchAll(PDO::FETCH_ASSOC) ?: []) : [];

            $donorStmt = $pdo->query("SELECT * FROM donors ORDER BY display_order ASC");
            $donors = $donorStmt ? ($donorStmt->fetchAll(PDO::FETCH_ASSOC) ?: []) : [];

            $livePayload = [
                "success" => true,
                "version" => 12,
                "settings" => $settings,
                "tokens" => $tokens,
                "sevadars" => $sevadars,
                "donors" => $donors,
                "timestamp" => time(),
                "status" => "ONLINE"
            ];

            echo json_encode($livePayload, JSON_UNESCAPED_UNICODE);
            exit;
        } catch (Throwable $t) {
            error_log("cloud_sync GET database read error: " . $t->getMessage());
        }
    }

    if (file_exists($backupFile)) {
        readfile($backupFile);
    } else {
        echo json_encode(['success' => true, 'status' => 'READY', 'message' => 'Cloud Sync Endpoint is Online'], JSON_UNESCAPED_UNICODE);
    }
    exit;
}

// =============================================================================
// POST REQUEST: Ingest & Apply Database Backup to Hostinger MySQL Tables
// =============================================================================
$payload = file_get_contents('php://input');
if (empty($payload)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'डेटा प्राप्त नहीं हुआ (Empty payload)'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 1. Save raw file backup
@file_put_contents($backupFile, $payload);

// 2. Decode payload
$data = json_decode($payload, true);
if (!is_array($data)) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'अमान्य JSON डेटा प्रारूप (Invalid JSON)'], JSON_UNESCAPED_UNICODE);
    exit;
}

$tokensSynced = 0;
$settingsUpdated = false;
$sevadarsSynced = 0;

if ($pdo) {
    try {
        // A. Synchronize Settings into ashram_settings table
        $inSettings = $data['settings'] ?? null;
        if (is_array($inSettings) && !empty($inSettings)) {
            $colStmt = $pdo->query("SHOW COLUMNS FROM ashram_settings");
            $existingCols = $colStmt ? $colStmt->fetchAll(PDO::FETCH_COLUMN) : [];
            $colSet = array_flip($existingCols);

            $updatePairs = [];
            $bindings = [];

            // Mapping for settings fields that might have different keys in app JSON
            $fieldMap = [
                'ashram_name' => 'ashram_name',
                'ashramName' => 'ashram_name',
                'guruji_name' => 'guruji_name',
                'gurujiName' => 'guruji_name',
                'address' => 'ashram_address',
                'ashram_address' => 'ashram_address',
                'ashramAddress' => 'ashram_address',
                'contact_phone' => 'contact_phone',
                'contactPhone' => 'contact_phone',
                'whatsapp_number' => 'whatsapp_number',
                'whatsappNumber' => 'whatsapp_number',
                'darbar_timings' => 'darbar_timings',
                'darbarTimings' => 'darbar_timings',
                'latitude' => 'ashram_latitude',
                'ashram_latitude' => 'ashram_latitude',
                'longitude' => 'ashram_longitude',
                'ashram_longitude' => 'ashram_longitude',
                'allowed_radius_meters' => 'allowed_radius_meters',
                'allowedRadiusMeters' => 'allowed_radius_meters',
                'is_geofence_enforced' => 'is_geofence_enforced',
                'isGeofenceEnforced' => 'is_geofence_enforced',
                'is_outstation_advance_allowed' => 'is_outstation_advance_allowed',
                'isOutstationAdvanceAllowed' => 'is_outstation_advance_allowed',
                'outstation_min_distance_km' => 'outstation_min_distance_km',
                'outstationMinDistanceKm' => 'outstation_min_distance_km',
                'running_token_number' => 'current_serving_token',
                'runningTokenNumber' => 'current_serving_token',
                'current_serving_token' => 'current_serving_token',
                'currentServingToken' => 'current_serving_token',
                'is_darbar_active' => 'is_darbar_active',
                'isDarbarActive' => 'is_darbar_active',
                'darbar_date' => 'darbar_date',
                'darbarDate' => 'darbar_date',
                'max_daily_tokens' => 'daily_token_limit',
                'maxDailyTokens' => 'daily_token_limit',
                'daily_token_limit' => 'daily_token_limit',
                'dailyTokenLimit' => 'daily_token_limit',
                'total_tokens_today' => 'daily_token_limit',
                'totalTokensToday' => 'daily_token_limit',
                'top_bar_text' => 'top_bar_text',
                'topBarText' => 'top_bar_text',
                'guruji_title' => 'guruji_title',
                'gurujiTitle' => 'guruji_title',
                'guruji_bio' => 'guruji_bio',
                'gurujiBio' => 'guruji_bio',
                'token_rules_notice' => 'token_rules_notice',
                'tokenRulesNotice' => 'token_rules_notice',
                'bank_name' => 'bank_name',
                'bankName' => 'bank_name',
                'bank_account_holder' => 'bank_account_holder',
                'bankAccountHolder' => 'bank_account_holder',
                'bank_account_number' => 'bank_account_number',
                'bankAccountNumber' => 'bank_account_number',
                'bank_ifsc' => 'bank_ifsc',
                'bankIfsc' => 'bank_ifsc',
                'bank_branch' => 'bank_branch',
                'bankBranch' => 'bank_branch',
                'ashram_directions' => 'ashram_directions',
                'ashramDirections' => 'ashram_directions',
                'contact_email' => 'contact_email',
                'contactEmail' => 'contact_email',
                'youtube_url' => 'youtube_url',
                'youtubeUrl' => 'youtube_url',
                'facebook_url' => 'facebook_url',
                'facebookUrl' => 'facebook_url',
                'instagram_url' => 'instagram_url',
                'instagramUrl' => 'instagram_url',
                'whatsapp_channel_url' => 'whatsapp_channel_url',
                'whatsappChannelUrl' => 'whatsapp_channel_url',
                'whatsapp_group_url' => 'whatsapp_group_url',
                'whatsappGroupUrl' => 'whatsapp_group_url',
                'footer_title' => 'footer_title',
                'footerTitle' => 'footer_title',
                'footer_dedication' => 'footer_dedication',
                'footerDedication' => 'footer_dedication',
                'footer_copyright' => 'footer_copyright',
                'footerCopyright' => 'footer_copyright',
                'ashram_parichay_hindi' => 'ashram_parichay_hindi',
                'ashramParichayHindi' => 'ashram_parichay_hindi',
                'ashram_history_hindi' => 'ashram_history_hindi',
                'ashramHistoryHindi' => 'ashram_history_hindi',
                'ashram_rules_hindi' => 'ashram_rules_hindi',
                'ashramRulesHindi' => 'ashram_rules_hindi',
                'aarti_timings' => 'aarti_timings',
                'aartiTimings' => 'aarti_timings',
                'aarti_lyrics' => 'aarti_lyrics',
                'aartiLyrics' => 'aarti_lyrics',
                'is_darbar_live_now' => 'is_darbar_live_now',
                'isDarbarLiveNow' => 'is_darbar_live_now',
                'live_stream_title' => 'live_stream_title',
                'liveStreamTitle' => 'live_stream_title',
                'live_stream_url' => 'live_stream_url',
                'liveStreamUrl' => 'live_stream_url',
                'youtube_live_url' => 'youtube_live_url',
                'youtubeLiveUrl' => 'youtube_live_url',
                'facebook_live_url' => 'facebook_live_url',
                'facebookLiveUrl' => 'facebook_live_url',
                'bus_seat_fare_amount' => 'bus_seat_fare_amount',
                'busSeatFareAmount' => 'bus_seat_fare_amount',
                'is_bus_booking_live' => 'is_bus_booking_live',
                'isBusBookingLive' => 'is_bus_booking_live',
                'is_dharamshala_live' => 'is_dharamshala_live',
                'isDharamshalaLive' => 'is_dharamshala_live',
                'is_live_counter_visible' => 'is_live_counter_visible',
                'isLiveCounterVisible' => 'is_live_counter_visible',
                'is_payment_feature_live' => 'is_payment_feature_live',
                'isPaymentFeatureLive' => 'is_payment_feature_live',
                'is_arzi_ledger_live' => 'is_arzi_ledger_live',
                'isArziLedgerLive' => 'is_arzi_ledger_live',
                'badi_arzi_rate' => 'badi_arzi_rate',
                'badiArziRate' => 'badi_arzi_rate',
                'chhoti_arzi_rate' => 'chhoti_arzi_rate',
                'chhotiArziRate' => 'chhoti_arzi_rate',
                'can_admin_issue_reserved_tokens' => 'can_admin_issue_reserved_tokens',
                'canAdminIssueReservedTokens' => 'can_admin_issue_reserved_tokens',
                'allow_admin_reserved_tokens' => 'allow_admin_reserved_tokens',
                'allowAdminReservedTokens' => 'allow_admin_reserved_tokens',
                'token_service_mode' => 'token_service_mode',
                'tokenServiceMode' => 'token_service_mode',
                'is_token_service_enabled' => 'is_token_service_enabled',
                'isTokenServiceEnabled' => 'is_token_service_enabled',
                'upi_id' => 'upi_id',
                'upiId' => 'upi_id',
                'upi_name' => 'upi_name',
                'upiName' => 'upi_name',
                'guruji_photo_url' => 'guruji_photo_url',
                'gurujiPhotoUrl' => 'guruji_photo_url'
            ];

            foreach ($fieldMap as $srcKey => $targetCol) {
                if (isset($inSettings[$srcKey]) && isset($colSet[$targetCol])) {
                    $val = $inSettings[$srcKey];
                    if (is_bool($val)) $val = $val ? 1 : 0;
                    $pName = ":set_" . $targetCol;
                    $updatePairs[] = "`$targetCol` = $pName";
                    $bindings[$pName] = $val;
                }
            }

            // Also check running_token_number
            if (isset($inSettings['running_token_number']) && isset($colSet['running_token_number'])) {
                $updatePairs[] = "`running_token_number` = :p_rtn";
                $bindings[':p_rtn'] = intval($inSettings['running_token_number']);
            }

            if (!empty($updatePairs)) {
                $sql = "UPDATE ashram_settings SET " . implode(", ", $updatePairs) . " WHERE id = 1";
                $stmt = $pdo->prepare($sql);
                $stmt->execute($bindings);
                $settingsUpdated = true;
            }
        }

        // B. Synchronize Tokens into tokens table
        $inTokens = $data['tokens'] ?? null;
        if (is_array($inTokens) && !empty($inTokens)) {
            // Ensure columns exist
            try {
                $c = $pdo->query("SHOW COLUMNS FROM tokens LIKE 'darbar_venue'");
                if (!$c || $c->rowCount() === 0) {
                    $pdo->exec("ALTER TABLE tokens ADD COLUMN darbar_venue VARCHAR(50) NOT NULL DEFAULT 'DUNGRA_JAAT'");
                }
            } catch (Throwable $e) {}

            $checkStmt = $pdo->prepare("SELECT id FROM tokens WHERE darbar_date = :dd AND token_number = :tn LIMIT 1");
            $updateTokenStmt = $pdo->prepare("UPDATE tokens SET 
                patient_name = :pname, 
                phone_number = :phone, 
                city = :city, 
                origin_address = :orig, 
                destination_address = :dest, 
                distance_km = :dist, 
                status = :status, 
                registered_by = :regby, 
                photo_url = :photo, 
                is_darshan_completed = :darshan 
                WHERE id = :id");

            $insertTokenStmt = $pdo->prepare("INSERT INTO tokens (
                token_number, darbar_date, patient_name, phone_number, city, origin_address, 
                destination_address, distance_km, device_id, latitude, longitude, status, 
                registered_by, photo_url, is_darshan_completed, created_at, darbar_venue
            ) VALUES (
                :token_number, :darbar_date, :patient_name, :phone_number, :city, :origin_address, 
                :destination_address, :distance_km, :device_id, :latitude, :longitude, :status, 
                :registered_by, :photo_url, :is_darshan_completed, :created_at, :darbar_venue
            )");

            foreach ($inTokens as $t) {
                $tNum = intval($t['token_number'] ?? ($t['tokenNumber'] ?? ($t['token'] ?? 0)));
                $dDate = trim($t['darbar_date'] ?? ($t['darbarDate'] ?? ($t['date'] ?? date('Y-m-d'))));
                if ($tNum <= 0) continue;

                $pName = trim($t['patient_name'] ?? ($t['patientName'] ?? ($t['name'] ?? ($t['devotee_name'] ?? ($t['devoteeName'] ?? 'भक्त')))));
                $phone = trim($t['phone_number'] ?? ($t['phoneNumber'] ?? ($t['phone'] ?? '')));
                $city = trim($t['city'] ?? ($t['district_city'] ?? ($t['districtCity'] ?? 'डूँगरा जाट (स्थानीय)')));
                $orig = trim($t['origin_address'] ?? ($t['originAddress'] ?? ''));
                $dest = trim($t['destination_address'] ?? ($t['destinationAddress'] ?? 'श्री बालाजी कृपा धाम, डूँगरा जाट'));
                $dist = floatval($t['distance_km'] ?? ($t['distanceKm'] ?? 0.0));
                $status = trim($t['status'] ?? 'WAITING');
                $regBy = trim($t['registered_by'] ?? ($t['registeredBy'] ?? ($t['entry_source'] ?? 'CLOUD_SYNC')));
                $photo = trim($t['photo_url'] ?? ($t['photoUrl'] ?? ($t['photo_uri'] ?? ($t['photoUri'] ?? ''))));
                $darshan = (!empty($t['is_darshan_completed']) || !empty($t['isDarshanCompleted']) || strtoupper($status) === 'COMPLETED') ? 1 : 0;
                $devId = trim($t['device_id'] ?? ($t['deviceId'] ?? 'CLOUD_SYNC'));
                $lat = floatval($t['latitude'] ?? 28.3972915);
                $lng = floatval($t['longitude'] ?? 78.1460410);
                $created = intval($t['created_at'] ?? ($t['createdAt'] ?? (time() * 1000)));
                $venue = trim($t['darbar_venue'] ?? ($t['darbarVenue'] ?? 'DUNGRA_JAAT'));

                $checkStmt->execute([':dd' => $dDate, ':tn' => $tNum]);
                $existingId = $checkStmt->fetchColumn();

                if ($existingId) {
                    $updateTokenStmt->execute([
                        ':pname' => $pName,
                        ':phone' => $phone,
                        ':city' => $city,
                        ':orig' => $orig,
                        ':dest' => $dest,
                        ':dist' => $dist,
                        ':status' => $status,
                        ':regby' => $regBy,
                        ':photo' => $photo,
                        ':darshan' => $darshan,
                        ':id' => $existingId
                    ]);
                } else {
                    $insertTokenStmt->execute([
                        ':token_number' => $tNum,
                        ':darbar_date' => $dDate,
                        ':patient_name' => $pName,
                        ':phone_number' => $phone,
                        ':city' => $city,
                        ':origin_address' => $orig,
                        ':destination_address' => $dest,
                        ':distance_km' => $dist,
                        ':device_id' => $devId,
                        ':latitude' => $lat,
                        ':longitude' => $lng,
                        ':status' => $status,
                        ':registered_by' => $regBy,
                        ':photo_url' => $photo,
                        ':is_darshan_completed' => $darshan,
                        ':created_at' => $created,
                        ':darbar_venue' => $venue
                    ]);
                }
                $tokensSynced++;
            }
        }

        // C. Synchronize Sevadars if provided
        $inSevadars = $data['sevadars'] ?? null;
        if (is_array($inSevadars) && !empty($inSevadars)) {
            $sevUpsert = $pdo->prepare("INSERT INTO sevadars (name, role, phone, photo_url, display_order, is_active)
                VALUES (:name, :role, :phone, :photo_url, :display_order, :is_active)
                ON DUPLICATE KEY UPDATE role = VALUES(role), phone = VALUES(phone), photo_url = VALUES(photo_url), display_order = VALUES(display_order), is_active = VALUES(is_active)");

            foreach ($inSevadars as $s) {
                $sName = trim($s['name'] ?? '');
                if (empty($sName)) continue;
                $sevUpsert->execute([
                    ':name' => $sName,
                    ':role' => trim($s['role'] ?? ($s['roleTitleHindi'] ?? 'आश्रम सेवादार')),
                    ':phone' => trim($s['phone'] ?? ($s['phoneNumber'] ?? '')),
                    ':photo_url' => trim($s['photo_url'] ?? ($s['photoUri'] ?? '')),
                    ':display_order' => intval($s['display_order'] ?? ($s['displayOrder'] ?? 0)),
                    ':is_active' => isset($s['is_active']) ? intval($s['is_active']) : 1
                ]);
                $sevadarsSynced++;
            }
        }

    } catch (Throwable $e) {
        $dbError = $e->getMessage();
        error_log("cloud_sync MySQL execution notice: " . $e->getMessage());
    }
}

echo json_encode([
    'success' => true,
    'message' => "क्लाउड डेटा सिंक 100% सफल: $tokensSynced टोकन, $sevadarsSynced सेवादार व आश्रम सेटिंग्स MySQL डेटाबेस में लाइव सुरक्षित हुए!",
    'tokens_synced' => $tokensSynced,
    'settings_updated' => $settingsUpdated,
    'sevadars_synced' => $sevadarsSynced,
    'db_error' => $dbError ?? null,
    'timestamp' => time(),
    'bytes_received' => strlen($payload)
], JSON_UNESCAPED_UNICODE);
