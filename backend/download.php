<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक हाई-स्पीड APK डाउनलोड सेवा
// Ultra High-Speed Direct Ashram Server APK Delivery Engine (v2.56.28)
// ==============================================================================

// Ankit EleveX Direct APK Delivery Block
$appParam = strtolower(trim($_GET['app'] ?? ''));
if ($appParam === 'elevex' || $appParam === 'ankit_elevex' || $appParam === 'ankit_elievex') {
    $elevexDir = __DIR__ . '/elevex_gateway/downloads';
    $manifestPath = __DIR__ . '/elevex_gateway/app_update.json';
    
    $latestVer = '5.9.26';
    if (file_exists($manifestPath)) {
        $manifest = @json_decode(@file_get_contents($manifestPath), true);
        if (!empty($manifest['latest_version'])) {
            $latestVer = trim($manifest['latest_version']);
        }
    }

    $targetFile = '';
    // 1. Check exact manifest version on local server
    $exactFile = "$elevexDir/Ankit_EleveX_v{$latestVer}_Final.apk";
    if (file_exists($exactFile) && filesize($exactFile) > 10000000) {
        $targetFile = $exactFile;
    } else {
        // 2. Fallback: Natural sort of all APKs descending (latest version always first)
        $files = glob("$elevexDir/Ankit_EleveX*.apk");
        if (!empty($files)) {
            natsort($files);
            $files = array_reverse($files);
            foreach ($files as $f) {
                if (filesize($f) > 10000000) {
                    $targetFile = $f;
                    break;
                }
            }
        }
    }

    if (!empty($targetFile) && file_exists($targetFile)) {
        $filesize = filesize($targetFile);
        $filename = basename($targetFile);

        while (ob_get_level()) ob_end_clean();
        header("Content-Type: application/vnd.android.package-archive");
        header("Content-Disposition: attachment; filename=\"{$filename}\"");
        header("Content-Length: {$filesize}");
        header("Accept-Ranges: bytes");
        header("Cache-Control: public, no-cache, no-store, must-revalidate, max-age=0");
        header("Pragma: no-cache");
        header("Expires: 0");

        $handle = fopen($targetFile, "rb");
        if ($handle) {
            while (!feof($handle)) {
                echo fread($handle, 1048576); // 1 MB chunk
                flush();
            }
            fclose($handle);
        } else {
            readfile($targetFile);
        }
        exit;
    }

    // High-Speed GitHub Releases Cloud Mirror Fallback (Always Guaranteed & Dynamic)
    header("Location: https://github.com/ankit261194/ankits-liftramban/releases/download/v{$latestVer}/Ankit_EleveX_v{$latestVer}_Final.apk");
    exit;
}

$requestedVer = isset($_GET['v']) ? intval($_GET['v']) : 0;
$targetFile = '';

// Priority 1: Requested version if specified
if ($requestedVer > 0) {
    $verFile = __DIR__ . "/downloads/ShriBalajiKripaDham-v{$requestedVer}.apk";
    if (file_exists($verFile) && filesize($verFile) > 10000000) {
        $targetFile = $verFile;
    }
}

// Priority 2: ShriBalajiKripaDham-release.apk (Always latest compiled build)
if (empty($targetFile)) {
    $relFile = __DIR__ . '/downloads/ShriBalajiKripaDham-release.apk';
    if (file_exists($relFile) && filesize($relFile) > 10000000) {
        $targetFile = $relFile;
    }
}

// Priority 3: Highest version found in downloads directory
if (empty($targetFile)) {
    $files = glob(__DIR__ . '/downloads/ShriBalajiKripaDham-*.apk');
    if (!empty($files)) {
        natsort($files);
        $files = array_reverse($files);
        foreach ($files as $f) {
            if (filesize($f) > 10000000) {
                $targetFile = $f;
                break;
            }
        }
    }
}

// 1. Direct High-Speed Static Delivery
if (file_exists($targetFile) && filesize($targetFile) > 10000000) {
    $filesize = filesize($targetFile);
    $filename = "ShriBalajiKripaDham.apk";
    $mtime = filemtime($targetFile);

    while (ob_get_level()) ob_end_clean();
    header("Content-Type: application/vnd.android.package-archive");
    header("Content-Disposition: attachment; filename=\"{$filename}\"");
    header("Accept-Ranges: bytes");
    header("Cache-Control: public, no-cache, no-store, must-revalidate, max-age=0");
    header("Pragma: no-cache");
    header("Expires: 0");

    // HTTP Range Support for pausing, resuming, and multi-stream download accelerators
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
            if ($fp) {
                fseek($fp, $from);
                $remaining = $to - $from + 1;
                while (!feof($fp) && $remaining > 0) {
                    $read = min(1048576, $remaining);
                    echo fread($fp, $read);
                    $remaining -= $read;
                    flush();
                }
                fclose($fp);
            }
            exit;
        }
    }

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

// 2. Fallback handling when local file is missing
$externalCdnUrl = '';
if (file_exists(__DIR__ . '/version.json')) {
    $vj = @json_decode(file_get_contents(__DIR__ . '/version.json'), true);
    if (!empty($vj['apk_url'])) {
        $candidateUrl = trim($vj['apk_url']);
        // Only redirect if candidateUrl is truly external (different host or not downloads/*.apk on same domain)
        $parsedHost = parse_url($candidateUrl, PHP_URL_HOST);
        $currentHost = $_SERVER['HTTP_HOST'] ?? 'shribalajikripadham.online';
        if ($parsedHost && $parsedHost !== $currentHost) {
            $externalCdnUrl = $candidateUrl;
        }
    }
}

if (!empty($externalCdnUrl)) {
    header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
    header("Pragma: no-cache");
    header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");
    header("Location: " . $externalCdnUrl, true, 302);
    exit;
}

// Devotional Fallback Status Page (Never redirect in a loop)
http_response_code(503);
header("Content-Type: text/html; charset=utf-8");
header("Retry-After: 30");
?>
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>श्री बालाजी कृपा धाम - ऐप डाउनलोड</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #FFF8E7; color: #3E2723; text-align: center; padding: 40px 20px; }
        .card { max-width: 500px; margin: 0 auto; background: white; border-radius: 20px; padding: 30px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); border-top: 5px solid #FF9800; }
        h1 { color: #E65100; font-size: 1.6rem; margin-bottom: 12px; }
        p { font-size: 1rem; line-height: 1.6; color: #5D4037; }
        .btn { display: inline-block; margin-top: 20px; padding: 12px 28px; background: linear-gradient(135deg, #FF9800, #E65100); color: white; text-decoration: none; border-radius: 25px; font-weight: bold; }
    </style>
</head>
<body>
    <div class="card">
        <h1>🚩 श्री बालाजी कृपा धाम</h1>
        <p>नवीनतम ऐप संस्करण सर्वर पर सिंक किया जा रहा है।<br>कृपया कुछ ही पलों में पुनः प्रयास करें।</p>
        <a href="download.php" class="btn">पुनः डाउनलोड करें</a>
    </div>
</body>
</html>
<?php
exit;

