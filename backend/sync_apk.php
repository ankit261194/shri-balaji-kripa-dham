<?php
// Resilient Chunked & Resumable APK Sync Engine for Shri Balaji Kripa Dham
ini_set('display_errors', 1);
error_reporting(E_ALL);
ini_set('max_execution_time', 120);
set_time_limit(120);

header('Content-Type: application/json; charset=utf-8');

$apkUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/latest/download/ShriBalajiKripaDham-release.apk";
$dlDir = __DIR__ . '/downloads';
if (!is_dir($dlDir)) {
    @mkdir($dlDir, 0755, true);
}
$targetFile = $dlDir . '/ShriBalajiKripaDham-release.apk';
$tmpFile = $targetFile . '.tmp';

// Sync docu_ai.php hook
if (isset($_GET['sync_docu'])) {
    $sha = $_GET['sha'] ?? 'main';
    $url = "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/{$sha}/backend/api/docu_ai.php?t=" . time();
    $docuContent = @file_get_contents($url);
    if ($docuContent) {
        @file_put_contents(__DIR__ . '/api/docu_ai.php', $docuContent);
        echo json_encode(["success" => true, "docu_ai_synced" => true, "bytes" => strlen($docuContent), "sha" => $sha]);
        exit;
    }
}

// Sync version.json hook
if (isset($_GET['sync_version'])) {
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $raw = file_get_contents('php://input');
        if (!empty($raw)) {
            @file_put_contents(__DIR__ . '/version.json', $raw);
            echo json_encode(["success" => true, "version_synced" => true, "bytes" => strlen($raw)]);
            exit;
        }
    }
    $sha = $_GET['sha'] ?? 'main';
    $url = "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/{$sha}/backend/version.json?t=" . time();
    $verContent = @file_get_contents($url);
    if ($verContent) {
        @file_put_contents(__DIR__ . '/version.json', $verContent);
        echo json_encode(["success" => true, "version_synced" => true, "bytes" => strlen($verContent), "sha" => $sha]);
        exit;
    }
}

// Reset requested
if (isset($_GET['reset'])) {
    if (file_exists($tmpFile)) @unlink($tmpFile);
    echo json_encode(["success" => true, "message" => "Temporary download reset"]);
    exit;
}

// 1. Direct Upload Mode (via raw stream)
if (isset($_GET['upload']) && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = fopen('php://input', 'rb');
    $fp = fopen($tmpFile, 'wb');
    if ($input && $fp) {
        stream_copy_to_stream($input, $fp);
        fclose($input);
        fclose($fp);
        if (filesize($tmpFile) > 10000000) {
            rename($tmpFile, $targetFile);
            echo json_encode([
                "success" => true,
                "method" => "upload",
                "size" => filesize($targetFile),
                "size_mb" => round(filesize($targetFile) / (1024 * 1024), 2) . " MB"
            ]);
            exit;
        }
    }
}

// 1B. Chunked Upload Mode (Appends 5MB chunks to avoid timeouts)
if (isset($_GET['chunk_upload']) && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $chunkIndex = intval($_GET['chunk'] ?? 0);
    $isLast = isset($_GET['last']) && ($_GET['last'] === '1' || $_GET['last'] === 'true');
    $fp = fopen($tmpFile, $chunkIndex === 0 ? 'wb' : 'ab');
    $input = fopen('php://input', 'rb');
    if ($input && $fp) {
        stream_copy_to_stream($input, $fp);
        fclose($input);
        fclose($fp);
        $currentSize = file_exists($tmpFile) ? filesize($tmpFile) : 0;
        if ($isLast && $currentSize > 10000000) {
            rename($tmpFile, $targetFile);
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-release.apk');
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v112.apk');
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v111.apk');
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v110.apk');
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v109.apk');
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v108.apk');
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v107.apk');
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v106.apk');
            @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v105.apk');
            echo json_encode([
                "success" => true,
                "complete" => true,
                "size" => filesize($targetFile),
                "size_mb" => round(filesize($targetFile) / (1024 * 1024), 2) . " MB",
                "v107" => file_exists($dlDir . '/ShriBalajiKripaDham-v107.apk')
            ]);
            exit;
        }
        echo json_encode([
            "success" => true,
            "chunk" => $chunkIndex,
            "bytes_so_far" => $currentSize
        ]);
        exit;
    }
}

// 2. Resumable Chunked Download from GitHub CDN
$chHead = curl_init($apkUrl);
curl_setopt($chHead, CURLOPT_NOBODY, true);
curl_setopt($chHead, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($chHead, CURLOPT_MAXREDIRS, 5);
curl_setopt($chHead, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($chHead, CURLOPT_TIMEOUT, 15);
curl_setopt($chHead, CURLOPT_USERAGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
curl_exec($chHead);
$resolvedUrl = curl_getinfo($chHead, CURLINFO_EFFECTIVE_URL);
$totalSize = curl_getinfo($chHead, CURLINFO_CONTENT_LENGTH_DOWNLOAD);
curl_close($chHead);

if ($totalSize <= 0) {
    $totalSize = 62395672; // default expected size for v2.56.13
}

$currentSize = file_exists($tmpFile) ? filesize($tmpFile) : 0;

// Chunk size: 15MB
$chunkBytes = 15 * 1024 * 1024;
$startByte = $currentSize;
$endByte = min($startByte + $chunkBytes - 1, $totalSize - 1);

if ($startByte >= $totalSize && $currentSize > 10000000) {
    rename($tmpFile, $targetFile);
    @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-release.apk');
    @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v108.apk');
    @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v107.apk');
    echo json_encode([
        "success" => true,
        "complete" => true,
        "message" => "APK completely synced",
        "size" => filesize($targetFile),
        "size_mb" => round(filesize($targetFile) / (1024 * 1024), 2) . " MB"
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

$fp = fopen($tmpFile, $startByte === 0 ? 'wb' : 'ab');
if (!$fp) {
    echo json_encode(["success" => false, "error" => "Cannot open tmp file"]);
    exit;
}

$ch = curl_init($resolvedUrl);
curl_setopt($ch, CURLOPT_FILE, $fp);
curl_setopt($ch, CURLOPT_RANGE, "$startByte-$endByte");
curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($ch, CURLOPT_MAXREDIRS, 5);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_TIMEOUT, 40);
curl_setopt($ch, CURLOPT_USERAGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
$exec = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlErr = curl_error($ch);
curl_close($ch);
fclose($fp);

$newSize = file_exists($tmpFile) ? filesize($tmpFile) : 0;

if ($newSize >= $totalSize && $newSize > 10000000) {
    rename($tmpFile, $targetFile);
    @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-release.apk');
    @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v108.apk');
    @copy($targetFile, $dlDir . '/ShriBalajiKripaDham-v107.apk');
    echo json_encode([
        "success" => true,
        "complete" => true,
        "message" => "APK successfully synced to Hostinger",
        "downloaded_bytes" => $newSize,
        "total_bytes" => $totalSize,
        "size_mb" => round(filesize($targetFile) / (1024 * 1024), 2) . " MB"
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode([
        "success" => true,
        "complete" => false,
        "message" => "Chunk downloaded successfully",
        "downloaded_bytes" => $newSize,
        "total_bytes" => $totalSize,
        "progress_percent" => round(($newSize / $totalSize) * 100, 1),
        "http_code" => $httpCode
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
}
