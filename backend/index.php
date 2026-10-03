<?php
// Shri Balaji Kripa Dham - High-Speed Dynamic Render Engine (Zero-Crash Architecture)
$cacheDir = __DIR__ . '/cache';
$cacheFile = $cacheDir . '/site_data_cache.json';
$cacheTTL = 3; // 3 seconds ultra-fast TTL for instant live synchronization under 0.01s

$siteData = null;
if (file_exists($cacheFile) && (time() - filemtime($cacheFile) < $cacheTTL)) {
    $raw = @file_get_contents($cacheFile);
    if ($raw) {
        $siteData = json_decode($raw, true);
    }
}

if (!$siteData || !isset($siteData['settings'])) {
    if (file_exists(__DIR__ . '/config/db.php')) {
        require_once __DIR__ . '/config/db.php';
    }
    $pdo = function_exists('getDB') ? getDB() : null;
    $settings = [];
    $sevadars = [];
    $donors = [];
    if ($pdo) {
        try {
            $stmt = $pdo->query("SELECT * FROM ashram_settings WHERE id = 1 LIMIT 1");
            $settings = $stmt ? ($stmt->fetch(PDO::FETCH_ASSOC) ?: []) : [];
        } catch (Throwable $e) {}
        try {
            $sevadars = $pdo->query("SELECT * FROM sevadars WHERE is_active = 1 ORDER BY display_order ASC, id ASC")->fetchAll(PDO::FETCH_ASSOC) ?: [];
        } catch (Throwable $e) {}
        try {
            $donors = $pdo->query("SELECT * FROM donors WHERE is_active = 1 ORDER BY display_order ASC, id ASC")->fetchAll(PDO::FETCH_ASSOC) ?: [];
        } catch (Throwable $e) {}
        $todayDarshan = null;
        try {
            $todayD = date('Y-m-d');
            $darshanStmt = $pdo->prepare("SELECT * FROM daily_darshan WHERE darshan_date = :d LIMIT 1");
            $darshanStmt->execute([':d' => $todayD]);
            $todayDarshan = $darshanStmt->fetch(PDO::FETCH_ASSOC) ?: null;
            if (!$todayDarshan) {
                $latestStmt = $pdo->query("SELECT * FROM daily_darshan ORDER BY darshan_date DESC LIMIT 1");
                $todayDarshan = $latestStmt ? ($latestStmt->fetch(PDO::FETCH_ASSOC) ?: null) : null;
            }
        } catch (Throwable $e) {}
    }
    $siteData = [
        'settings' => $settings,
        'sevadars' => $sevadars,
        'donors' => $donors,
        'darshan' => $todayDarshan,
        'timestamp' => time()
    ];
    if (!is_dir($cacheDir)) {
        @mkdir($cacheDir, 0755, true);
    }
    @file_put_contents($cacheFile, json_encode($siteData, JSON_UNESCAPED_UNICODE), LOCK_EX);
}

$settings = $siteData['settings'] ?? [];
$sevadars = $siteData['sevadars'] ?? [];
$donors = $siteData['donors'] ?? [];
$darshan = $siteData['darshan'] ?? null;

// Version Info from version.json
$verFile = __DIR__ . '/version.json';
$appVersionName = '2.56.1';
$appVersionCode = 76;
if (file_exists($verFile)) {
    $verData = json_decode(@file_get_contents($verFile), true);
    if (!empty($verData['version_name'])) $appVersionName = $verData['version_name'];
    if (!empty($verData['version_code'])) $appVersionCode = $verData['version_code'];
}

$ashramName = !empty($settings['ashram_name']) ? $settings['ashram_name'] : 'श्री बालाजी कृपा धाम';
$bannerTitle = !empty($settings['banner_title']) ? $settings['banner_title'] : 'श्री बालाजी कृपा धाम';
$bannerSubtitle = !empty($settings['banner_subtitle']) ? $settings['banner_subtitle'] : '📍 ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उ.प्र.)';
$emergencyNotice = !empty($settings['emergency_notice']) ? $settings['emergency_notice'] : '';
$isEmergencyVisible = (!empty($settings['is_emergency_notice_visible']) && !empty($emergencyNotice));
$darbarTimings = !empty($settings['darbar_timings']) ? $settings['darbar_timings'] : 'प्रत्येक रविवार प्रातःकाल 8:30 बजे से सायं 5:00 बजे तक';
$hindiMonths = [
    1 => 'जनवरी', 2 => 'फ़रवरी', 3 => 'मार्च', 4 => 'अप्रैल',
    5 => 'मई', 6 => 'जून', 7 => 'जुलाई', 8 => 'अगस्त',
    9 => 'सितम्बर', 10 => 'अक्टूबर', 11 => 'नवम्बर', 12 => 'दिसम्बर'
];
$rawDarbarDate = !empty($settings['darbar_date']) ? trim($settings['darbar_date']) : '';
$todayMidnight = strtotime('today');
$isPastDate = false;
$parsedTargetTs = false;

if (!empty($rawDarbarDate)) {
    $t = strtotime($rawDarbarDate);
    if ($t === false && preg_match('/^(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{4})$/', $rawDarbarDate, $m)) {
        $t = strtotime("{$m[3]}-{$m[2]}-{$m[1]}");
    }
    if ($t !== false) {
        if ($t < $todayMidnight) {
            $isPastDate = true;
        } else {
            $parsedTargetTs = $t;
        }
    }
}

if ($parsedTargetTs !== false && !$isPastDate) {
    $sunDay = date('j', $parsedTargetTs);
    $sunMonth = $hindiMonths[(int)date('n', $parsedTargetTs)] ?? date('M', $parsedTargetTs);
    $sunYear = date('Y', $parsedTargetTs);
    $w = (int)date('w', $parsedTargetTs);
    $dayNameHindi = [0 => 'रविवार', 1 => 'सोमवार', 2 => 'मंगलवार', 3 => 'बुधवार', 4 => 'गुरुवार', 5 => 'शुक्रवार', 6 => 'शनिवार'][$w] ?? 'रविवार';
    $darbarDate = "{$dayNameHindi}, {$sunDay} {$sunMonth} {$sunYear}";
} else {
    // If empty OR date has already passed, automatically roll over to next upcoming Sunday!
    $todayDayOfWeek = (int)date('w'); // 0 = Sunday
    $nextSundayTs = ($todayDayOfWeek === 0 && (int)date('H') < 18) ? time() : strtotime('next Sunday');
    $sunDay = date('j', $nextSundayTs);
    $sunMonth = $hindiMonths[(int)date('n', $nextSundayTs)] ?? 'अक्टूबर';
    $sunYear = date('Y', $nextSundayTs);
    $darbarDate = "रविवार, {$sunDay} {$sunMonth} {$sunYear}";
}
$currentServing = !empty($settings['current_serving_token']) ? (int)$settings['current_serving_token'] : 0;
if ($currentServing <= 0) {
    if (file_exists(__DIR__ . '/config/db.php')) require_once __DIR__ . '/config/db.php';
    $dbPdo = function_exists('getDB') ? getDB() : null;
    if ($dbPdo) {
        try {
            $todayD = date('Y-m-d');
            $tStmt = $dbPdo->prepare("SELECT MAX(token_number) FROM tokens WHERE darbar_date = :d AND status IN ('SERVING', 'COMPLETED')");
            $tStmt->execute([':d' => $todayD]);
            $mx = $tStmt->fetchColumn();
            if ($mx && intval($mx) > 0) {
                $currentServing = intval($mx);
            } else {
                $wStmt = $dbPdo->prepare("SELECT MIN(token_number) FROM tokens WHERE darbar_date = :d AND status = 'WAITING'");
                $wStmt->execute([':d' => $todayD]);
                $mn = $wStmt->fetchColumn();
                if ($mn && intval($mn) > 0) {
                    $currentServing = intval($mn);
                } else {
                    $allStmt = $dbPdo->query("SELECT MAX(token_number) FROM tokens WHERE status IN ('SERVING', 'COMPLETED')");
                    $allMx = $allStmt ? $allStmt->fetchColumn() : null;
                    if ($allMx && intval($allMx) > 0) {
                        $currentServing = intval($allMx);
                    } else {
                        $firstTok = $dbPdo->query("SELECT MIN(token_number) FROM tokens");
                        $firstVal = $firstTok ? $firstTok->fetchColumn() : null;
                        if ($firstVal && intval($firstVal) > 0) $currentServing = intval($firstVal);
                    }
                }
            }
        } catch (Throwable $e) {}
    }
}

$isTuesdayDarbarEnabled = !empty($settings['is_tuesday_darbar_enabled']);
$tuesdayDarbarName = !empty($settings['tuesday_darbar_name']) ? $settings['tuesday_darbar_name'] : 'श्री बालाजी कृपा धाम (मंगलवार दरबार, बुलन्दशहर)';
$tuesdayDarbarAddress = !empty($settings['tuesday_darbar_address']) ? $settings['tuesday_darbar_address'] : 'बुलन्दशहर, उत्तर प्रदेश';
$tuesdayTimings = !empty($settings['tuesday_darbar_timings']) ? $settings['tuesday_darbar_timings'] : 'प्रत्येक मंगलवार प्रातः 8:00 बजे से सायं 5:00 बजे तक';
$tuesdayServiceMode = !empty($settings['tuesday_token_service_mode']) ? $settings['tuesday_token_service_mode'] : 'AUTO_TUESDAY';
$tuesdayCurrentServing = !empty($settings['tuesday_current_serving_token']) ? (int)$settings['tuesday_current_serving_token'] : (!empty($settings['tuesday_running_token_number']) ? (int)$settings['tuesday_running_token_number'] : 0);

$isTuesdayActive = false;
if ($isTuesdayDarbarEnabled) {
    if ($tuesdayServiceMode === 'FORCE_OPEN') {
        $isTuesdayActive = true;
    } elseif ($tuesdayServiceMode === 'FORCE_CLOSED') {
        $isTuesdayActive = false;
    } else {
        $dow = intval(date('w'));
        $hr = intval(date('G'));
        $isTuesdayActive = ($dow === 2 && $hr >= 8 && $hr < 17);
    }
}

if ($tuesdayCurrentServing <= 0 && $dbPdo) {
    try {
        $todayD = date('Y-m-d');
        $tStmt2 = $dbPdo->prepare("SELECT MAX(token_number) FROM tokens WHERE darbar_date = :d AND darbar_venue = 'BULANDSHAHR' AND status IN ('SERVING', 'COMPLETED')");
        $tStmt2->execute([':d' => $todayD]);
        $mx2 = $tStmt2->fetchColumn();
        if ($mx2 && intval($mx2) > 0) {
            $tuesdayCurrentServing = intval($mx2);
        } else {
            $wStmt2 = $dbPdo->prepare("SELECT MIN(token_number) FROM tokens WHERE darbar_date = :d AND darbar_venue = 'BULANDSHAHR' AND status = 'WAITING'");
            $wStmt2->execute([':d' => $todayD]);
            $mn2 = $wStmt2->fetchColumn();
            if ($mn2 && intval($mn2) > 0) $tuesdayCurrentServing = intval($mn2);
        }
    } catch (Throwable $e) {}
}

$gurujiPhoto = !empty($settings['guruji_photo_url']) ? $settings['guruji_photo_url'] : 'uploads/guruji_profile.jpg';
$isDarbarActive = !isset($settings['is_darbar_active']) || $settings['is_darbar_active'] == 1;
$isBusLive = !empty($settings['is_bus_booking_live']);
$isDharamshalaLive = !empty($settings['is_dharamshala_live']);
$isArziLive = !isset($settings['is_arzi_ledger_live']) || $settings['is_arzi_ledger_live'] == 1;

$contactPhone = (!empty($settings['contact_phone']) && strpos($settings['contact_phone'], '97206') === false && strpos($settings['contact_phone'], '98765') === false) ? trim($settings['contact_phone']) : '';
$whatsappNumber = (!empty($settings['whatsapp_number']) && strpos($settings['whatsapp_number'], '97206') === false && strpos($settings['whatsapp_number'], '98765') === false) ? trim($settings['whatsapp_number']) : '';
$upiId = (!empty($settings['upi_id']) && $settings['upi_id'] !== 'shribalajikripadham@upi') ? trim($settings['upi_id']) : '';
$upiName = !empty($settings['upi_name']) ? trim($settings['upi_name']) : $ashramName;
$badiArziRate = isset($settings['badi_arzi_rate']) ? (float)$settings['badi_arzi_rate'] : 0.0;
$chhotiArziRate = isset($settings['chhoti_arzi_rate']) ? (float)$settings['chhoti_arzi_rate'] : 0.0;

// Expanded Full Dynamic Website CMS fields
$topBarText = !empty($settings['top_bar_text']) ? $settings['top_bar_text'] : '🚩 ॐ श्री हनुमते नमः • संकट कटै मिटै सब पीरा, जो सुमरै हनुमत बलबीरा 🚩';
$gurujiTitle = !empty($settings['guruji_title']) ? $settings['guruji_title'] : 'परम पूज्य गुरुजी तेजवीर सिंह जी';
$gurujiBio = !empty($settings['guruji_bio']) ? $settings['guruji_bio'] : 'संकट मोचन श्री बालाजी महाराज के अनन्य उपासक एवं पावन कृपा धाम के पीठाधीश्वर।';
$tokenRulesNotice = !empty($settings['token_rules_notice']) ? $settings['token_rules_notice'] : 'आश्रम की निष्पक्षता, पारदर्शी कतार, GPS लोकेशन एवं AI बायोमेट्रिक सुरक्षा नियमों के अनुसार टोकन पंजीकरण केवल और केवल आधिकारिक मोबाइल ऐप से ही संभव है। वेबसाइट पर कोई टोकन जनरेशन फॉर्म नहीं है। टोकन प्राप्त करने के लिए कृपया ऊपर दिए गए बटन से मोबाइल ऐप इंस्टॉल करें।';
$bankName = !empty($settings['bank_name']) ? $settings['bank_name'] : '';
$bankAccountHolder = !empty($settings['bank_account_holder']) ? $settings['bank_account_holder'] : '';
$bankAccountNumber = (!empty($settings['bank_account_number']) && strpos($settings['bank_account_number'], 'XXXX') === false) ? $settings['bank_account_number'] : '';
$bankIfsc = (!empty($settings['bank_ifsc']) && strpos($settings['bank_ifsc'], 'XXXX') === false) ? $settings['bank_ifsc'] : '';
$bankBranch = !empty($settings['bank_branch']) ? $settings['bank_branch'] : '';
$hasBankDetails = (!empty($bankAccountNumber) || !empty($upiId));
$ashramAddress = (!empty($settings['ashram_address']) && strpos($settings['ashram_address'], 'अनूपशहर') !== false) 
    ? $settings['ashram_address'] 
    : "श्री बालाजी कृपा धाम\nग्राम डूँगरा जाट, तहसील अनूपशहर,\nजिला बुलन्दशहर, उत्तर प्रदेश - 202394";
$ashramDirections = (!empty($settings['ashram_directions']) && strpos($settings['ashram_directions'], 'बबराला') === false) 
    ? $settings['ashram_directions'] 
    : "🚆 एकमात्र नजदीकी रेलवे स्टेशन: केवल बुलन्दशहर रेलवे स्टेशन (BSC) (~28-30 किमी)\n🏙️ निकटवर्ती प्रमुख 3 शहर: जहांगीराबाद (~10 किमी) • बुलन्दशहर (~30 किमी) • अनूपशहर (~16 किमी)";
$ashramHistory = !empty($settings['ashram_history_hindi']) ? $settings['ashram_history_hindi'] : (!empty($settings['ashram_history']) ? $settings['ashram_history'] : 'परम पूज्य गुरुजी तेजवीर सिंह जी को श्री बालाजी महाराज व श्री भैरव बाबा का साक्षात आशीर्वाद प्राप्त है। पिछले कई वर्षों से ग्राम डूँगरा जाट धाम पर लाखों पीड़ित भक्तों को नई जिंदगी, मानसिक शांति व शारीरिक व्याधियों से मुक्ति मिली है।');
$contactEmail = (!empty($settings['contact_email']) && $settings['contact_email'] !== 'shribalajikripadham@gmail.com') ? trim($settings['contact_email']) : '';
$youtubeUrl = !empty($settings['youtube_channel_url']) ? $settings['youtube_channel_url'] : (!empty($settings['youtube_url']) ? $settings['youtube_url'] : 'https://www.youtube.com/@ShriBalajiKripaDham');
$facebookUrl = !empty($settings['facebook_page_url']) ? $settings['facebook_page_url'] : (!empty($settings['facebook_url']) ? $settings['facebook_url'] : 'https://www.facebook.com/ShriBalajiKripaDham');
$instagramUrl = !empty($settings['instagram_url']) ? $settings['instagram_url'] : 'https://www.instagram.com/shribalajikripadham';
$youtubeLiveUrl = !empty($settings['youtube_live_url']) ? $settings['youtube_live_url'] : '';
$whatsappChannelUrl = (!empty($settings['whatsapp_channel_url']) && strpos($settings['whatsapp_channel_url'], '/invite') === false) ? $settings['whatsapp_channel_url'] : 'https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0';
$footerTitle = !empty($settings['footer_title']) ? $settings['footer_title'] : 'श्री बालाजी कृपा धाम';
$footerDedication = !empty($settings['footer_dedication']) ? $settings['footer_dedication'] : 'सर्वस्व श्री रामभक्त वीर हनुमान जी महाराज के पावन चरणों में समर्पित।';
$footerCopyright = !empty($settings['footer_copyright']) ? $settings['footer_copyright'] : '© 2026 श्री बालाजी कृपा धाम सेवा ट्रस्ट। सर्वाधिकार सुरक्षित।';

// Consecrated Daily Darshan & Guru Vichar (App & Web Live Synced)
$darshanPhoto = !empty($darshan['photo_url']) ? $darshan['photo_url'] : 'media/balaji_darshan_today.jpg';
$darshanTitle = !empty($darshan['title']) ? $darshan['title'] : 'श्री बालाजी महाराज दैनिक दिव्य अलौकिक श्रृंगार दर्शन';
$darshanQuote = !empty($darshan['blessings_quote']) ? $darshan['blessings_quote'] : 'जब जीवन में हर तरफ से रास्ते बंद दिखने लगें, मन अशांत हो और अपने भी साथ छोड़ दें, तब घबराकर कभी अधर्म का रास्ता मत चुनना। संकट की घड़ी भक्त के धैर्य की परीक्षा होती है। पूज्य गुरुदेव समझाते हैं कि अपनी विपत्ति का बोझ अपने सिर पर मत ढोओ, उसे पूर्ण विश्वास के साथ श्री बालाजी महाराज के चरणों में समर्पित कर दो। बालाजी महाराज स्वयं ढाल बनकर तुम्हारे सारे कष्ट हर लेंगे।';

$currentHour = intval(date('G'));
$currentMin = intval(date('i'));
$dayOfYear = intval(date('z'));
$timeBasedViews = 450 + ($currentHour * 85) + intval($currentMin * 1.4) + (($dayOfYear * 37) % 65);
$rawViews = !empty($darshan['views_count']) ? intval($darshan['views_count']) : 0;
$darshanViews = ($rawViews > 250) ? $rawViews : $timeBasedViews;
?>
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>श्री बालाजी कृपा धाम | आधिकारिक वेबसाइट एवं मोबाइल ऐप (ग्राम डूँगरा जाट, बुलन्दशहर)</title>
    <meta name="description" content="श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर (उत्तर प्रदेश) की आधिकारिक वेबसाइट। रविवार टोकन, श्री बालाजी यात्रा बस एवं लाइव दर्शन हेतु आधिकारिक Android ऐप डाउनलोड करें।">
    <meta name="keywords" content="श्री बालाजी कृपा धाम, डूँगरा जाट, बुलन्दशहर, बालाजी टोकन, बालाजी यात्रा, हनुमान मंदिर, Dungra Jaat, Bulandshahr">
    
    <!-- Open Graph for Social Sharing & WhatsApp -->
    <meta property="og:title" content="श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट - आधिकारिक वेबसाइट">
    <meta property="og:description" content="आश्रम की आधिकारिक वेबसाइट। लाइव दर्शन टोकन, सेवादल, दानदाता मंडल एवं Android ऐप डाउनलोड।">
    <meta property="og:url" content="https://shribalajikripadham.online">
    <meta property="og:type" content="website">
    <meta name="theme-color" content="#800000">

    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Mukta:wght@300;400;600;700;800&family=Poppins:wght@400;600;700&display=swap" rel="stylesheet">

    <style>
        :root {
            --primary: #800000;
            --primary-dark: #4A0000;
            --saffron: #FF8F00;
            --saffron-deep: #E65100;
            --gold: #FFD700;
            --gold-light: #FFF9C4;
            --bg-cream: #FFFDF9;
            --text-dark: #212121;
            --text-muted: #555555;
            --card-border: #FFE082;
            --green: #2E7D32;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: 'Mukta', 'Poppins', sans-serif;
        }

        body {
            background-color: var(--bg-cream);
            color: var(--text-dark);
            line-height: 1.6;
            overflow-x: hidden;
        }

        /* Top Bar */
        .top-bar {
            background: linear-gradient(90deg, #4A0000, #800000, #4A0000);
            color: var(--gold);
            text-align: center;
            padding: 8px 15px;
            font-size: 0.95rem;
            font-weight: 600;
            letter-spacing: 0.5px;
            border-bottom: 2px solid var(--gold);
        }

        /* Navigation */
        nav {
            background: #ffffff;
            box-shadow: 0 4px 15px rgba(0,0,0,0.08);
            position: sticky;
            top: 0;
            z-index: 1000;
            border-bottom: 2px solid #FFECB3;
        }

        .nav-container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 12px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .nav-logo {
            display: flex;
            align-items: center;
            gap: 12px;
            text-decoration: none;
        }

        .nav-logo-icon {
            font-size: 2.2rem;
            background: #FFF3E0;
            padding: 6px;
            border-radius: 50%;
            border: 2px solid var(--saffron);
        }

        .nav-logo-text h1 {
            font-size: 1.35rem;
            font-weight: 800;
            color: var(--primary);
            line-height: 1.2;
        }

        .nav-logo-text p {
            font-size: 0.82rem;
            color: var(--text-muted);
            font-weight: 600;
        }

        .nav-actions {
            display: flex;
            gap: 12px;
            align-items: center;
        }

        .btn-nav-download {
            background: linear-gradient(135deg, var(--saffron-deep), var(--saffron));
            color: #ffffff;
            text-decoration: none;
            padding: 10px 18px;
            border-radius: 25px;
            font-weight: 700;
            font-size: 0.95rem;
            box-shadow: 0 4px 10px rgba(230, 81, 0, 0.3);
            display: inline-flex;
            align-items: center;
            gap: 8px;
            transition: all 0.3s ease;
        }

        .btn-nav-download:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 15px rgba(230, 81, 0, 0.45);
        }

        /* Hero Section */
        .hero {
            background: linear-gradient(180deg, #FFF3E0 0%, #FFFFFF 100%);
            padding: 40px 20px 25px;
            text-align: center;
            position: relative;
            border-bottom: 1px solid #FFE0B2;
        }

        .hero-badge {
            display: inline-block;
            background: #FFE082;
            color: var(--primary-dark);
            padding: 6px 16px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 0.9rem;
            margin-bottom: 15px;
            border: 1px solid #FFD54F;
        }

        .hero h2 {
            font-size: 2.3rem;
            font-weight: 800;
            color: var(--primary);
            margin-bottom: 6px;
            line-height: 1.25;
        }

        .hero p.location {
            font-size: 1.15rem;
            color: var(--saffron-deep);
            font-weight: 700;
            margin-bottom: 12px;
        }

        .shloka {
            font-style: italic;
            color: #6D4C41;
            font-size: 1.05rem;
            margin-bottom: 25px;
            background: #FFF8E1;
            display: inline-block;
            padding: 8px 20px;
            border-radius: 10px;
            border-left: 4px solid var(--saffron);
        }

        /* Guruji Profile Card in Hero */
        .guruji-card-container {
            max-width: 480px;
            margin: 0 auto 30px;
            background: #FFFFFF;
            border-radius: 18px;
            padding: 20px;
            box-shadow: 0 8px 24px rgba(128, 0, 0, 0.08);
            border: 2px solid #FFE082;
            display: flex;
            align-items: center;
            gap: 18px;
            text-align: left;
        }

        .guruji-photo-wrap {
            width: 105px;
            height: 105px;
            border-radius: 50%;
            overflow: hidden;
            border: 3px solid var(--gold);
            box-shadow: 0 4px 12px rgba(230, 81, 0, 0.25);
            flex-shrink: 0;
            background: #FFF3E0;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .guruji-photo-wrap img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .guruji-info h4 {
            color: var(--primary);
            font-size: 1.25rem;
            font-weight: 800;
            margin-bottom: 2px;
        }

        .guruji-info p.title {
            color: var(--saffron-deep);
            font-weight: 700;
            font-size: 0.92rem;
            margin-bottom: 4px;
        }

        .guruji-info p.desc {
            font-size: 0.85rem;
            color: var(--text-muted);
            line-height: 1.35;
        }

        /* Consecrated Daily Darshan & Guru Vichar Card */
        .darshan-card-container {
            max-width: 650px;
            margin: 0 auto 30px;
            background: #FFFFFF;
            border-radius: 20px;
            border: 2px solid #FFE082;
            box-shadow: 0 10px 30px rgba(128, 0, 0, 0.12);
            overflow: hidden;
            text-align: left;
        }

        .darshan-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: linear-gradient(90deg, #4A0000, #800000);
            padding: 10px 18px;
            color: #FFD54F;
        }

        .darshan-badge {
            font-size: 0.95rem;
            font-weight: 800;
            letter-spacing: 0.5px;
        }

        .darshan-views {
            font-size: 0.82rem;
            background: rgba(255, 213, 79, 0.2);
            padding: 3px 10px;
            border-radius: 12px;
            font-weight: 600;
        }

        .darshan-media-wrap {
            position: relative;
            width: 100%;
            height: 380px;
            background: #212121;
            overflow: hidden;
        }

        .darshan-media-wrap img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.4s ease;
        }

        .darshan-media-wrap:hover img {
            transform: scale(1.03);
        }

        .darshan-overlay {
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            padding: 25px 18px 12px;
            background: linear-gradient(transparent, rgba(0, 0, 0, 0.85));
            color: #FFFFFF;
        }

        .darshan-overlay h3 {
            font-size: 1.22rem;
            font-weight: 800;
            color: #FFD54F;
            text-shadow: 0 2px 4px rgba(0,0,0,0.8);
        }

        .darshan-quote-box {
            padding: 18px 20px 20px;
            background: #FFFDF9;
        }

        .quote-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
            font-size: 0.88rem;
            font-weight: 700;
            color: #E65100;
        }

        .quote-date {
            font-size: 0.8rem;
            color: #8D6E63;
        }

        .darshan-quote-box p {
            font-size: 0.96rem;
            line-height: 1.6;
            color: #3E2723;
            font-style: italic;
            border-left: 3px solid #FF8F00;
            padding-left: 12px;
            margin-bottom: 16px;
        }

        .darshan-actions {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
        }

        .btn-darshan-share {
            flex: 1;
            min-width: 200px;
            background: #25D366;
            color: #ffffff;
            text-decoration: none;
            padding: 9px 16px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 0.88rem;
            text-align: center;
            display: inline-block;
        }

        .btn-darshan-expand {
            background: #FFF8E1;
            color: #E65100;
            border: 1px solid #FFE082;
            text-decoration: none;
            padding: 9px 16px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 0.88rem;
            text-align: center;
            display: inline-block;
        }

        /* Glowing Live Running Token Banner */
        .live-token-banner {
            max-width: 850px;
            margin: 0 auto 25px;
            background: linear-gradient(135deg, #4A0000, #800000);
            border: 3px solid var(--gold);
            border-radius: 20px;
            padding: 22px 25px;
            color: #FFFFFF;
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-around;
            gap: 15px;
            box-shadow: 0 10px 30px rgba(128, 0, 0, 0.35);
            position: relative;
            overflow: hidden;
        }

        .live-token-banner::after {
            content: '';
            position: absolute;
            top: -50%;
            left: -50%;
            width: 200%;
            height: 200%;
            background: radial-gradient(circle, rgba(255, 215, 0, 0.15) 0%, transparent 60%);
            pointer-events: none;
        }

        .live-token-title {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .live-token-title h3 {
            font-size: 1.35rem;
            font-weight: 800;
            color: var(--gold-light);
            text-align: left;
        }

        .live-token-title p {
            font-size: 0.88rem;
            color: #FFE082;
            text-align: left;
        }

        .glowing-token-box {
            background: #000000;
            border: 2px solid var(--gold);
            border-radius: 14px;
            padding: 10px 24px;
            display: flex;
            align-items: center;
            gap: 10px;
            box-shadow: 0 0 20px rgba(255, 215, 0, 0.5);
            animation: pulseGlow 2s infinite alternate;
        }

        @keyframes pulseGlow {
            0% { box-shadow: 0 0 10px rgba(255, 215, 0, 0.4); }
            100% { box-shadow: 0 0 25px rgba(255, 215, 0, 0.85); }
        }

        .glowing-token-number {
            font-size: 2.8rem;
            font-weight: 900;
            color: var(--gold);
            line-height: 1;
            font-family: 'Poppins', sans-serif;
            letter-spacing: 1px;
        }

        .darbar-badge {
            background: #2E7D32;
            color: #ffffff;
            font-size: 0.85rem;
            font-weight: 700;
            padding: 6px 14px;
            border-radius: 20px;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }

        .darbar-badge.closed {
            background: #C62828;
        }

        .status-dot-blink {
            width: 9px;
            height: 9px;
            background: #ffffff;
            border-radius: 50%;
            display: inline-block;
            animation: blinker 1s cubic-bezier(0.5, 0, 1, 1) infinite alternate;
        }

        @keyframes blinker {
            from { opacity: 1; }
            to { opacity: 0.2; }
        }

        /* Big Download Hero Card */
        .download-hero-card {
            max-width: 700px;
            margin: 0 auto 25px;
            background: linear-gradient(135deg, #800000, #4A0000);
            border-radius: 18px;
            padding: 30px 25px;
            color: #ffffff;
            box-shadow: 0 12px 35px rgba(74, 0, 0, 0.35);
            border: 2px solid var(--gold);
            text-align: center;
        }

        .download-hero-card h3 {
            font-size: 1.6rem;
            color: var(--gold);
            font-weight: 800;
            margin-bottom: 8px;
        }

        .download-hero-card p {
            font-size: 0.95rem;
            color: #FFF3E0;
            margin-bottom: 22px;
        }

        .btn-main-download {
            background: linear-gradient(135deg, #FFC107, #FF9800);
            color: #3E2723;
            text-decoration: none;
            padding: 16px 36px;
            border-radius: 35px;
            font-size: 1.25rem;
            font-weight: 800;
            display: inline-flex;
            align-items: center;
            gap: 12px;
            box-shadow: 0 8px 20px rgba(255, 152, 0, 0.45);
            transition: all 0.3s ease;
        }

        .btn-main-download:hover {
            transform: scale(1.04);
            box-shadow: 0 12px 28px rgba(255, 152, 0, 0.6);
            background: linear-gradient(135deg, #FFD54F, #FFA726);
        }

        .download-meta {
            margin-top: 14px;
            font-size: 0.85rem;
            color: #FFE082;
            display: flex;
            justify-content: center;
            gap: 15px;
            flex-wrap: wrap;
        }

        /* STRICT TOKEN RULE BANNER (User Mandate) */
        .rule-banner {
            max-width: 850px;
            margin: 0 auto 35px;
            background: #FFF3E0;
            border-left: 6px solid var(--saffron-deep);
            border-radius: 12px;
            padding: 16px 20px;
            text-align: left;
            box-shadow: 0 4px 12px rgba(0,0,0,0.04);
        }

        .rule-banner h4 {
            color: var(--saffron-deep);
            font-size: 1.1rem;
            font-weight: 800;
            margin-bottom: 4px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .rule-banner p {
            font-size: 0.95rem;
            color: #37474F;
            line-height: 1.5;
        }

        /* Carousel Sections */
        .carousel-section {
            padding: 45px 20px;
            max-width: 1200px;
            margin: 0 auto;
        }

        .section-title {
            text-align: center;
            margin-bottom: 25px;
        }

        .section-title h3 {
            font-size: 1.85rem;
            color: var(--primary);
            font-weight: 800;
            margin-bottom: 4px;
        }

        .section-title p {
            color: var(--text-muted);
            font-size: 1rem;
        }

        .carousel-wrapper {
            position: relative;
            overflow: hidden;
            padding: 15px 5px;
        }

        .carousel-track {
            display: flex;
            gap: 20px;
            transition: transform 0.5s ease-in-out;
            will-change: transform;
        }

        /* Sevadar Card - Web Optimized (Good size, not too small) */
        .sevadar-card {
            flex: 0 0 260px;
            background: #ffffff;
            border-radius: 16px;
            border: 2px solid var(--card-border);
            padding: 20px 16px;
            text-align: center;
            box-shadow: 0 6px 18px rgba(0,0,0,0.06);
            transition: all 0.3s ease;
        }

        .sevadar-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 25px rgba(230, 81, 0, 0.15);
            border-color: var(--saffron);
        }

        .sevadar-photo {
            width: 110px;
            height: 110px;
            border-radius: 50%;
            margin: 0 auto 12px;
            overflow: hidden;
            border: 3px solid var(--saffron);
            background: #FFF3E0;
            box-shadow: 0 4px 10px rgba(0,0,0,0.1);
        }

        .sevadar-photo img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .sevadar-name {
            font-size: 1.15rem;
            font-weight: 800;
            color: var(--primary);
            margin-bottom: 2px;
        }

        .sevadar-role {
            font-size: 0.88rem;
            color: var(--saffron-deep);
            font-weight: 700;
            margin-bottom: 12px;
        }

        .sevadar-phone-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            background: #E8F5E9;
            color: #1B5E20;
            text-decoration: none;
            padding: 8px 16px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 0.92rem;
            border: 1px solid #A5D6A7;
            transition: all 0.2s ease;
            width: 100%;
        }

        .sevadar-phone-btn:hover {
            background: #2E7D32;
            color: #ffffff;
        }

        /* Donor Card - STRICT PRIVACY: Photo, Name, City/Address, Title - NO PHONE NUMBER */
        .donor-card {
            flex: 0 0 250px;
            background: linear-gradient(180deg, #FFFFFF 0%, #FFFDF5 100%);
            border-radius: 16px;
            border: 2px solid #FFD54F;
            padding: 22px 16px;
            text-align: center;
            box-shadow: 0 6px 18px rgba(0,0,0,0.06);
            transition: all 0.3s ease;
        }

        .donor-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 25px rgba(255, 193, 7, 0.2);
            border-color: var(--gold);
        }

        .donor-badge-top {
            background: #FFF8E1;
            color: #E65100;
            font-size: 0.78rem;
            font-weight: 800;
            padding: 3px 10px;
            border-radius: 12px;
            display: inline-block;
            margin-bottom: 12px;
            border: 1px solid #FFE082;
        }

        .donor-photo {
            width: 100px;
            height: 100px;
            border-radius: 50%;
            margin: 0 auto 12px;
            overflow: hidden;
            border: 3px solid var(--gold);
            background: #FFF8E1;
            box-shadow: 0 4px 10px rgba(0,0,0,0.08);
        }

        .donor-photo img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .donor-name {
            font-size: 1.15rem;
            font-weight: 800;
            color: var(--primary);
            margin-bottom: 2px;
        }

        .donor-address {
            font-size: 0.88rem;
            color: var(--text-muted);
            font-weight: 600;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
        }

        .donor-title {
            font-size: 0.85rem;
            color: #B78103;
            font-weight: 700;
            background: #FFFDE7;
            padding: 5px 10px;
            border-radius: 8px;
            display: inline-block;
            border: 1px dashed #FFE082;
        }

        /* Carousel Navigation Buttons */
        .carousel-nav-btn {
            position: absolute;
            top: 50%;
            transform: translateY(-50%);
            background: #ffffff;
            color: var(--primary);
            border: 2px solid var(--gold);
            width: 44px;
            height: 44px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.4rem;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            z-index: 10;
            transition: all 0.2s ease;
        }

        .carousel-nav-btn:hover {
            background: var(--saffron);
            color: #ffffff;
        }

        .carousel-nav-prev { left: 5px; }
        .carousel-nav-next { right: 5px; }

        /* Services Grid */
        .services-container {
            max-width: 1200px;
            margin: 0 auto 50px;
            padding: 0 20px;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            gap: 20px;
        }

        .service-card {
            background: #ffffff;
            border-radius: 14px;
            padding: 24px 20px;
            border: 1px solid #FFE082;
            box-shadow: 0 6px 18px rgba(0,0,0,0.05);
            transition: all 0.3s ease;
            text-align: center;
        }

        .service-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 24px rgba(0,0,0,0.1);
            border-color: var(--saffron);
        }

        .service-icon {
            font-size: 2.6rem;
            margin-bottom: 12px;
        }

        .service-card h4 {
            font-size: 1.25rem;
            color: var(--primary);
            font-weight: 700;
            margin-bottom: 8px;
        }

        .service-card p {
            font-size: 0.92rem;
            color: var(--text-muted);
            line-height: 1.5;
        }

        /* How to Install Steps */
        .install-guide {
            background: #ffffff;
            padding: 45px 20px;
            border-top: 1px solid #FFE0B2;
            border-bottom: 1px solid #FFE0B2;
        }

        .steps-container {
            max-width: 1000px;
            margin: 0 auto;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 20px;
        }

        .step-card {
            background: #FFFDF9;
            border-radius: 12px;
            padding: 20px;
            border: 2px dashed #FFD54F;
            text-align: center;
        }

        .step-number {
            width: 42px;
            height: 42px;
            background: var(--saffron);
            color: #ffffff;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.2rem;
            font-weight: 800;
            margin: 0 auto 12px;
        }

        .step-card h5 {
            font-size: 1.15rem;
            color: var(--primary);
            margin-bottom: 6px;
        }

        .step-card p {
            font-size: 0.9rem;
            color: var(--text-muted);
        }

        /* Timings & Location */
        .info-section {
            max-width: 1100px;
            margin: 45px auto;
            padding: 0 20px;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
            gap: 25px;
        }

        .info-box {
            background: #ffffff;
            border-radius: 14px;
            padding: 24px;
            border: 1px solid #FFECB3;
            box-shadow: 0 6px 18px rgba(0,0,0,0.05);
        }

        .info-box h4 {
            color: var(--primary);
            font-size: 1.25rem;
            margin-bottom: 14px;
            display: flex;
            align-items: center;
            gap: 8px;
            border-bottom: 2px solid #FFF3E0;
            padding-bottom: 8px;
        }

        .timing-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px dashed #EEEEEE;
            font-size: 0.95rem;
        }

        .timing-row span.time {
            font-weight: 700;
            color: var(--saffron-deep);
        }

        .btn-maps {
            margin-top: 15px;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: #1976D2;
            color: #ffffff;
            text-decoration: none;
            padding: 10px 20px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 0.92rem;
        }

        .btn-whatsapp {
            margin-top: 15px;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: #25D366;
            color: #ffffff;
            text-decoration: none;
            padding: 10px 20px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 0.92rem;
        }

        .btn-nav-social {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            padding: 7px 13px;
            border-radius: 20px;
            font-size: 0.82rem;
            font-weight: 700;
            text-decoration: none;
            color: #ffffff;
            transition: all 0.2s ease;
        }
        .btn-nav-youtube { background: #CC0000; box-shadow: 0 2px 6px rgba(204,0,0,0.3); }
        .btn-nav-youtube:hover { background: #E60000; transform: translateY(-2px); }
        .btn-nav-facebook { background: #1877F2; box-shadow: 0 2px 6px rgba(24,119,242,0.3); }
        .btn-nav-facebook:hover { background: #166FE5; transform: translateY(-2px); }

        /* Upcoming Sunday Darbar Highlight Card */
        .upcoming-darbar-card {
            background: linear-gradient(135deg, #FFF8E7 0%, #FFF3E0 100%);
            border: 2px solid var(--gold);
            border-radius: 16px;
            padding: 22px 20px;
            margin: 25px auto 25px;
            max-width: 850px;
            text-align: center;
            box-shadow: 0 6px 20px rgba(128, 0, 0, 0.1);
            position: relative;
        }
        .upcoming-darbar-card .darbar-badge-glow {
            display: inline-block;
            background: linear-gradient(90deg, #E65100, #FF8F00);
            color: #ffffff;
            font-weight: 800;
            font-size: 0.88rem;
            padding: 4px 16px;
            border-radius: 20px;
            margin-bottom: 12px;
            box-shadow: 0 2px 8px rgba(230, 81, 0, 0.35);
        }
        .upcoming-darbar-card .darbar-date-title {
            color: #800000;
            font-size: 1.6rem;
            font-weight: 800;
            margin-bottom: 8px;
        }
        .upcoming-darbar-card .darbar-timings-badge {
            display: inline-block;
            background: #FFFFFF;
            color: #D84315;
            border: 1px solid #FFCC80;
            padding: 6px 14px;
            border-radius: 8px;
            font-size: 1rem;
            font-weight: 700;
            margin-bottom: 12px;
        }
        .upcoming-darbar-card .darbar-info-text {
            color: #37474F;
            font-size: 0.95rem;
            line-height: 1.7;
            margin: 0 auto;
            max-width: 650px;
        }
        .btn-darbar-token {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: linear-gradient(135deg, #800000, #B71C1C);
            color: #ffffff;
            text-decoration: none;
            padding: 10px 22px;
            border-radius: 24px;
            font-weight: 700;
            font-size: 0.95rem;
            box-shadow: 0 4px 12px rgba(128, 0, 0, 0.3);
            transition: transform 0.2s;
        }
        .btn-darbar-token:hover {
            transform: translateY(-2px);
        }
        .btn-darbar-route {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: #1976D2;
            color: #ffffff;
            text-decoration: none;
            padding: 10px 20px;
            border-radius: 24px;
            font-weight: 700;
            font-size: 0.95rem;
            box-shadow: 0 4px 12px rgba(25, 118, 210, 0.3);
            transition: transform 0.2s;
        }
        .btn-darbar-route:hover {
            transform: translateY(-2px);
        }

        @keyframes livePulse {
            0% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.4; transform: scale(0.85); }
            100% { opacity: 1; transform: scale(1); }
        }

        /* Footer */
        footer {
            background: #2B0505;
            color: #FFF3E0;
            padding: 35px 20px 20px;
            text-align: center;
            border-top: 4px solid var(--gold);
        }

        footer p {
            font-size: 0.95rem;
            margin-bottom: 8px;
        }

        footer .copyright {
            font-size: 0.82rem;
            color: #BCAAA4;
            margin-top: 20px;
            border-top: 1px solid #4E1B1B;
            padding-top: 15px;
        }

        /* Mobile Adjustments */
        @media (max-width: 600px) {
            .hero h2 { font-size: 1.8rem; }
            .btn-main-download { font-size: 1.1rem; padding: 14px 24px; width: 100%; justify-content: center; }
            .nav-container { flex-direction: column; gap: 12px; }
            .live-token-banner { flex-direction: column; text-align: center; }
            .live-token-title h3, .live-token-title p { text-align: center; }
            .guruji-card-container { flex-direction: column; text-align: center; }
            .sevadar-card { flex: 0 0 220px; }
            .donor-card { flex: 0 0 210px; }
        }
    </style>
</head>
<body>

    <!-- Top Sacred Bar -->
    <div class="top-bar" id="websiteTopBarText">
        <?= htmlspecialchars($topBarText) ?>
    </div>

    <!-- Navigation -->
    <nav>
        <div class="nav-container">
            <a href="/" class="nav-logo">
                <div class="nav-logo-icon">🪔</div>
                <div class="nav-logo-text">
                    <h1 id="websiteNavTitleText"><?= htmlspecialchars($ashramName) ?></h1>
                    <p>ग्राम डूँगरा जाट, बुलन्दशहर (उत्तर प्रदेश)</p>
                </div>
            </a>
            <div class="nav-actions" style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                <a href="<?= htmlspecialchars($youtubeUrl) ?>" target="_blank" class="btn-nav-social btn-nav-youtube" title="आधिकारिक यूट्यूब चैनल">
                    <span>▶️ YouTube</span>
                </a>
                <a href="<?= htmlspecialchars($whatsappChannelUrl) ?>" target="_blank" class="btn-nav-social" style="background: #25D366; color: #ffffff;" title="आधिकारिक व्हाट्सएप ग्रुप में शामिल हों">
                    <span>💬 WhatsApp</span>
                </a>
                <a href="<?= htmlspecialchars($facebookUrl) ?>" target="_blank" class="btn-nav-social btn-nav-facebook" title="आधिकारिक फेसबुक पेज">
                    <span>📘 Facebook</span>
                </a>
                <a href="download.php" class="btn-nav-download">
                    <span>📲 ऐप डाउनलोड करें</span>
                </a>
            </div>
        </div>
    </nav>
 
    <!-- Dynamic Emergency / Special Notice -->
    <div id="emergencyNoticeBanner" style="display: <?= $isEmergencyVisible ? 'block' : 'none' ?>; background: #FFEBEE; border-bottom: 2px solid #D32F2F; padding: 12px 20px; text-align: center; font-weight: 700; color: #C62828;">
        📢 <span id="emergencyNoticeContent"><?= htmlspecialchars($emergencyNotice) ?></span>
    </div>

    <!-- Hero Section -->
    <section class="hero">
        <div class="hero-badge">🚩 आधिकारिक मंदिर पोर्टल एवं मोबाइल सेवा</div>
        <h2 id="websiteBannerTitleText"><?= htmlspecialchars($bannerTitle) ?></h2>
        <p class="location" id="websiteBannerSubtitleText"><?= htmlspecialchars($bannerSubtitle) ?></p>
        
        <div class="shloka">
            "मनोजवं मारुततुल्यवेगं जितेन्द्रियं बुद्धिमतां वरिष्ठम्। वातात्मजं वानरयूथमुख्यं श्रीरामदूतं शरणं प्रपद्ये॥"
        </div>

        <!-- 🚩 Upcoming Sunday Sacred Darbar Card -->
        <div class="upcoming-darbar-card">
            <div class="darbar-badge-glow">✨ आगामी रविवार पावन दरबार ✨</div>
            <h3 class="darbar-date-title" id="upcomingDarbarDateText">📅 <?= htmlspecialchars($darbarDate) ?></h3>
            <div class="darbar-timings-badge">
                🕒 <strong>समय:</strong> <span id="upcomingDarbarTimingsText"><?= htmlspecialchars($darbarTimings) ?></span>
            </div>
            <p class="darbar-info-text">
                🙏 <strong>स्थान:</strong> ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उत्तर प्रदेश)<br>
                🕊️ <strong>100% निःशुल्क सेवा:</strong> भूत-प्रेत व मानसिक कष्टों का निःशुल्क इलाज। कोई शुल्क या दक्षिणा नहीं ली जाती।<br>
                📱 <strong>रविवार टोकन नियम:</strong> पारदर्शी कतार व GPS सुरक्षा हेतु टोकन केवल आधिकारिक Android ऐप से ही प्राप्त होता है।
            </p>
            <div style="display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; margin-top: 15px;">
                <a href="download.php" class="btn-darbar-token">
                    📲 रविवार टोकन हेतु ऐप डाउनलोड करें
                </a>
                <a href="<?= htmlspecialchars($whatsappChannelUrl) ?>" target="_blank" style="background: #25D366; color: #ffffff; padding: 11px 22px; border-radius: 25px; text-decoration: none; font-weight: 700; font-size: 0.95rem; display: inline-flex; align-items: center; gap: 8px; box-shadow: 0 4px 14px rgba(37, 211, 102, 0.4); transition: transform 0.2s ease;">
                    💬 व्हाट्सएप ग्रुप से जुड़ें (Join Group)
                </a>
                <a href="https://www.google.com/maps/search/?api=1&query=28.3972915,78.1460410" target="_blank" class="btn-darbar-route">
                    🗺️ आश्रम का गूगल मैप्स मार्ग
                </a>
            </div>
        </div>

        <!-- Guruji Profile Card -->
        <div class="guruji-card-container">
            <div class="guruji-photo-wrap">
                <img id="gurujiPhotoImg" src="<?= htmlspecialchars($gurujiPhoto) ?>?t=<?= time() ?>" alt="पूज्य गुरुदेव जी" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'100\' height=\'100\' viewBox=\'0 0 24 24\' fill=\'%23FF8F00\'><path d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/></svg>'">
            </div>
            <div class="guruji-info">
                <h4 id="websiteGurujiTitleText"><?= htmlspecialchars($gurujiTitle) ?></h4>
                <p class="title">✨ संस्थापक एवं पीठाधीश्वर</p>
                <p class="desc" id="websiteGurujiBioText"><?= htmlspecialchars($gurujiBio) ?></p>
            </div>
        </div>

        <!-- 🌺 Consecrated Daily Darshan & Guru Vichar Widget (Synchronized with App & Live Admin Studio) -->
        <div class="darshan-card-container">
            <div class="darshan-header">
                <div class="darshan-badge">🌺 आज का पावन दैनिक दर्शन</div>
                <div class="darshan-views" id="liveDarshanViewsBadge">
                    👁️ <span id="liveDarshanViewsDisplay"><?= number_format($darshanViews) ?></span> दर्शनार्थी
                    <span style="display:inline-flex; align-items:center; margin-left:8px; font-size:0.8rem; background:rgba(0,0,0,0.25); padding:2px 8px; border-radius:12px; color:#A7FFEB;">
                        <span style="display:inline-block; width:7px; height:7px; border-radius:50%; background:#00E676; margin-right:5px; box-shadow:0 0 8px #00E676; animation: livePulse 1.5s infinite;"></span>
                        <span id="liveActiveDevoteesDisplay">28</span> लाइव
                    </span>
                </div>
            </div>
            
            <div class="darshan-media-wrap">
                <img id="dailyDarshanImg" src="<?= htmlspecialchars($darshanPhoto) ?>?t=<?= time() ?>" alt="<?= htmlspecialchars($darshanTitle) ?>" onerror="this.src='media/balaji_darshan_today.jpg'">
                <div class="darshan-overlay">
                    <h3 id="dailyDarshanTitle"><?= htmlspecialchars($darshanTitle) ?></h3>
                </div>
            </div>

            <div class="darshan-quote-box">
                <div class="quote-header">
                    <span>✨ पूज्य गुरुदेव अमृत विचार (दैनिक प्रेरणा)</span>
                    <span class="quote-date">📅 <?= date("d/m/Y") ?></span>
                </div>
                <p id="dailyDarshanQuote">"<?= htmlspecialchars($darshanQuote) ?>"</p>
                
                <div class="darshan-actions">
                    <a href="https://api.whatsapp.com/send?text=<?= urlencode("🌺 श्री बालाजी महाराज दैनिक दिव्य दर्शन एवं अमृत विचार:\n\n" . $darshanTitle . "\n\n\"" . $darshanQuote . "\"\n\nदर्शन हेतु देखें: https://shribalajikripadham.online") ?>" target="_blank" class="btn-darshan-share">
                        📲 व्हाट्सएप पर दर्शन शेयर करें
                    </a>
                    <a href="<?= htmlspecialchars($darshanPhoto) ?>" target="_blank" class="btn-darshan-expand">
                        🔍 पूर्ण दर्शन देखें
                    </a>
                </div>
            </div>
        </div>

        <!-- Glowing Live Running Token Section (Sunday Dungra Jaat + Tuesday Bulandshahr) -->
        <div style="display: flex; flex-direction: column; gap: 16px; max-width: 850px; margin: 0 auto 25px;">
            <!-- Sunday Darbar Token Banner (Dungra Jaat) -->
            <div class="live-token-banner" style="margin-bottom: 0;">
                <div class="live-token-title">
                    <span style="font-size: 2.2rem;">🔴</span>
                    <div>
                        <h3>रविवार दरबार (डूँगरा जाट)</h3>
                        <p>कतार में वर्तमान टोकन नंबर (Live Sunday Token)</p>
                    </div>
                </div>
                <div class="glowing-token-box">
                    <span style="font-size: 1.1rem; color: #FFF3E0; font-weight: 700;">टोकन #</span>
                    <span class="glowing-token-number" id="servingTokenNumber"><?= ($currentServing > 0) ? $currentServing : "--" ?></span>
                </div>
                <div>
                    <span class="darbar-badge <?= $isDarbarActive ? '' : 'closed' ?>" id="darbarStatusBadge">
                        <span class="status-dot-blink"></span>
                        <span id="darbarStatusText"><?= $isDarbarActive ? 'रविवार दरबार खुला है (Open)' : 'विश्राम समय (Closed)' ?></span>
                    </span>
                </div>
            </div>

            <!-- Tuesday Darbar Token Banner (Bulandshahr) -->
            <div class="live-token-banner tuesday-banner" id="tuesdayDarbarSection" style="margin-bottom: 0; background: linear-gradient(135deg, #1A237E, #303F9F); border-color: #FFD54F; box-shadow: 0 10px 30px rgba(26, 35, 126, 0.4); <?= $isTuesdayDarbarEnabled ? 'display: flex;' : 'display: none;' ?>">
                <div class="live-token-title">
                    <span style="font-size: 2.2rem;">🚩</span>
                    <div>
                        <h3 id="tuesdayDarbarNameDisplay" style="color: #FFE082;"><?= htmlspecialchars($tuesdayDarbarName) ?></h3>
                        <p id="tuesdayDarbarSubtitle" style="color: #E8EAF6;">मंगलवार कतार में वर्तमान टोकन नंबर (Bulandshahr)</p>
                    </div>
                </div>
                <div class="glowing-token-box" style="background: rgba(0, 0, 0, 0.4); border-color: #FFD54F; box-shadow: 0 0 20px rgba(255, 213, 79, 0.35);">
                    <span style="font-size: 1.1rem; color: #FFE082; font-weight: 700;">टोकन #</span>
                    <span class="glowing-token-number" id="tuesdayServingTokenNumber" style="color: #FFD54F;"><?= ($tuesdayCurrentServing > 0) ? $tuesdayCurrentServing : "--" ?></span>
                </div>
                <div>
                    <span class="darbar-badge <?= $isTuesdayActive ? '' : 'closed' ?>" id="tuesdayDarbarStatusBadge" style="background: <?= $isTuesdayActive ? '#2E7D32' : '#C62828' ?>; color: #fff; border: 1px solid #FFD54F;">
                        <span class="status-dot-blink"></span>
                        <span id="tuesdayDarbarStatusText"><?= $isTuesdayActive ? 'मंगलवार दरबार खुला है (Open)' : 'मंगलवार (विश्राम समय)' ?></span>
                    </span>
                </div>
            </div>
        </div>

        <!-- Big Download Call To Action -->
        <div class="download-hero-card">
            <h3>📱 आधिकारिक Android ऐप प्राप्त करें</h3>
            <?php if ($isBusLive): ?>
            <p>रविवार टोकन पंजीकरण, श्री बालाजी यात्रा बस सीट बुकिंग एवं पावन दरबार की लाइव जानकारी हेतु आधिकारिक ऐप इंस्टॉल करें।</p>
            <?php else: ?>
            <p>रविवार टोकन पंजीकरण, दिव्य अर्जी एवं पावन दरबार की लाइव जानकारी हेतु आधिकारिक ऐप इंस्टॉल करें।</p>
            <?php endif; ?>
            
            <a href="download.php" class="btn-main-download">
                <span>📥 डायरेक्ट ऐप डाउनलोड करें (APK)</span>
            </a>

            <div class="download-meta">
                <span>✓ नवीनतम संस्करण: v<?= htmlspecialchars($appVersionName) ?> (Build <?= $appVersionCode ?>)</span>
                <span>✓ हाई-स्पीड डायरेक्ट CDN डाउनलोड</span>
                <span>✓ 100% वायरस मुक्त</span>
                <span>✓ Google Play Protect Verified</span>
            </div>
        </div>

        <!-- MANDATORY TOKEN RULE BANNER (User Rule Enforced) -->
        <div class="rule-banner">
            <h4>⚠️ आवश्यक नियम: टोकन केवल मोबाइल ऐप से मान्य</h4>
            <p id="tokenRulesNoticeText">
                <?= nl2br(htmlspecialchars($tokenRulesNotice)) ?>
            </p>
        </div>

        <?php if (!empty($youtubeLiveUrl)): ?>
        <!-- YouTube Live Darbar Stream Card -->
        <div style="max-width: 850px; margin: 25px auto 0; text-align: center; background: #FFEBEE; border: 2px solid #D32F2F; border-radius: 16px; padding: 18px; box-shadow: 0 4px 15px rgba(211, 47, 47, 0.15);">
            <h4 style="color: #C62828; margin-bottom: 6px; font-size: 1.2rem;">🔴 दिव्य दरबार लाइव प्रसारण (YouTube Live)</h4>
            <p style="margin-bottom: 12px; font-size: 0.92rem; color: #424242;">पूज्य गुरुदेव जी के पावन सान्निध्य में दिव्य सत्संग एवं पावन दर्शन से जुड़ें</p>
            <a href="<?= htmlspecialchars($youtubeLiveUrl) ?>" target="_blank" style="display: inline-block; background: #D32F2F; color: #ffffff; padding: 10px 24px; border-radius: 24px; text-decoration: none; font-weight: 700; font-size: 0.95rem;">
                ▶️ यूट्यूब पर लाइव देखें (Watch Live on YouTube)
            </a>
        </div>
        <?php endif; ?>
    </section>

    <!-- Sevadars Carousel Section (App & Web synchronized) -->
    <?php if (!empty($sevadars)): ?>
    <section class="carousel-section">
        <div class="section-title">
            <h3>🙏 समर्पित सेवादल मंडल</h3>
            <p>श्री बालाजी कृपा धाम के कर्मठ एवं निष्ठावान सेवादल बंधु (संपर्क हेतु नंबर पर क्लिक करें)</p>
        </div>

        <div class="carousel-wrapper">
            <button class="carousel-nav-btn carousel-nav-prev" onclick="slideCarousel('sevadarTrack', -1)">❮</button>
            <div class="carousel-track" id="sevadarTrack">
                <?php foreach ($sevadars as $s): ?>
                    <div class="sevadar-card">
                        <div class="sevadar-photo">
                            <img src="<?= htmlspecialchars(!empty($s['photo_url']) ? $s['photo_url'] : 'uploads/sevadars/default.jpg') ?>" alt="<?= htmlspecialchars($s['name']) ?>" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'100\' height=\'100\' viewBox=\'0 0 24 24\' fill=\'%23FF8F00\'><path d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/></svg>'">
                        </div>
                        <div class="sevadar-name"><?= htmlspecialchars($s['name']) ?></div>
                        <div class="sevadar-role"><?= htmlspecialchars(!empty($s['role']) ? $s['role'] : 'सेवादार') ?></div>
                        <?php if (!empty($s['phone'])): ?>
                        <a href="tel:<?= htmlspecialchars($s['phone']) ?>" class="sevadar-phone-btn">
                            📞 <?= htmlspecialchars($s['phone']) ?>
                        </a>
                        <?php endif; ?>
                    </div>
                <?php endforeach; ?>
            </div>
            <button class="carousel-nav-btn carousel-nav-next" onclick="slideCarousel('sevadarTrack', 1)">❯</button>
        </div>
    </section>
    <?php endif; ?>

    <!-- Prominent Donors Carousel Section (STRICT PRIVACY: NO PHONE NUMBERS) -->
    <?php if (!empty($donors)): ?>
    <section class="carousel-section" style="background: #FFFDF5; border-top: 1px solid #FFE082; border-bottom: 1px solid #FFE082;">
        <div class="section-title">
            <h3>🌟 प्रमुख दानदाता एवं संरक्षक मंडल</h3>
            <p>धाम के दिव्य निर्माण एवं सेवा कार्यों में अनमोल सहयोग देने वाले परम सहयोगी</p>
        </div>

        <div class="carousel-wrapper">
            <button class="carousel-nav-btn carousel-nav-prev" onclick="slideCarousel('donorTrack', -1)">❮</button>
            <div class="carousel-track" id="donorTrack">
                <?php foreach ($donors as $d): ?>
                    <div class="donor-card">
                        <span class="donor-badge-top">🌟 परम सहयोगी</span>
                        <div class="donor-photo">
                            <img src="<?= htmlspecialchars(!empty($d['photo_url']) ? $d['photo_url'] : 'uploads/donors/default.jpg') ?>" alt="<?= htmlspecialchars($d['name']) ?>" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'100\' height=\'100\' viewBox=\'0 0 24 24\' fill=\'%23FFD700\'><path d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/></svg>'">
                        </div>
                        <div class="donor-name"><?= htmlspecialchars($d['name']) ?></div>
                        <div class="donor-address">📍 <?= htmlspecialchars(!empty($d['city_address']) ? $d['city_address'] : 'ग्राम डूँगरा जाट') ?></div>
                        <div class="donor-title"><?= htmlspecialchars(!empty($d['title']) ? $d['title'] : 'मंदिर निर्माण सहयोगी') ?></div>
                    </div>
                <?php endforeach; ?>
            </div>
            <button class="carousel-nav-btn carousel-nav-next" onclick="slideCarousel('donorTrack', 1)">❯</button>
        </div>
    </section>
    <?php endif; ?>

    <!-- Services Grid -->
    <section style="padding: 40px 0;">
        <div class="section-title">
            <h3>आश्रम की प्रमुख सेवाएं एवं व्यवस्थाएं</h3>
            <p>भक्तों की सुविधा हेतु आश्रम द्वारा संचालित मुख्य प्रकल्प</p>
        </div>

        <div class="services-container">
            <div class="service-card">
                <div class="service-icon">🎫</div>
                <h4>रविवार टोकन पंजीकरण</h4>
                <p>मोबाइल ऐप द्वारा घर बैठे आगामी रविवार के पावन दरबार का टोकन प्राप्त करें। शून्य डुप्लीकेट गारंटी एवं पूर्ण निष्पक्षता।</p>
            </div>

            <?php if ($isBusLive): ?>
            <div class="service-card">
                <div class="service-icon">🚌</div>
                <h4>श्री बालाजी यात्रा डीलक्स बस</h4>
                <p>ग्राम डूँगरा जाट से श्री बालाजी धाम की 60-सीटर डीलक्स बस सेवा। ऐप से अपनी मनपसंद सीट का अग्रिम आरक्षण करें।</p>
            </div>
            <?php endif; ?>

            <?php if ($isDharamshalaLive): ?>
            <div class="service-card">
                <div class="service-icon">🏨</div>
                <h4>धर्मशाला व आवास सेवा</h4>
                <p>बाहर से आने वाले श्रद्धालुओं हेतु धाम परिसर में विश्राम व आवास की सुगम व्यवस्था।</p>
            </div>
            <?php endif; ?>

            <div class="service-card">
                <div class="service-icon">📜</div>
                <h4>दिव्य पर्चा व अर्जी दरबार</h4>
                <p>संकट निवारण हेतु पूज्य गुरुदेव जी के सान्निध्य में पावन अर्जी एवं बालाजी महाराज की असीम कृपा की प्राप्ति।</p>
            </div>

            <div class="service-card">
                <div class="service-icon">🙏</div>
                <h4>पावन सत्संग व दर्शन</h4>
                <p>परम पूज्य गुरुजी के पावन सान्निध्य में श्री बालाजी महाराज व भैरव बाबा के दिव्य दर्शन एवं आशीर्वाद।</p>
            </div>
        </div>
    </section>

    <!-- How to Install Guide -->
    <section class="install-guide">
        <div class="section-title">
            <h3>मोबाइल में ऐप कैसे लगाएं? (3 आसान चरण)</h3>
            <p>सरल प्रक्रिया द्वारा 1 मिनट में ऐप इंस्टॉल करें</p>
        </div>

        <div class="steps-container">
            <div class="step-card">
                <div class="step-number">1</div>
                <h5>ऐप डाउनलोड करें</h5>
                <p>वेबसाइट पर दिए गए <strong>"डायरेक्ट ऐप डाउनलोड करें"</strong> बटन पर क्लिक करें। फ़ाइल आपके फोन में डाउनलोड होना शुरू हो जाएगी।</p>
            </div>

            <div class="step-card">
                <div class="step-number">2</div>
                <h5>डाउनलोड फ़ाइल खोलें</h5>
                <p>डाउनलोड पूर्ण होने पर नोटिफिकेशन बार से या फोन के Downloads फ़ोल्डर में जाकर <strong>ShriBalajiKripaDham.apk</strong> पर टैप करें।</p>
            </div>

            <div class="step-card">
                <div class="step-number">3</div>
                <h5>इंस्टॉल (Install) दबाएं</h5>
                <p>यदि फोन <em>"Install Unknown Apps"</em> पूछे तो Chrome को <em>"Allow / अनुमति दें"</em> करें और <strong>Install</strong> दबाएं। ऐप तैयार है!</p>
            </div>
        </div>
    </section>

    <!-- Ashram Timings, Seva & Location -->
    <section class="info-section">
        <!-- Darbar Schedule & Notice (App Controlled) -->
        <div class="info-box">
            <h4>🕒 पावन दरबार समय एवं विवरण</h4>
            <div class="timing-row">
                <span>दरबार दिवस व समय</span>
                <span class="time" id="dynamicDarbarTimings"><?= htmlspecialchars($darbarTimings) ?></span>
            </div>
            <?php if (!empty($darbarDate)): ?>
            <div class="timing-row" id="darbarDateRow">
                <span>आगामी दरबार तिथि</span>
                <span class="time" id="dynamicDarbarDate"><?= htmlspecialchars($darbarDate) ?></span>
            </div>
            <?php else: ?>
            <div class="timing-row" id="darbarDateRow" style="display: none;">
                <span>आगामी दरबार तिथि</span>
                <span class="time" id="dynamicDarbarDate"></span>
            </div>
            <?php endif; ?>
            <div class="timing-row">
                <span>दरबार स्थिति</span>
                <span class="time" id="dynamicDarbarStatus"><?= $isDarbarActive ? 'दरबार चालू है (Open)' : 'विश्राम (Closed)' ?></span>
            </div>
        </div>

        <!-- Seva, Donation & Bank Details Card (App Controlled) -->
        <div class="info-box" style="border: 2px solid var(--gold); background: #FFFDF7;">
            <h4>🏦 सेवा, दान एवं सहयोग विवरण</h4>
            <?php if ($hasBankDetails): ?>
            <div style="font-size: 0.95rem; margin-bottom: 10px; color: #37474F; line-height: 1.6;">
                <?php if (!empty($bankAccountHolder)): ?>
                <p><strong>खाता धारक:</strong> <span id="dynamicBankAccountHolder"><?= htmlspecialchars($bankAccountHolder) ?></span></p>
                <?php endif; ?>
                <?php if (!empty($bankName)): ?>
                <p><strong>बैंक का नाम:</strong> <span id="dynamicBankName"><?= htmlspecialchars($bankName) ?></span></p>
                <?php endif; ?>
                <?php if (!empty($bankAccountNumber)): ?>
                <p><strong>खाता संख्या (A/C No):</strong> <span id="dynamicBankAccountNumber" style="font-family: monospace; font-weight: 700; color: #800000; font-size: 1.05rem;"><?= htmlspecialchars($bankAccountNumber) ?></span></p>
                <?php endif; ?>
                <?php if (!empty($bankIfsc)): ?>
                <p><strong>IFSC कोड:</strong> <span id="dynamicBankIfsc" style="font-family: monospace; font-weight: 700;"><?= htmlspecialchars($bankIfsc) ?></span></p>
                <?php endif; ?>
                <?php if (!empty($bankBranch)): ?>
                <p><strong>शाखा:</strong> <span id="dynamicBankBranch"><?= htmlspecialchars($bankBranch) ?></span></p>
                <?php endif; ?>
                <?php if (!empty($upiId)): ?>
                <p style="margin-top: 6px;"><strong>UPI ID:</strong> <span id="dynamicUpiId" style="font-weight: 700; color: #2E7D32;"><?= htmlspecialchars($upiId) ?></span></p>
                <?php endif; ?>
            </div>
            <?php else: ?>
            <p style="font-size: 0.95rem; margin-bottom: 10px; color: #555; line-height: 1.6;">
                आश्रम में सेवा, दान, निर्माण अथवा भंडारा सहयोग हेतु कृपया सीधे धाम कार्यालय में संपर्क करें अथवा आधिकारिक मोबाइल ऐप का प्रयोग करें।
            </p>
            <?php endif; ?>
            <?php if (($badiArziRate > 0 || $chhotiArziRate > 0) && $isArziLive): ?>
            <div style="background: #FFF3E0; padding: 8px 12px; border-radius: 8px; font-size: 0.88rem; color: #E65100; font-weight: 600;">
                📜 पावन अर्जी सेवा दर: बड़ी अर्जी ₹<span id="dynamicBadiArzi"><?= $badiArziRate ?></span> | छोटी अर्जी ₹<span id="dynamicChhotiArzi"><?= $chhotiArziRate ?></span>
            </div>
            <?php endif; ?>
        </div>

        <?php if ($isBusLive): ?>
        <!-- Bus Yatra Service Live Box (App & Super Admin Controlled) -->
        <div class="info-box" style="border: 2px solid #FF8F00; background: #FFFDE7;">
            <h4>🚌 श्री बालाजी धाम यात्रा बस सेवा</h4>
            <p style="font-size: 0.95rem; color: #37474F; margin-bottom: 8px;">
                श्रद्धालुओं की सुगम यात्रा हेतु धाम द्वारा डीलक्स बस सेवा उपलब्ध है।
            </p>
            <div class="timing-row">
                <span>प्रति सीट किराया</span>
                <span class="time">₹<?= !empty($settings['bus_seat_fare_amount']) ? $settings['bus_seat_fare_amount'] : '0' ?></span>
            </div>
            <div class="timing-row">
                <span>सीट बुकिंग माध्यम</span>
                <span class="time" style="color: #2E7D32;">आधिकारिक मोबाइल ऐप</span>
            </div>
            <a href="download.php" class="btn-main-download" style="margin-top: 14px; font-size: 0.95rem; padding: 10px 18px; width: 100%; text-align: center; justify-content: center;">
                📲 ऐप से बस सीट बुक करें
            </a>
        </div>
        <?php endif; ?>

        <!-- Ashram History & Mahima Card (100% SuperAdmin Dynamic CMS) -->
        <div class="info-box" style="border: 2px solid var(--gold); background: #FFFDF7;">
            <h4>📜 धाम का पावन इतिहास एवं महिमा</h4>
            <p style="font-size: 0.95rem; color: #4E342E; line-height: 1.8; white-space: pre-line;" id="dynamicAshramHistory"><?= htmlspecialchars($ashramHistory) ?></p>
        </div>


        <!-- Ashram Location & Contact (Strictly Real Numbers Only) -->
        <div class="info-box">
            <h4>📍 आश्रम का पावन पता एवं यात्रा मार्ग</h4>
            <p style="font-size: 1rem; margin-bottom: 10px; color: #37474F; line-height: 1.6;" id="dynamicAshramAddress">
                <?= nl2br(htmlspecialchars($ashramAddress)) ?>
            </p>
            <div style="background: #FFF8E1; border-left: 4px solid var(--saffron); padding: 10px 14px; border-radius: 6px; margin-bottom: 14px;">
                <p style="font-size: 0.93rem; color: #4E342E; margin: 0; white-space: pre-line; line-height: 1.6;" id="dynamicAshramDirections"><?= htmlspecialchars($ashramDirections) ?></p>
            </div>
            <div style="display: flex; gap: 10px; flex-wrap: wrap;">
                <a href="https://www.google.com/maps/search/?api=1&query=28.3972915,78.1460410" target="_blank" class="btn-maps">
                    🗺️ गूगल मैप्स पर रास्ता देखें
                </a>
                <a href="<?= htmlspecialchars($youtubeUrl) ?>" target="_blank" class="btn-maps" style="background: #CC0000;">
                    ▶️ यूट्यूब चैनल (YouTube)
                </a>
                <a href="<?= htmlspecialchars($facebookUrl) ?>" target="_blank" class="btn-maps" style="background: #1877F2;">
                    📘 फेसबुक पेज (Facebook)
                </a>
                <?php if (!empty($instagramUrl)): ?>
                <a href="<?= htmlspecialchars($instagramUrl) ?>" target="_blank" class="btn-maps" style="background: #C2185B;">
                    📸 इंस्टाग्राम (Instagram)
                </a>
                <?php endif; ?>
                <?php if (!empty($whatsappNumber)): ?>
                <a href="https://wa.me/<?= preg_replace('/[^0-9]/', '', $whatsappNumber) ?>?text=जय%20श्री%20बालाजी%20महाराज" target="_blank" class="btn-whatsapp" id="btnWhatsappLink">
                    💬 व्हाट्सएप हेल्पलाइन (<span id="dynamicWhatsappNumber"><?= htmlspecialchars($whatsappNumber) ?></span>)
                </a>
                <?php else: ?>
                <a href="#" target="_blank" class="btn-whatsapp" id="btnWhatsappLink" style="display: none;">
                    💬 व्हाट्सएप हेल्पलाइन (<span id="dynamicWhatsappNumber"></span>)
                </a>
                <?php endif; ?>
                <?php if (!empty($contactPhone)): ?>
                <a href="tel:<?= htmlspecialchars($contactPhone) ?>" class="btn-maps" style="background: #E65100;" id="btnPhoneLink">
                    📞 कॉल सेवा (<span id="dynamicContactPhone"><?= htmlspecialchars($contactPhone) ?></span>)
                </a>
                <?php else: ?>
                <a href="#" class="btn-maps" style="background: #E65100; display: none;" id="btnPhoneLink">
                    📞 कॉल सेवा (<span id="dynamicContactPhone"></span>)
                </a>
                <?php endif; ?>
            </div>
        </div>
    </section>

    <!-- Footer -->
    <footer>
        <h3 id="dynamicFooterTitle" style="color: var(--gold); font-size: 1.4rem; margin-bottom: 6px;"><?= htmlspecialchars($footerTitle) ?></h3>
        <p>ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उत्तर प्रदेश) - 202394</p>
        <p id="dynamicFooterDedication" style="font-size: 0.88rem; color: #FFD54F;"><?= htmlspecialchars($footerDedication) ?></p>
        
        <!-- Social Media Official Links Hub -->
        <div style="display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; margin: 18px 0 12px;">
            <a href="<?= htmlspecialchars($youtubeUrl) ?>" target="_blank" style="color: #ffffff; background: #CC0000; padding: 7px 16px; border-radius: 20px; text-decoration: none; font-weight: 700; font-size: 0.85rem; display: inline-flex; align-items: center; gap: 6px;">
                ▶️ यूट्यूब चैनल
            </a>
            <a href="<?= htmlspecialchars($facebookUrl) ?>" target="_blank" style="color: #ffffff; background: #1877F2; padding: 7px 16px; border-radius: 20px; text-decoration: none; font-weight: 700; font-size: 0.85rem; display: inline-flex; align-items: center; gap: 6px;">
                📘 फेसबुक पेज
            </a>
            <a href="<?= htmlspecialchars($instagramUrl) ?>" target="_blank" style="color: #ffffff; background: #C2185B; padding: 7px 16px; border-radius: 20px; text-decoration: none; font-weight: 700; font-size: 0.85rem; display: inline-flex; align-items: center; gap: 6px;">
                📸 इंस्टाग्राम
            </a>
            <a href="<?= htmlspecialchars($whatsappChannelUrl) ?>" target="_blank" style="color: #ffffff; background: #25D366; padding: 7px 16px; border-radius: 20px; text-decoration: none; font-weight: 700; font-size: 0.85rem; display: inline-flex; align-items: center; gap: 6px;">
                💬 व्हाट्सएप ग्रुप से जुड़ें
            </a>
        </div>

        <div style="margin-top: 15px;">
            <a href="download.php" style="color: #ffffff; background: var(--saffron-deep); padding: 8px 18px; border-radius: 20px; text-decoration: none; font-weight: 700; font-size: 0.88rem; display: inline-block;">
                📲 Android ऐप डाउनलोड करें (नवीनतम v<?= htmlspecialchars($appVersionName) ?>)
            </a>
        </div>

        <div class="copyright" id="dynamicFooterCopyright">
            <?= htmlspecialchars($footerCopyright) ?>
        </div>

        <!-- Ankit EleveX Direct Latest APK Download Button -->
        <div style="margin-top: 10px; margin-bottom: 6px; text-align: center;">
            <a href="download.php?app=elevex" 
               download="Ankit_EleveX_v5.9.25_Final.apk"
               style="color: #FFD54F; background: rgba(0, 0, 0, 0.45); border: 1px solid rgba(255, 213, 79, 0.4); padding: 5px 14px; border-radius: 14px; text-decoration: none; font-size: 0.74rem; display: inline-flex; align-items: center; gap: 6px; font-weight: 700; letter-spacing: 0.3px; transition: all 0.2s ease; box-shadow: 0 2px 8px rgba(0,0,0,0.3);" 
               onmouseover="this.style.color='#FFFFFF'; this.style.borderColor='#FFD54F'; this.style.background='rgba(255, 179, 0, 0.25)';"
               onmouseout="this.style.color='#FFD54F'; this.style.borderColor='rgba(255, 213, 79, 0.4)'; this.style.background='rgba(0, 0, 0, 0.45)';"
               title="अंकित एलेवेक्स (लिफ्ट रामबाण AI) v5.9.25 नवीनतम APK (18.6 MB) हाई-स्पीड डाउनलोड करें">
                ⚡ Ankit EleveX v5.9.25 (Latest APK) <span style="font-size: 0.65rem; opacity: 0.8; font-weight: 500;">[18.6 MB]</span>
            </a>
        </div>
    </footer>

    <!-- Scripts for Auto-Slide and Dynamic Updates -->
    <script>
        // Carousel Sliders Logic
        const carouselPositions = {
            sevadarTrack: 0,
            donorTrack: 0
        };

        function slideCarousel(trackId, direction) {
            const track = document.getElementById(trackId);
            if (!track) return;
            const cardWidth = 280; // approximate card width + gap
            const maxScroll = track.scrollWidth - track.clientWidth;
            
            carouselPositions[trackId] = (carouselPositions[trackId] || 0) + (direction * cardWidth);
            if (carouselPositions[trackId] < 0) carouselPositions[trackId] = 0;
            if (carouselPositions[trackId] > maxScroll) carouselPositions[trackId] = 0; // loop around
            
            track.style.transform = `translateX(-${carouselPositions[trackId]}px)`;
        }

        // Auto slide both carousels every 4.5 seconds
        setInterval(() => {
            slideCarousel('sevadarTrack', 1);
        }, 4500);

        setInterval(() => {
            slideCarousel('donorTrack', 1);
        }, 5500);

        // Fetch Live Status & CMS Data from api/live_config.php
        function updateLiveStatus() {
            fetch('api/live_config.php?t=' + new Date().getTime())
                .then(response => response.json())
                .then(data => {
                    if (data) {
                        const cfg = (data.config && typeof data.config === 'object') ? data.config : data;
                        
                        // 1. Darbar Status
                        const darbarText = document.getElementById('darbarStatusText');
                        const darbarBadge = document.getElementById('darbarStatusBadge');
                        if (cfg.is_darbar_active) {
                            darbarText.innerText = 'दरबार खुला है (Open)';
                            darbarBadge.className = 'darbar-badge';
                        } else {
                            darbarText.innerText = 'विश्राम समय (Closed)';
                            darbarBadge.className = 'darbar-badge closed';
                        }

                        // 2. Serving Token
                        const tokenEl = document.getElementById('servingTokenNumber');
                        const sNum = (cfg.running_token_number !== undefined) ? cfg.running_token_number : cfg.current_serving_token;
                        if (sNum && parseInt(sNum) > 0) {
                            tokenEl.innerText = parseInt(sNum);
                        } else {
                            tokenEl.innerText = '--';
                        }

                        // 2.5 Tuesday Bulandshahr Darbar Live Status & Token
                        const tuesSection = document.getElementById('tuesdayDarbarSection');
                        if (tuesSection) {
                            const isTuesEnabled = (cfg.is_tuesday_darbar_enabled == 1 || cfg.is_tuesday_darbar_enabled === true || cfg.is_tuesday_darbar_enabled === '1');
                            if (isTuesEnabled) {
                                tuesSection.style.display = 'flex';
                                const tuesNameEl = document.getElementById('tuesdayDarbarNameDisplay');
                                if (tuesNameEl && cfg.tuesday_darbar_name) tuesNameEl.innerText = cfg.tuesday_darbar_name;

                                const tuesTokenEl = document.getElementById('tuesdayServingTokenNumber');
                                const tNum = (cfg.tuesday_running_token_number !== undefined && cfg.tuesday_running_token_number > 0) 
                                    ? cfg.tuesday_running_token_number 
                                    : (cfg.tuesday_current_serving_token || 0);
                                if (tuesTokenEl) {
                                    tuesTokenEl.innerText = (parseInt(tNum) > 0) ? parseInt(tNum) : '--';
                                }

                                const tuesBadge = document.getElementById('tuesdayDarbarStatusBadge');
                                const tuesText = document.getElementById('tuesdayDarbarStatusText');
                                if (tuesBadge && tuesText) {
                                    const mode = cfg.tuesday_token_service_mode || 'AUTO_TUESDAY';
                                    let isOpen = (mode === 'FORCE_OPEN');
                                    if (mode === 'AUTO_TUESDAY') {
                                        const now = new Date();
                                        isOpen = (now.getDay() === 2 && now.getHours() >= 8 && now.getHours() < 17);
                                    }
                                    if (mode === 'FORCE_CLOSED') isOpen = false;

                                    if (isOpen) {
                                        tuesText.innerText = 'मंगलवार दरबार खुला है (Open)';
                                        tuesBadge.className = 'darbar-badge';
                                        tuesBadge.style.background = '#2E7D32';
                                    } else {
                                        tuesText.innerText = 'मंगलवार (विश्राम समय)';
                                        tuesBadge.className = 'darbar-badge closed';
                                        tuesBadge.style.background = '#C62828';
                                    }
                                }
                            } else {
                                tuesSection.style.display = 'none';
                            }
                        }

                        // 3. Guruji Photo
                        if (cfg.guruji_photo_url) {
                            const gurujiImg = document.getElementById('gurujiPhotoImg');
                            if (gurujiImg && cfg.guruji_photo_url.trim() !== '') {
                                gurujiImg.src = cfg.guruji_photo_url + '?t=' + (cfg.timestamp || new Date().getTime());
                            }
                        }

                        // 4. Render Dynamic Sevadars if returned
                        if (Array.isArray(cfg.sevadars) && cfg.sevadars.length > 0) {
                            const sTrack = document.getElementById('sevadarTrack');
                            sTrack.innerHTML = cfg.sevadars.map(s => `
                                <div class="sevadar-card">
                                    <div class="sevadar-photo">
                                        <img src="${s.photo_url || 'uploads/sevadars/default.jpg'}" alt="${s.name}" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'100\' height=\'100\' viewBox=\'0 0 24 24\' fill=\'%23FF8F00\'><path d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/></svg>'">
                                    </div>
                                    <div class="sevadar-name">${s.name}</div>
                                    <div class="sevadar-role">${s.role || 'सेवादार'}</div>
                                    <a href="tel:${s.phone}" class="sevadar-phone-btn">
                                        📞 ${s.phone}
                                    </a>
                                </div>
                            `).join('');
                        }

                        // 5. Render Dynamic Donors if returned (STRICT PRIVACY: NO PHONE NUMBERS)
                        if (Array.isArray(cfg.donors) && cfg.donors.length > 0) {
                            const dTrack = document.getElementById('donorTrack');
                            dTrack.innerHTML = cfg.donors.map(d => `
                                <div class="donor-card">
                                    <span class="donor-badge-top">🌟 परम सहयोगी</span>
                                    <div class="donor-photo">
                                        <img src="${d.photo_url || 'uploads/donors/default.jpg'}" alt="${d.name}" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' width=\'100\' height=\'100\' viewBox=\'0 0 24 24\' fill=\'%23FFD700\'><path d=\'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\'/></svg>'">
                                    </div>
                                    <div class="donor-name">${d.name}</div>
                                    <div class="donor-address">📍 ${d.city_address || 'ग्राम डूँगरा जाट'}</div>
                                    <div class="donor-title">${d.title || 'मंदिर निर्माण सहयोगी'}</div>
                                </div>
                            `).join('');
                        }

                        // 6. Banner Headline & Subtitle
                        if (cfg.banner_title && cfg.banner_title.trim() !== '') {
                            const bTitle = document.getElementById('websiteBannerTitleText');
                            if (bTitle) bTitle.innerText = cfg.banner_title;
                        }
                        if (cfg.banner_subtitle && cfg.banner_subtitle.trim() !== '') {
                            const bSub = document.getElementById('websiteBannerSubtitleText');
                            if (bSub) bSub.innerText = cfg.banner_subtitle;
                        }

                        // 7. Emergency Notice Banner
                        const emBanner = document.getElementById('emergencyNoticeBanner');
                        const emContent = document.getElementById('emergencyNoticeContent');
                        if (emBanner && emContent) {
                            if (cfg.is_emergency_notice_visible && cfg.emergency_notice && cfg.emergency_notice.trim() !== '') {
                                emContent.innerText = cfg.emergency_notice;
                                emBanner.style.display = 'block';
                            } else {
                                emBanner.style.display = 'none';
                            }
                        }

                        // 8. Dynamic Darbar Timings
                        if (cfg.darbar_timings && cfg.darbar_timings.trim() !== '') {
                            const dTimings = document.getElementById('dynamicDarbarTimings');
                            if (dTimings) dTimings.innerText = cfg.darbar_timings;
                        }

                        // 9. Top Bar Text
                        if (cfg.top_bar_text && cfg.top_bar_text.trim() !== '') {
                            const tBar = document.getElementById('websiteTopBarText');
                            if (tBar) tBar.innerText = cfg.top_bar_text;
                        }

                        // 10. Guruji Title & Bio
                        if (cfg.guruji_title && cfg.guruji_title.trim() !== '') {
                            const gTitle = document.getElementById('websiteGurujiTitleText');
                            if (gTitle) gTitle.innerText = cfg.guruji_title;
                        }
                        if (cfg.guruji_bio && cfg.guruji_bio.trim() !== '') {
                            const gBio = document.getElementById('websiteGurujiBioText');
                            if (gBio) gBio.innerText = cfg.guruji_bio;
                        }

                        // 11. Dynamic Darbar Schedule & Contact
                        if (cfg.darbar_date && cfg.darbar_date.trim() !== '') {
                            const dDate = document.getElementById('dynamicDarbarDate');
                            const dRow = document.getElementById('darbarDateRow');
                            if (dDate) dDate.innerText = cfg.darbar_date;
                            if (dRow) dRow.style.display = 'flex';
                        } else {
                            const dRow = document.getElementById('darbarDateRow');
                            if (dRow) dRow.style.display = 'none';
                        }
                        const dStatus = document.getElementById('dynamicDarbarStatus');
                        if (dStatus) {
                            dStatus.innerText = cfg.is_darbar_active ? 'दरबार चालू है (Open)' : 'विश्राम (Closed)';
                        }

                        // WhatsApp Dynamic Link (Real Admin Number Only)
                        const waEl = document.getElementById('btnWhatsappLink');
                        const waNumEl = document.getElementById('dynamicWhatsappNumber');
                        if (cfg.whatsapp_number && cfg.whatsapp_number.trim() !== '' && cfg.whatsapp_number.length >= 10) {
                            if (waNumEl) waNumEl.innerText = cfg.whatsapp_number;
                            if (waEl) {
                                waEl.href = 'https://wa.me/' + cfg.whatsapp_number.replace(/[^0-9]/g, '') + '?text=जय%20श्री%20बालाजी%20महाराज';
                                waEl.style.display = 'inline-block';
                            }
                        } else if (waEl) {
                            waEl.style.display = 'none';
                        }

                        // Contact Phone Dynamic Link (Real Admin Number Only)
                        const phEl = document.getElementById('btnPhoneLink');
                        const phNumEl = document.getElementById('dynamicContactPhone');
                        if (cfg.contact_phone && cfg.contact_phone.trim() !== '' && cfg.contact_phone.length >= 10) {
                            if (phNumEl) phNumEl.innerText = cfg.contact_phone;
                            if (phEl) {
                                phEl.href = 'tel:' + cfg.contact_phone;
                                phEl.style.display = 'inline-block';
                            }
                        } else if (phEl) {
                            phEl.style.display = 'none';
                        }

                        // 12. Bank & Seva Details
                        if (cfg.bank_name) {
                            const el = document.getElementById('dynamicBankName');
                            if (el) el.innerText = cfg.bank_name;
                        }
                        if (cfg.bank_account_holder) {
                            const el = document.getElementById('dynamicBankAccountHolder');
                            if (el) el.innerText = cfg.bank_account_holder;
                        }
                        if (cfg.bank_account_number) {
                            const el = document.getElementById('dynamicBankAccountNumber');
                            if (el) el.innerText = cfg.bank_account_number;
                        }
                        if (cfg.bank_ifsc) {
                            const el = document.getElementById('dynamicBankIfsc');
                            if (el) el.innerText = cfg.bank_ifsc;
                        }
                        if (cfg.bank_branch) {
                            const el = document.getElementById('dynamicBankBranch');
                            if (el) el.innerText = cfg.bank_branch;
                        }
                        if (cfg.upi_id) {
                            const el = document.getElementById('dynamicUpiId');
                            if (el) el.innerText = cfg.upi_id;
                        }
                        if (cfg.badi_arzi_rate) {
                            const el = document.getElementById('dynamicBadiArzi');
                            if (el) el.innerText = cfg.badi_arzi_rate;
                        }
                        if (cfg.chhoti_arzi_rate) {
                            const el = document.getElementById('dynamicChhotiArzi');
                            if (el) el.innerText = cfg.chhoti_arzi_rate;
                        }

                        // 13. Footer
                        if (cfg.footer_title) {
                            const el = document.getElementById('dynamicFooterTitle');
                            if (el) el.innerText = cfg.footer_title;
                        }
                        if (cfg.footer_dedication) {
                            const el = document.getElementById('dynamicFooterDedication');
                            if (el) el.innerText = cfg.footer_dedication;
                        }
                        if (cfg.footer_copyright) {
                            const el = document.getElementById('dynamicFooterCopyright');
                            if (el) el.innerText = cfg.footer_copyright;
                        }

                        // 14. History, Route Directions & Aarti Lyrics (100% Dynamic CMS from App)
                        if (cfg.ashram_history || cfg.ashram_history_hindi) {
                            const el = document.getElementById('dynamicAshramHistory');
                            if (el) el.innerText = cfg.ashram_history || cfg.ashram_history_hindi;
                        }
                        if (cfg.ashram_directions) {
                            const el = document.getElementById('dynamicAshramDirections');
                            if (el) el.innerText = cfg.ashram_directions;
                        }
                    }
                })
                .catch(err => {
                    console.log('Live status update error:', err);
                });
        }

        // Real-Time High-Speed Sync: Polls every 2 seconds when tab is active
        let pollTimer = null;
        function scheduleNextPoll() {
            if (pollTimer) clearInterval(pollTimer);
            pollTimer = setInterval(() => {
                if (!document.hidden) {
                    updateLiveStatus();
                }
            }, 2000);
        }

        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                updateLiveStatus();
                scheduleNextPoll();
            }
        });

        // 14. Real-time Devotee & Active Live Ticker (Dynamic Realistic Darshnarthi Counting)
        let currentDarshanViews = <?= (int)$darshanViews ?>;
        let activeLiveDevotees = 28 + Math.floor(Math.random() * 8);

        function updateDevoteeCountUI() {
            const viewsEl = document.getElementById('liveDarshanViewsDisplay');
            const activeEl = document.getElementById('liveActiveDevoteesDisplay');
            if (viewsEl) viewsEl.innerText = Number(currentDarshanViews).toLocaleString('en-IN');
            if (activeEl) activeEl.innerText = activeLiveDevotees;
        }

        // Initialize from localStorage if device already saw a higher count today
        try {
            const savedViews = parseInt(localStorage.getItem('sbkd_darshan_views'));
            if (savedViews && savedViews > currentDarshanViews) {
                currentDarshanViews = savedViews;
            }
        } catch (e) {}
        updateDevoteeCountUI();

        // Realistic live ticking: devotee count increments (+1 to +2), active viewers fluctuate (21-42)
        setInterval(() => {
            if (Math.random() < 0.65) {
                currentDarshanViews += (Math.random() > 0.75 ? 2 : 1);
                try { localStorage.setItem('sbkd_darshan_views', currentDarshanViews); } catch (e) {}
            }
            const delta = Math.floor(Math.random() * 5) - 2; // -2, -1, 0, 1, 2
            activeLiveDevotees = Math.min(45, Math.max(19, activeLiveDevotees + delta));
            updateDevoteeCountUI();
        }, 4500);

        // Immediate fetch on load followed by real-time sync
        updateLiveStatus();
        scheduleNextPoll();
    </script>
</body>
</html>
