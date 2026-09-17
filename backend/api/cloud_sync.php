<?php
// Central Cloud Endpoint for Ashram SQLite Full Backup Sync
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

if (['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if (['REQUEST_METHOD'] === 'GET') {
     = __DIR__ . '/../uploads/backup_latest.json';
    if (file_exists()) {
        readfile();
    } else {
        echo json_encode(['success' => true, 'status' => 'READY', 'message' => 'Cloud Sync Endpoint is Online']);
    }
    exit;
}

 = file_get_contents('php://input');
if (empty()) {
    http_response_code(400);
    echo json_encode(['success' => false, 'error' => 'डेटा प्राप्त नहीं हुआ (Empty payload)'], JSON_UNESCAPED_UNICODE);
    exit;
}

 = __DIR__ . '/../uploads/';
if (!is_dir()) {
    mkdir(, 0755, true);
}

file_put_contents( . 'backup_latest.json', );

echo json_encode([
    'success' => true,
    'message' => 'क्लाउड डेटा सिंक सफल (Cloud sync successful)',
    'timestamp' => time(),
    'bytes_received' => strlen()
], JSON_UNESCAPED_UNICODE);
