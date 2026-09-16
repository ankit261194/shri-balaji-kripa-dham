<?php
header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
echo json_encode([
    "status" => "ONLINE",
    "name" => "श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक सेंट्रल API",
    "version" => "1.0.0",
    "domain" => "shribalajikripadham.online",
    "timestamp" => time()
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
