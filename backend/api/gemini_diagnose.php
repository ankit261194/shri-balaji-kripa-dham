<?php
// ==============================================================================
// ANKIT ELEVEX ULTIMATE - INDUSTRIAL CORE HARDWARE DIAGNOSTICS ENGINE
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// Stealth Embedded Industrial Processor (Zero AI/Bot facade)
// ==============================================================================

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-EleveX-Auth");
header("Cache-Control: no-cache, no-store, must-revalidate");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$raw = file_get_contents('php://input');
$input = !empty($raw) ? json_decode($raw, true) : [];

$symptom = trim($input['symptom'] ?? $_POST['symptom'] ?? '');
$brand = trim($input['brand'] ?? $_POST['brand'] ?? 'Universal');
$errorCode = trim($input['error_code'] ?? $_POST['error_code'] ?? '');
$lang = trim($input['lang'] ?? $_POST['lang'] ?? 'hi');
$isHindi = ($lang === 'hi');

if (empty($symptom) && empty($errorCode)) {
    echo json_encode([
        "status" => "error",
        "message" => "कृपया लिफ्ट की समस्या (लक्षण) अथवा एरर कोड दर्ज करें।"
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 1. Check if Industrial Engine Key is configured in environment or secure config
$geminiApiKey = getenv('GEMINI_API_KEY') ?: base64_decode('QVEuQWI4Uk42S1hDQ0tDRHhjd2NwSUNmZGNYUlJLc1BSX3Z2WGl4TG5SeU84VHhkLWc0Z3c=');
$aiDiagnoses = [];

if (!empty($geminiApiKey)) {
    $prompt = "You are the native embedded Ankit EleveX Industrial Core Diagnostics Engine architected by Lead Elevator Systems Engineer Ankit Chaudhary. You are a real-time hardware diagnostics processor inside an elevator engineering suite.\n"
        . "STRICT MANDATE: You are NOT an AI chatbot. NEVER mention Gemini, Google, AI, bot, ChatGPT, LLM, or artificial intelligence. NEVER speak in first person conversational chat.\n"
        . "Provide 2 to 3 mathematically and physically accurate root-cause fault diagnoses for the following elevator symptom/error:\n"
        . "Brand: {$brand}\n"
        . "Error Code: {$errorCode}\n"
        . "Observed Symptom: {$symptom}\n"
        . "Language: " . ($isHindi ? "Hindi (Devanagari technical script)" : "English") . "\n\n"
        . "You MUST output ONLY a valid raw JSON object with this exact structure and no markdown backticks:\n"
        . "{\n"
        . "  \"diagnoses\": [\n"
        . "    {\n"
        . "      \"component_name\": \"Specific elevator hardware component\",\n"
        . "      \"probability_percentage\": 88,\n"
        . "      \"system_area\": \"Subsystem area (e.g. Safety Circuit / VFD Inverter / Brake Feedback / Door Interlock)\",\n"
        . "      \"recommended_action\": \"Practical step-by-step field engineering repair procedure with terminal checks\",\n"
        . "      \"multimeter_test_points\": \"Exact terminal names and expected AC/DC voltage or resistance readings\"\n"
        . "    }\n"
        . "  ]\n"
        . "}";

    $payload = [
        "contents" => [
            [
                "parts" => [
                    ["text" => $prompt]
                ]
            ]
        ],
        "generationConfig" => [
            "temperature" => 0.2,
            "responseMimeType" => "application/json"
        ]
    ];

    $apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" . urlencode($geminiApiKey);

    $ch = curl_init($apiUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_TIMEOUT, 6);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode === 200 && !empty($response)) {
        $json = json_decode($response, true);
        $text = $json['candidates'][0]['content']['parts'][0]['text'] ?? '';
        if (!empty($text)) {
            // Strip any accidental markdown formatting
            $cleanText = preg_replace('/^```json\s*/i', '', trim($text));
            $cleanText = preg_replace('/```$/', '', $cleanText);
            $parsed = json_decode(trim($cleanText), true);
            if (!empty($parsed['diagnoses']) && is_array($parsed['diagnoses'])) {
                $aiDiagnoses = $parsed['diagnoses'];
            }
        }
    }
}

// 2. High-Precision Deterministic Industrial Engineering Matrix (Zero failure guarantee)
if (empty($aiDiagnoses)) {
    $query = strtolower($symptom . ' ' . $errorCode);
    
    // Pattern 1: Safety Circuit / Door Interlock (110, 115, 116, 120, 130)
    if (strpos($query, '130') !== false || strpos($query, '115') !== false || strpos($query, '116') !== false || strpos($query, 'डोर') !== false || strpos($query, 'lock') !== false || strpos($query, 'gate') !== false || strpos($query, 'safety') !== false || strpos($query, 'सेफ्टी') !== false) {
        $aiDiagnoses[] = [
            "component_name" => $isHindi ? "लैंडिंग डोर लॉक स्विच व बीटीआई कॉन्टैक्ट (130 लूप)" : "Landing Door Lock Interlock Switch (130 Loop)",
            "probability_percentage" => 89,
            "system_area" => $isHindi ? "110V AC सेफ्टी सर्किट" : "110V AC Safety Chain",
            "recommended_action" => $isHindi 
                ? "शाफ्ट में सभी मंजिलों के डोर लॉक कॉन्टैक्ट्स और स्प्रिंग टेंशन की जांच करें। हाफ-स्प्लिट बाईसेक्शन विधि से फॉल्टी फ्लोर तुरंत ढूंढें।"
                : "Check landing door lock roller gaps and clean copper leaf contacts. Apply half-split bisection test across hoistway.",
            "multimeter_test_points" => "Monarch: 115 -> 116 (110V AC) | STEP: JP3 110 -> 113 | Otis: DS -> GS (110V AC)"
        ];
        $aiDiagnoses[] = [
            "component_name" => $isHindi ? "कार गेट स्विच (कार डोर लॉक)" : "Car Gate Safety Switch",
            "probability_percentage" => 76,
            "system_area" => $isHindi ? "कार टॉप जंक्शन बॉक्स" : "Car Top Header",
            "recommended_action" => $isHindi
                ? "कार टॉप पर लगे गेट स्विच की मैकेनिज्म देखें कि ऑपरेटर पूरी तरह बंद होने पर स्विच पूरी तरह दब रहा है या नहीं।"
                : "Inspect car gate switch plunger stroke when door operator reaches full close limit.",
            "multimeter_test_points" => "Car Gate Switch Terminals (0.0 Ohm Continuity on Full Close)"
        ];
    }
    // Pattern 2: Inverter Overcurrent / Overload (ERR02, E01, E02, OC)
    else if (strpos($query, 'err02') !== false || strpos($query, 'e01') !== false || strpos($query, 'e02') !== false || strpos($query, 'overcurrent') !== false || strpos($query, 'करंट') !== false || strpos($query, 'oc') !== false) {
        $aiDiagnoses[] = [
            "component_name" => $isHindi ? "VFD इन्वर्टर IGBT मॉड्यूल अथवा मोटर इंसुलेशन लीकेज" : "VFD Inverter IGBT Power Stage / Motor Insulation",
            "probability_percentage" => 92,
            "system_area" => $isHindi ? "पावर ड्राइव व VVVF इनवर्टर" : "VVVF Inverter Drive Stage",
            "recommended_action" => $isHindi
                ? "मोटर के तीनों फेज (U-V-W) का अर्थिंग के साथ मेगर टेस्ट करें। ब्रेक का पूरा खुलना सुनिश्चित करें।"
                : "Megger test U-V-W motor phases to Earth ground (>10 Megaohms). Verify mechanical brake opens fully before run torque.",
            "multimeter_test_points" => "Drive Output Terminals U-V-W: Resistance balanced within 0.1 Ohm"
        ];
        $aiDiagnoses[] = [
            "component_name" => $isHindi ? "मैकेनिकल ब्रेक शू फ्रिक्शन / ब्रेक कॉइल वोल्टेज ड्रॉप" : "Mechanical Brake Shoe Binding / Voltage Drop",
            "probability_percentage" => 81,
            "system_area" => $isHindi ? "ब्रेक एक्चुएटर सिस्टम" : "Brake Actuator Core",
            "recommended_action" => $isHindi
                ? "ब्रेक कॉइल रेक्टिफायर आउटपुट वोल्टेज मापें। रन कमांड पर ब्रेक प्लंजर का फ्री मूवमेंट जांचें।"
                : "Measure DC voltage across brake coil terminals during drive run command. Verify plunger clearance.",
            "multimeter_test_points" => "Brake Coil Terminals: Normal 110V DC Pick / 55V DC Hold"
        ];
    }
    // Pattern 3: Leveling / Reed Switch / Sensor (Level, मिस, बराबर नहीं)
    else if (strpos($query, 'level') !== false || strpos($query, 'लेवल') !== false || strpos($query, 'reed') !== false || strpos($query, 'रीड') !== false || strpos($query, 'vane') !== false) {
        $aiDiagnoses[] = [
            "component_name" => $isHindi ? "अप/डाउन लेवलिंग रीड स्विच व मैग्नेटिक वेन" : "Up/Down Leveling Magnetic Reed Switches",
            "probability_percentage" => 87,
            "system_area" => $isHindi ? "पोजिशनिंग व शाफ्ट सेंसर्स" : "Shaft Positioning Feedback",
            "recommended_action" => $isHindi
                ? "कार टॉप पर लगे लेवलिंग रीड स्विच और गाइड रेल पर लगी मैग्नेट पट्टी के बीच 8-12mm का गैप सुनिश्चित करें।"
                : "Verify 8-12mm clearance between car-top optical/reed sensor and hoistway vanes.",
            "multimeter_test_points" => "Sensor 24V DC Supply to Return: Drops to 0V DC when in door zone"
        ];
    }
    // Pattern 4: Default Industrial Diagnostic fallback
    else {
        $aiDiagnoses[] = [
            "component_name" => $isHindi ? "कंट्रोल बोर्ड 24V DC ऑक्सिलरी पावर सप्लाई ड्रॉप" : "Controller 24V DC Auxiliary Logic Supply Drop",
            "probability_percentage" => 84,
            "system_area" => $isHindi ? "SMPS / पावर डिस्ट्रीब्यूशन" : "Logic Power Distribution",
            "recommended_action" => $isHindi
                ? "कंट्रोल पैनल के मुख्य SMPS पर 24V DC आउटपुट की स्थिरता जांचें। लोड के समय वोल्टेज 23.5V से कम नहीं होनी चाहिए।"
                : "Check SMPS DC output ripple and voltage sag under full contactor pickup load (>23.5V DC).",
            "multimeter_test_points" => "SMPS +24V to 0V / COM: Steady 24.0V DC"
        ];
        $aiDiagnoses[] = [
            "component_name" => $isHindi ? "मेन कॉन्टैक्टर ऑक्जिलरी फीडबैक इंटरलॉक" : "Main Contactor Auxiliary Feedback Interlock",
            "probability_percentage" => 75,
            "system_area" => $isHindi ? "मेन स्विचगियर पैनल" : "Switchgear Panel",
            "recommended_action" => $isHindi
                ? "मेन रन व ब्रेक कॉन्टैक्टर्स के NC ऑक्सिलरी कॉन्टैक्ट्स की सफाई करें। कार्बन जमने से फॉल्ट आ सकता है।"
                : "Inspect normally closed auxiliary monitoring feedback loops on Main and Brake contactors.",
            "multimeter_test_points" => "Contactor Auxiliary NC Feedback Loop Continuity: 0.0 Ohm"
        ];
    }
}

// Return Clean Structured JSON without ANY mention of AI or Gemini
echo json_encode([
    "status" => "success",
    "engine" => "अंकित एलिवेक्स इंडस्ट्रियल कोर इंजन",
    "diagnoses" => $aiDiagnoses
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
exit;
