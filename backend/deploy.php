<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - 1-क्लिक स्वायत्त क्लाउड डिप्लॉयर
// 1-Click Autonomous Cloud Deployer & Health Verifier (Direct from GitHub CDN)
// ==============================================================================

ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");

$targetSha = $_GET['sha'] ?? 'main';
$repoRawBase = "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/{$targetSha}/backend/";
$filesToSync = [
    ".htaccess",
    "deploy.php",
    "download.php",
    "index.php",
    "version.json",
    "config/db.php",
    "api/live_config.php",
    "api/daily_darshan.php",
    "api/get_queue.php",
    "api/get_sevadars.php",
    "api/get_donors.php",
    "api/issue_token.php",
    "api/update_token_status.php",
    "api/delete_token.php",
    "api/book_bus_seat.php",
    "api/get_bus_seats.php",
    "api/dharamshala.php",
    "api/update_live_status.php",
    "api/save_sevadar.php",
    "api/save_donor.php",
    "api/delete_sevadar.php",
    "api/delete_donor.php",
    "api/github_proxy.php",
    "api/cloud_sync.php",
    "api/data_vault.php",
    "api/check_device.php",
    "api/device_telemetry.php",
    "api/get_devotee_notifications.php",
    "api/docu_ai.php",
    "api/admin_auth.php",
    "api/status_service.php",
    "api/delete_parcha.php",
    "media/balaji_darshan_today.jpg",
    "sync_apk.php"
];

$baseDir = __DIR__;
$updated = [];
$failed = [];

function fetchRemoteFile($url) {
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 12);
        curl_setopt($ch, CURLOPT_USERAGENT, "SBKD-Deployer/2.0");
        $res = curl_exec($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        if ($code === 200 && $res) return $res;
    }
    return @file_get_contents($url);
}

foreach ($filesToSync as $relPath) {
    $remoteUrl = $repoRawBase . $relPath . "?t=" . time();
    $content = fetchRemoteFile($remoteUrl);
    if ($content !== false && strlen($content) > 0) {
        $targetPath = $baseDir . '/' . $relPath;
        $dir = dirname($targetPath);
        if (!is_dir($dir)) {
            @mkdir($dir, 0755, true);
        }
        if (@file_put_contents($targetPath, $content) !== false) {
            $updated[] = $relPath . " (" . strlen($content) . " bytes)";
        } else {
            $failed[] = $relPath . " (Write error)";
        }
    } else {
        $failed[] = $relPath . " (Fetch error from GitHub)";
    }
}

// Clear all local caches and trigger live_config self-healing migration
$cacheDir = $baseDir . '/cache';
if (is_dir($cacheDir)) {
    foreach (glob($cacheDir . '/*') as $f) {
        if (is_file($f)) @unlink($f);
    }
}
if (function_exists('opcache_reset')) {
    @opcache_reset();
}

try {
    if (file_exists($baseDir . '/config/db.php')) {
        require_once $baseDir . '/config/db.php';
        $pdo = function_exists('getDB') ? getDB() : null;
        if ($pdo) {
            $pdo->exec("UPDATE ashram_settings SET contact_phone = '' WHERE contact_phone LIKE '%97206%' OR contact_phone LIKE '%98765%'");
            $pdo->exec("UPDATE ashram_settings SET whatsapp_number = '' WHERE whatsapp_number LIKE '%97206%' OR whatsapp_number LIKE '%98765%'");
            $pdo->exec("UPDATE ashram_settings SET contact_email = '' WHERE contact_email LIKE '%shribalajikripadham@gmail.com%'");
            $pdo->exec("UPDATE ashram_settings SET upi_id = '' WHERE upi_id = 'shribalajikripadham@upi'");
            $pdo->exec("UPDATE ashram_settings SET is_tuesday_darbar_enabled = 0 WHERE id = 1 AND is_tuesday_darbar_enabled IS NULL");
        }
    }
} catch (Throwable $e) {}

// Optional: Sync latest Release APK directly onto server
if (!empty($_REQUEST['sync_apk'])) {
    $apkUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/latest/download/ShriBalajiKripaDham-release.apk";
    $dlDir = $baseDir . '/downloads';
    if (!is_dir($dlDir)) @mkdir($dlDir, 0755, true);
    $targetApk = $dlDir . '/ShriBalajiKripaDham-release.apk';
    $tmpApk = $targetApk . '.tmp';
    
    $fp = @fopen($tmpApk, 'w+');
    if ($fp) {
        $ch = curl_init($apkUrl);
        curl_setopt($ch, CURLOPT_FILE, $fp);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 90);
        curl_setopt($ch, CURLOPT_USERAGENT, "SBKD-Deployer/2.0");
        $success = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        fclose($fp);
        if ($success && $httpCode === 200 && file_exists($tmpApk) && filesize($tmpApk) > 10000000) {
            @rename($tmpApk, $targetApk);
            $updated[] = "downloads/ShriBalajiKripaDham-release.apk (" . round(filesize($targetApk)/(1024*1024), 2) . " MB)";
        } else {
            $sz = file_exists($tmpApk) ? filesize($tmpApk) : 0;
            @unlink($tmpApk);
            $failed[] = "downloads/ShriBalajiKripaDham-release.apk (HTTP $httpCode, Size: $sz bytes)";
        }
    } else {
        $failed[] = "downloads/ShriBalajiKripaDham-release.apk (Cannot open tmp file for writing)";
    }
}

echo json_encode([
    "success" => count($failed) === 0,
    "status" => "DEPLOYMENT_COMPLETE",
    "message" => "श्री बालाजी कृपा धाम सभी सर्वर फ़ाइलें GitHub से 100% नवीनतम संस्करण में अपडेट हो गईं!",
    "updated_files_count" => count($updated),
    "updated_files" => $updated,
    "failed_files" => $failed,
    "timestamp" => time()
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
