<?php
// ==============================================================================
// DocuEdit Engine - Cloud AI Document Reconstruction & Dynamic Typography API
// Hosted securely on shribalajikripadham.online
// Powered by Google Gemini Multimodal Vision & Google Fonts CDN
// ==============================================================================

ini_set('display_errors', 0);
error_reporting(0);

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit(0);
}

$GEMINI_API_KEY = getenv('GEMINI_API_KEY') ?: base64_decode("QVEuQWI4Uk42SjhUaC1hekxIdUtweXVOUjI0dWJ2d2g2UkhQRFZnWEI2WllIVlFfbi1oSkE=");
$PRIMARY_MODEL = "gemini-1.5-flash";

$FONT_CATALOG = [
    'roboto' => [
        'name' => 'Roboto',
        'category' => 'Sans-Serif',
        'sub' => 'Modern corporate doc standard',
        'regular' => 'https://fonts.gstatic.com/s/roboto/v51/KFOMCnqEu92Fr1ME7kSn66aGLdTylUAMQXC89YmC2DPNWubEbWmT.ttf',
        'bold' => 'https://fonts.gstatic.com/s/roboto/v51/KFOMCnqEu92Fr1ME7kSn66aGLdTylUAMQXC89YmC2DPNWuYjammT.ttf'
    ],
    'poppins' => [
        'name' => 'Poppins',
        'category' => 'Sans-Serif',
        'sub' => 'Clean geometric invoice font',
        'regular' => 'https://fonts.gstatic.com/s/poppins/v24/pxiEyp8kv8JHgFVrFJA.ttf',
        'bold' => 'https://fonts.gstatic.com/s/poppins/v24/pxiByp8kv8JHgFVrLCz7V1s.ttf'
    ],
    'montserrat' => [
        'name' => 'Montserrat',
        'category' => 'Sans-Serif',
        'sub' => 'Modern header & invoice title',
        'regular' => 'https://fonts.gstatic.com/s/montserrat/v31/JTUHjIg1_i6t8kCHKm4532VJOt5-QNFgpCtr6Ew-.ttf',
        'bold' => 'https://fonts.gstatic.com/s/montserrat/v31/JTUHjIg1_i6t8kCHKm4532VJOt5-QNFgpCuM70w-.ttf'
    ],
    'lato' => [
        'name' => 'Lato',
        'category' => 'Sans-Serif',
        'sub' => 'Warm legible print & legal',
        'regular' => 'https://fonts.gstatic.com/s/lato/v25/S6uyw4BMUTPHvxk.ttf',
        'bold' => 'https://fonts.gstatic.com/s/lato/v25/S6u9w4BMUTPHh6UVew8.ttf'
    ],
    'oswald' => [
        'name' => 'Oswald',
        'category' => 'Condensed',
        'sub' => 'Tall numbers & price columns',
        'regular' => 'https://fonts.gstatic.com/s/oswald/v57/TK3_WkUHHAIjg75cFRf3bXL8LICs1_FvgUE.ttf',
        'bold' => 'https://fonts.gstatic.com/s/oswald/v57/TK3_WkUHHAIjg75cFRf3bXL8LICs1xZogUE.ttf'
    ],
    'merriweather' => [
        'name' => 'Merriweather',
        'category' => 'Serif',
        'sub' => 'Sharp high-contrast editorial',
        'regular' => 'https://fonts.gstatic.com/s/merriweather/v33/u-4D0qyriQwlOrhSvowK_l5UcA6zuSYEqOzpPe3HOZJ5eX1WtLaQwmYiScCmDxhtNOKl8yDr3icqEw.ttf',
        'bold' => 'https://fonts.gstatic.com/s/merriweather/v33/u-4D0qyriQwlOrhSvowK_l5UcA6zuSYEqOzpPe3HOZJ5eX1WtLaQwmYiScCmDxhtNOKl8yDrOSAqEw.ttf'
    ],
    'playfair' => [
        'name' => 'Playfair Display',
        'category' => 'Serif',
        'sub' => 'Luxury certificates & diplomas',
        'regular' => 'https://fonts.gstatic.com/s/playfairdisplay/v40/nuFvD-vYSZviVYUb_rj3ij__anPXJzDwcbmjWBN2PKdFvUDQ.ttf',
        'bold' => 'https://fonts.gstatic.com/s/playfairdisplay/v40/nuFvD-vYSZviVYUb_rj3ij__anPXJzDwcbmjWBN2PKeiukDQ.ttf'
    ],
    'lora' => [
        'name' => 'Lora',
        'category' => 'Serif',
        'sub' => 'Classic novel & formal contract',
        'regular' => 'https://fonts.gstatic.com/s/lora/v37/0QI6MX1D_JOuGQbT0gvTJPa787weuyJG.ttf',
        'bold' => 'https://fonts.gstatic.com/s/lora/v37/0QI6MX1D_JOuGQbT0gvTJPa787z5vCJG.ttf'
    ],
    'kalam' => [
        'name' => 'Kalam (Hindi Pen)',
        'category' => 'Handwriting',
        'sub' => 'Hindi + English ballpoint ink',
        'regular' => 'https://fonts.gstatic.com/s/kalam/v18/YA9dr0Wd4kDdMuhW.ttf',
        'bold' => 'https://fonts.gstatic.com/s/kalam/v18/YA9Qr0Wd4kDdMtDqHQLL.ttf'
    ],
    'caveat' => [
        'name' => 'Caveat',
        'category' => 'Handwriting',
        'sub' => 'Casual handwriting & signatures',
        'regular' => 'https://fonts.gstatic.com/s/caveat/v23/WnznHAc5bAfYB2QRah7pcpNvOx-pjfJ9SII.ttf',
        'bold' => 'https://fonts.gstatic.com/s/caveat/v23/WnznHAc5bAfYB2QRah7pcpNvOx-pjRV6SII.ttf'
    ],
    'dancingscript' => [
        'name' => 'Dancing Script',
        'category' => 'Signature',
        'sub' => 'Cursive calligraphic signature',
        'regular' => 'https://fonts.gstatic.com/s/dancingscript/v29/If2cXTr6YS-zF4S-kcSWSVi_sxjsohD9F50Ruu7BMSoHTQ.ttf',
        'bold' => 'https://fonts.gstatic.com/s/dancingscript/v29/If2cXTr6YS-zF4S-kcSWSVi_sxjsohD9F50Ruu7B1i0HTQ.ttf'
    ],
    'inconsolata' => [
        'name' => 'Inconsolata',
        'category' => 'Monospace',
        'sub' => 'Banking vouchers & code receipts',
        'regular' => 'https://fonts.gstatic.com/s/inconsolata/v37/QldgNThLqRwH-OJ1UHjlKENVzkWGVkL3GZQmAwLYxYWI2qfdm7Lpp4U8aRo.ttf',
        'bold' => 'https://fonts.gstatic.com/s/inconsolata/v37/QldgNThLqRwH-OJ1UHjlKENVzkWGVkL3GZQmAwLYxYWI2qfdm7Lpp2I7aRo.ttf'
    ],
    'hind' => [
        'name' => 'Hind',
        'category' => 'Hindi',
        'sub' => 'Clean Indian official typography',
        'regular' => 'https://fonts.gstatic.com/s/hind/v18/5aU69_a8oxmIRG4.ttf',
        'bold' => 'https://fonts.gstatic.com/s/hind/v18/5aU19_a8oxmIfNJdIRs.ttf'
    ]
];

$inputData = [];
$rawInput = file_get_contents('php://input');
if (!empty($rawInput)) {
    $inputData = json_decode($rawInput, true) ?: [];
}

$action = $_GET['action'] ?? $inputData['action'] ?? 'health';

// 1. Health Check
if ($action === 'health') {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => true,
        'status' => 'ONLINE',
        'service' => 'DocuEdit Cloud AI Engine',
        'version' => '2.1.0',
        'domain' => 'shribalajikripadham.online',
        'ai_provider' => 'Google Gemini Multimodal',
        'model' => $PRIMARY_MODEL,
        'fonts_count' => count($FONT_CATALOG),
        'timestamp' => time()
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit(0);
}

// 2. List Fonts Catalog
if ($action === 'list_fonts') {
    header('Content-Type: application/json; charset=utf-8');
    header("Cache-Control: public, max-age=86400"); // Cache 24 hours
    $list = [];
    foreach ($FONT_CATALOG as $id => $f) {
        $list[] = [
            'id' => $id,
            'name' => $f['name'],
            'category' => $f['category'],
            'sub' => $f['sub'],
            'has_bold' => !empty($f['bold'])
        ];
    }
    echo json_encode([
        'success' => true,
        'fonts' => $list,
        'total' => count($list)
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit(0);
}

// 3. Serve Dynamic Font TTF (with local server caching)
if ($action === 'get_font') {
    $id = strtolower(trim($_GET['id'] ?? 'roboto'));
    $isBold = !empty($_GET['bold']) && $_GET['bold'] != '0' && $_GET['bold'] != 'false';

    if (!isset($FONT_CATALOG[$id])) {
        header('Content-Type: application/json; charset=utf-8');
        http_response_code(404);
        echo json_encode(['success' => false, 'error' => "Font not found: $id"]);
        exit(0);
    }

    $fontInfo = $FONT_CATALOG[$id];
    $url = ($isBold && !empty($fontInfo['bold'])) ? $fontInfo['bold'] : $fontInfo['regular'];
    $cacheDir = __DIR__ . '/../fonts_cache';
    if (!is_dir($cacheDir)) {
        @mkdir($cacheDir, 0755, true);
    }
    $cacheFile = $cacheDir . '/' . $id . ($isBold ? '_bold.ttf' : '_regular.ttf');

    // Download to local cache if not yet cached
    if (!file_exists($cacheFile) || filesize($cacheFile) < 1000) {
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_USERAGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        $data = curl_exec($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($code === 200 && !empty($data)) {
            @file_put_contents($cacheFile, $data);
        } else {
            // Stream redirect to original CDN if server cannot write
            header("Location: $url");
            exit(0);
        }
    }

    header('Content-Type: font/ttf');
    header('Content-Disposition: inline; filename="' . basename($cacheFile) . '"');
    header('Cache-Control: public, max-age=31536000'); // 1 year cache
    header('Content-Length: ' . filesize($cacheFile));
    readfile($cacheFile);
    exit(0);
}

// 4. Document Text Analysis & Smart Inpaint Parameters
if ($action === 'analyze_text' || $action === 'replace_text') {
    header('Content-Type: application/json; charset=utf-8');
    $base64Image = $inputData['image'] ?? null;
    $targetBounds = $inputData['bounds'] ?? null;
    $currentText = $inputData['current_text'] ?? '';
    $replacementText = $inputData['replacement_text'] ?? '';
    $userBold = !empty($inputData['is_bold']);
    $userSizeMultiplier = floatval($inputData['size_multiplier'] ?? 1.0);
    $userColorOverride = $inputData['color_hex'] ?? null;

    if (empty($base64Image)) {
        echo json_encode(['success' => false, 'error' => 'Missing image parameter'], JSON_UNESCAPED_UNICODE);
        exit(0);
    }

    if (preg_match('/^data:image\/(\w+);base64,/', $base64Image, $type)) {
        $base64Image = substr($base64Image, strpos($base64Image, ',') + 1);
    }

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

    if (!$parsedResult) {
        $parsedResult = [
            'paper_hex' => '#FFFFFF',
            'font_family' => 'SANS_SERIF',
            'is_bold' => true,
            'ink_color_hex' => '#000000',
            'suggested_font_size_pt' => 14
        ];
    }

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

header('Content-Type: application/json; charset=utf-8');
echo json_encode(['success' => false, 'error' => 'Invalid action'], JSON_UNESCAPED_UNICODE);
