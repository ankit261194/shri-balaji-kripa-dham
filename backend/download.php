<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक हाई-स्पीड APK डाउनलोड सेवा
// Ultra High-Speed Direct Ashram Server APK Delivery Engine (v2.56.28)
// ==============================================================================

$targetFile = __DIR__ . '/downloads/ShriBalajiKripaDham-release.apk';
if (!file_exists($targetFile) || filesize($targetFile) < 10000000) {
    // Also check version specific APK
    $files = glob(__DIR__ . '/downloads/ShriBalajiKripaDham-*.apk');
    if (!empty($files)) {
        rsort($files);
        $targetFile = $files[0];
    }
}

// 1. Direct High-Speed Static Delivery
if (file_exists($targetFile) && filesize($targetFile) > 10000000) {
    $filesize = filesize($targetFile);
    $filename = "ShriBalajiKripaDham.apk";
    $mtime = filemtime($targetFile);

    // LiteSpeed internal location acceleration (instant kernel-level sendfile)
    if (isset($_SERVER['SERVER_SOFTWARE']) && stripos($_SERVER['SERVER_SOFTWARE'], 'litespeed') !== false) {
        while (ob_get_level()) ob_end_clean();
        header("Content-Type: application/vnd.android.package-archive");
        header("Content-Disposition: attachment; filename=\"{$filename}\"");
        header("Accept-Ranges: bytes");
        header("Cache-Control: public, no-cache, no-store, must-revalidate, max-age=0");
        header("X-LiteSpeed-Location: /downloads/ShriBalajiKripaDham-release.apk");
        exit;
    }

    // Direct 302 Redirect to static file with cache-buster timestamp:
    // Enables multi-threaded HTTP/2 and HTTP/3 download direct from web server SSD at full 50-100 Mbps
    if (!isset($_GET['stream'])) {
        header("Cache-Control: no-cache, no-store, must-revalidate, max-age=0");
        header("Pragma: no-cache");
        header("Expires: 0");
        header("Location: downloads/ShriBalajiKripaDham-release.apk?v=" . $mtime, true, 302);
        exit;
    }

    // Direct chunked stream fallback for clients that don't follow redirects
    while (ob_get_level()) ob_end_clean();
    header("Content-Type: application/vnd.android.package-archive");
    header("Content-Disposition: attachment; filename=\"{$filename}\"");
    header("Accept-Ranges: bytes");
    header("Cache-Control: no-cache, no-store, must-revalidate, max-age=0");
    header("Pragma: no-cache");
    header("Expires: 0");
    header("Content-Length: {$filesize}");

    @set_time_limit(0);
    $fp = fopen($targetFile, 'rb');
    if ($fp) {
        while (!feof($fp)) {
            echo fread($fp, 1048576); // 1 MB chunks for high throughput
            flush();
        }
        fclose($fp);
    }
    exit;
}

// 2. Fallback to external CDN or GitHub if local file is missing
$cdnUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/latest/download/ShriBalajiKripaDham-release.apk";
if (file_exists(__DIR__ . '/version.json')) {
    $vj = @json_decode(file_get_contents(__DIR__ . '/version.json'), true);
    if (!empty($vj['apk_url'])) {
        $cdnUrl = $vj['apk_url'];
    }
}
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");
header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");
header("Location: " . $cdnUrl, true, 302);
exit;
