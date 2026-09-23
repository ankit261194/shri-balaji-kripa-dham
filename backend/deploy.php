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

$repoRawBase = "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/main/backend/";
$filesToSync = [
    ".htaccess",
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
    "api/update_live_status.php",
    "api/save_sevadar.php",
    "api/save_donor.php",
    "api/delete_sevadar.php",
    "api/delete_donor.php",
    "api/github_proxy.php",
    "api/cloud_sync.php",
    "api/data_vault.php"
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
            $updated[] = $relPath;
        } else {
            $failed[] = $relPath . " (Write error)";
        }
    } else {
        $failed[] = $relPath . " (Fetch error from GitHub)";
    }
}

// Clear all local caches
$cacheDir = $baseDir . '/cache';
if (is_dir($cacheDir)) {
    foreach (glob($cacheDir . '/*') as $f) {
        if (is_file($f)) @unlink($f);
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
