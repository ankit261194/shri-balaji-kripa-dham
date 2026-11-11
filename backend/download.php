<?php
// ==============================================================================
// SHRI BALAJI KRIPA DHAM - OFFICIAL APP DOWNLOAD & DISTRIBUTION ENGINE
// Mandir: Gram Dungra Jaat, Bulandshahr, Uttar Pradesh
// Hostinger High-Speed NVMe Cluster: https://shribalajikripadham.online
// ==============================================================================

ini_set('display_errors', 0);
error_reporting(0);

$appParam = strtolower(trim($_GET['app'] ?? ''));
$viewParam = strtolower(trim($_GET['view'] ?? ''));
$actionParam = strtolower(trim($_GET['action'] ?? ''));

// ==============================================================================
// 1. ELEVEX MULTI-APP HANDLER (ONLY IF EXPLICITLY REQUESTED VIA ?app=...)
// ==============================================================================
$elevexApps = [
    'superadmin' => [
        'name' => 'EleveX SuperAdmin App',
        'title_hi' => 'सुपर एडमिन सुप्रीम कंट्रोलर',
        'package' => 'com.ankit.elevex.superadmin',
        'version' => '5.9.52',
        'build'   => 642,
        'filename' => 'EleveX_SuperAdmin_v5.9.52.apk',
        'size'    => '33.08 MB',
        'color' => '#8B5CF6',
        'icon' => '🛡️'
    ],
    'supervisor' => [
        'name' => 'EleveX Supervisor Cockpit',
        'title_hi' => 'सुपरवाइज़र फ्लीट कॉकपिट',
        'package' => 'com.ankit.elevex.supervisor',
        'version' => '5.9.52',
        'build'   => 642,
        'filename' => 'EleveX_Supervisor_v5.9.52.apk',
        'size'    => '32.40 MB',
        'color' => '#0284C7',
        'icon' => '📡'
    ],
    'technician' => [
        'name' => 'EleveX Technician Terminal',
        'title_hi' => 'फील्ड टेक्नीशियन टर्मिनल',
        'package' => 'com.ankit.elevex.technician',
        'version' => '5.9.52',
        'build'   => 642,
        'filename' => 'EleveX_Technician_v5.9.52.apk',
        'size'    => '36.71 MB',
        'color' => '#10B981',
        'icon' => '🔧'
    ],
    'owner' => [
        'name' => 'EleveX Lift Owner App',
        'title_hi' => 'लिफ्ट ओनर क्लाइंट पोर्टल',
        'package' => 'com.ankit.elevex.owner',
        'version' => '5.9.52',
        'build'   => 642,
        'filename' => 'EleveX_Owner_v5.9.52.apk',
        'size'    => '31.73 MB',
        'color' => '#F59E0B',
        'icon' => '🏢'
    ],
    'gateway' => [
        'name' => 'EleveX Master Gateway Portal',
        'title_hi' => 'मास्टर 4-इन-1 गेटवे पोर्टल',
        'package' => 'com.ankit.liftramban.ankits_lift_ramban_ai',
        'version' => '5.9.52',
        'build'   => 642,
        'filename' => 'EleveX_Master_Gateway_v5.9.52.apk',
        'size'    => '38.41 MB',
        'color' => '#3B82F6',
        'icon' => '🌐'
    ]
];

if ($appParam === 'elevex' || $appParam === 'ankit_elevex') {
    $appParam = 'gateway';
}

if (isset($elevexApps[$appParam])) {
    $appInfo = $elevexApps[$appParam];
    $filename = $appInfo['filename'];
    $candidateDirs = array_filter([
        __DIR__ . '/elevex_gateway/downloads',
        isset($_SERVER['DOCUMENT_ROOT']) ? $_SERVER['DOCUMENT_ROOT'] . '/elevex_gateway/downloads' : null,
        dirname(__DIR__) . '/elevex_gateway/downloads',
        __DIR__ . '/downloads',
        '/home/u237101617/domains/shribalajikripadham.online/public_html/elevex_gateway/downloads'
    ]);
    $targetFile = '';
    foreach ($candidateDirs as $dir) {
        if (!is_dir($dir)) continue;
        $specific = "$dir/{$filename}";
        if (file_exists($specific) && filesize($specific) > 10000000) {
            $targetFile = $specific;
            break;
        }
    }
    if (!empty($targetFile) && file_exists($targetFile)) {
        $filesize = filesize($targetFile);
        while (ob_get_level()) ob_end_clean();
        header("Content-Type: application/vnd.android.package-archive");
        header("Content-Disposition: attachment; filename=\"" . $filename . "\"");
        header("Content-Length: {$filesize}");
        header("Accept-Ranges: bytes");
        header("Cache-Control: public, no-cache, no-store, must-revalidate, max-age=0");
        header("Pragma: no-cache");
        header("Expires: 0");
        $handle = fopen($targetFile, "rb");
        if ($handle) {
            while (!feof($handle)) {
                echo fread($handle, 1048576);
                flush();
            }
            fclose($handle);
        } else {
            readfile($targetFile);
        }
        exit;
    }
    header("Location: https://github.com/ankit261194/ankits-liftramban/releases/download/v5.9.39/Ankit_EleveX_v5.9.39_Final.apk", true, 302);
    exit;
}

// ==============================================================================
// 2. PRIMARY DEFAULT: SHRI BALAJI KRIPA DHAM OFFICIAL APK DELIVERY
// ==============================================================================
$tag = 'v2.82.0';
$buildCode = 169;
$versionName = '2.82.0';
$apkFilename = 'ShriBalajiKripaDham-release.apk';
$fallbackUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/{$tag}/ShriBalajiKripaDham-release.apk";

// Candidate search locations for physical APK on Hostinger SSD
$ashramCandidateDirs = array_filter([
    __DIR__ . '/downloads',
    isset($_SERVER['DOCUMENT_ROOT']) ? $_SERVER['DOCUMENT_ROOT'] . '/downloads' : null,
    dirname(__DIR__) . '/downloads',
    '/home/u237101617/domains/shribalajikripadham.online/public_html/downloads'
]);

$ashramTargetFile = '';
foreach ($ashramCandidateDirs as $d) {
    if (!$d || !is_dir($d)) continue;
    // 1. Check exact ShriBalajiKripaDham-release.apk
    $p1 = "$d/{$apkFilename}";
    if (file_exists($p1) && filesize($p1) > 10000000) {
        $ashramTargetFile = $p1;
        break;
    }
    // 2. Check version specific ShriBalajiKripaDham-v169.apk
    $p2 = "$d/ShriBalajiKripaDham-v{$buildCode}.apk";
    if (file_exists($p2) && filesize($p2) > 10000000) {
        $ashramTargetFile = $p2;
        break;
    }
    // 3. Any ShriBalajiKripaDham*.apk
    $m = glob("$d/ShriBalajiKripaDham*.apk");
    if (!empty($m)) {
        natsort($m);
        $m = array_reverse($m);
        foreach ($m as $candidate) {
            if (filesize($candidate) > 10000000) {
                $ashramTargetFile = $candidate;
                break 2;
            }
        }
    }
}

// Check if user specifically requested the HTML information page
$showInfoPage = ($viewParam === 'page' || $viewParam === 'info' || isset($_GET['info']) || $actionParam === 'info');

if (!$showInfoPage) {
    // DIRECT HIGH-SPEED BINARY DOWNLOAD STREAM (DEFAULT BEHAVIOR)
    if (!empty($ashramTargetFile) && file_exists($ashramTargetFile)) {
        $filesize = filesize($ashramTargetFile);
        while (ob_get_level()) ob_end_clean();
        header("Content-Type: application/vnd.android.package-archive");
        header("Content-Disposition: attachment; filename=\"{$apkFilename}\"");
        header("Content-Length: {$filesize}");
        header("Accept-Ranges: bytes");
        header("Cache-Control: public, no-cache, no-store, must-revalidate, max-age=0");
        header("Pragma: no-cache");
        header("Expires: 0");

        $handle = fopen($ashramTargetFile, "rb");
        if ($handle) {
            while (!feof($handle)) {
                echo fread($handle, 1048576);
                flush();
            }
            fclose($handle);
        } else {
            readfile($ashramTargetFile);
        }
        exit;
    } else {
        // High-Speed Guaranteed GitHub Releases Mirror Fallback
        header("Location: " . $fallbackUrl, true, 302);
        exit;
    }
}

// ==============================================================================
// 3. BEAUTIFUL SACRED WEB DOWNLOAD PORTAL (when ?view=page is requested)
// ==============================================================================
$filesizeMB = (!empty($ashramTargetFile) && file_exists($ashramTargetFile))
    ? round(filesize($ashramTargetFile) / (1024 * 1024), 2)
    : 80.84;

header("Content-Type: text/html; charset=utf-8");
?>
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>श्री बालाजी कृपा धाम — आधिकारिक मोबाइल ऐप डाउनलोड (v<?= htmlspecialchars($versionName) ?>)</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&family=Noto+Sans+Devanagari:wght@400;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-color: #FFFDF9;
            --primary-saffron: #E65100;
            --primary-gold: #FF8F00;
            --maroon-dark: #800000;
            --card-bg: #FFFFFF;
            --text-main: #212121;
            --text-muted: #555555;
            --border-color: #FFE0B2;
            --success-green: #2E7D32;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Plus Jakarta Sans', 'Noto Sans Devanagari', -apple-system, sans-serif;
            background: linear-gradient(180deg, #FFF8E1 0%, #FFFFFF 100%);
            color: var(--text-main);
            min-height: 100vh;
            padding: 30px 16px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .container {
            max-width: 680px;
            width: 100%;
            background: var(--card-bg);
            border: 2px solid var(--border-color);
            border-radius: 24px;
            box-shadow: 0 12px 40px rgba(230, 81, 0, 0.1);
            padding: 36px 24px;
            text-align: center;
        }
        .mandir-icon {
            font-size: 54px;
            margin-bottom: 12px;
            display: inline-block;
            animation: pulse 2s infinite ease-in-out;
        }
        @keyframes pulse {
            0% { transform: scale(1); }
            50% { transform: scale(1.05); }
            100% { transform: scale(1); }
        }
        h1 {
            color: var(--maroon-dark);
            font-size: 1.8rem;
            font-weight: 800;
            margin-bottom: 8px;
        }
        .subtitle {
            color: var(--text-muted);
            font-size: 0.95rem;
            margin-bottom: 24px;
        }
        .badge-ver {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 8px 18px;
            background: #FFF3E0;
            border: 1.5px solid var(--primary-gold);
            border-radius: 9999px;
            color: var(--primary-saffron);
            font-weight: 700;
            font-size: 0.9rem;
            margin-bottom: 24px;
        }
        .btn-download-primary {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 12px;
            width: 100%;
            padding: 16px 28px;
            background: linear-gradient(135deg, #E65100 0%, #F57C00 100%);
            color: #FFFFFF;
            font-size: 1.15rem;
            font-weight: 800;
            text-decoration: none;
            border-radius: 16px;
            box-shadow: 0 8px 24px rgba(230, 81, 0, 0.35);
            transition: all 0.2s ease;
            margin-bottom: 16px;
        }
        .btn-download-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 12px 30px rgba(230, 81, 0, 0.45);
        }
        .btn-download-secondary {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            width: 100%;
            padding: 12px 20px;
            background: #F5F5F5;
            color: #424242;
            font-size: 0.95rem;
            font-weight: 600;
            text-decoration: none;
            border-radius: 12px;
            border: 1px solid #E0E0E0;
            transition: all 0.2s ease;
        }
        .btn-download-secondary:hover {
            background: #EEEEEE;
        }
        .features-list {
            margin-top: 28px;
            text-align: left;
            background: #FAFAFA;
            border-radius: 16px;
            padding: 20px;
            border: 1px solid #EEEEEE;
        }
        .features-list h3 {
            font-size: 1rem;
            color: var(--maroon-dark);
            margin-bottom: 12px;
            font-weight: 700;
        }
        .features-list ul {
            list-style: none;
            padding: 0;
        }
        .features-list li {
            font-size: 0.9rem;
            color: #424242;
            margin-bottom: 10px;
            display: flex;
            align-items: flex-start;
            gap: 10px;
        }
        .features-list li::before {
            content: '✔';
            color: var(--success-green);
            font-weight: bold;
        }
        .install-guide {
            margin-top: 24px;
            padding: 16px;
            background: #E8F5E9;
            border-radius: 14px;
            border: 1px solid #C8E6C9;
            text-align: left;
            font-size: 0.85rem;
            color: #1B5E20;
            line-height: 1.5;
        }
        .footer-text {
            margin-top: 30px;
            font-size: 0.8rem;
            color: #9E9E9E;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="mandir-icon">🚩</div>
        <h1>श्री बालाजी कृपा धाम</h1>
        <div class="subtitle">ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उत्तर प्रदेश)</div>

        <div class="badge-ver">
            <span>✨ नवीनतम संस्करण: v<?= htmlspecialchars($versionName) ?> (बिल्ड <?= $buildCode ?>)</span>
            <span>•</span>
            <span><?= $filesizeMB ?> MB</span>
        </div>

        <a href="download.php" class="btn-download-primary">
            <span>📥</span>
            <span>श्री बालाजी कृपा धाम ऐप डाउनलोड करें (APK)</span>
        </a>

        <a href="<?= htmlspecialchars($fallbackUrl) ?>" class="btn-download-secondary" target="_blank">
            <span>🌐</span>
            <span>वैकल्पिक डाउनलोड लिंक (GitHub CDN Mirror)</span>
        </a>

        <div class="features-list">
            <h3>🌟 इस नए संस्करण (v<?= htmlspecialchars($versionName) ?>) की प्रमुख विशेषताएँ:</h3>
            <ul>
                <li><strong>🛰️ आश्रम 200m जीपीएस सैटेलाइट शुद्धता इंजन:</strong> मोबाइल टावर द्वारा जीपीएस कैंसल होने की समस्या का स्थायी समाधान। आश्रम परिसर (200 मीटर) में खड़े होने पर तत्काल सटीक सैटेलाइट टोकन सत्यापन।</li>
                <li><strong>📏 मीटर में लाइव दूरी प्रदर्शन:</strong> 1 किमी के भीतर सटीक मीटर (जैसे 45 मीटर, 120 मीटर) में दूरी। कोई भ्रामक दशमलव या सड़क कारक नहीं।</li>
                <li><strong>⏳ पावन 180° फ्लिप रेत-घड़ी एनीमेशन:</strong> टोकन उल्टी गिनती में सजीव सोने के कण व रेत प्रवाह।</li>
                <li><strong>🛡️ 100% सुरक्षित व प्रमाणित:</strong> मंदिर के वास्तविक भक्तों हेतु निर्मित आधिकारिक एप्लिकेशन।</li>
            </ul>
        </div>

        <div class="install-guide">
            <strong>📲 फोन में इनस्टॉल कैसे करें:</strong><br>
            1. 'ऐप डाउनलोड करें' बटन दबाएं।<br>
            2. डाउनलोड पूरा होने पर नोटिफिकेशन बार से <code>ShriBalajiKripaDham-release.apk</code> पर टैप करें।<br>
            3. यदि क्रोम "Unknown Sources" या "अज्ञात स्रोत" पूछे, तो 'Allow from this source' (अनुमति दें) पर क्लिक करें।<br>
            4. 'Install' (इनस्टॉल) दबाएं और ऐप चालू करें।
        </div>

        <div class="footer-text">
            © <?= date('Y') ?> श्री बालाजी कृपा धाम, डूँगरा जाट • सर्वाधिकार सुरक्षित
        </div>
    </div>
</body>
</html>
