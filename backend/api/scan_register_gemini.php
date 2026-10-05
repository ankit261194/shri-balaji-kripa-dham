<?php
// ==============================================================================
// 🌺 SHRI BALAJI KRIPA DHAM - PAPER REGISTER VISION AI GATEWAY
// Uses Google Gemini Vision Multimodal AI to read handwritten Hindi paper registers
// Converted directly into structured Devotee records (Serial, Name, Phone, City).
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-SBKD-API-KEY");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

date_default_timezone_set('Asia/Kolkata');

// Retrieve base64 image or uploaded file
$rawInput = file_get_contents('php://input');
$data = !empty($rawInput) ? json_decode($rawInput, true) : [];

$imageBase64 = '';
if (!empty($data['image_base64'])) {
    $imageBase64 = $data['image_base64'];
} elseif (isset($_FILES['photo']) && is_uploaded_file($_FILES['photo']['tmp_name'])) {
    $imageBase64 = base64_encode(file_get_contents($_FILES['photo']['tmp_name']));
}

if (empty($imageBase64)) {
    echo json_encode([
        "success" => false,
        "message" => "रजिस्टर की फोटो प्राप्त नहीं हुई।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// Gemini API Key from environment or fallback
$geminiApiKey = getenv('GEMINI_API_KEY') ?: base64_decode('QVEuQWI4Uk42S1hDQ0tDRHhjd2NwSUNmZGNYUlJLc1BSX3Z2WGl4TG5SeU84VHhkLWc0Z3c=');

if (empty($geminiApiKey)) {
    echo json_encode([
        "success" => false,
        "message" => "Gemini Vision API Key कॉन्फ़िगर नहीं है।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$prompt = "You are an expert handwriting recognition processor for Indian temple & ashram devotee paper registers.\n"
    . "The provided image shows a handwritten Hindi notebook/register page containing devotee information.\n"
    . "Each line/row represents a devotee and typically contains:\n"
    . "1. Serial Number (क्र. सं.)\n"
    . "2. Devotee / Patient Name (भक्त या मरीज का नाम)\n"
    . "3. Mobile / Phone number (10 अंकों का मोबाइल नंबर)\n"
    . "4. Village / City / District (गाँव, शहर, जिला या पता)\n\n"
    . "Instructions:\n"
    . "- Read cursive and hasty Hindi handwriting carefully.\n"
    . "- Normalize phone numbers to 10 digits starting with 6, 7, 8, or 9. If incomplete, include whatever digits are visible.\n"
    . "- For city, if empty or nearby local, default to 'डूँगरा जाट'.\n"
    . "- Output ONLY a valid JSON array of objects with the exact schema:\n"
    . "[{\"serial\": 1, \"name\": \"...\", \"phone\": \"...\", \"city\": \"...\"}]\n"
    . "Do NOT wrap with markdown backticks, explanations, or conversational text. Output pure JSON only.";

$payload = [
    "contents" => [
        [
            "parts" => [
                ["text" => $prompt],
                [
                    "inline_data" => [
                        "mime_type" => "image/jpeg",
                        "data" => $imageBase64
                    ]
                ]
            ]
        ]
    ],
    "generationConfig" => [
        "temperature" => 0.1,
        "responseMimeType" => "application/json"
    ]
];

$apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" . urlencode($geminiApiKey);

$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch, CURLOPT_TIMEOUT, 20);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode === 200 && !empty($response)) {
    $resJson = json_decode($response, true);
    $text = $resJson['candidates'][0]['content']['parts'][0]['text'] ?? '';
    
    // Clean markdown
    $cleanText = trim($text);
    if (strpos($cleanText, '```') !== false) {
        $cleanText = preg_replace('/^```(?:json)?\s*/i', '', $cleanText);
        $cleanText = preg_replace('/```$/', '', trim($cleanText));
    }
    
    $parsedList = json_decode(trim($cleanText), true);
    if (is_array($parsedList)) {
        echo json_encode([
            "success" => true,
            "entries" => $parsedList,
            "raw_text" => $cleanText
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

echo json_encode([
    "success" => false,
    "message" => "Gemini Vision AI द्वारा इमेज प्रोसेस नहीं हो सकी (HTTP $httpCode)",
    "raw_response" => $response
], JSON_UNESCAPED_UNICODE);
exit;
