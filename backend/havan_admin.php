<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - पावन हवन आवेदन एडमिन पोर्टल
// Havan Devotee Applications Admin Dashboard
// ==============================================================================
?>
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>हवन एवं अनुष्ठान आवेदन प्रबंधन | श्री बालाजी कृपा धाम</title>
    <link rel="icon" href="media/balaji_darshan_today.jpg" type="image/jpeg">
    <link href="https://fonts.googleapis.com/css2?family=Yantramanav:wght@400;500;700;900&display=swap" rel="stylesheet">
    <style>
        :root {
            --maroon: #800000;
            --maroon-deep: #4A0000;
            --saffron: #FF8F00;
            --saffron-bright: #FF6F00;
            --gold: #FFD54F;
            --bg-light: #FBF8F5;
            --card-bg: #FFFFFF;
            --text-dark: #212121;
            --text-muted: #616161;
            --border-color: #E0D6CC;
            --success: #2E7D32;
            --warning: #F57F17;
            --info: #0277BD;
            --danger: #C62828;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Yantramanav', sans-serif;
        }

        body {
            background-color: var(--bg-light);
            color: var(--text-dark);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* Header */
        header {
            background: linear-gradient(135deg, var(--maroon-deep), var(--maroon));
            color: white;
            padding: 16px 20px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.15);
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
        }

        .header-title {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .header-title h1 {
            font-size: 1.35rem;
            color: var(--gold);
            font-weight: 700;
        }

        .header-title p {
            font-size: 0.85rem;
            color: #FFE082;
        }

        .header-actions {
            display: flex;
            gap: 10px;
            align-items: center;
        }

        .btn-header {
            background: rgba(255,255,255,0.15);
            border: 1px solid rgba(255,255,255,0.3);
            color: white;
            padding: 7px 14px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 700;
            cursor: pointer;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s;
        }

        .btn-header:hover {
            background: rgba(255,255,255,0.25);
        }

        /* Container */
        .container {
            max-width: 1200px;
            width: 100%;
            margin: 20px auto;
            padding: 0 16px;
            flex: 1;
        }

        /* Stats Cards */
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 14px;
            margin-bottom: 20px;
        }

        .stat-card {
            background: var(--card-bg);
            border-radius: 12px;
            padding: 16px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.06);
            border-top: 4px solid var(--maroon);
            display: flex;
            flex-direction: column;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
        }

        .stat-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 14px rgba(0,0,0,0.12);
        }

        .stat-card.active-filter {
            border-color: var(--saffron-bright);
            background: #FFF9E6;
        }

        .stat-num {
            font-size: 1.8rem;
            font-weight: 900;
            color: var(--maroon);
            margin-bottom: 4px;
        }

        .stat-label {
            font-size: 0.88rem;
            color: var(--text-muted);
            font-weight: 500;
        }

        /* Search & Filter Toolbar */
        .toolbar {
            background: var(--card-bg);
            padding: 14px 18px;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.06);
            margin-bottom: 20px;
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-between;
        }

        .search-box {
            flex: 1;
            min-width: 250px;
            position: relative;
        }

        .search-box input {
            width: 100%;
            padding: 10px 14px 10px 36px;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            font-size: 0.95rem;
            outline: none;
        }

        .search-box input:focus {
            border-color: var(--maroon);
            box-shadow: 0 0 0 3px rgba(128,0,0,0.1);
        }

        .search-box::before {
            content: "🔍";
            position: absolute;
            left: 12px;
            top: 50%;
            transform: translateY(-50%);
            font-size: 0.9rem;
        }

        /* Application Cards Grid */
        .app-list {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .app-card {
            background: var(--card-bg);
            border-radius: 14px;
            padding: 18px 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.07);
            border: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            gap: 14px;
            position: relative;
            transition: all 0.2s;
        }

        .app-card:hover {
            box-shadow: 0 5px 18px rgba(0,0,0,0.12);
        }

        .app-card-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            flex-wrap: wrap;
            gap: 10px;
            border-bottom: 1px solid #F0EAE1;
            padding-bottom: 12px;
        }

        .app-title-group h3 {
            font-size: 1.25rem;
            color: var(--maroon);
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .app-no-badge {
            background: #FFF3E0;
            color: #E65100;
            border: 1px solid #FFE0B2;
            padding: 3px 10px;
            border-radius: 6px;
            font-size: 0.82rem;
            font-weight: 700;
            display: inline-block;
            margin-top: 4px;
        }

        .status-badge {
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 700;
            text-transform: uppercase;
        }

        .status-PENDING { background: #FFF9C4; color: #F57F17; border: 1px solid #FFF59D; }
        .status-CONTACTED { background: #E1F5FE; color: #0277BD; border: 1px solid #B3E5FC; }
        .status-APPROVED { background: #E8F5E9; color: #2E7D32; border: 1px solid #C8E6C9; }
        .status-COMPLETED { background: #EDE7F6; color: #512DA8; border: 1px solid #D1C4E9; }
        .status-CANCELLED { background: #FFEBEE; color: #C62828; border: 1px solid #FFCDD2; }

        .app-details-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
            gap: 14px;
            font-size: 0.92rem;
        }

        .detail-item {
            display: flex;
            flex-direction: column;
            gap: 3px;
        }

        .detail-label {
            font-size: 0.78rem;
            color: var(--text-muted);
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .detail-val {
            color: #212121;
            font-weight: 500;
            line-height: 1.4;
        }

        /* Policy Confirmation Badges */
        .policy-ack-box {
            background: #F1F8E9;
            border-left: 4px solid #43A047;
            padding: 8px 14px;
            border-radius: 6px;
            font-size: 0.86rem;
            color: #2E7D32;
            display: flex;
            align-items: center;
            gap: 8px;
            font-weight: 600;
        }

        /* Actions Bar */
        .app-actions-bar {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-between;
            padding-top: 10px;
            border-top: 1px solid #F0EAE1;
        }

        .quick-contact-group {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
        }

        .btn-call {
            background: #2E7D32;
            color: white;
            padding: 7px 14px;
            border-radius: 8px;
            text-decoration: none;
            font-weight: 700;
            font-size: 0.85rem;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: opacity 0.2s;
        }

        .btn-wa {
            background: #25D366;
            color: white;
            padding: 7px 14px;
            border-radius: 8px;
            text-decoration: none;
            font-weight: 700;
            font-size: 0.85rem;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: opacity 0.2s;
        }

        .btn-call:hover, .btn-wa:hover {
            opacity: 0.9;
        }

        .status-update-form {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .status-select {
            padding: 7px 10px;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            font-size: 0.85rem;
            font-weight: 600;
            background: white;
            cursor: pointer;
        }

        .btn-save-status {
            background: var(--maroon);
            color: white;
            border: none;
            padding: 7px 14px;
            border-radius: 8px;
            font-weight: 700;
            font-size: 0.85rem;
            cursor: pointer;
        }

        .btn-delete {
            background: transparent;
            color: #C62828;
            border: 1px solid #FFCDD2;
            padding: 6px 12px;
            border-radius: 8px;
            font-size: 0.82rem;
            cursor: pointer;
        }

        .btn-delete:hover {
            background: #FFEBEE;
        }

        /* Remarks Box */
        .notes-box {
            width: 100%;
            margin-top: 6px;
        }

        .notes-input {
            width: 100%;
            padding: 8px 12px;
            border: 1px solid var(--border-color);
            border-radius: 6px;
            font-size: 0.85rem;
            background: #FAFAFA;
        }

        /* PIN Login Modal */
        #pinModal {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.7);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 9999;
            padding: 16px;
        }

        .pin-card {
            background: white;
            width: 100%;
            max-width: 380px;
            border-radius: 16px;
            padding: 28px 24px;
            text-align: center;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
        }

        .pin-card h2 {
            color: var(--maroon);
            margin-bottom: 8px;
            font-size: 1.4rem;
        }

        .pin-card p {
            font-size: 0.9rem;
            color: var(--text-muted);
            margin-bottom: 20px;
        }

        .pin-input {
            width: 100%;
            padding: 12px;
            font-size: 1.3rem;
            text-align: center;
            letter-spacing: 8px;
            border: 2px solid var(--border-color);
            border-radius: 8px;
            outline: none;
            margin-bottom: 16px;
        }

        .pin-input:focus {
            border-color: var(--maroon);
        }

        .btn-pin-submit {
            width: 100%;
            background: var(--maroon);
            color: white;
            padding: 12px;
            border: none;
            border-radius: 8px;
            font-size: 1rem;
            font-weight: 700;
            cursor: pointer;
        }

        .empty-state {
            text-align: center;
            padding: 50px 20px;
            background: white;
            border-radius: 12px;
            color: var(--text-muted);
        }

        .empty-state h3 {
            color: var(--maroon);
            margin-bottom: 8px;
        }

        @media (max-width: 600px) {
            .app-actions-bar {
                flex-direction: column;
                align-items: stretch;
            }
            .quick-contact-group {
                justify-content: stretch;
            }
            .btn-call, .btn-wa {
                flex: 1;
                justify-content: center;
            }
            .status-update-form {
                width: 100%;
            }
            .status-select {
                flex: 1;
            }
        }
    </style>
</head>
<body>

    <!-- Header -->
    <header>
        <div class="header-title">
            <span style="font-size: 1.8rem;">🔥</span>
            <div>
                <h1>हवन एवं अनुष्ठान सेवा प्रबंधन</h1>
                <p>श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) • एडमिनिस्ट्रेटर पोर्टल</p>
            </div>
        </div>
        <div class="header-actions">
            <button id="btnSevadarAccess" class="btn-header" onclick="openSevadarModal()" style="display: none; background: #FF8F00; color: #212121; border-color: #FFA000; font-weight: bold;">👥 सेवादार एक्सेस</button>
            <button class="btn-header" onclick="loadApplications()">🔄 रिफ्रेश (Refresh)</button>
            <a href="index.php" class="btn-header">🌐 मुख्य वेबसाइट</a>
            <button class="btn-header" onclick="logoutAdmin()" style="background: rgba(255,0,0,0.3);">🚪 लॉगआउट</button>
        </div>
    </header>

    <!-- Main Container -->
    <div class="container">
        <!-- Stats Grid -->
        <div class="stats-grid">
            <div class="stat-card" id="cardAll" onclick="setFilter('ALL')">
                <div class="stat-num" id="countTotal">0</div>
                <div class="stat-label">📋 कुल आवेदन (Total)</div>
            </div>
            <div class="stat-card" id="cardPending" onclick="setFilter('PENDING')">
                <div class="stat-num" id="countPending" style="color: #F57F17;">0</div>
                <div class="stat-label">⏳ लंबित (Pending)</div>
            </div>
            <div class="stat-card" id="cardContacted" onclick="setFilter('CONTACTED')">
                <div class="stat-num" id="countContacted" style="color: #0277BD;">0</div>
                <div class="stat-label">📞 संपर्क किया गया (Contacted)</div>
            </div>
            <div class="stat-card" id="cardApproved" onclick="setFilter('APPROVED')">
                <div class="stat-num" id="countApproved" style="color: #2E7D32;">0</div>
                <div class="stat-label">✅ स्वीकृत / तय तिथि (Approved)</div>
            </div>
            <div class="stat-card" id="cardCompleted" onclick="setFilter('COMPLETED')">
                <div class="stat-num" id="countCompleted" style="color: #512DA8;">0</div>
                <div class="stat-label">🚩 पूर्ण अनुष्ठान (Completed)</div>
            </div>
        </div>

        <!-- Toolbar -->
        <div class="toolbar">
            <div class="search-box">
                <input type="text" id="searchInput" placeholder="भक्त का नाम, फोन नंबर, आवेदन क्रमांक या स्थान से खोजें..." oninput="onSearchChange()">
            </div>
            <div style="font-size: 0.9rem; color: var(--text-muted);">
                वर्तमान फ़िल्टर: <strong id="currentFilterLabel" style="color: var(--maroon);">सभी आवेदन</strong>
            </div>
        </div>

        <!-- Application Cards List -->
        <div class="app-list" id="appListContainer">
            <div class="empty-state">
                <h3>लोड हो रहा है...</h3>
                <p>कृपया प्रतीक्षा करें, हवन आवेदन सूची प्राप्त की जा रही है।</p>
            </div>
        </div>
    </div>

    <!-- PIN Authentication Modal -->
    <div id="pinModal">
        <div class="pin-card">
            <div style="font-size: 2.5rem; margin-bottom: 10px;">🔐</div>
            <h2>सुरक्षा प्रमाणीकरण</h2>
            <p>हवन आवेदन प्रबंधन हेतु अपना 4-अंकीय व्यवस्थापक PIN दर्ज करें:</p>
            <input type="password" id="adminPinInput" class="pin-input" maxlength="8" placeholder="••••" autofocus onkeypress="if(event.key==='Enter') verifyPin()">
            <div id="pinErrorMsg" style="color: #C62828; font-size: 0.85rem; margin-bottom: 12px; display: none;"></div>
            <button class="btn-pin-submit" onclick="verifyPin()">सत्यापित करें (Unlock)</button>
        </div>
    </div>

    <!-- Sevadar Access Management Modal (Super Admin Only) -->
    <div id="sevadarModal" style="display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.65); z-index: 1000; align-items: center; justify-content: center; padding: 16px;">
        <div style="background: white; border-radius: 16px; max-width: 650px; width: 100%; max-height: 85vh; display: flex; flex-direction: column; box-shadow: 0 10px 40px rgba(0,0,0,0.3); overflow: hidden;">
            <div style="background: linear-gradient(135deg, var(--maroon-deep), var(--maroon)); color: white; padding: 16px 20px; display: flex; justify-content: space-between; align-items: center;">
                <div style="display: flex; align-items: center; gap: 10px;">
                    <span style="font-size: 1.5rem;">👥</span>
                    <div>
                        <h3 style="font-size: 1.15rem; color: var(--gold); margin: 0;">सेवादार एक्सेस नियंत्रण (Access Matrix)</h3>
                        <p style="font-size: 0.8rem; color: #FFE082; margin-top: 2px;">केवल सुपर एडमिन द्वारा स्वीकृत सेवादार ही हवन आवेदन देख सकते हैं</p>
                    </div>
                </div>
                <button onclick="closeSevadarModal()" style="background: none; border: none; color: white; font-size: 1.6rem; cursor: pointer; line-height: 1;">&times;</button>
            </div>
            <div id="sevadarListContainer" style="padding: 16px; overflow-y: auto; flex: 1;">
                <div style="text-align: center; padding: 30px; color: var(--text-muted);">
                    <div style="font-size: 2rem; margin-bottom: 8px;">⏳</div>
                    <p>सेवादार सूची लोड हो रही है...</p>
                </div>
            </div>
            <div style="background: #F5F5F5; padding: 12px 20px; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #E0E0E0;">
                <span id="sevadarActionStatus" style="font-size: 0.85rem; color: #2E7D32; font-weight: bold;"></span>
                <button onclick="closeSevadarModal()" style="background: var(--maroon); color: white; border: none; padding: 7px 18px; border-radius: 8px; font-weight: bold; cursor: pointer;">बंद करें (Close)</button>
            </div>
        </div>
    </div>

    <script>
        const API_URL = 'api/havan_service.php';
        let currentPin = localStorage.getItem('sbkd_havan_admin_pin') || '';
        let currentIsSuper = localStorage.getItem('sbkd_havan_admin_is_super') === '1' || currentPin === '1234';
        let currentStatusFilter = 'ALL';
        let searchDebounceTimer = null;
        let allApplications = [];

        // Check Login on load
        window.addEventListener('DOMContentLoaded', () => {
            if (currentPin) {
                document.getElementById('pinModal').style.display = 'none';
                if (currentIsSuper) {
                    document.getElementById('btnSevadarAccess').style.display = 'inline-flex';
                }
                loadApplications();
            } else {
                document.getElementById('pinModal').style.display = 'flex';
                document.getElementById('adminPinInput').focus();
            }
        });

        async function verifyPin() {
            const pinVal = document.getElementById('adminPinInput').value.trim();
            const errEl = document.getElementById('pinErrorMsg');
            if (!pinVal) {
                errEl.innerText = 'कृपया PIN दर्ज करें!';
                errEl.style.display = 'block';
                return;
            }

            try {
                const res = await fetch(API_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action: 'ADMIN_LOGIN', pin: pinVal })
                });
                const data = await res.json();
                if (data.success) {
                    currentPin = pinVal;
                    currentIsSuper = (data.is_super === true || data.role === 'SUPER_ADMIN' || currentPin === '1234');
                    localStorage.setItem('sbkd_havan_admin_pin', currentPin);
                    localStorage.setItem('sbkd_havan_admin_is_super', currentIsSuper ? '1' : '0');
                    document.getElementById('pinModal').style.display = 'none';
                    if (currentIsSuper) {
                        document.getElementById('btnSevadarAccess').style.display = 'inline-flex';
                    } else {
                        document.getElementById('btnSevadarAccess').style.display = 'none';
                    }
                    loadApplications();
                } else {
                    errEl.innerText = data.error || 'अमान्य PIN! पुनः प्रयास करें।';
                    errEl.style.display = 'block';
                }
            } catch (err) {
                errEl.innerText = 'सर्वर से संपर्क नहीं हो सका। कृपया पुनः प्रयास करें।';
                errEl.style.display = 'block';
            }
        }

        function logoutAdmin() {
            localStorage.removeItem('sbkd_havan_admin_pin');
            localStorage.removeItem('sbkd_havan_admin_is_super');
            currentPin = '';
            currentIsSuper = false;
            document.getElementById('btnSevadarAccess').style.display = 'none';
            document.getElementById('adminPinInput').value = '';
            document.getElementById('pinErrorMsg').style.display = 'none';
            document.getElementById('pinModal').style.display = 'flex';
        }

        // Sevadar Access Management (Super Admin Exclusive)
        function openSevadarModal() {
            document.getElementById('sevadarModal').style.display = 'flex';
            document.getElementById('sevadarActionStatus').innerText = '';
            loadSevadarsList();
        }

        function closeSevadarModal() {
            document.getElementById('sevadarModal').style.display = 'none';
        }

        async function loadSevadarsList() {
            const container = document.getElementById('sevadarListContainer');
            container.innerHTML = '<div style="text-align:center; padding:30px; color:#757575;"><div style="font-size:1.8rem; margin-bottom:8px;">⏳</div>सेवादार सूची लोड हो रही है...</div>';
            try {
                const res = await fetch(`${API_URL}?action=GET_SEVADAR_ACCESS&admin_pin=${encodeURIComponent(currentPin)}`);
                const data = await res.json();
                if (data.success && data.sevadars) {
                    if (data.sevadars.length === 0) {
                        container.innerHTML = '<div style="text-align:center; padding:30px; color:#757575;">कोई सेवादार खाता नहीं मिला।</div>';
                        return;
                    }
                    let html = '<div style="display:flex; flex-direction:column; gap:10px;">';
                    data.sevadars.forEach(s => {
                        const isSuper = (s.role === 'SUPER_ADMIN' || s.username === 'admin');
                        const canManage = (s.can_manage_havan == 1) || isSuper;
                        html += `
                            <div style="background:#FAFAFA; border:1px solid #E0E0E0; border-radius:10px; padding:12px 14px; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
                                <div>
                                    <div style="font-weight:bold; font-size:0.95rem; color:#800000; display:flex; align-items:center; gap:6px;">
                                        ${escapeHtml(s.name)}
                                        ${isSuper ? '<span style="background:#4A148C; color:white; font-size:0.7rem; padding:2px 6px; border-radius:10px;">Super Admin</span>' : '<span style="background:#E0E0E0; color:#333; font-size:0.7rem; padding:2px 6px; border-radius:10px;">सेवादार</span>'}
                                    </div>
                                    <div style="font-size:0.8rem; color:#616161; margin-top:2px;">
                                        👤 यूजरनेम: <b>${escapeHtml(s.username)}</b> ${s.phone_number ? '• 📞 ' + escapeHtml(s.phone_number) : ''}
                                    </div>
                                </div>
                                <div>
                                    ${isSuper ? 
                                        '<span style="background:#E8F5E9; color:#2E7D32; font-weight:bold; font-size:0.8rem; padding:4px 10px; border-radius:20px; border:1px solid #A5D6A7;">✓ पूर्ण अधिकार (Master)</span>' :
                                        `<button onclick="toggleSevadarHavanAccess(${s.id}, ${canManage ? 0 : 1})" style="background:${canManage ? '#2E7D32' : '#757575'}; color:white; border:none; padding:6px 14px; border-radius:20px; font-size:0.8rem; font-weight:bold; cursor:pointer; display:inline-flex; align-items:center; gap:6px; transition:all 0.2s;">
                                            ${canManage ? '✓ अनुमति प्राप्त (Active)' : '🔒 अनुमति नहीं (Inactive)'}
                                        </button>`
                                    }
                                </div>
                            </div>
                        `;
                    });
                    html += '</div>';
                    container.innerHTML = html;
                } else {
                    container.innerHTML = `<div style="color:#C62828; text-align:center; padding:20px;">${escapeHtml(data.error || 'सेवादार सूची लोड नहीं हो सकी')}</div>`;
                }
            } catch (err) {
                container.innerHTML = '<div style="color:#C62828; text-align:center; padding:20px;">सर्वर से संपर्क नहीं हो सका।</div>';
            }
        }

        async function toggleSevadarHavanAccess(sevadarId, newStatus) {
            const statusEl = document.getElementById('sevadarActionStatus');
            statusEl.innerText = 'अपडेट हो रहा है...';
            try {
                const res = await fetch(API_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        action: 'TOGGLE_SEVADAR_ACCESS',
                        admin_pin: currentPin,
                        sevadar_id: sevadarId,
                        can_manage_havan: newStatus
                    })
                });
                const data = await res.json();
                if (data.success) {
                    statusEl.innerText = data.message || 'सफलतापूर्वक अपडेट हुआ!';
                    loadSevadarsList();
                } else {
                    statusEl.innerText = data.error || 'अपडेट विफल!';
                }
            } catch (err) {
                statusEl.innerText = 'सर्वर त्रुटि!';
            }
        }

        function setFilter(status) {
            currentStatusFilter = status;
            document.querySelectorAll('.stat-card').forEach(el => el.classList.remove('active-filter'));
            
            const map = {
                'ALL': 'cardAll',
                'PENDING': 'cardPending',
                'CONTACTED': 'cardContacted',
                'APPROVED': 'cardApproved',
                'COMPLETED': 'cardCompleted'
            };
            if (map[status]) document.getElementById(map[status]).classList.add('active-filter');

            const labelMap = {
                'ALL': 'सभी आवेदन',
                'PENDING': 'लंबित (Pending)',
                'CONTACTED': 'संपर्क किया गया (Contacted)',
                'APPROVED': 'स्वीकृत (Approved)',
                'COMPLETED': 'पूर्ण अनुष्ठान (Completed)'
            };
            document.getElementById('currentFilterLabel').innerText = labelMap[status] || status;
            loadApplications();
        }

        function onSearchChange() {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                loadApplications();
            }, 300);
        }

        async function loadApplications() {
            const container = document.getElementById('appListContainer');
            const searchVal = document.getElementById('searchInput').value.trim();

            try {
                const url = `${API_URL}?action=GET_APPLICATIONS&admin_pin=${encodeURIComponent(currentPin)}&status=${encodeURIComponent(currentStatusFilter)}&search=${encodeURIComponent(searchVal)}`;
                const res = await fetch(url);
                const data = await res.json();

                if (!data.success) {
                    if (res.status === 401) {
                        logoutAdmin();
                        return;
                    }
                    if (res.status === 403 || data.access_denied) {
                        container.innerHTML = `
                            <div class="empty-state" style="border: 2px solid #FFA000; background: #FFF8E1;">
                                <div style="font-size: 3rem; margin-bottom: 10px;">🔒</div>
                                <h3 style="color: #E65100;">अनुमति प्रतिबंधित (Access Denied)</h3>
                                <p style="color: #5D4037; max-width: 500px; margin: 8px auto;">${escapeHtml(data.error || 'हवन आवेदन देखने की अनुमति केवल सुपर एडमिन द्वारा स्वीकृत सेवादारों को ही है। कृपया सुपर एडमिन से संपर्क करें।')}</p>
                                <button onclick="logoutAdmin()" class="btn-header" style="background: #E65100; margin-top: 15px;">अन्य PIN से लॉगिन करें</button>
                            </div>
                        `;
                        return;
                    }
                    container.innerHTML = `<div class="empty-state"><h3>त्रुटि</h3><p>${escapeHtml(data.error || 'डेटा प्राप्त करने में समस्या')}</p></div>`;
                    return;
                }

                // Update Stats Counts
                if (data.counts) {
                    document.getElementById('countTotal').innerText = data.counts.total || 0;
                    document.getElementById('countPending').innerText = data.counts.pending || 0;
                    document.getElementById('countContacted').innerText = data.counts.contacted || 0;
                    document.getElementById('countApproved').innerText = data.counts.approved || 0;
                    document.getElementById('countCompleted').innerText = data.counts.completed || 0;
                }

                allApplications = data.applications || [];
                renderApplications(allApplications);

            } catch (e) {
                container.innerHTML = `<div class="empty-state"><h3>कनेक्शन समस्या</h3><p>सर्वर से संपर्क नहीं हो सका। कृपया पुनः प्रयास करें।</p></div>`;
            }
        }

        function renderApplications(list) {
            const container = document.getElementById('appListContainer');
            if (!list || list.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <div style="font-size: 3rem; margin-bottom: 10px;">📭</div>
                        <h3>कोई आवेदन प्राप्त नहीं हुआ</h3>
                        <p>इस फ़िल्टर में वर्तमान में कोई भी हवन आवेदन मौजूद नहीं है।</p>
                    </div>`;
                return;
            }

            let html = '';
            list.forEach(app => {
                const statusNames = {
                    'PENDING': '⏳ लंबित',
                    'CONTACTED': '📞 संपर्क किया',
                    'APPROVED': '✅ स्वीकृत',
                    'COMPLETED': '🚩 पूर्ण',
                    'CANCELLED': '❌ निरस्त'
                };

                const cleanPhone = (app.phone_number || '').replace(/[^0-9]/g, '');
                const waPhone = (app.whatsapp_number || app.phone_number || '').replace(/[^0-9]/g, '');
                const waMessage = encodeURIComponent(`जय श्री बालाजी महाराज! 🙏\nश्री बालाजी कृपा धाम (डूँगरा जाट) से आपके पावन हवन आवेदन (क्रमांक: ${app.application_no}) के संबंध में संपर्क किया जा रहा है।\n\nयजमान: ${app.devotee_name}\nप्रस्तावित तिथि: ${app.preferred_date}\nस्थान: ${app.address}\n\nकृपया सुविधा अनुसार बातचीत का समय बताएं।`);

                html += `
                <div class="app-card" id="card-${app.id}">
                    <div class="app-card-header">
                        <div class="app-title-group">
                            <h3>🔥 ${escapeHtml(app.devotee_name)} ${app.gotra ? '<span style="font-size: 0.95rem; color: #616161; font-weight: normal;">(गोत्र: ' + escapeHtml(app.gotra) + ')</span>' : ''}</h3>
                            <div class="app-no-badge">क्रमांक: ${escapeHtml(app.application_no)}</div>
                        </div>
                        <div class="status-badge status-${app.status}">
                            ${statusNames[app.status] || app.status}
                        </div>
                    </div>

                    <div class="app-details-grid">
                        <div class="detail-item">
                            <span class="detail-label">📞 संपर्क नंबर</span>
                            <span class="detail-val">
                                <strong>${escapeHtml(app.phone_number)}</strong>
                                ${app.whatsapp_number && app.whatsapp_number !== app.phone_number ? '<br><small>WhatsApp: ' + escapeHtml(app.whatsapp_number) + '</small>' : ''}
                            </span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">📅 प्रस्तावित हवन तिथि</span>
                            <span class="detail-val" style="color: var(--maroon); font-weight: 700;">
                                ${escapeHtml(app.preferred_date)}
                            </span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">🎯 हवन का प्रयोजन</span>
                            <span class="detail-val" style="color: #E65100; font-weight: 600;">
                                ${escapeHtml(app.havan_purpose)}
                            </span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">👨‍👩‍👦 परिवार सदस्य</span>
                            <span class="detail-val">${app.family_members_count || 1} सदस्य</span>
                        </div>
                    </div>

                    <div class="detail-item">
                        <span class="detail-label">📍 हवन कराने का पूरा पता (स्थान)</span>
                        <span class="detail-val" style="background: #FAFAFA; padding: 8px 12px; border-radius: 6px; border: 1px dashed #E0E0E0;">
                            ${escapeHtml(app.address)}
                            ${app.district ? ' • ' + escapeHtml(app.district) : ''}
                            ${app.state ? ' (' + escapeHtml(app.state) + ')' : ''}
                            ${app.pincode ? ' - ' + escapeHtml(app.pincode) : ''}
                        </span>
                    </div>

                    ${app.problem_details ? `
                    <div class="detail-item">
                        <span class="detail-label">📝 समस्या / संकल्प का विवरण</span>
                        <span class="detail-val" style="font-style: italic; color: #424242;">
                            "${escapeHtml(app.problem_details)}"
                        </span>
                    </div>
                    ` : ''}

                    <div class="policy-ack-box">
                        <span>✅ <strong>नियम स्वीकृति:</strong> यजमान द्वारा हवन सामग्री खर्च (~₹14,000) एवं वाहन का आने-जाने का किराया स्वयं वहन करने की सहमति दर्ज है।</span>
                    </div>

                    <div class="notes-box">
                        <span class="detail-label">✍️ व्यवस्थापक टिप्पणी (Admin Notes):</span>
                        <input type="text" class="notes-input" id="notes-${app.id}" value="${escapeHtml(app.admin_notes || '')}" placeholder="यहाँ टिप्पणी लिखें (उदा. बात हो गई है, पंडित जी की तिथि तय है)...">
                    </div>

                    <div class="app-actions-bar">
                        <div class="quick-contact-group">
                            <a href="tel:${cleanPhone}" class="btn-call">
                                📞 तुरंत कॉल करें
                            </a>
                            <a href="https://wa.me/91${waPhone}?text=${waMessage}" target="_blank" class="btn-wa">
                                💬 WhatsApp संदेश
                            </a>
                        </div>

                        <div class="status-update-form">
                            <select class="status-select" id="statusSelect-${app.id}">
                                <option value="PENDING" ${app.status === 'PENDING' ? 'selected' : ''}>⏳ लंबित (Pending)</option>
                                <option value="CONTACTED" ${app.status === 'CONTACTED' ? 'selected' : ''}>📞 संपर्क किया (Contacted)</option>
                                <option value="APPROVED" ${app.status === 'APPROVED' ? 'selected' : ''}>✅ स्वीकृत (Approved)</option>
                                <option value="COMPLETED" ${app.status === 'COMPLETED' ? 'selected' : ''}>🚩 पूर्ण (Completed)</option>
                                <option value="CANCELLED" ${app.status === 'CANCELLED' ? 'selected' : ''}>❌ निरस्त (Cancelled)</option>
                            </select>
                            <button class="btn-save-status" onclick="updateAppStatus(${app.id}, '${escapeHtml(app.application_no)}')">
                                💾 सहेजें
                            </button>
                            <button class="btn-delete" title="आवेदन हटाएं" onclick="deleteApplication(${app.id}, '${escapeHtml(app.application_no)}')">
                                🗑️
                            </button>
                        </div>
                    </div>
                    <div style="font-size: 0.75rem; color: #9E9E9E; display: flex; justify-content: space-between;">
                        <span>पंजीकरण समय: ${app.formatted_created_at || ''}</span>
                        ${app.contacted_by ? `<span>अंतिम संपादन: ${escapeHtml(app.contacted_by)}</span>` : ''}
                    </div>
                </div>
                `;
            });

            container.innerHTML = html;
        }

        async function updateAppStatus(id, appNo) {
            const statusSelect = document.getElementById(`statusSelect-${id}`);
            const notesInput = document.getElementById(`notes-${id}`);
            const newStatus = statusSelect.value;
            const newNotes = notesInput.value.trim();

            try {
                const res = await fetch(API_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        action: 'UPDATE_STATUS',
                        admin_pin: currentPin,
                        id: id,
                        application_no: appNo,
                        status: newStatus,
                        admin_notes: newNotes
                    })
                });
                const data = await res.json();
                if (data.success) {
                    alert('✅ आवेदन स्थिति एवं टिप्पणी सफलतापूर्वक अपडेट कर दी गई है!');
                    loadApplications();
                } else {
                    alert('त्रुटि: ' + (data.error || 'अपडेट नहीं हो सका'));
                }
            } catch (err) {
                alert('सर्वर त्रुटि! कृपया पुनः प्रयास करें।');
            }
        }

        async function deleteApplication(id, appNo) {
            if (!confirm(`क्या आप सचमुच आवेदन संख्या ${appNo} को हटाना चाहते हैं?`)) return;

            try {
                const res = await fetch(API_URL, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        action: 'DELETE_APPLICATION',
                        admin_pin: currentPin,
                        id: id,
                        application_no: appNo
                    })
                });
                const data = await res.json();
                if (data.success) {
                    alert('आवेदन हटा दिया गया!');
                    loadApplications();
                } else {
                    alert('त्रुटि: ' + (data.error || 'हटाने में समस्या'));
                }
            } catch (err) {
                alert('सर्वर त्रुटि!');
            }
        }

        function escapeHtml(text) {
            if (!text) return '';
            return String(text)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        }

        // Auto Refresh every 45s
        setInterval(() => {
            if (currentPin && document.getElementById('pinModal').style.display === 'none') {
                loadApplications();
            }
        }, 45000);
    </script>
</body>
</html>
