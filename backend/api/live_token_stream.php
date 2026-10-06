<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - अखंड रियल-टाइम टोकन स्ट्रीम इंजन
// Real-Time Server-Sent Events (SSE) & Ultra-Fast Live Token Stream API
// Delivers sub-second queue updates and eliminates delayed polling.
// ==============================================================================

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} else {
    require_once __DIR__ . '/config/db.php';
}

// 1. Check if client wants instant JSON poll instead of SSE stream
if (isset($_GET['poll']) || isset($_GET['json']) || (isset($_SERVER['HTTP_ACCEPT']) && strpos($_SERVER['HTTP_ACCEPT'], 'application/json') !== false && !isset($_GET['sse']))) {
    header('Content-Type: application/json; charset=utf-8');
    header("Access-Control-Allow-Origin: *");
    header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
    header("Pragma: no-cache");

    $pdo = getDB();
    if (!$pdo) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => "Database connection unavailable"], JSON_UNESCAPED_UNICODE);
        exit;
    }

    try {
        $st = $pdo->query("SELECT current_serving_token, is_darbar_active, darbar_date, is_token_service_enabled FROM ashram_settings WHERE id = 1 LIMIT 1");
        $settings = $st ? ($st->fetch(PDO::FETCH_ASSOC) ?: []) : [];

        $today = date('Y-m-d');
        $activeServing = intval($settings['current_serving_token'] ?? 0);

        // Fetch currently called token details
        $currentDevotee = null;
        if ($activeServing > 0) {
            $tokStmt = $pdo->prepare("SELECT token_number, patient_name, city, status, darbar_venue FROM tokens WHERE darbar_date = :d AND token_number = :t LIMIT 1");
            $tokStmt->execute([':d' => $today, ':t' => $activeServing]);
            $currentDevotee = $tokStmt->fetch(PDO::FETCH_ASSOC) ?: null;
        }

        echo json_encode([
            "success" => true,
            "running_token" => $activeServing,
            "is_darbar_active" => intval($settings['is_darbar_active'] ?? 1),
            "darbar_date" => $settings['darbar_date'] ?? $today,
            "is_service_enabled" => intval($settings['is_token_service_enabled'] ?? 1),
            "current_devotee" => $currentDevotee,
            "server_timestamp" => round(microtime(true) * 1000)
        ], JSON_UNESCAPED_UNICODE);
    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
    }
    exit;
}

// 2. Server-Sent Events (SSE) Real-Time Stream Mode
header('Content-Type: text/event-stream; charset=utf-8');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Connection: keep-alive');
header('Access-Control-Allow-Origin: *');
header('X-Accel-Buffering: no'); // Nginx / Hostinger disable output buffering

// Disable output compression for instant streaming
if (function_exists('apache_setenv')) {
    @apache_setenv('no-gzip', '1');
}
@ini_set('zlib.output_compression', '0');
@ini_set('implicit_flush', '1');
while (ob_get_level()) {
    ob_end_flush();
}
flush();

$pdo = getDB();
if (!$pdo) {
    echo "event: error\ndata: " . json_encode(["error" => "Database unavailable"]) . "\n\n";
    flush();
    exit;
}

$lastTokenSent = -1;
$lastStatusSent = "";
$startTime = time();
$maxStreamDuration = 25; // 25 seconds max duration per SSE cycle to comply with shared hosting execution limits

function getLiveTokenSnapshot($pdo) {
    try {
        $st = $pdo->query("SELECT current_serving_token, is_darbar_active, darbar_date, is_token_service_enabled FROM ashram_settings WHERE id = 1 LIMIT 1");
        $settings = $st ? ($st->fetch(PDO::FETCH_ASSOC) ?: []) : [];

        $today = date('Y-m-d');
        $activeServing = intval($settings['current_serving_token'] ?? 0);

        $currentDevotee = null;
        if ($activeServing > 0) {
            $tokStmt = $pdo->prepare("SELECT token_number, patient_name, city, status, darbar_venue FROM tokens WHERE darbar_date = :d AND token_number = :t LIMIT 1");
            $tokStmt->execute([':d' => $today, ':t' => $activeServing]);
            $currentDevotee = $tokStmt->fetch(PDO::FETCH_ASSOC) ?: null;
        }

        return [
            "running_token" => $activeServing,
            "is_darbar_active" => intval($settings['is_darbar_active'] ?? 1),
            "darbar_date" => $settings['darbar_date'] ?? $today,
            "is_service_enabled" => intval($settings['is_token_service_enabled'] ?? 1),
            "current_devotee" => $currentDevotee,
            "server_timestamp" => round(microtime(true) * 1000)
        ];
    } catch (Exception $e) {
        return null;
    }
}

// Initial Immediate Emission
$initial = getLiveTokenSnapshot($pdo);
if ($initial) {
    $lastTokenSent = $initial['running_token'];
    $lastStatusSent = json_encode($initial);
    echo "event: init\ndata: " . json_encode($initial, JSON_UNESCAPED_UNICODE) . "\n\n";
    flush();
}

// Stream Loop: Check every 1.5 seconds
while ((time() - $startTime) < $maxStreamDuration) {
    if (connection_aborted()) {
        break;
    }

    $current = getLiveTokenSnapshot($pdo);
    if ($current) {
        $encoded = json_encode($current);
        if ($current['running_token'] !== $lastTokenSent || $encoded !== $lastStatusSent) {
            $lastTokenSent = $current['running_token'];
            $lastStatusSent = $encoded;
            echo "event: queue_update\ndata: " . json_encode($current, JSON_UNESCAPED_UNICODE) . "\n\n";
            flush();
        }
    }

    // Keepalive Ping every 10 seconds
    if ((time() - $startTime) % 10 === 0) {
        echo "event: ping\ndata: " . json_encode(["time" => time()]) . "\n\n";
        flush();
    }

    usleep(1500000); // 1.5 seconds sleep
}

echo "event: reconnect\ndata: " . json_encode(["status" => "CYCLE_END"]) . "\n\n";
flush();
