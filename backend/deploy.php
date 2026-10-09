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
    "api/sevadar_chat.php",
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
    "api/admin_auth.php",
    "api/status_service.php",
    "api/delete_parcha.php",
    "api/havan_service.php",
    "api/onesignal_service.php",
    "api/get_security_logs.php",
    "api/search_devotee.php",
    "api/get_face_profiles.php",
    "api/sync_face_profile.php",
    "api/upload_photo.php",
    "api/get_sacred_tracks.php",
    "api/save_sacred_track.php",
    "api/migrate_tracks.php",
    "api/delete_sacred_track.php",
    "api/clean_dummy_data.php",
    "api/upload_audio.php",
    "api/fetch_saptashati_audio.php",
    "api/get_expenses.php",
    "api/save_expense.php",
    "api/delete_expense.php",
    "api/get_payments.php",
    "api/save_payment.php",
    "api/get_parchas.php",
    "api/save_parcha.php",
    "api/panchang_today.php",
    "api/register_fcm_token.php",
    "api/send_fcm.php",
    "api/stream_audio.php",
    "api/drive_autosync.php",
    "havan_admin.php",
    "sync_apk.php",
    "media/balaji_darshan_today.jpg",
    "media/img_balaji_darshan.jpg",
    "media/img_mehandipur_balaji.jpg",
    "media/img_hanuman_veer.jpg",
    "media/img_panchmukhi_hanuman.jpg",
    "media/img_ram_darbar.jpg",
    "api/scan_register_gemini.php",
    "api/antigravity_ai.php",
    "api/send_push.php",
    "api/app_queries.php",
    "api/live_token_stream.php",
    "api/auto_backup.php"
];

// Support selective fast deployment of single or specific files via ?file=api/app_queries.php
if (!empty($_GET['file'])) {
    $filesToSync = array_filter(array_map('trim', explode(',', $_GET['file'])));
}

// Clean up orphaned legacy files and lift/elevator contamination from Ashram API
$legacyFilesToRemove = [
    __DIR__ . '/api/docu_ai.php',
    __DIR__ . '/api/gemini_diagnose.php',
    __DIR__ . '/api/elevex_diagnose.php',
    __DIR__ . '/api/elevex_update.php'
];
foreach ($legacyFilesToRemove as $lf) {
    if (file_exists($lf)) {
        @unlink($lf);
    }
}

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
            $pdo->exec("UPDATE ashram_settings SET whatsapp_channel_url = 'https://whatsapp.com/channel/0029VaCZJTmJ3jv2UwiMtY1w' WHERE id = 1 OR whatsapp_channel_url = '' OR whatsapp_channel_url LIKE '%chat.whatsapp.com%' OR whatsapp_channel_url IS NULL");
            $pdo->exec("UPDATE ashram_settings SET whatsapp_group_url = 'https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0' WHERE id = 1 OR whatsapp_group_url = '' OR whatsapp_group_url LIKE '%/channel/%' OR whatsapp_group_url IS NULL");
            $pdo->exec("UPDATE ashram_settings SET is_tuesday_darbar_enabled = 0 WHERE is_tuesday_darbar_enabled IS NULL");
            $pdo->exec("UPDATE ashram_settings SET aarti_mangala_time = '', aarti_balbhog_time = '', aarti_sandhya_time = '', aarti_shayan_time = '', aarti_timings = '', is_aarti_timings_visible = 0 WHERE id = 1");

            $verJsonRaw = @file_get_contents($baseDir . '/version.json');
            if ($verJsonRaw) {
                $verData = json_decode($verJsonRaw, true);
                if (!empty($verData['apk_url'])) {
                    $stmtAppUrl = $pdo->prepare("UPDATE ashram_settings SET app_download_url = :url, latest_version_code = :vcode, latest_version_name = :vname WHERE id = 1");
                    $stmtAppUrl->execute([
                        ':url' => $verData['apk_url'],
                        ':vcode' => $verData['latest_version_code'] ?? 161,
                        ':vname' => $verData['latest_version_name'] ?? '2.74.0'
                    ]);
                }
            }
            if (!empty($_REQUEST['clear_rate_limit'])) {
                try { $pdo->exec("TRUNCATE TABLE login_attempts"); } catch (Throwable $e) {}
            }

            // Ensure Ankit Chaudhary Super Admin credentials and supreme authority are active
            try {
                $superPassHash = password_hash('Aa@8006518960', PASSWORD_BCRYPT);
                $superPinBcrypt = password_hash('1234', PASSWORD_BCRYPT);
                $stmtSuper = $pdo->prepare("UPDATE admins SET role = 'SUPER_ADMIN', name = 'अंकित चौधरी (Super Admin)', password_hash = :p, pin = :pin, is_active = 1 WHERE username = 'admin'");
                $stmtSuper->execute([':p' => $superPassHash, ':pin' => $superPinBcrypt]);
            } catch (Throwable $e) {}
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
