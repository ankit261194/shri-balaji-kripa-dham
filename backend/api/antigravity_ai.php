<?php
// ==============================================================================
// 🌺 SHRI BALAJI KRIPA DHAM - ANTIGRAVITY MOBILE AI GATEWAY
// Real Generative AI powered by Google Gemini 1.5 Flash
// Understands Super Admin & Guruji natural Hindi conversation, system queries,
// feature commands, and temple operations without static canned responses.
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

$rawInput = file_get_contents('php://input');
$data = !empty($rawInput) ? json_decode($rawInput, true) : [];

$userMessage = trim($data['message'] ?? $_POST['message'] ?? '');
$systemContext = $data['context'] ?? [];

if (empty($userMessage)) {
    echo json_encode([
        "success" => false,
        "reply" => "कृपया अपना संदेश या निर्देश लिखें।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// Gemini API Key from environment or fallback
$geminiApiKey = getenv('GEMINI_API_KEY') ?: base64_decode('QVEuQWI4Uk42S1hDQ0tDRHhjd2NwSUNmZGNYUlJLc1BSX3Z2WGl4TG5SeU84VHhkLWc0Z3c=');

if (empty($geminiApiKey)) {
    echo json_encode([
        "success" => false,
        "reply" => "Gemini AI API Key उपलब्ध नहीं है।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$systemPrompt = "आप 'एंटीग्रेविटी मोबाइल मास्टर AI' (Antigravity Mobile Studio AI) हैं — श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट, बुलन्दशहर) के सुपर एडमिन और पूज्य गुरुजी के निजी तकनीकी सहायक।\n"
    . "आप सीधे एडमिन / गुरुजी से हिंदी में बात कर रहे हैं।\n\n"
    . "आपके पास संपूर्ण ऐप का ज्ञान है:\n"
    . "1. 'पूज्य गुरुजी दरबार बिग स्क्रीन (Guruji Darbar Mode)': बुजुर्ग गुरुजी के लिए विशालकाय फुल-स्क्रीन मोड है जहाँ स्क्रीन पर कहीं भी हाथ मारने (टैप करने) पर अगला टोकन खुद-ब-खुद माइक/स्पीकर पर बोल जाता है और कतार आगे बढ़ती है। इसमें केवल वर्तमान टोकन नंबर, भक्त का नाम और अगला/पिछला बटन बहुत बड़े अक्षरों में दिखता है।\n"
    . "2. 'मास्टर टोकन बाईपास': 1-टैप में 0 सेकंड में पूरे देश/दुनिया के भक्तों के लिए टोकन चालू या बंद करने की सुविधा।\n"
    . "3. 'फ़ेक जीपीएस किल-स्विच': निर्दोष भक्तों को फ़ेक जीपीएस एरर से बचाने हेतु जांच बंद करने की सुविधा।\n"
    . "4. 'कागजी रजिस्टर स्कैनर': हाथ से लिखे रजिस्टर की फोटो से Gemini Vision AI द्वारा 1 सेकंड में टेबल बनाकर टोकन बनाने की सुविधा।\n"
    . "5. 'SQL कंसोल' और 'क्लाउड डिप्लॉयर': सीधे फोन से डेटाबेस क्वेरी और होस्टिंगर सर्वर अपडेट।\n\n"
    . "निर्देश:\n"
    . "- कभी भी रटा-रटाया या रोबोटिक जवाब न दें।\n"
    . "- यूजर के सवाल या विचार को ध्यान से समझें और दोस्ताना, आदरपूर्ण व स्पष्ट हिंदी में व्यावहारिक उत्तर दें।\n"
    . "- यदि यूजर गुरुजी के लिए बड़ी स्क्रीन या टोकन बुलाने की बात कर रहा है, तो उन्हें बताएं कि 'पूज्य गुरुजी दरबार स्क्रीन' तैयार है और इसे तुरंत खोलने का विकल्प दें।\n"
    . "- उत्तर 2 से 4 वाक्यों में संक्षिप्त, सटीक और सम्मानजनक रखें।";

$contextStr = "";
if (!empty($systemContext)) {
    $contextStr = "\n\nवर्तमान सिस्टम स्थिति: " . json_encode($systemContext, JSON_UNESCAPED_UNICODE);
}

$payload = [
    "contents" => [
        [
            "role" => "user",
            "parts" => [
                ["text" => $systemPrompt . $contextStr . "\n\nयूजर का संदेश: " . $userMessage]
            ]
        ]
    ],
    "generationConfig" => [
        "temperature" => 0.4,
        "maxOutputTokens" => 600
    ]
];

$apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" . urlencode($geminiApiKey);

$ch = curl_init($apiUrl);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
curl_setopt($ch, CURLOPT_TIMEOUT, 15);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($httpCode === 200 && !empty($response)) {
    $resJson = json_decode($response, true);
    $replyText = $resJson['candidates'][0]['content']['parts'][0]['text'] ?? '';
    if (!empty(trim($replyText))) {
        // Detect action intent
        $suggestedAction = null;
        $lowerMsg = mb_strtolower($userMessage, 'UTF-8');
        if (strpos($lowerMsg, 'guruji') !== false || strpos($lowerMsg, 'गुरुजी') !== false || strpos($lowerMsg, 'बड़ा') !== false || strpos($lowerMsg, 'हाथ') !== false || strpos($lowerMsg, 'स्क्रीन') !== false || strpos($lowerMsg, 'screen') !== false) {
            $suggestedAction = "OPEN_GURUJI_SCREEN";
        } elseif (strpos($lowerMsg, 'बाईपास') !== false || strpos($lowerMsg, 'bypass') !== false) {
            $suggestedAction = "TOGGLE_BYPASS";
        } elseif (strpos($lowerMsg, 'डिप्लॉय') !== false || strpos($lowerMsg, 'deploy') !== false) {
            $suggestedAction = "TRIGGER_DEPLOY";
        }

        echo json_encode([
            "success" => true,
            "reply" => trim($replyText),
            "suggested_action" => $suggestedAction
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }
}

// Fallback if network issue with Gemini
echo json_encode([
    "success" => false,
    "reply" => "क्षमा करें, AI सेवा से कनेक्ट नहीं हो सका। कृपया पुनः प्रयास करें।",
    "http_code" => $httpCode
], JSON_UNESCAPED_UNICODE);
exit;
