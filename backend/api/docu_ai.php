<?php
// ==============================================================================
// DocuEdit Engine - Cloud AI Document Reconstruction & Typography API
// Hosted securely on shribalajikripadham.online
// Powered by Google Gemini Multimodal Vision
// ==============================================================================

ini_set('display_errors', 0);
error_reporting(0);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit(0);
}

$GEMINI_API_KEY = getenv('GEMINI_API_KEY') ?: base64_decode("QVEuQWI4Uk42SjhUaC1hekxIdUtweXVOUjI0dWJ2d2g2UkhQRFZnWEI2WllIVlFfbi1oSkE=");
$PRIMARY_MODEL = "gemini-3.1-flash-lite";

$inputData = [];
$rawInput = file_get_contents('php://input');
if (!empty($rawInput)) {
    $inputData = json_decode($rawInput, true) ?: [];
}

$action = $_GET['action'] ?? $inputData['action'] ?? 'health';

// 1. Health Check
if ($action === 'health' || $_SERVER['REQUEST_METHOD'] === 'GET') {
    echo json_encode([
        'success' => true,
        'status' => 'ONLINE',
        'service' => 'DocuEdit Cloud AI Engine',
        'version' => '2.0.0',
        'domain' => 'shribalajikripadham.online',
        'ai_provider' => 'Google Gemini Multimodal',
        'model' => $PRIMARY_MODEL,
        'timestamp' => time()
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit(0);
}

// 2. Document Text Analysis & Smart Inpaint Parameters
if ($action === 'analyze_text' || $action === 'replace_text') {
    $base64Image = $inputData['image'] ?? null;
    $targetBounds = $inputData['bounds'] ?? null; // [left, top, right, bottom]
    $currentText = $inputData['current_text'] ?? '';
    $replacementText = $inputData['replacement_text'] ?? '';
    $userBold = !empty($inputData['is_bold']);
    $userSizeMultiplier = floatval($inputData['size_multiplier'] ?? 1.0);
    $userColorOverride = $inputData['color_hex'] ?? null;

    if (empty($base64Image)) {
        echo json_encode(['success' => false, 'error' => 'Missing image parameter'], JSON_UNESCAPED_UNICODE);
        exit(0);
    }

    // Strip data:image/...;base64, prefix if present
    if (preg_match('/^data:image\/(\w+);base64,/', $base64Image, $type)) {
        $base64Image = substr($base64Image, strpos($base64Image, ',') + 1);
    }

    // Call Gemini Multimodal to inspect typography and paper color
    $prompt = "You are a professional forensic document analysis and typography engine.\n" .
              "Look at this document image section. The user wants to replace the text '$currentText' with '$replacementText'.\n" .
              "Please analyze:\n" .
              "1. paper_hex: The EXACT hex color of the clean paper background immediately surrounding this text. CRITICAL: Strictly avoid sampling any dark table borders, lines, or gray table headers above/below!\n" .
              "2. font_family: Font category (SANS_SERIF, SERIF, or MONOSPACE).\n" .
              "3. is_bold: True if the original text was bold/heavy printed font, false if normal/light.\n" .
              "4. ink_color_hex: The rich dark ink color of the printed text (e.g. #000000 or deep navy/charcoal).\n" .
              "5. suggested_font_size_pt: Estimated visual point size (e.g. 10, 12, 14, 16).\n" .
              "Return STRICT JSON only matching this format:\n" .
              "{\"paper_hex\": \"#FFFFFF\", \"font_family\": \"SANS_SERIF\", \"is_bold\": true, \"ink_color_hex\": \"#000000\", \"suggested_font_size_pt\": 14}";

    $geminiPayload = [
        'contents' => [
            [
                'parts' => [
                    ['text' => $prompt],
                    [
                        'inlineData' => [
                            'mimeType' => 'image/jpeg',
                            'data' => $base64Image
                        ]
                    ]
                ]
            ]
        ],
        'generationConfig' => [
            'responseMimeType' => 'application/json',
            'temperature' => 0.1
        ]
    ];

    $geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/{$PRIMARY_MODEL}:generateContent?key={$GEMINI_API_KEY}";

    $ch = curl_init($geminiUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($geminiPayload));
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_TIMEOUT, 15);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

    $geminiResponse = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    $parsedResult = null;
    if ($httpCode === 200 && !empty($geminiResponse)) {
        $respJson = json_decode($geminiResponse, true);
        $candidateText = $respJson['candidates'][0]['content']['parts'][0]['text'] ?? null;
        if (!empty($candidateText)) {
            $parsedResult = json_decode($candidateText, true);
        }
    }

    // Fallback defaults if Gemini returns empty or throttled
    if (!$parsedResult) {
        $parsedResult = [
            'paper_hex' => '#FFFFFF',
            'font_family' => 'SANS_SERIF',
            'is_bold' => true,
            'ink_color_hex' => '#000000',
            'suggested_font_size_pt' => 14
        ];
    }

    // Apply user overrides if specified
    if ($userBold) $parsedResult['is_bold'] = true;
    if (!empty($userColorOverride)) $parsedResult['ink_color_hex'] = $userColorOverride;

    echo json_encode([
        'success' => true,
        'cloud_processed' => true,
        'model_used' => $PRIMARY_MODEL,
        'typography' => $parsedResult,
        'replacement_text' => $replacementText,
        'original_text' => $currentText,
        'timestamp' => time()
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit(0);
}

// Fallback for unknown action
echo json_encode(['success' => false, 'error' => 'Invalid action'], JSON_UNESCAPED_UNICODE);
