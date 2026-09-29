<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - दिव्य भक्ति स्टेटस एवं स्टोरी सेवा
// Divine Bhakti Status & WhatsApp Story Generation Engine (24-Hour Lifecycle)
// ==============================================================================

ini_set('display_errors', 0);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/../config/db.php';

$pdo = getDB();
if (!$pdo) {
    echo json_encode(["success" => false, "error" => "Database connection unavailable"]);
    exit;
}

// 1. Ensure required tables exist
try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS ashram_statuses (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        device_id VARCHAR(120) NOT NULL,
        user_name VARCHAR(100) NOT NULL,
        phone_number VARCHAR(30) NOT NULL DEFAULT '',
        city VARCHAR(100) NOT NULL DEFAULT '',
        caption TEXT,
        media_url VARCHAR(255) NOT NULL,
        is_official TINYINT(1) DEFAULT 0,
        audio_snippet_url VARCHAR(255) DEFAULT '',
        created_at BIGINT NOT NULL,
        expires_at BIGINT NOT NULL,
        views_count INT DEFAULT 0,
        status VARCHAR(20) DEFAULT 'ACTIVE',
        INDEX idx_expires (expires_at),
        INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    $pdo->exec("CREATE TABLE IF NOT EXISTS status_views (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        status_id BIGINT NOT NULL,
        viewer_device_id VARCHAR(120) NOT NULL,
        viewer_name VARCHAR(100) NOT NULL DEFAULT 'भक्त',
        viewer_phone VARCHAR(30) NOT NULL DEFAULT '',
        viewed_at BIGINT NOT NULL,
        UNIQUE KEY uq_status_viewer (status_id, viewer_device_id),
        INDEX idx_status_id (status_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
} catch (Exception $e) {
    // Continue if tables already exist
}

// Ensure upload directory exists
$uploadDir = __DIR__ . '/../uploads/statuses';
if (!is_dir($uploadDir)) {
    @mkdir($uploadDir, 0755, true);
}

// Helper: Purge expired statuses (>24h old)
function purgeExpiredStatuses($pdo, $uploadDir) {
    try {
        $now = time();
        $stmt = $pdo->prepare("SELECT id, media_url FROM ashram_statuses WHERE expires_at < :now AND is_official = 0");
        $stmt->execute([':now' => $now]);
        $expired = $stmt->fetchAll(PDO::FETCH_ASSOC);

        foreach ($expired as $item) {
            $filename = basename($item['media_url']);
            $filePath = $uploadDir . '/' . $filename;
            if (file_exists($filePath)) {
                @unlink($filePath);
            }
        }

        // Delete expired views and statuses
        $pdo->prepare("DELETE FROM status_views WHERE status_id IN (SELECT id FROM ashram_statuses WHERE expires_at < :now AND is_official = 0)")
            ->execute([':now' => $now]);
        $pdo->prepare("DELETE FROM ashram_statuses WHERE expires_at < :now AND is_official = 0")
            ->execute([':now' => $now]);
    } catch (Exception $e) {
        // Silently ignore cleanup errors
    }
}

// Curated Divine Suvichars / Chaupais for Daily Official Ashram Status
function getTodayCuratedSuvichar() {
    $suvichars = [
        [
            "quote" => "कवन सो काज कठिन जग माहीं । जो नहिं होत तात तुम्ह पाहीं ॥",
            "meaning" => "संसार में ऐसा कोई कठिन कार्य नहीं जो श्री बालाजी महाराज की कृपा से सुगम न हो सके।",
            "source" => "श्री रामचरितमानस (सुंदरकांड)"
        ],
        [
            "quote" => "संकट कटै मिटै सब पीरा । जो सुमिरै हनुमत बलबीरा ॥",
            "meaning" => "हनुमान जी के नाम का स्मरण करने मात्र से समस्त संकट और व्याधियां दूर हो जाती हैं।",
            "source" => "हनुमान चालीसा"
        ],
        [
            "quote" => "जै जै जै हनुमान गोसाईं । कृपा करहु गुरुदेव की नाईं ॥",
            "meaning" => "हे भक्तवत्सल हनुमान जी! आप सद्गुरु के समान हम पर अपनी अखंड कृपा दृष्टि बनाए रखें।",
            "source" => "हनुमान चालीसा"
        ],
        [
            "quote" => "प्रभु पद पंकज कपट तजि जो भजइ सो मोहि परम प्रिय ।",
            "meaning" => "जो भी मनुष्य छल-कपट त्यागकर प्रभु चरणों में अनुराग करता है, वह बालाजी को अति प्रिय है।",
            "source" => "श्री रामचरितमानस"
        ],
        [
            "quote" => "भूत पिसाच निकट नहिं आवै । महाबीर जब नाम सुनावै ॥",
            "meaning" => "महावीर बालाजी का नाम गूंजते ही नकारात्मक शक्तियां और भय कोसों दूर भाग जाते हैं।",
            "source" => "हनुमान चालीसा"
        ],
        [
            "quote" => "नासै रोग हरै सब पीरा । जपत निरंतर हनुमत बीरा ॥",
            "meaning" => "निरंतर बालाजी महाराज का नाम जपने से तन और मन के समस्त रोग समूल नष्ट होते हैं।",
            "source" => "हनुमान चालीसा"
        ],
        [
            "quote" => "दीन दयाल बिरिदु संभारी । हरहु नाथ मम संकट भारी ॥",
            "meaning" => "हे दीनदयालु बालाजी महाराज! अपने दया के स्वभाव को स्मरण कर मेरे सभी संकट हर लें।",
            "source" => "श्री रामचरितमानस"
        ],
        [
            "quote" => "राम काज करिबे को आतुर । प्रभु चरित सुनिबे को रसिया ॥",
            "meaning" => "परोपकार और धर्म सेवा में सदैव तत्पर रहना ही हनुमान जी की सच्ची भक्ति है।",
            "source" => "हनुमान चालीसा"
        ]
    ];

    $dayOfYear = intval(date('z'));
    $index = $dayOfYear % count($suvichars);
    return $suvichars[$index];
}

// Ensure Daily Official Ashram Status Exists
function ensureDailyOfficialStatus($pdo) {
    try {
        $todayStart = strtotime("today 00:00:00");
        $todayEnd = strtotime("today 23:59:59");

        $stmt = $pdo->prepare("SELECT id FROM ashram_statuses WHERE is_official = 1 AND created_at >= :tstart LIMIT 1");
        $stmt->execute([':tstart' => $todayStart]);
        if (!$stmt->fetch()) {
            $suvichar = getTodayCuratedSuvichar();
            $caption = "🚩 " . $suvichar['quote'] . "\n\n✨ " . $suvichar['meaning'] . "\n\n— श्री बालाजी कृपा धाम (डूँगरा जाट)";
            $mediaUrl = "https://shribalajikripadham.online/media/balaji_darshan_today.jpg";

            $ins = $pdo->prepare("INSERT INTO ashram_statuses (
                device_id, user_name, phone_number, city, caption, media_url, is_official, audio_snippet_url, created_at, expires_at, status
            ) VALUES (
                'OFFICIAL_ASHRAM', 'श्री बालाजी कृपा धाम', '', 'डूँगरा जाट (बुलन्दशहर)', :cap, :med, 1, '', :cat, :eat, 'ACTIVE'
            )");
            $ins->execute([
                ':cap' => $caption,
                ':med' => $mediaUrl,
                ':cat' => time(),
                ':eat' => $todayEnd + 86400
            ]);
        }
    } catch (Exception $e) {
        // Silently ignore
    }
}

// Route Requests
$action = $_GET['action'] ?? $_POST['action'] ?? 'get_active_statuses';

// Purge expired statuses periodically
purgeExpiredStatuses($pdo, $uploadDir);
ensureDailyOfficialStatus($pdo);

switch ($action) {

    // =========================================================================
    // 1. Get All Active Statuses (Official Ashram Statuses + Devotee Statuses)
    // =========================================================================
    case 'get_active_statuses':
        try {
            $now = time();
            $stmt = $pdo->prepare("SELECT id, device_id, user_name, phone_number, city, caption, media_url, is_official, audio_snippet_url, created_at, expires_at, views_count 
                                   FROM ashram_statuses 
                                   WHERE expires_at > :now AND status = 'ACTIVE' 
                                   ORDER BY is_official DESC, created_at DESC 
                                   LIMIT 60");
            $stmt->execute([':now' => $now]);
            $statuses = $stmt->fetchAll(PDO::FETCH_ASSOC);

            // Fetch today's Suvichar
            $suvichar = getTodayCuratedSuvichar();

            echo json_encode([
                "success" => true,
                "total" => count($statuses),
                "statuses" => $statuses,
                "daily_suvichar" => $suvichar,
                "server_time" => time()
            ], JSON_UNESCAPED_UNICODE);
        } catch (Exception $e) {
            echo json_encode(["success" => false, "error" => $e->getMessage()]);
        }
        break;

    // =========================================================================
    // 2. Upload Devotee Status (Photo + Optional Devotee Frame)
    // =========================================================================
    case 'upload_status':
        try {
            $deviceId = trim($_POST['device_id'] ?? '');
            $userName = trim($_POST['user_name'] ?? 'श्री बालाजी भक्त');
            $phoneNumber = trim($_POST['phone_number'] ?? '');
            $city = trim($_POST['city'] ?? '');
            $caption = trim($_POST['caption'] ?? '');
            $base64Image = $_POST['image_base64'] ?? '';

            if (empty($deviceId)) {
                echo json_encode(["success" => false, "error" => "Device ID required"]);
                exit;
            }

            // Rate limit: Max 3 active statuses per device to prevent flooding
            $chk = $pdo->prepare("SELECT COUNT(*) FROM ashram_statuses WHERE device_id = :did AND expires_at > :now AND status = 'ACTIVE'");
            $chk->execute([':did' => $deviceId, ':now' => time()]);
            if ($chk->fetchColumn() >= 3) {
                echo json_encode([
                    "success" => false, 
                    "error" => "आपके पिछले स्टेटस अभी सक्रिय हैं। एक समय में अधिकतम 3 स्टेटस लगाए जा सकते हैं।"
                ]);
                exit;
            }

            $mediaUrl = '';

            // Handle direct file upload or base64
            if (!empty($_FILES['image_file']['tmp_name'])) {
                $ext = strtolower(pathinfo($_FILES['image_file']['name'], PATHINFO_EXTENSION));
                if (!in_array($ext, ['jpg', 'jpeg', 'png', 'webp'])) $ext = 'jpg';
                $filename = 'status_' . time() . '_' . substr(md5(uniqid(mt_rand(), true)), 0, 8) . '.' . $ext;
                $targetPath = $uploadDir . '/' . $filename;
                if (move_uploaded_file($_FILES['image_file']['tmp_name'], $targetPath)) {
                    $mediaUrl = "https://shribalajikripadham.online/uploads/statuses/" . $filename;
                }
            } elseif (!empty($base64Image)) {
                $cleanBase64 = preg_replace('#^data:image/\w+;base64,#i', '', $base64Image);
                $imgData = base64_decode($cleanBase64);
                if ($imgData && strlen($imgData) > 500) {
                    $filename = 'status_' . time() . '_' . substr(md5(uniqid(mt_rand(), true)), 0, 8) . '.jpg';
                    $targetPath = $uploadDir . '/' . $filename;
                    if (file_put_contents($targetPath, $imgData)) {
                        $mediaUrl = "https://shribalajikripadham.online/uploads/statuses/" . $filename;
                    }
                }
            }

            if (empty($mediaUrl)) {
                echo json_encode(["success" => false, "error" => "फ़ोटो अपलोड नहीं हो सकी। कृपया पुनः प्रयास करें।"]);
                exit;
            }

            $now = time();
            $expiresAt = $now + (24 * 3600); // 24-hour lifetime

            $ins = $pdo->prepare("INSERT INTO ashram_statuses (
                device_id, user_name, phone_number, city, caption, media_url, is_official, audio_snippet_url, created_at, expires_at, status
            ) VALUES (
                :did, :name, :phone, :city, :cap, :url, 0, '', :cat, :eat, 'ACTIVE'
            )");
            $ins->execute([
                ':did' => $deviceId,
                ':name' => $userName,
                ':phone' => $phoneNumber,
                ':city' => $city,
                ':cap' => $caption,
                ':url' => $mediaUrl,
                ':cat' => $now,
                ':eat' => $expiresAt
            ]);

            $newId = $pdo->lastInsertId();

            echo json_encode([
                "success" => true,
                "message" => "आपका स्टेटस 24 घंटे के लिए धाम ऐप पर सफलतापूर्वक लग गया है!",
                "status_id" => $newId,
                "media_url" => $mediaUrl,
                "expires_at" => $expiresAt
            ], JSON_UNESCAPED_UNICODE);

        } catch (Exception $e) {
            echo json_encode(["success" => false, "error" => $e->getMessage()]);
        }
        break;

    // =========================================================================
    // 3. Record Status View (Tracking who viewed which status)
    // =========================================================================
    case 'record_view':
        try {
            $statusId = intval($_POST['status_id'] ?? 0);
            $viewerDeviceId = trim($_POST['viewer_device_id'] ?? '');
            $viewerName = trim($_POST['viewer_name'] ?? 'भक्त');
            $viewerPhone = trim($_POST['viewer_phone'] ?? '');

            if ($statusId > 0 && !empty($viewerDeviceId)) {
                $now = time();
                $stmt = $pdo->prepare("INSERT IGNORE INTO status_views (
                    status_id, viewer_device_id, viewer_name, viewer_phone, viewed_at
                ) VALUES (:sid, :vdid, :vname, :vphone, :vat)");
                $stmt->execute([
                    ':sid' => $statusId,
                    ':vdid' => $viewerDeviceId,
                    ':vname' => $viewerName,
                    ':vphone' => $viewerPhone,
                    ':vat' => $now
                ]);

                if ($stmt->rowCount() > 0) {
                    $pdo->prepare("UPDATE ashram_statuses SET views_count = views_count + 1 WHERE id = :sid")
                        ->execute([':sid' => $statusId]);
                }
            }

            echo json_encode(["success" => true]);
        } catch (Exception $e) {
            echo json_encode(["success" => false, "error" => $e->getMessage()]);
        }
        break;

    // =========================================================================
    // 4. Get Status Viewers (Admin Tracking: List of Devotees who viewed)
    // =========================================================================
    case 'get_viewers':
        try {
            $statusId = intval($_GET['status_id'] ?? $_POST['status_id'] ?? 0);
            if ($statusId <= 0) {
                echo json_encode(["success" => false, "error" => "Invalid status ID"]);
                exit;
            }

            // Fetch status metadata
            $stStmt = $pdo->prepare("SELECT id, user_name, phone_number, city, created_at, views_count FROM ashram_statuses WHERE id = :sid LIMIT 1");
            $stStmt->execute([':sid' => $statusId]);
            $statusInfo = $stStmt->fetch(PDO::FETCH_ASSOC);

            // Fetch viewer list
            $vStmt = $pdo->prepare("SELECT viewer_name, viewer_phone, viewed_at FROM status_views WHERE status_id = :sid ORDER BY viewed_at DESC LIMIT 200");
            $vStmt->execute([':sid' => $statusId]);
            $viewers = $vStmt->fetchAll(PDO::FETCH_ASSOC);

            echo json_encode([
                "success" => true,
                "status_info" => $statusInfo,
                "total_views" => count($viewers),
                "viewers" => $viewers
            ], JSON_UNESCAPED_UNICODE);

        } catch (Exception $e) {
            echo json_encode(["success" => false, "error" => $e->getMessage()]);
        }
        break;

    // =========================================================================
    // 5. Admin Delete Inappropriate Status
    // =========================================================================
    case 'delete_status':
        try {
            $statusId = intval($_POST['status_id'] ?? 0);
            $adminDeviceId = trim($_POST['admin_device_id'] ?? '');

            if ($statusId <= 0) {
                echo json_encode(["success" => false, "error" => "Invalid status ID"]);
                exit;
            }

            // Fetch media url to delete file
            $fStmt = $pdo->prepare("SELECT media_url FROM ashram_statuses WHERE id = :sid LIMIT 1");
            $fStmt->execute([':sid' => $statusId]);
            $item = $fStmt->fetch(PDO::FETCH_ASSOC);

            if ($item) {
                $filename = basename($item['media_url']);
                $filePath = $uploadDir . '/' . $filename;
                if (file_exists($filePath)) {
                    @unlink($filePath);
                }
            }

            $pdo->prepare("DELETE FROM status_views WHERE status_id = :sid")->execute([':sid' => $statusId]);
            $pdo->prepare("DELETE FROM ashram_statuses WHERE id = :sid")->execute([':sid' => $statusId]);

            echo json_encode([
                "success" => true,
                "message" => "स्टेटस को सफलतापूर्वक हटा दिया गया है।"
            ], JSON_UNESCAPED_UNICODE);

        } catch (Exception $e) {
            echo json_encode(["success" => false, "error" => $e->getMessage()]);
        }
        break;

    default:
        echo json_encode(["success" => false, "error" => "Invalid action specified"]);
        break;
}
