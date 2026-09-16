<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>श्री बालाजी कृपा धाम | आधिकारिक वेबसाइट एवं मोबाइल ऐप (ग्राम डूँगरा जाट, बुलन्दशहर)</title>
    <meta name="description" content="श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर (उत्तर प्रदेश) की आधिकारिक वेबसाइट। रविवार टोकन, श्री बालाजी यात्रा बस एवं लाइव दर्शन हेतु आधिकारिक Android ऐप डाउनलोड करें।">
    <meta name="keywords" content="श्री बालाजी कृपा धाम, डूँगरा जाट, बुलन्दशहर, बालाजी टोकन, बालाजी यात्रा, हनुमान मंदिर, Dungra Jaat, Bulandshahr">
    
    <!-- Open Graph for Social Sharing & WhatsApp -->
    <meta property="og:title" content="श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट - आधिकारिक ऐप डाउनलोड">
    <meta property="og:description" content="आश्रम की आधिकारिक वेबसाइट से Android ऐप डाउनलोड करें। रविवार टोकन, बस बुकिंग एवं लाइव दर्शन की सुविधा।">
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
            padding: 45px 20px 30px;
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
            margin-bottom: 8px;
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

        /* Live Status Strip */
        .live-strip {
            max-width: 850px;
            margin: 0 auto 30px;
            background: #ffffff;
            border-radius: 14px;
            padding: 16px 20px;
            box-shadow: 0 8px 25px rgba(0,0,0,0.06);
            border: 2px solid var(--card-border);
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            text-align: center;
        }

        .live-item {
            padding: 8px;
        }

        .live-item .label {
            font-size: 0.85rem;
            color: var(--text-muted);
            font-weight: 600;
            margin-bottom: 4px;
        }

        .live-item .value {
            font-size: 1.2rem;
            font-weight: 800;
            color: var(--primary);
        }

        .status-pill {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #E8F5E9;
            color: var(--green);
            padding: 4px 12px;
            border-radius: 15px;
            font-size: 0.9rem;
            font-weight: 700;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            background: var(--green);
            border-radius: 50%;
            display: inline-block;
            animation: pulse 1.5s infinite;
        }

        @keyframes pulse {
            0% { opacity: 0.4; }
            50% { opacity: 1; }
            100% { opacity: 0.4; }
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
            margin: 0 auto 40px;
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

        /* Services Grid */
        .section-title {
            text-align: center;
            margin-bottom: 30px;
        }

        .section-title h3 {
            font-size: 1.8rem;
            color: var(--primary);
            font-weight: 800;
        }

        .section-title p {
            color: var(--text-muted);
            font-size: 1rem;
        }

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
            .live-strip { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>

    <!-- Top Sacred Bar -->
    <div class="top-bar">
        🚩 ॐ श्री हनुमते नमः • संकट कटै मिटै सब पीरा, जो सुमरै हनुमत बलबीरा 🚩
    </div>

    <!-- Navigation -->
    <nav>
        <div class="nav-container">
            <a href="/" class="nav-logo">
                <div class="nav-logo-icon">🪔</div>
                <div class="nav-logo-text">
                    <h1>श्री बालाजी कृपा धाम</h1>
                    <p>ग्राम डूँगरा जाट, बुलन्दशहर (उत्तर प्रदेश)</p>
                </div>
            </a>
            <div class="nav-actions">
                <a href="download.php" class="btn-nav-download">
                    <span>📲 ऐप डाउनलोड करें</span>
                </a>
            </div>
        </div>
    </nav>

    <!-- Hero Section -->
    <section class="hero">
        <div class="hero-badge">🚩 आधिकारिक मंदिर पोर्टल एवं मोबाइल सेवा</div>
        <h2>श्री बालाजी कृपा धाम</h2>
        <p class="location">📍 ग्राम डूँगरा जाट, तहसील स्याना, जिला बुलन्दशहर (उ.प्र.)</p>
        
        <div class="shloka">
            "मनोजवं मारुततुल्यवेगं जितेन्द्रियं बुद्धिमतां वरिष्ठम्। वातात्मजं वानरयूथमुख्यं श्रीरामदूतं शरणं प्रपद्ये॥"
        </div>

        <!-- Live Status Strip (Connected to api/live_config.php) -->
        <div class="live-strip">
            <div class="live-item">
                <div class="label">दरबार स्थिति</div>
                <div class="value">
                    <span class="status-pill" id="darbarStatusPill">
                        <span class="status-dot"></span>
                        <span id="darbarStatusText">लाइव सक्रिय (Open)</span>
                    </span>
                </div>
            </div>
            <div class="live-item">
                <div class="label">वर्तमान टोकन नंबर</div>
                <div class="value" id="servingTokenNumber">#--</div>
            </div>
            <div class="live-item">
                <div class="label">टोकन सेवा स्थिति</div>
                <div class="value" id="tokenServiceText" style="color: var(--saffron-deep); font-size: 1.05rem;">
                    सक्रिय (Active)
                </div>
            </div>
        </div>

        <!-- Big Download Call To Action -->
        <div class="download-hero-card">
            <h3>📱 आधिकारिक Android ऐप प्राप्त करें</h3>
            <p>रविवार टोकन पंजीकरण, 60-सीटर डीलक्स बस सीट बुकिंग एवं पावन दरबार की लाइव जानकारी हेतु आधिकारिक ऐप इंस्टॉल करें।</p>
            
            <a href="download.php" class="btn-main-download">
                <span>📥 डायरेक्ट ऐप डाउनलोड करें (APK)</span>
            </a>

            <div class="download-meta">
                <span>✓ संस्करण: v2.38.0 Pro</span>
                <span>✓ साइज़: ~107 MB</span>
                <span>✓ 100% वायरस मुक्त</span>
                <span>✓ Google Play Protect Verified</span>
            </div>
        </div>

        <!-- MANDATORY TOKEN RULE BANNER (User Rule Enforced) -->
        <div class="rule-banner">
            <h4>⚠️ आवश्यक नियम: टोकन केवल मोबाइल ऐप से मान्य</h4>
            <p>
                आश्रम की निष्पक्षता, पारदर्शी कतार, GPS लोकेशन एवं AI बायोमेट्रिक सुरक्षा नियमों के अनुसार <strong>टोकन पंजीकरण केवल और केवल आधिकारिक मोबाइल ऐप से ही संभव है</strong>। वेबसाइट पर कोई टोकन जनरेशन फॉर्म नहीं है। टोकन प्राप्त करने के लिए कृपया ऊपर दिए गए बटन से मोबाइल ऐप इंस्टॉल करें।
            </p>
        </div>
    </section>

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

            <div class="service-card">
                <div class="service-icon">🚌</div>
                <h4>श्री बालाजी यात्रा डीलक्स बस</h4>
                <p>ग्राम डूँगरा जाट से श्री बालाजी धाम की 60-सीटर डीलक्स बस सेवा। ऐप से अपनी मनपसंद सीट का अग्रिम आरक्षण करें।</p>
            </div>

            <div class="service-card">
                <div class="service-icon">📜</div>
                <h4>दिव्य पर्चा व अर्जी दरबार</h4>
                <p>संकट निवारण हेतु पूज्य गुरुदेव जी के सान्निध्य में पावन अर्जी एवं बालाजी महाराज की असीम कृपा की प्राप्ति।</p>
            </div>

            <div class="service-card">
                <div class="service-icon">🪔</div>
                <h4>नित्य महाआरती व दर्शन</h4>
                <p>प्रतिदिन प्रातः मंगला आरती, बालभोग व सांध्य महाआरती में सम्मिलित होकर पुण्य लाभ अर्जित करें।</p>
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

    <!-- Ashram Timings & Location -->
    <section class="info-section">
        <!-- Darbar Timings -->
        <div class="info-box">
            <h4>🕒 नित्य आरती एवं दरबार समय</h4>
            <div class="timing-row">
                <span>मंगला महाआरती</span>
                <span class="time">प्रातः 05:30 बजे</span>
            </div>
            <div class="timing-row">
                <span>बालभोग एवं प्रातः दर्शन</span>
                <span class="time">प्रातः 08:00 बजे</span>
            </div>
            <div class="timing-row">
                <span>रविवार विशेष दरबार</span>
                <span class="time">प्रातः 09:00 बजे से प्रभु इच्छा तक</span>
            </div>
            <div class="timing-row">
                <span>सांध्य महाआरती</span>
                <span class="time">सायं 07:00 बजे</span>
            </div>
            <div class="timing-row">
                <span>शयन आरती</span>
                <span class="time">रात्रि 09:00 बजे</span>
            </div>
        </div>

        <!-- Ashram Location -->
        <div class="info-box">
            <h4>📍 आश्रम का पावन पता एवं संपर्क</h4>
            <p style="font-size: 1rem; margin-bottom: 8px; color: #37474F;">
                <strong>श्री बालाजी कृपा धाम</strong><br>
                ग्राम डूँगरा जाट, पोस्ट स्याना,<br>
                जिला बुलन्दशहर, उत्तर प्रदेश - 203412
            </p>
            <p style="font-size: 0.9rem; color: var(--text-muted); margin-bottom: 12px;">
                निकटतम रेलवे स्टेशन: हापुड़ / बुलन्दशहर • निकटतम बस स्टैंड: स्याना
            </p>
            <div style="display: flex; gap: 10px; flex-wrap: wrap;">
                <a href="https://www.google.com/maps/search/?api=1&query=28.3972915,78.1460410" target="_blank" class="btn-maps">
                    🗺️ गूगल मैप्स पर रास्ता देखें
                </a>
                <a href="https://wa.me/919999999999?text=जय%20श्री%20बालाजी%20महाराज" target="_blank" class="btn-whatsapp">
                    💬 व्हाट्सएप हेल्पलाइन
                </a>
            </div>
        </div>
    </section>

    <!-- Footer -->
    <footer>
        <h3 style="color: var(--gold); font-size: 1.4rem; margin-bottom: 6px;">श्री बालाजी कृपा धाम</h3>
        <p>ग्राम डूँगरा जाट, बुलन्दशहर (उत्तर प्रदेश)</p>
        <p style="font-size: 0.88rem; color: #FFD54F;">सर्वस्व श्री रामभक्त वीर हनुमान जी महाराज के पावन चरणों में समर्पित।</p>
        
        <div style="margin-top: 15px;">
            <a href="download.php" style="color: #ffffff; background: var(--saffron-deep); padding: 8px 18px; border-radius: 20px; text-decoration: none; font-weight: 700; font-size: 0.88rem;">
                📲 Android ऐप डाउनलोड करें (v2.38.0)
            </a>
        </div>

        <div class="copyright">
            © 2026 श्री बालाजी कृपा धाम सेवा ट्रस्ट। सर्वाधिकार सुरक्षित।
        </div>
    </footer>

    <!-- Auto-Updating Live Status Script -->
    <script>
        function updateLiveStatus() {
            fetch('api/live_config.php')
                .then(response => response.json())
                .then(data => {
                    if (data && data.status === 'SUCCESS' && data.config) {
                        const cfg = data.config;
                        
                        // Darbar Status
                        const darbarText = document.getElementById('darbarStatusText');
                        const darbarPill = document.getElementById('darbarStatusPill');
                        if (cfg.is_darbar_active) {
                            darbarText.innerText = 'लाइव सक्रिय (Open)';
                            darbarPill.style.background = '#E8F5E9';
                            darbarPill.style.color = '#2E7D32';
                        } else {
                            darbarText.innerText = 'विश्राम समय (Closed)';
                            darbarPill.style.background = '#FFEBEE';
                            darbarPill.style.color = '#C62828';
                        }

                        // Serving Token
                        const tokenEl = document.getElementById('servingTokenNumber');
                        if (cfg.running_token_number > 0) {
                            tokenEl.innerText = '#' + cfg.running_token_number;
                        } else {
                            tokenEl.innerText = '#--';
                        }

                        // Token Service Status
                        const srvEl = document.getElementById('tokenServiceText');
                        if (cfg.is_token_service_enabled) {
                            srvEl.innerText = 'सक्रिय (खुला है)';
                            srvEl.style.color = '#2E7D32';
                        } else {
                            srvEl.innerText = 'बंद (Closed)';
                            srvEl.style.color = '#C62828';
                        }
                    }
                })
                .catch(err => {
                    console.log('Live status fetch info:', err);
                });
        }

        // Fetch immediately and update every 12 seconds
        updateLiveStatus();
        setInterval(updateLiveStatus, 12000);
    </script>
</body>
</html>
