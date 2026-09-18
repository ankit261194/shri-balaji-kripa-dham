<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक हाई-स्पीड APK डाउनलोड सेवा
// High-Speed Direct Ashram Server APK Delivery Engine (v2.54.0 Build 72)
// ==============================================================================

$cdnUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/v2.54.0/ShriBalajiKripaDham-release.apk";
header("Location: " . $cdnUrl, true, 302);
exit;

// 1. Direct High-Speed Download Delivery if file exists on server
if ($targetFile && file_exists($targetFile)) {
    $filesize = filesize($targetFile);
    $filename = "ShriBalajiKripaDham-release.apk";

    // Set headers for APK download
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

// 2. If APK is temporarily pending upload on server, show clean download portal
$baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . ($_SERVER['HTTP_HOST'] ?? 'shribalajikripadham.online');
?>
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>श्री बालाजी कृपा धाम - ऐप डाउनलोड (v2.51.0)</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #FFF8E7; color: #212121; text-align: center; padding: 40px 20px; }
        .card { max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 20px; padding: 30px; box-shadow: 0 8px 30px rgba(128,0,0,0.12); border: 2px solid #FFD54F; }
        h1 { color: #800000; font-size: 1.5rem; margin-bottom: 8px; }
        p { color: #424242; font-size: 0.95rem; line-height: 1.6; }
        .btn { display: inline-block; background: #FF8F00; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 30px; font-weight: bold; font-size: 1.1rem; margin-top: 15px; box-shadow: 0 4px 15px rgba(255,143,0,0.4); }
        .btn:hover { background: #E65100; }
        .note { font-size: 0.82rem; color: #757575; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="card">
        <div style="font-size: 3rem; margin-bottom: 10px;">🚩</div>
        <h1>श्री बालाजी कृपा धाम</h1>
        <p><strong>आधिकारिक मोबाइल ऐप (v2.51.0 - Build 69)</strong></p>
        <p>रविवार दरबार टोकन, लाइव दर्शन, आरती व संपूर्ण आश्रम सेवाओं के लिए ऐप डाउनलोड करें।</p>
        <a href="<?= $baseUrl ?>/downloads/ShriBalajiKripaDham-release.apk" class="btn">📲 ऐप डाउनलोड करें (Direct APK)</a>
        <div class="note">
            यदि डाउनलोड स्वतः शुरू न हो, तो कृपया कुछ क्षण बाद पुनः प्रयास करें अथवा आश्रम व्यवस्थापक से संपर्क करें।
        </div>
    </div>
</body>
</html>
