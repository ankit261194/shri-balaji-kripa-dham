<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);
// ==============================================================================
// ANKIT ELEVEX PRO - ENTERPRISE MULTI-APP DOWNLOAD & DISTRIBUTION HUB
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// Hostinger NVMe SSD Cluster: https://shribalajikripadham.online
// ==============================================================================

$appParam = strtolower(trim($_GET['app'] ?? ''));
$viewParam = strtolower(trim($_GET['view'] ?? ''));
$isPortal = ($appParam === 'hub' || $appParam === 'portal' || $viewParam === 'portal' || isset($_GET['hub']));

// 1. Dedicated EleveX Multi-App Mapping
$elevexApps = [
    'superadmin' => [
        'name' => 'EleveX SuperAdmin App',
        'title_hi' => 'सुपर एडमिन सुप्रीम कंट्रोलर',
        'package' => 'com.ankit.elevex.superadmin',
        'version' => '5.9.40',
        'build' => 630,
        'filename' => 'EleveX_SuperAdmin_v5.9.40.apk',
        'size' => '39.0 MB',
        'sha256' => 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        'color' => '#8B5CF6',
        'icon' => '🛡️',
        'features' => ['घोस्ट मोड (Ghost Mode Supreme)', 'मल्टी-टेनेंट कंपनी प्रोविजनिंग', 'डायनामिक फीचर टॉगल्स', 'अपरिवर्तनीय लेज़र ऑडिट ट्रेल']
    ],
    'supervisor' => [
        'name' => 'EleveX Supervisor Cockpit',
        'title_hi' => 'सुपरवाइज़र फ्लीट कॉकपिट',
        'package' => 'com.ankit.elevex.supervisor',
        'version' => '5.9.40',
        'build' => 630,
        'filename' => 'EleveX_Supervisor_v5.9.40.apk',
        'size' => '39.0 MB',
        'sha256' => 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        'color' => '#0284C7',
        'icon' => '📡',
        'features' => ['लाइव OpenStreetMap लैंडमार्क', 'स्मार्ट हैवर्सिन ऑटो-डिस्पैच', 'नॉन-AMC आपातकालीन डिस्पैच', 'रियल-टाइम पावर/बैटरी स्थिति']
    ],
    'technician' => [
        'name' => 'EleveX Technician Terminal',
        'title_hi' => 'फील्ड टेक्नीशियन टर्मिनल',
        'package' => 'com.ankit.elevex.technician',
        'version' => '5.9.40',
        'build' => 630,
        'filename' => 'EleveX_Technician_v5.9.40.apk',
        'size' => '39.0 MB',
        'sha256' => 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        'color' => '#10B981',
        'icon' => '🔧',
        'features' => ['1,012+ ऑफलाइन OEM फॉल्ट कोड्स', '4-लेयर एंटी-मॉक GPS सुरक्षा', '150m जियोफेंस कस्टमर OTP लॉक', 'रूट व USB डिबगिंग डिटेक्शन']
    ],
    'owner' => [
        'name' => 'EleveX Lift Owner App',
        'title_hi' => 'लिफ्ट ओनर क्लाइंट पोर्टल',
        'package' => 'com.ankit.elevex.owner',
        'version' => '5.9.40',
        'build' => 630,
        'filename' => 'EleveX_Owner_v5.9.40.apk',
        'size' => '39.0 MB',
        'sha256' => 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        'color' => '#F59E0B',
        'icon' => '🏢',
        'features' => ['1-टैप आपातकालीन SOS डिस्पैच', 'लाइव टेक्नीशियन राडार व ETA', 'डायनामिक 6-अंकीय समापन OTP', '1 से 5 स्टार सर्विस ऑडिट रेटिंग']
    ],
    'gateway' => [
        'name' => 'EleveX Master Gateway Portal',
        'title_hi' => 'मास्टर 4-इन-1 गेटवे पोर्टल',
        'package' => 'com.ankit.liftramban.ankits_lift_ramban_ai',
        'version' => '5.9.40',
        'build' => 630,
        'filename' => 'EleveX_Master_Gateway_v5.9.40.apk',
        'size' => '39.0 MB',
        'sha256' => 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        'color' => '#3B82F6',
        'icon' => '🌐',
        'features' => ['एकीकृत 4-ऐप स्विचर', 'मल्टी-टेनेंट कंपनी टेस्टिंग', '47 इंडस्ट्रियल इंजीनियरिंग टूल्स', '1-टैप द्विभाषी स्विच (Hindi/Eng)']
    ]
];

// Alias for generic elevex
if ($appParam === 'elevex' || $appParam === 'ankit_elevex' || $appParam === 'ankit_elievex') {
    $appParam = 'gateway';
}

// 2. Direct APK Download Delivery
if (isset($elevexApps[$appParam])) {
    $appInfo = $elevexApps[$appParam];
    $filename = $appInfo['filename'];

    $candidateDirs = array_filter([
        __DIR__ . '/elevex_gateway/downloads',
        isset($_SERVER['DOCUMENT_ROOT']) ? $_SERVER['DOCUMENT_ROOT'] . '/elevex_gateway/downloads' : null,
        dirname(__DIR__) . '/elevex_gateway/downloads',
        __DIR__ . '/downloads',
        dirname(__DIR__) . '/downloads',
        '/home/u237101617/domains/shribalajikripadham.online/public_html/elevex_gateway/downloads'
    ]);

    $targetFile = '';
    foreach ($candidateDirs as $dir) {
        if (!is_dir($dir)) continue;

        // Specific named APK
        $specific = "$dir/{$filename}";
        if (file_exists($specific) && filesize($specific) > 10000000) {
            $targetFile = $specific;
            break;
        }

        // Generic Release APK fallback
        $rel = "$dir/Ankit_EleveX_Release.apk";
        if (file_exists($rel) && filesize($rel) > 10000000) {
            $targetFile = $rel;
            break;
        }

        // Any v5.9.39 or newer APK
        $matches = glob("$dir/Ankit_EleveX*.apk");
        if (!empty($matches)) {
            natsort($matches);
            $matches = array_reverse($matches);
            foreach ($matches as $m) {
                if (filesize($m) > 10000000) {
                    $targetFile = $m;
                    break 2;
                }
            }
        }
    }

    if (!empty($targetFile) && file_exists($targetFile)) {
        $filesize = filesize($targetFile);
        while (ob_get_level()) ob_end_clean();
        header("Content-Type: application/vnd.android.package-archive");
        header("Content-Disposition: attachment; filename="{$filename}"");
        header("Content-Length: {$filesize}");
        header("Accept-Ranges: bytes");
        header("Cache-Control: public, no-cache, no-store, must-revalidate, max-age=0");
        header("Pragma: no-cache");
        header("Expires: 0");

        $handle = fopen($targetFile, "rb");
        if ($handle) {
            while (!feof($handle)) {
                echo fread($handle, 1048576); // 1 MB chunk
                flush();
            }
            fclose($handle);
        } else {
            readfile($targetFile);
        }
        exit;
    }

    // High-Speed Guaranteed GitHub Releases Mirror
    header("Location: https://github.com/ankit261194/ankits-liftramban/releases/download/v5.9.39/Ankit_EleveX_v5.9.39_Final.apk", true, 302);
    exit;
}

// 3. Fallback for Ashram App if explicitly requested
if ($appParam === 'ashram' || $appParam === 'balaji') {
    $tag = 'v2.81.0';
    $externalCdnUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/{$tag}/ShriBalajiKripaDham-release.apk";
    header("Location: " . $externalCdnUrl, true, 302);
    exit;
}

// 4. Ultra-Modern Responsive Enterprise Web Download Portal
header("Content-Type: text/html; charset=utf-8");
?>
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ankit EleveX Pro — Enterprise Multi-App Download Hub</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&family=Noto+Sans+Devanagari:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-dark: #070B14;
            --surface-dark: #0F172A;
            --border-color: #1E293B;
            --text-primary: #F8FAFC;
            --text-secondary: #94A3B8;
            --accent-blue: #0284C7;
            --accent-purple: #8B5CF6;
            --accent-green: #10B981;
            --accent-amber: #F59E0B;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Inter', 'Noto Sans Devanagari', -apple-system, sans-serif;
            background-color: var(--bg-dark);
            color: var(--text-primary);
            min-height: 100vh;
            padding: 30px 16px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .header {
            text-align: center;
            max-width: 800px;
            margin-bottom: 36px;
        }
        .badge-live {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 14px;
            background: rgba(16, 185, 129, 0.12);
            border: 1px solid rgba(16, 185, 129, 0.3);
            border-radius: 9999px;
            color: #34D399;
            font-size: 0.82rem;
            font-weight: 700;
            margin-bottom: 14px;
        }
        .badge-live::before {
            content: '';
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #10B981;
            box-shadow: 0 0 8px #10B981;
        }
        h1 {
            font-size: 2.1rem;
            font-weight: 800;
            letter-spacing: -0.5px;
            margin-bottom: 8px;
            background: linear-gradient(135deg, #FFFFFF, #94A3B8);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .subtitle {
            font-size: 1rem;
            color: var(--text-secondary);
            line-height: 1.5;
        }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
            gap: 20px;
            max-width: 1140px;
            width: 100%;
            margin-bottom: 40px;
        }
        .card {
            background: var(--surface-dark);
            border: 1px solid var(--border-color);
            border-radius: 18px;
            padding: 24px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: transform 0.2s, border-color 0.2s;
            position: relative;
            overflow: hidden;
        }
        .card:hover {
            transform: translateY(-3px);
            border-color: rgba(2, 132, 199, 0.5);
        }
        .card-top {
            display: flex;
            align-items: flex-start;
            gap: 14px;
            margin-bottom: 16px;
        }
        .card-icon {
            font-size: 2.2rem;
            width: 54px;
            height: 54px;
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: rgba(255, 255, 255, 0.05);
            flex-shrink: 0;
        }
        .card-title {
            font-size: 1.2rem;
            font-weight: 700;
            color: white;
            line-height: 1.3;
        }
        .card-sub {
            font-size: 0.85rem;
            color: var(--text-secondary);
            margin-top: 2px;
        }
        .feature-list {
            list-style: none;
            margin: 16px 0 20px 0;
        }
        .feature-list li {
            font-size: 0.88rem;
            color: #CBD5E1;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .feature-list li::before {
            content: '✓';
            color: #10B981;
            font-weight: bold;
        }
        .meta-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 14px;
            background: rgba(0, 0, 0, 0.25);
            border-radius: 10px;
            font-size: 0.78rem;
            color: var(--text-secondary);
            margin-bottom: 16px;
        }
        .btn-download {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            padding: 13px 20px;
            background: linear-gradient(135deg, var(--accent-blue), #0369A1);
            color: white;
            text-decoration: none;
            border-radius: 12px;
            font-weight: 700;
            font-size: 0.95rem;
            box-shadow: 0 4px 14px rgba(2, 132, 199, 0.35);
            transition: filter 0.2s;
        }
        .btn-download:hover {
            filter: brightness(1.15);
        }
        .footer {
            text-align: center;
            color: var(--text-secondary);
            font-size: 0.82rem;
            line-height: 1.6;
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="badge-live">HOSTINGER NVMe SSD • HIGH-SPEED MIRROR</div>
        <h1>Ankit EleveX Pro Suite</h1>
        <p class="subtitle">अंकित एलेवेक्स प्रो — इंडस्ट्रियल लिफ्ट फ्लीट इकोसिस्टम (v5.9.40 • Build 630)<br>Dedicated Standalone Enterprise APKs for Super Admin, Supervisor, Technician & Lift Owners</p>
    </div>

    <div class="grid">
        <?php foreach ($elevexApps as $key => $app): ?>
        <div class="card">
            <div>
                <div class="card-top">
                    <div class="card-icon"><?= $app['icon'] ?></div>
                    <div>
                        <div class="card-title"><?= $app['name'] ?></div>
                        <div class="card-sub"><?= $app['title_hi'] ?></div>
                    </div>
                </div>
                <ul class="feature-list">
                    <?php foreach ($app['features'] as $feat): ?>
                    <li><?= $feat ?></li>
                    <?php endforeach; ?>
                </ul>
            </div>
            <div>
                <div class="meta-bar">
                    <span>Package: <?= $app['package'] ?></span>
                    <span><?= $app['size'] ?></span>
                </div>
                <a href="download.php?app=<?= $key ?>" class="btn-download" style="background: linear-gradient(135deg, <?= $app['color'] ?>, #0284C7);">
                    📥 Download APK (v<?= $app['version'] ?>)
                </a>
            </div>
        </div>
        <?php endforeach; ?>
    </div>

    <div class="footer">
        <p>Conceived & Engineered by <strong>Ankit Chaudhary (8533955333)</strong></p>
        <p>Protected by Android Enterprise Device Admin & Immutable Server Ledger Audit • All 4 Apps Supported on Android 7.0+</p>
    </div>
</body>
</html>
<?php
exit;
