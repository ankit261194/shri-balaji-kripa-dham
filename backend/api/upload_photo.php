<?php
header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["success" => false, "error" => "Method Not Allowed"]);
    exit;
}

if (!isset($_FILES['photo']) || $_FILES['photo']['error'] !== UPLOAD_ERR_OK) {
    http_response_code(400);
    echo json_encode(["success" => false, "error" => "फ़ोटो फाइल प्राप्त नहीं हुई"], JSON_UNESCAPED_UNICODE);
    exit;
}

$file = $_FILES['photo'];
$ext = strtolower(pathinfo($file['name'], PATHINFO_EXTENSION));
if (!in_array($ext, ['jpg', 'jpeg', 'png', 'webp'])) {
    $ext = 'jpg';
}

$uploadDir = __DIR__ . '/../uploads/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

$photoType = strtolower(trim($_POST['photo_type'] ?? $_POST['type'] ?? 'devotee'));
$isGuruji = ($photoType === 'guruji' || strpos(strtolower($file['name']), 'guruji') !== false);
$isDarshan = ($photoType === 'darshan' || strpos(strtolower($file['name']), 'darshan') !== false);
$isCarousel = ($photoType === 'carousel' || strpos(strtolower($file['name']), 'carousel') !== false);

if ($isGuruji) {
    $filename = 'guruji_' . time() . '_' . bin2hex(random_bytes(3)) . '.' . $ext;
} elseif ($isDarshan) {
    $filename = 'darshan_' . time() . '_' . bin2hex(random_bytes(3)) . '.' . $ext;
} elseif ($isCarousel) {
    $filename = 'carousel_' . time() . '_' . bin2hex(random_bytes(3)) . '.' . $ext;
} else {
    $filename = 'devotee_' . time() . '_' . bin2hex(random_bytes(4)) . '.' . $ext;
}

$targetPath = $uploadDir . $filename;

if (move_uploaded_file($file['tmp_name'], $targetPath)) {
    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
    $host = $_SERVER['HTTP_HOST'];
    $photoUrl = "{$protocol}://{$host}/uploads/{$filename}";

    // If it's Darshan photo, mirror to media/balaji_darshan_today.jpg AND immediately update daily_darshan MySQL table
    if ($isDarshan) {
        $mediaDir = __DIR__ . '/../media/';
        if (!is_dir($mediaDir)) {
            @mkdir($mediaDir, 0755, true);
        }
        @copy($targetPath, $mediaDir . 'balaji_darshan_today.jpg');

        try {
            if (file_exists(__DIR__ . '/../config/db.php')) {
                require_once __DIR__ . '/../config/db.php';
            }
            if (function_exists('getDB')) {
                $pdo = getDB();
                if ($pdo) {
                    $todayDate = date("Y-m-d");
                    $now = time();
                    $stmt = $pdo->prepare("INSERT INTO daily_darshan (darshan_date, title, photo_url, blessings_quote, views_count, created_at, updated_at)
                        VALUES (:date, 'श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन', :photo, '', 1, :now1, :now2)
                        ON DUPLICATE KEY UPDATE photo_url = :photo2, updated_at = :now3");
                    $stmt->execute([
                        ':date' => $todayDate,
                        ':photo' => $photoUrl,
                        ':now1' => $now,
                        ':now2' => $now,
                        ':photo2' => $photoUrl,
                        ':now3' => $now
                    ]);
                }
            }
        } catch (Throwable $e) {
            error_log("Failed to update daily_darshan in DB: " . $e->getMessage());
        }
    }

    // Authorized direct copy to /media/ for system sacred assets
    $saveToMedia = trim($_POST['save_to_media'] ?? '');
    if (!empty($saveToMedia) && preg_match('/^[a-zA-Z0-9_\-]+\.(jpg|jpeg|png|webp)$/i', $saveToMedia)) {
        $apiKey = $_SERVER['HTTP_X_SBKD_API_KEY'] ?? $_SERVER['X_SBKD_API_KEY'] ?? $_POST['api_key'] ?? '';
        if ($apiKey === 'SBKD_SECURE_TOKEN_9100100251233433_V243') {
            $mediaDir = __DIR__ . '/../media/';
            if (!is_dir($mediaDir)) {
                @mkdir($mediaDir, 0755, true);
            }
            @copy($targetPath, $mediaDir . $saveToMedia);
        }
    }

    // If it's Guruji photo, also mirror to canonical uploads/guruji_profile.jpg
    if ($isGuruji) {
        $canonicalPath = $uploadDir . 'guruji_profile.jpg';
        @copy($targetPath, $canonicalPath);

        // Also update ashram_settings in MySQL if db.php is available
        try {
            if (file_exists(__DIR__ . '/../config/db.php')) {
                require_once __DIR__ . '/../config/db.php';
            }
            if (function_exists('getDB')) {
                $pdo = getDB();
                if ($pdo) {
                    $stmt = $pdo->prepare("UPDATE ashram_settings SET guruji_photo_url = :photo_url, updated_at = NOW() WHERE id = 1");
                    $stmt->execute([':photo_url' => $photoUrl]);
                }
            }
        } catch (Exception $e) {
            // Non-blocking log
            error_log("Failed to update guruji_photo_url in DB: " . $e->getMessage());
        }
    }

    echo json_encode([
        "success" => true,
        "filename" => $filename,
        "photo_url" => $photoUrl,
        "is_guruji" => $isGuruji,
        "canonical_url" => $isGuruji ? "{$protocol}://{$host}/uploads/guruji_profile.jpg" : null,
        "message" => "फ़ोटो सफलतापूर्वक अपलोड हो गई!"
    ], JSON_UNESCAPED_UNICODE);
} else {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => "फ़ोटो सर्वर पर सुरक्षित करने में असमर्थ"], JSON_UNESCAPED_UNICODE);
}
