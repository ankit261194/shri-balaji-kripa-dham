<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक हाई-स्पीड APK डाउनलोड सेवा
// High-Speed Direct Ashram Server APK Delivery Engine (v2.50.0)
// ==============================================================================

$version = "v2.50.0";
$localReleaseApk = __DIR__ . '/downloads/ShriBalajiKripaDham-release.apk';
$localVersionApk = __DIR__ . "/downloads/ShriBalajiKripaDham-{$version}.apk";

// Determine best local file candidate
$targetFile = null;
if (file_exists($localReleaseApk) && filesize($localReleaseApk) > 10000000) {
    $targetFile = $localReleaseApk;
} elseif (file_exists($localVersionApk) && filesize($localVersionApk) > 10000000) {
    $targetFile = $localVersionApk;
}

// Fallback GitHub release CDN url
$githubFallback = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/{$version}/ShriBalajiKripaDham-release.apk";

// If stream requested or direct client download without static server redirect
if (isset($_GET['stream']) && $targetFile) {
    $filesize = filesize($targetFile);
    $filename = basename($targetFile);

    header("Content-Type: application/vnd.android.package-archive");
    header("Content-Disposition: attachment; filename=\"{$filename}\"");
    header("Accept-Ranges: bytes");
    header("Cache-Control: public, max-age=3600");
    header("Pragma: public");

    if (isset($_SERVER['HTTP_RANGE'])) {
        list($param, $range) = explode('=', $_SERVER['HTTP_RANGE']);
        if (strtolower(trim($param)) === 'bytes') {
            list($from, $to) = explode('-', $range);
            $from = trim($from) === '' ? 0 : intval($from);
            $to = trim($to) === '' ? $filesize - 1 : intval($to);
            if ($to >= $filesize) $to = $filesize - 1;

            header('HTTP/1.1 206 Partial Content');
            header("Content-Range: bytes {$from}-{$to}/{$filesize}");
            header('Content-Length: ' . ($to - $from + 1));

            $fp = fopen($targetFile, 'rb');
            fseek($fp, $from);
            $remaining = $to - $from + 1;
            while (!feof($fp) && $remaining > 0) {
                $read = min(65536, $remaining);
                echo fread($fp, $read);
                $remaining -= $read;
                flush();
            }
            fclose($fp);
            exit;
        }
    }

    header("Content-Length: {$filesize}");
    readfile($targetFile);
    exit;
}

// Redirect to direct static file path for maximum web server sendfile speed
$baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . ($_SERVER['HTTP_HOST'] ?? 'shribalajikripadham.online');
if ($targetFile) {
    $publicPath = "{$baseUrl}/downloads/" . basename($targetFile);
    header("HTTP/1.1 302 Found");
    header("Location: " . $publicPath);
    header("Cache-Control: no-cache, must-revalidate");
    exit;
}

// Fallback to GitHub Release
header("HTTP/1.1 302 Found");
header("Location: " . $githubFallback);
header("Cache-Control: no-cache, must-revalidate");
exit;
