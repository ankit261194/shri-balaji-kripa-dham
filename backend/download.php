<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक हाई-स्पीड APK डाउनलोड सेवा
// Ultra High-Speed Direct Ashram Server APK Delivery Engine (v2.56.28)
// ==============================================================================

// Ankit EleveX Direct APK Delivery Block
$appParam = strtolower(trim($_GET['app'] ?? ''));
if ($appParam === 'elevex' || $appParam === 'ankit_elevex' || $appParam === 'ankit_elievex') {
    $targetFile = '';
    $elevexDir = __DIR__ . '/elevex_gateway/downloads';

    // 1. Look for v5.9.26 Final
    if (file_exists("$elevexDir/Ankit_EleveX_v5.9.26_Final.apk") && filesize("$elevexDir/Ankit_EleveX_v5.9.26_Final.apk") > 10000000) {
        $targetFile = "$elevexDir/Ankit_EleveX_v5.9.26_Final.apk";
    } else if (file_exists("$elevexDir/Ankit_EleveX_v5.9.25_Final.apk") && filesize("$elevexDir/Ankit_EleveX_v5.9.25_Final.apk") > 10000000) {
        $targetFile = "$elevexDir/Ankit_EleveX_v5.9.25_Final.apk";
    } else if (file_exists("$elevexDir/Ankit_EleveX_v5.9.24_Final.apk") && filesize("$elevexDir/Ankit_EleveX_v5.9.24_Final.apk") > 10000000) {
        $targetFile = "$elevexDir/Ankit_EleveX_v5.9.24_Final.apk";
    } else if (file_exists("$elevexDir/Ankit_EleveX_Release.apk") && filesize("$elevexDir/Ankit_EleveX_Release.apk") > 10000000) {
        $targetFile = "$elevexDir/Ankit_EleveX_Release.apk";
    } else {
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
        $filename = "Ankit_EleveX_v5.9.26_Final.apk";

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

    // High-Speed GitHub Releases Cloud Mirror Fallback (Always Guaranteed)
    header("Location: https://github.com/ankit261194/ankits-liftramban/releases/download/v5.9.26/Ankit_EleveX_v5.9.26_Final.apk");
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

// 2. Fallback to external CDN or GitHub if local file is missing
$cdnUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/latest/download/ShriBalajiKripaDham-release.apk";
if (file_exists(__DIR__ . '/version.json')) {
    $vj = @json_decode(file_get_contents(__DIR__ . '/version.json'), true);
    if (!empty($vj['apk_url']) && strpos($vj['apk_url'], 'download.php') === false) {
        $cdnUrl = $vj['apk_url'];
    }
}
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");
header("Expires: Mon, 26 Jul 1997 05:00:00 GMT");
header("Location: " . $cdnUrl, true, 302);
exit;
