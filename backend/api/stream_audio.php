<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक ऑडियो स्ट्रीमिंग इंजन
// Sacred Audio Streaming Engine with HTTP 206 Byte Range Support (v2.48.0)
// ==============================================================================

// Allow cross-origin media requests from app and web players
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, HEAD, OPTIONS");
header("Access-Control-Allow-Headers: Range, Content-Type, Accept");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$trackKey = strtolower(trim($_GET['track'] ?? ''));

// Map of sacred tracks to local filenames and high-speed spiritual CDN streams
$trackMap = [
    'hanuman_chalisa' => [
        'title' => 'Shri Hanuman Chalisa',
        'local_file' => 'hanuman_chalisa.mp3',
        'cdn_url' => 'https://ia800808.us.archive.org/5/items/05-shri-hanuman-chalisa/05%20SHRI%20HANUMAN%20CHALISA.mp3'
    ],
    'balaji_aarti' => [
        'title' => 'Shri Balaji Maha Aarti (Dungra Jat)',
        'local_file' => 'balaji_aarti.mp3',
        'cdn_url' => 'https://ia800808.us.archive.org/5/items/05-shri-hanuman-chalisa/07%20AARTI%20KIJAI%20HANUMAN%20LALA%20KI.mp3'
    ],
    'bajrang_baan' => [
        'title' => 'Bajrang Baan',
        'local_file' => 'bajrang_baan.mp3',
        'cdn_url' => 'https://ia801509.us.archive.org/7/items/bajrang-baan_202606/Bajrang%20Baan.mp3'
    ],
    'sankatmochan' => [
        'title' => 'Sankat Mochan Hanumanashtak',
        'local_file' => 'sankatmochan.mp3',
        'cdn_url' => 'https://ia800808.us.archive.org/5/items/05-shri-hanuman-chalisa/06%20SANKATMOCHAN%28HANUMAN%20ASHTAK%29.mp3'
    ],
    'aarti_kije' => [
        'title' => 'Aarti Kije Hanuman Lala Ki',
        'local_file' => 'aarti_kije.mp3',
        'cdn_url' => 'https://ia800808.us.archive.org/5/items/05-shri-hanuman-chalisa/07%20AARTI%20KIJAI%20HANUMAN%20LALA%20KI.mp3'
    ],
    'ram_stuti' => [
        'title' => 'Shri Ramchandra Kripalu Bhajuman',
        'local_file' => 'ram_stuti.mp3',
        'cdn_url' => 'https://ia800808.us.archive.org/5/items/05-shri-hanuman-chalisa/02%20Mangal%20Moorti%20Maruti%20Nandan%20-%20Jai%20Jai%20Bajrang%20Bali.mp3'
    ]
];

if (!isset($trackMap[$trackKey])) {
    header("Content-Type: application/json; charset=UTF-8");
    echo json_encode([
        'status' => 'error',
        'message' => 'Invalid track specified',
        'available_tracks' => array_keys($trackMap)
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

$trackInfo = $trackMap[$trackKey];
$localPath = __DIR__ . '/../media/' . $trackInfo['local_file'];

// If local file exists, serve with HTTP 206 Range support for instant playback & seek
if (file_exists($localPath) && filesize($localPath) > 50000) {
    serveLocalAudioFile($localPath, $trackInfo['local_file']);
    exit;
}

// If local file does not exist on disk yet, redirect directly to resilient CDN
$cdnUrl = $trackInfo['cdn_url'];
header("HTTP/1.1 302 Found");
header("Location: " . $cdnUrl);
header("Cache-Control: public, max-age=86400");
exit;

/**
 * Serves audio with complete HTTP 206 Partial Content byte range support.
 */
function serveLocalAudioFile($filePath, $fileName) {
    $fileSize = filesize($filePath);
    $fp = fopen($filePath, 'rb');
    if (!$fp) {
        http_response_code(500);
        echo 'Unable to read audio file';
        exit;
    }

    $start = 0;
    $end = $fileSize - 1;

    header('Content-Type: audio/mpeg');
    header('Accept-Ranges: bytes');
    header('Content-Disposition: inline; filename="' . $fileName . '"');
    header('Cache-Control: public, max-age=86400');

    if (isset($_SERVER['HTTP_RANGE'])) {
        $rangeHeader = $_SERVER['HTTP_RANGE'];
        if (preg_match('/bytes=\h*(\d+)-(\d*)[\D.*]?/i', $rangeHeader, $matches)) {
            $start = intval($matches[1]);
            if (!empty($matches[2])) {
                $end = intval($matches[2]);
            }
        }

        if ($start > $end || $start >= $fileSize) {
            header('HTTP/1.1 416 Requested Range Not Satisfiable');
            header("Content-Range: bytes */{$fileSize}");
            fclose($fp);
            exit;
        }

        if ($end >= $fileSize) {
            $end = $fileSize - 1;
        }

        $length = $end - $start + 1;
        header('HTTP/1.1 206 Partial Content');
        header("Content-Range: bytes {$start}-{$end}/{$fileSize}");
        header("Content-Length: {$length}");
    } else {
        header("Content-Length: {$fileSize}");
    }

    fseek($fp, $start);
    $remaining = $end - $start + 1;
    $bufferSize = 65536; // 64 KB streaming buffer

    while (!feof($fp) && $remaining > 0) {
        $readBytes = min($bufferSize, $remaining);
        $chunk = fread($fp, $readBytes);
        echo $chunk;
        $remaining -= strlen($chunk);
        flush();
    }

    fclose($fp);
    exit;
}