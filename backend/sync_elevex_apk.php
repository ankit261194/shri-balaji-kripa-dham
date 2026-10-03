<?php
// ==============================================================================
// Ankit EleveX High-Speed APK Synchronizer & Cloud Storage Engine
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// ==============================================================================

ini_set('display_errors', 1);
error_reporting(E_ALL);
ini_set('max_execution_time', 300);
set_time_limit(300);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");

$targetDir = __DIR__ . '/elevex_gateway/downloads';
if (!is_dir($targetDir)) {
    @mkdir($targetDir, 0755, true);
}

$manifestPath = __DIR__ . '/elevex_gateway/app_update.json';
$latestVer = '5.9.27';
if (file_exists($manifestPath)) {
    $manifest = @json_decode(@file_get_contents($manifestPath), true);
    if (!empty($manifest['latest_version'])) {
        $latestVer = trim($manifest['latest_version']);
    }
}

if (!empty($_GET['version'])) {
    $latestVer = trim($_GET['version']);
}

// 0. Sync Manifest from GitHub
if (isset($_GET['sync_manifest'])) {
    $rawUrl = "https://raw.githubusercontent.com/ankit261194/shri-balaji-kripa-dham/main/backend/elevex_gateway/app_update.json";
    $json = @file_get_contents($rawUrl);
    if ($json && json_decode($json)) {
        @file_put_contents($manifestPath, $json);
        echo json_encode(["success" => true, "manifest" => json_decode($json, true)]);
        exit;
    }
}

$apkFilename = "Ankit_EleveX_v{$latestVer}_Final.apk";
$targetFile = $targetDir . '/' . $apkFilename;
$tmpFile = $targetFile . '.tmp';

// 1. Status Check
if (isset($_GET['status'])) {
    $exists = file_exists($targetFile);
    $size = $exists ? filesize($targetFile) : 0;
    echo json_encode([
        "success" => true,
        "filename" => $apkFilename,
        "exists" => $exists,
        "size" => $size,
        "size_mb" => round($size / (1024 * 1024), 2) . " MB",
        "last_modified" => $exists ? date("Y-m-d H:i:s", filemtime($targetFile)) : null
    ]);
    exit;
}

// 2. Reset Temporary Upload
if (isset($_GET['reset'])) {
    if (file_exists($tmpFile)) @unlink($tmpFile);
    echo json_encode(["success" => true, "message" => "Temporary upload reset"]);
    exit;
}

// 3. Chunked Upload Mode (for uploading from local PC in 2MB-5MB chunks)
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
            @copy($targetFile, $targetDir . '/Ankit_EleveX_Release.apk');
            echo json_encode([
                "success" => true,
                "completed" => true,
                "filename" => $apkFilename,
                "size" => filesize($targetFile),
                "size_mb" => round(filesize($targetFile) / (1024 * 1024), 2) . " MB"
            ]);
            exit;
        }
        
        echo json_encode([
            "success" => true,
            "chunk" => $chunkIndex,
            "bytes_so_far" => $currentSize
        ]);
        exit;
    } else {
        echo json_encode(["success" => false, "error" => "Could not open stream"]);
        exit;
    }
}

// 4. Server-Side Direct Pull from GitHub Releases (Fastest datacenter-to-datacenter)
if (isset($_GET['pull_github'])) {
    $githubUrl = "https://github.com/ankit261194/ankits-liftramban/releases/download/v{$latestVer}/Ankit_EleveX_v{$latestVer}_Final.apk";
    
    $fp = fopen($tmpFile, 'wb');
    if (!$fp) {
        echo json_encode(["success" => false, "error" => "Cannot write to tmpFile"]);
        exit;
    }
    
    $ch = curl_init($githubUrl);
    curl_setopt($ch, CURLOPT_FILE, $fp);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_MAXREDIRS, 5);
    curl_setopt($ch, CURLOPT_TIMEOUT, 180);
    curl_setopt($ch, CURLOPT_USERAGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    
    $success = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    fclose($fp);
    
    if ($success && ($httpCode == 200 || $httpCode == 302) && filesize($tmpFile) > 10000000) {
        rename($tmpFile, $targetFile);
        @copy($targetFile, $targetDir . '/Ankit_EleveX_Release.apk');
        echo json_encode([
            "success" => true,
            "method" => "pull_github",
            "size" => filesize($targetFile),
            "size_mb" => round(filesize($targetFile) / (1024 * 1024), 2) . " MB"
        ]);
        exit;
    } else {
        if (file_exists($tmpFile)) @unlink($tmpFile);
        echo json_encode([
            "success" => false,
            "http_code" => $httpCode,
            "curl_error" => $error
        ]);
        exit;
    }
}

echo json_encode(["message" => "Ankit EleveX Sync Engine Ready"]);
