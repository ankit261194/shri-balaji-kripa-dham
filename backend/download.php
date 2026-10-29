<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक हाई-स्पीड APK डाउनलोड सेवा
// Ultra High-Speed Direct Ashram Server APK Delivery Engine (v2.56.28)
// ==============================================================================

// Ankit EleveX Direct APK Delivery Block
$appParam = strtolower(trim($_GET['app'] ?? ''));
if ($appParam === 'elevex' || $appParam === 'ankit_elevex' || $appParam === 'ankit_elievex') {
    $candidateDirs = array_filter([
        __DIR__ . '/elevex_gateway/downloads',
        isset($_SERVER['DOCUMENT_ROOT']) ? $_SERVER['DOCUMENT_ROOT'] . '/elevex_gateway/downloads' : null,
        dirname(__DIR__) . '/elevex_gateway/downloads',
        __DIR__ . '/downloads',
        dirname(__DIR__) . '/downloads',
        '/home/u237101617/domains/shribalajikripadham.online/public_html/elevex_gateway/downloads'
    ]);

    $targetFile = '';
    foreach ($candidateDirs as $dir) {
        if (!is_dir($dir)) continue;

        // 1. Try exact v5.9.39 Final
        $v37 = "$dir/Ankit_EleveX_v5.9.39_Final.apk";
        if (file_exists($v37) && filesize($v37) > 10000000) {
            $targetFile = $v37;
            break;
        }

        // 2. Try exact v5.9.36 Final
        $v36 = "$dir/Ankit_EleveX_v5.9.36_Final.apk";
        if (file_exists($v36) && filesize($v36) > 10000000) {
            $targetFile = $v36;
            break;
        }

        // 3. Try Release APK
        $rel = "$dir/Ankit_EleveX_Release.apk";
        if (file_exists($rel) && filesize($rel) > 10000000) {
            $targetFile = $rel;
            break;
        }

        // 3. Glob any Ankit_EleveX*.apk descending
        $matches = glob("$dir/Ankit_EleveX*.apk");
        if (!empty($matches)) {
            natsort($matches);
            $matches = array_reverse($matches);
            foreach ($matches as $m) {
                if (filesize($m) > 10000000) {
                    $targetFile = $m;
                    break 2;
                }
            }
        }
    }

    if (!empty($targetFile) && file_exists($targetFile)) {
        $filesize = filesize($targetFile);
        $filename = "Ankit_EleveX_v5.9.39_Final.apk";

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

    // High-Speed Guaranteed GitHub CDN Mirror (v5.9.39 is 100% live on GitHub Releases)
    header("Location: https://github.com/ankit261194/ankits-liftramban/releases/download/v5.9.39/Ankit_EleveX_v5.9.39_Final.apk", true, 302);
    exit;
}

$requestedVer = isset($_GET['v']) ? intval($_GET['v']) : 0;
$candidateDirs = array_filter([
    __DIR__ . '/downloads',
    isset($_SERVER['DOCUMENT_ROOT']) ? $_SERVER['DOCUMENT_ROOT'] . '/downloads' : null,
    dirname(__DIR__) . '/downloads',
    '/home/u237101617/domains/shribalajikripadham.online/public_html/downloads'
]);

$targetFile = '';

// Priority 1: Requested version if specified
if ($requestedVer > 0) {
    foreach ($candidateDirs as $dir) {
        $verFile = "$dir/ShriBalajiKripaDham-v{$requestedVer}.apk";
        if (file_exists($verFile) && filesize($verFile) > 10000000) {
            $targetFile = $verFile;
            break;
        }
    }
}

// Priority 2: ShriBalajiKripaDham-release.apk (Always latest compiled build)
if (empty($targetFile)) {
    foreach ($candidateDirs as $dir) {
        $relFile = "$dir/ShriBalajiKripaDham-release.apk";
        if (file_exists($relFile) && filesize($relFile) > 10000000) {
            $targetFile = $relFile;
            break;
        }
    }
}

// Priority 3: Highest version found in downloads directory
if (empty($targetFile)) {
    foreach ($candidateDirs as $dir) {
        if (!is_dir($dir)) continue;
        $files = glob("$dir/ShriBalajiKripaDham-*.apk");
        if (!empty($files)) {
            natsort($files);
            $files = array_reverse($files);
            foreach ($files as $f) {
                if (filesize($f) > 10000000) {
                    $targetFile = $f;
                    break 2;
                }
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

// 2. Guaranteed Fallback handling when local file is missing
$externalCdnUrl = '';
if (file_exists(__DIR__ . '/version.json')) {
    $vj = @json_decode(file_get_contents(__DIR__ . '/version.json'), true);
    if (!empty($vj['apk_url'])) {
        $candidateUrl = trim($vj['apk_url']);
        $parsedHost = parse_url($candidateUrl, PHP_URL_HOST);
        $currentHost = $_SERVER['HTTP_HOST'] ?? 'shribalajikripadham.online';
        if ($parsedHost && $parsedHost !== $currentHost) {
            $externalCdnUrl = $candidateUrl;
        }
    }
}

// Guaranteed High-Speed GitHub Releases CDN Mirror (v2.81.0 Build 168 is 100% LIVE)
if (empty($externalCdnUrl)) {
    $tag = 'v2.81.0';
    $externalCdnUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/{$tag}/ShriBalajiKripaDham-release.apk";
}

if (!empty($externalCdnUrl)) {
    header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
    header("Pragma: no-cache");
    header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");
    header("Location: " . $externalCdnUrl, true, 302);
    exit;
}

// Devotional Fallback Status Page
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
