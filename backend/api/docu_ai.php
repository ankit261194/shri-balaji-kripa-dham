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

    // Server-Side Image Patch Synthesis with GD & TrueType
    $editedPatchBase64 = null;
    $decodedImg = @base64_decode($base64Image);
    if ($decodedImg && ($srcImg = @imagecreatefromstring($decodedImg))) {
        $imgW = imagesx($srcImg);
        $imgH = imagesy($srcImg);

        // Parse ink color
        $inkHex = ltrim($parsedResult['ink_color_hex'] ?? '#000000', '#');
        $r = hexdec(substr($inkHex, 0, 2) ?: '00');
        $g = hexdec(substr($inkHex, 2, 2) ?: '00');
        $b = hexdec(substr($inkHex, 4, 2) ?: '00');

        // Parse paper color
        $paperHex = ltrim($parsedResult['paper_hex'] ?? '#FFFFFF', '#');
        $pr = hexdec(substr($paperHex, 0, 2) ?: 'FF');
        $pg = hexdec(substr($paperHex, 2, 2) ?: 'FF');
        $pb = hexdec(substr($paperHex, 4, 2) ?: 'FF');

        $paperColor = imagecolorallocate($srcImg, $pr, $pg, $pb);
        $textColor = imagecolorallocate($srcImg, $r, $g, $b);

        // Clear text zone with ambient paper color
        $pad = 4;
        imagefilledrectangle($srcImg, $pad, $pad, $imgW - $pad, $imgH - $pad, $paperColor);

        // Determine font TTF
        $fontName = ($parsedResult['font_family'] === 'SERIF') ? 'merriweather' : 'roboto';
        $fontPath = __DIR__ . '/../fonts_cache/' . $fontName . ($parsedResult['is_bold'] ? '_bold.ttf' : '_regular.ttf');

        $fontSize = max(9, min(40, intval(($imgH - $pad * 2) * 0.65)));
        $textX = $pad + 4;
        $textY = intval($imgH / 2 + $fontSize / 2.3);

        if (file_exists($fontPath)) {
            @imagettftext($srcImg, $fontSize, 0, $textX, $textY, $textColor, $fontPath, $replacementText);
        } else {
            @imagestring($srcImg, 5, $textX, intval($imgH / 2 - 8), $replacementText, $textColor);
        }

        ob_start();
        imagejpeg($srcImg, null, 92);
        $jpgData = ob_get_clean();
        imagedestroy($srcImg);

        if (!empty($jpgData)) {
            $editedPatchBase64 = base64_encode($jpgData);
        }
    }

    echo json_encode([
        'success' => true,
        'cloud_processed' => true,
        'model_used' => $PRIMARY_MODEL,
        'typography' => $parsedResult,
        'edited_patch_base64' => $editedPatchBase64,
        'replacement_text' => $replacementText,
        'original_text' => $currentText,
        'timestamp' => time()
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit(0);
}

// 5. Enterprise Handwriting to Text AI (Gemini 1.5 Flash Vision OCR)
if ($action === 'handwriting_ocr') {
    header('Content-Type: application/json; charset=utf-8');
    $base64Image = $inputData['image'] ?? null;
    $customKey = $inputData['gemini_api_key'] ?? $_SERVER['HTTP_X_GEMINI_KEY'] ?? null;
    $activeKey = (!empty($customKey) && strlen($customKey) > 10) ? $customKey : $GEMINI_API_KEY;

    if (empty($base64Image)) {
        echo json_encode(['success' => false, 'error' => 'Missing image parameter'], JSON_UNESCAPED_UNICODE);
        exit(0);
    }

    if (preg_match('/^data:image\/(\w+);base64,/', $base64Image, $type)) {
        $base64Image = substr($base64Image, strpos($base64Image, ',') + 1);
    }

    $prompt = "You are a world-class paleographer, forensic handwriting examiner, and document transcription AI.\n" .
              "Carefully transcribe ALL handwritten or cursive text in this image with extreme accuracy.\n" .
              "Guidelines:\n" .
              "1. Faithfully extract English, Hindi (Devanagari), numbers, mathematical notations, and special characters.\n" .
              "2. Maintain document structure: use markdown '# Heading' for main titles, '## Subheading' for sections, '- ' or '1. ' for lists.\n" .
              "3. If tabular notes or columns exist, format them as clean markdown tables (| col1 | col2 |).\n" .
              "4. Preserve paragraph breaks with double newlines.\n" .
              "5. Do NOT add conversational banter, intro greetings, or meta commentary. Return ONLY the transcribed text.";

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
            'temperature' => 0.15,
            'maxOutputTokens' => 4096
        ]
    ];

    $geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/{$PRIMARY_MODEL}:generateContent?key={$activeKey}";

    $ch = curl_init($geminiUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($geminiPayload));
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

    $geminiResponse = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    $transcription = "";
    if ($httpCode === 200 && !empty($geminiResponse)) {
        $respJson = json_decode($geminiResponse, true);
        $candidateText = $respJson['candidates'][0]['content']['parts'][0]['text'] ?? '';
        $transcription = trim($candidateText);
    }

    if (empty($transcription)) {
        echo json_encode([
            'success' => false,
            'error' => 'Handwriting transcription failed or empty. Please ensure clear lighting.'
        ], JSON_UNESCAPED_UNICODE);
        exit(0);
    }

    $wordCount = count(preg_split('/\s+/', $transcription, -1, PREG_SPLIT_NO_EMPTY));
    $charCount = mb_strlen($transcription, 'UTF-8');

    echo json_encode([
        'success' => true,
        'cloud_ai' => true,
        'model' => $PRIMARY_MODEL,
        'transcription' => $transcription,
        'word_count' => $wordCount,
        'char_count' => $charCount,
        'timestamp' => time()
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit(0);
}

// 5.5 Enterprise Document Line OCR with Bounding Boxes (Gemini 1.5 Flash)
if ($action === 'detect_text_boxes') {
    header('Content-Type: application/json; charset=utf-8');
    $base64Image = $inputData['image'] ?? null;
    $customKey = $inputData['gemini_api_key'] ?? $_SERVER['HTTP_X_GEMINI_KEY'] ?? null;
    $activeKey = (!empty($customKey) && strlen($customKey) > 10) ? $customKey : $GEMINI_API_KEY;

    if (empty($base64Image)) {
        echo json_encode(['success' => false, 'error' => 'Missing image parameter'], JSON_UNESCAPED_UNICODE);
        exit(0);
    }

    if (preg_match('/^data:image\/\w+;base64,/', $base64Image, $type)) {
        $base64Image = substr($base64Image, strpos($base64Image, ',') + 1);
    }

    $prompt = "You are an enterprise document OCR engine. Detect ALL lines of text in this document image (in Hindi or English).\n" .
              "For each line, return its text and its 2D bounding box normalized to 0..1000 in [ymin, xmin, ymax, xmax] format.\n" .
              "STRICT JSON output only in this structure:\n" .
              "{\"lines\": [{\"text\": \"string\", \"box_2d\": [ymin, xmin, ymax, xmax]}]}";

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
            'temperature' => 0.1,
            'maxOutputTokens' => 4096
        ]
    ];

    $geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/{$PRIMARY_MODEL}:generateContent?key={$activeKey}";

    $ch = curl_init($geminiUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($geminiPayload));
    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
    curl_setopt($ch, CURLOPT_TIMEOUT, 25);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

    $geminiResponse = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    $lines = [];
    if ($httpCode === 200 && !empty($geminiResponse)) {
        $respJson = json_decode($geminiResponse, true);
        $candidateText = $respJson['candidates'][0]['content']['parts'][0]['text'] ?? '';
        if (!empty($candidateText)) {
            $parsed = json_decode($candidateText, true);
            if (isset($parsed['lines']) && is_array($parsed['lines'])) {
                $lines = $parsed['lines'];
            }
        }
    }

    echo json_encode([
        'success' => true,
        'cloud_ai' => true,
        'count' => count($lines),
        'lines' => $lines
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit(0);
}

// 6. Multi-Device Cloud Sync & Document Hosting (CamScanner-Style Web Portal)
if ($action === 'cloud_upload') {
    header('Content-Type: application/json; charset=utf-8');
    $fileBase64 = $inputData['file_base64'] ?? null;
    $fileType = strtolower(trim($inputData['file_type'] ?? 'pdf'));
    $title = trim($inputData['title'] ?? 'Document_' . date('Ymd_His'));
    $pagesCount = intval($inputData['pages_count'] ?? 1);

    if (empty($fileBase64)) {
        echo json_encode(['success' => false, 'error' => 'Missing file_base64 parameter'], JSON_UNESCAPED_UNICODE);
        exit(0);
    }

    if (preg_match('/^data:(application\/pdf|image\/\w+);base64,/', $fileBase64, $type)) {
        $fileBase64 = substr($fileBase64, strpos($fileBase64, ',') + 1);
    }

    $fileData = @base64_decode($fileBase64);
    if (!$fileData || strlen($fileData) < 10) {
        echo json_encode(['success' => false, 'error' => 'Invalid file data'], JSON_UNESCAPED_UNICODE);
        exit(0);
    }

    $cloudDir = __DIR__ . '/../docu_cloud';
    if (!is_dir($cloudDir)) {
        @mkdir($cloudDir, 0755, true);
    }

    $docId = 'doc_' . substr(md5(uniqid(rand(), true)), 0, 10);
    $ext = ($fileType === 'pdf') ? 'pdf' : 'jpg';
    $filePath = $cloudDir . '/' . $docId . '.' . $ext;
    @file_put_contents($filePath, $fileData);

    $meta = [
        'doc_id' => $docId,
        'title' => $title,
        'file_type' => $fileType,
        'ext' => $ext,
        'pages_count' => $pagesCount,
        'file_size' => strlen($fileData),
        'created_at' => time(),
        'created_date' => date('d M Y, h:i A')
    ];
    @file_put_contents($cloudDir . '/' . $docId . '.json', json_encode($meta, JSON_PRETTY_PRINT));

    $baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $shareUrl = $baseUrl . "/api/docu_ai.php?action=view&id=" . $docId;
    $downloadUrl = $baseUrl . "/api/docu_ai.php?action=download&id=" . $docId;
    $qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" . urlencode($shareUrl);

    echo json_encode([
        'success' => true,
        'doc_id' => $docId,
        'title' => $title,
        'share_url' => $shareUrl,
        'download_url' => $downloadUrl,
        'qr_url' => $qrUrl,
        'file_size_bytes' => strlen($fileData),
        'file_size_formatted' => round(strlen($fileData) / 1024, 1) . ' KB',
        'pages_count' => $pagesCount,
        'timestamp' => time()
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit(0);
}

// 7. Direct Cloud Document Download
if ($action === 'download') {
    $id = preg_replace('/[^a-zA-Z0-9_-]/', '', $_GET['id'] ?? '');
    $cloudDir = __DIR__ . '/../docu_cloud';
    $metaFile = $cloudDir . '/' . $id . '.json';
    if (!file_exists($metaFile)) {
        header('HTTP/1.0 404 Not Found');
        echo "Document not found or expired.";
        exit(0);
    }
    $meta = json_decode(file_get_contents($metaFile), true);
    $ext = $meta['ext'] ?? 'pdf';
    $filePath = $cloudDir . '/' . $id . '.' . $ext;
    if (!file_exists($filePath)) {
        header('HTTP/1.0 404 Not Found');
        echo "Document file missing.";
        exit(0);
    }

    $mime = ($ext === 'pdf') ? 'application/pdf' : 'image/jpeg';
    $safeTitle = preg_replace('/[^a-zA-Z0-9_\-\.]/', '_', $meta['title'] ?? 'scanned_doc') . '.' . $ext;

    header('Content-Type: ' . $mime);
    header('Content-Disposition: attachment; filename="' . $safeTitle . '"');
    header('Content-Length: ' . filesize($filePath));
    readfile($filePath);
    exit(0);
}

// 8. Responsive CamScanner-Style Web Viewer (Desktop & Mobile)
if ($action === 'view') {
    $id = preg_replace('/[^a-zA-Z0-9_-]/', '', $_GET['id'] ?? '');
    $cloudDir = __DIR__ . '/../docu_cloud';
    $metaFile = $cloudDir . '/' . $id . '.json';
    if (!file_exists($metaFile)) {
        header('HTTP/1.0 404 Not Found');
        echo "<!DOCTYPE html><html><body style='font-family:sans-serif;text-align:center;padding:50px;'><h2>404 - Document Not Found</h2><p>This document may have been deleted or expired.</p></body></html>";
        exit(0);
    }

    $meta = json_decode(file_get_contents($metaFile), true);
    $title = htmlspecialchars($meta['title'] ?? 'Scanned Document');
    $ext = $meta['ext'] ?? 'pdf';
    $createdDate = htmlspecialchars($meta['created_date'] ?? date('d M Y'));
    $fileSize = round(($meta['file_size'] ?? 0) / 1024, 1) . ' KB';
    $pagesCount = intval($meta['pages_count'] ?? 1);

    $baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http") . "://" . $_SERVER['HTTP_HOST'];
    $fileDirectUrl = $baseUrl . "/api/docu_ai.php?action=download&id=" . $id;
    $qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" . urlencode($baseUrl . "/api/docu_ai.php?action=view&id=" . $id);

    header('Content-Type: text/html; charset=utf-8');
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?php echo $title; ?> - DocuEdit Cloud Web Viewer</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }
        body { background-color: #0f172a; color: #f8fafc; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
        header { background: #1e293b; border-bottom: 1px solid #334155; padding: 12px 20px; display: flex; align-items: center; justify-content: space-between; z-index: 10; box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
        .logo-wrap { display: flex; align-items: center; gap: 10px; }
        .logo-badge { background: linear-gradient(135deg, #2563eb, #38bdf8); color: white; font-weight: 800; font-size: 14px; padding: 6px 12px; border-radius: 8px; letter-spacing: 0.5px; }
        .doc-title { font-size: 16px; font-weight: 700; color: #f1f5f9; max-width: 320px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .doc-sub { font-size: 12px; color: #94a3b8; }
        .actions-wrap { display: flex; align-items: center; gap: 10px; }
        .btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; text-decoration: none; cursor: pointer; transition: all 0.2s ease; border: none; }
        .btn-primary { background: #2563eb; color: white; }
        .btn-primary:hover { background: #1d4ed8; }
        .btn-outline { background: #334155; color: #e2e8f0; }
        .btn-outline:hover { background: #475569; }
        .btn-emerald { background: #059669; color: white; }
        .btn-emerald:hover { background: #047857; }
        main { flex: 1; display: flex; align-items: center; justify-content: center; position: relative; background: #090d16; overflow: auto; padding: 16px; }
        .viewer-card { width: 100%; height: 100%; max-width: 1080px; background: #1e293b; border-radius: 12px; overflow: hidden; display: flex; align-items: center; justify-content: center; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
        iframe { width: 100%; height: 100%; border: none; background: white; }
        img.doc-preview { max-width: 100%; max-height: 100%; object-fit: contain; }
        /* QR Modal */
        #qrModal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.7); backdrop-filter: blur(4px); z-index: 100; align-items: center; justify-content: center; }
        .modal-content { background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 24px; text-align: center; max-width: 320px; box-shadow: 0 20px 40px rgba(0,0,0,0.6); }
        .qr-img { width: 220px; height: 220px; border-radius: 12px; margin: 16px 0; background: white; padding: 8px; }
        @media (max-width: 768px) {
            header { flex-direction: column; gap: 10px; align-items: flex-start; }
            .actions-wrap { width: 100%; justify-content: space-between; }
            .doc-title { max-width: 200px; }
        }
    </style>
</head>
<body>
    <header>
        <div class="logo-wrap">
            <span class="logo-badge">DOCUEDIT ☁️</span>
            <div>
                <div class="doc-title"><?php echo $title; ?></div>
                <div class="doc-sub"><?php echo $pagesCount; ?> page(s) • <?php echo $fileSize; ?> • Synced: <?php echo $createdDate; ?></div>
            </div>
        </div>
        <div class="actions-wrap">
            <button class="btn btn-outline" onclick="copyLink()">📋 Copy Link</button>
            <button class="btn btn-outline" onclick="openQr()">📱 QR Code</button>
            <button class="btn btn-emerald" onclick="window.print()">🖨️ Print</button>
            <a href="<?php echo $fileDirectUrl; ?>" class="btn btn-primary" download>⬇️ Download <?php echo strtoupper($ext); ?></a>
        </div>
    </header>

    <main>
        <div class="viewer-card">
            <?php if ($ext === 'pdf'): ?>
                <iframe src="<?php echo $fileDirectUrl; ?>#toolbar=1&navpanes=0"></iframe>
            <?php else: ?>
                <img src="<?php echo $fileDirectUrl; ?>" alt="Document Preview" class="doc-preview" />
            <?php endif; ?>
        </div>
    </main>

    <div id="qrModal" onclick="closeQr(event)">
        <div class="modal-content" onclick="event.stopPropagation()">
            <h3 style="font-size:16px; font-weight:700;">Scan to View on Phone</h3>
            <p style="font-size:12px; color:#94a3b8; margin-top:4px;">Open your phone camera to view or download instantly.</p>
            <img src="<?php echo $qrUrl; ?>" alt="QR Code" class="qr-img" />
            <button class="btn btn-outline" style="width:100%;" onclick="closeQr()">Done</button>
        </div>
    </div>

    <script>
        function openQr() { document.getElementById('qrModal').style.display = 'flex'; }
        function closeQr() { document.getElementById('qrModal').style.display = 'none'; }
        function copyLink() {
            navigator.clipboard.writeText(window.location.href);
            alert("✅ Web Viewer link copied to clipboard!");
        }
    </script>
</body>
</html>
<?php
    exit(0);
}

header('Content-Type: application/json; charset=utf-8');
echo json_encode(['success' => false, 'error' => 'Invalid action'], JSON_UNESCAPED_UNICODE);
