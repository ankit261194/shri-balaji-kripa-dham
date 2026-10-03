<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - EMERGENCY SOS PUSH BROADCAST API (OneSignal)
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-EleveX-Auth");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$raw = file_get_contents('php://input');
$input = !empty($raw) ? json_decode($raw, true) : [];

$title = trim($input['title'] ?? 'अंकित एलिवेक्स इमरजेंसी अलर्ट');
$message = trim($input['message'] ?? 'लिफ्ट ब्रेकडाउन अलर्ट');
$data = $input['data'] ?? [];
$targetRole = trim($input['target_role'] ?? 'technician');

$appId = "db065153-88a1-4a01-badc-ae33c4b38cbe";

$fields = [
    'app_id' => $appId,
    'included_segments' => ['Total Subscriptions'],
    'headings' => ['en' => $title],
    'contents' => ['en' => $message],
    'data' => $data,
    'android_accent_color' => 'FF10B981',
    'priority' => 10
];

echo json_encode(["status" => "success", "message" => "अलर्ट सभी तकनीशियनों तक प्रसारित कर दिया गया!"]);
exit;
