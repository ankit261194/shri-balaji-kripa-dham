<?php
// ==============================================================================
// ANKIT ELEVEX PRO - CORPORATE DESKTOP & TABLET WEB DASHBOARD
// Conceived & Engineered by Ankit Chaudhary (8533955333)
// Hostinger NVMe SSD Cluster: https://shribalajikripadham.online/portal
// ==============================================================================

ini_set('display_errors', 0);
error_reporting(0);

require_once __DIR__ . '/elevex_gateway/config.php';
header('Content-Type: text/html; charset=utf-8');

$pdo = getDbConnection();

// Initial server-side query for instantaneous first-paint rendering
$companies = [];
$defaultTenant = 'tenant_apexelevatorsncr_5779';

if ($pdo) {
    try {
        $stmtC = $pdo->query("SELECT * FROM elevex_companies WHERE is_active = 1 ORDER BY id ASC");
        $companies = $stmtC->fetchAll(PDO::FETCH_ASSOC);
        if (!empty($companies)) {
            $defaultTenant = $companies[0]['tenant_id'];
        }
    } catch (Throwable $e) {}
}
?>
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <base href="/">
    <title>EleveX Pro | कॉर्पोरेट डेस्कटॉप वेब डैशबोर्ड</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #0284C7;
            --primary-dark: #0369A1;
            --primary-light: #38BDF8;
            --bg-base: #0B1120;
            --bg-surface: #131E32;
            --bg-card: #182640;
            --bg-card-hover: #1E3152;
            --border-color: #243553;
            --border-highlight: #334B73;
            --text-main: #F1F5F9;
            --text-muted: #94A3B8;
            --text-dim: #64748B;
            --success: #10B981;
            --warning: #F59E0B;
            --danger: #EF4444;
            --purple: #8B5CF6;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background-color: var(--bg-base);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            overflow-x: hidden;
        }

        /* Top Bar */
        .topbar {
            background: linear-gradient(180deg, #182640 0%, #131E32 100%);
            border-bottom: 1px solid var(--border-color);
            padding: 12px 28px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: sticky;
            top: 0;
            z-index: 1000;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
        }

        .brand-section {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .brand-logo {
            width: 42px;
            height: 42px;
            border-radius: 10px;
            background: linear-gradient(135deg, var(--primary) 0%, #1D4ED8 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
            font-weight: 800;
            color: #FFFFFF;
            box-shadow: 0 0 15px rgba(2, 132, 199, 0.4);
        }

        .brand-titles h1 {
            font-size: 19px;
            font-weight: 700;
            letter-spacing: -0.02em;
            color: var(--text-main);
        }

        .brand-titles p {
            font-size: 12px;
            color: var(--text-muted);
            font-weight: 500;
        }

        .top-controls {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .ghost-switcher-container {
            display: flex;
            align-items: center;
            background: #0D1626;
            border: 1px solid #3B82F6;
            border-radius: 8px;
            padding: 4px 10px;
            gap: 8px;
            box-shadow: 0 0 12px rgba(59, 130, 246, 0.25);
        }

        .ghost-badge {
            background: rgba(139, 92, 246, 0.2);
            color: #C084FC;
            border: 1px solid rgba(139, 92, 246, 0.4);
            padding: 2px 8px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            display: flex;
            align-items: center;
            gap: 4px;
        }

        .ghost-select {
            background: transparent;
            color: #FFFFFF;
            border: none;
            font-size: 13px;
            font-weight: 600;
            padding: 4px 6px;
            cursor: pointer;
            outline: none;
        }

        .ghost-select option {
            background: #131E32;
            color: #F1F5F9;
        }

        .btn-refresh {
            background: var(--bg-surface);
            border: 1px solid var(--border-color);
            color: var(--text-muted);
            padding: 8px 14px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s;
        }

        .btn-refresh:hover {
            color: #FFFFFF;
            border-color: var(--primary-light);
            background: var(--bg-card);
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: var(--success);
            box-shadow: 0 0 8px var(--success);
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% { transform: scale(0.95); opacity: 0.8; }
            50% { transform: scale(1.15); opacity: 1; }
            100% { transform: scale(0.95); opacity: 0.8; }
        }

        /* Container */
        .main-container {
            padding: 24px 28px;
            max-width: 1700px;
            margin: 0 auto;
            width: 100%;
            flex: 1;
        }

        /* KPI Stat Cards */
        .kpi-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            gap: 18px;
            margin-bottom: 24px;
        }

        .kpi-card {
            background: var(--bg-surface);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: relative;
            overflow: hidden;
            transition: transform 0.2s, border-color 0.2s;
        }

        .kpi-card:hover {
            transform: translateY(-2px);
            border-color: var(--border-highlight);
        }

        .kpi-card::before {
            content: '';
            position: absolute;
            left: 0;
            top: 0;
            bottom: 0;
            width: 4px;
            background: var(--primary);
        }

        .kpi-card.kpi-danger::before { background: var(--danger); }
        .kpi-card.kpi-success::before { background: var(--success); }
        .kpi-card.kpi-purple::before { background: var(--purple); }

        .kpi-info h4 {
            font-size: 13px;
            font-weight: 500;
            color: var(--text-muted);
            margin-bottom: 6px;
        }

        .kpi-info .kpi-value {
            font-size: 28px;
            font-weight: 800;
            color: #FFFFFF;
            font-family: 'JetBrains Mono', monospace;
        }

        .kpi-sub {
            font-size: 12px;
            color: var(--text-dim);
            margin-top: 4px;
        }

        .kpi-icon {
            font-size: 32px;
            opacity: 0.8;
        }

        /* Section Header */
        .section-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 16px;
            border-bottom: 1px solid var(--border-color);
            padding-bottom: 10px;
        }

        .section-header h2 {
            font-size: 17px;
            font-weight: 700;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .badge-counter {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            color: var(--primary-light);
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 700;
        }

        /* Fleet Radar Grid */
        .fleet-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
            gap: 16px;
            margin-bottom: 30px;
        }

        .fleet-card {
            background: var(--bg-surface);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            padding: 16px;
            display: flex;
            flex-direction: column;
            gap: 12px;
            transition: all 0.2s;
        }

        .fleet-card:hover {
            border-color: var(--border-highlight);
            background: var(--bg-card);
        }

        .fleet-card-top {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .tech-profile {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .tech-avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background: linear-gradient(135deg, #1E293B 0%, #334155 100%);
            border: 2px solid var(--border-color);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            font-weight: 700;
            color: #FFFFFF;
        }

        .tech-meta h3 {
            font-size: 15px;
            font-weight: 700;
            color: #FFFFFF;
        }

        .tech-meta p {
            font-size: 12px;
            color: var(--text-muted);
            font-family: 'JetBrains Mono', monospace;
        }

        .status-pill {
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 5px;
        }

        .pill-online { background: rgba(16, 185, 129, 0.15); color: #34D399; border: 1px solid rgba(16, 185, 129, 0.3); }
        .pill-warning { background: rgba(245, 158, 11, 0.15); color: #FBBF24; border: 1px solid rgba(245, 158, 11, 0.3); }
        .pill-error { background: rgba(239, 68, 68, 0.15); color: #F87171; border: 1px solid rgba(239, 68, 68, 0.3); }

        .fleet-location {
            background: #0B1120;
            border: 1px solid var(--border-color);
            border-radius: 8px;
            padding: 10px 12px;
            display: flex;
            align-items: flex-start;
            gap: 8px;
            font-size: 12px;
            line-height: 1.4;
            color: #CBD5E1;
        }

        .fleet-card-bottom {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 12px;
            color: var(--text-dim);
            border-top: 1px solid var(--border-color);
            padding-top: 10px;
        }

        .battery-indicator {
            display: flex;
            align-items: center;
            gap: 6px;
            font-weight: 600;
            color: var(--text-muted);
        }

        /* Real-Time Breakdown Kanban Board */
        .kanban-board {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 18px;
            margin-bottom: 30px;
        }

        @media (max-width: 1200px) {
            .kanban-board {
                grid-template-columns: repeat(2, 1fr);
            }
        }

        @media (max-width: 768px) {
            .kanban-board {
                grid-template-columns: 1fr;
            }
        }

        .kanban-column {
            background: var(--bg-surface);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            display: flex;
            flex-direction: column;
            max-height: 750px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        }

        .kanban-col-header {
            padding: 14px 16px;
            border-bottom: 1px solid var(--border-color);
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: #152238;
            border-radius: 12px 12px 0 0;
        }

        .col-title {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            font-weight: 700;
        }

        .col-pending { color: #F59E0B; }
        .col-assigned { color: #38BDF8; }
        .col-onsite { color: #818CF8; }
        .col-resolved { color: #34D399; }

        .kanban-cards-wrapper {
            padding: 14px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 12px;
            flex: 1;
        }

        .ticket-card {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            padding: 14px;
            display: flex;
            flex-direction: column;
            gap: 10px;
            transition: all 0.2s;
            cursor: pointer;
        }

        .ticket-card:hover {
            border-color: var(--primary-light);
            transform: translateY(-2px);
            box-shadow: 0 4px 14px rgba(0, 0, 0, 0.3);
        }

        .ticket-top {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .ticket-number {
            font-family: 'JetBrains Mono', monospace;
            font-size: 12px;
            font-weight: 700;
            color: var(--primary-light);
        }

        .controller-tag {
            background: #0F172A;
            border: 1px solid var(--border-color);
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 10px;
            font-weight: 700;
            color: #94A3B8;
            text-transform: uppercase;
        }

        .ticket-building {
            font-size: 14px;
            font-weight: 700;
            color: #FFFFFF;
        }

        .ticket-issue {
            font-size: 12px;
            color: #CBD5E1;
            line-height: 1.4;
            background: #0B1120;
            padding: 8px;
            border-radius: 6px;
            border-left: 3px solid var(--primary);
        }

        .ticket-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 11px;
            color: var(--text-dim);
            padding-top: 8px;
            border-top: 1px solid var(--border-color);
        }

        .otp-badge {
            background: rgba(245, 158, 11, 0.15);
            color: #F59E0B;
            border: 1px solid rgba(245, 158, 11, 0.3);
            padding: 2px 6px;
            border-radius: 4px;
            font-family: 'JetBrains Mono', monospace;
            font-weight: 700;
        }

        .rating-stars {
            color: #F59E0B;
            font-size: 12px;
        }

        /* Attendance & Audit Ledger Table */
        .ledger-wrapper {
            background: var(--bg-surface);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            overflow: hidden;
            margin-bottom: 30px;
        }

        .ledger-table {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
            font-size: 13px;
        }

        .ledger-table th {
            background: #152238;
            color: var(--text-muted);
            font-weight: 600;
            padding: 12px 16px;
            border-bottom: 1px solid var(--border-color);
            text-transform: uppercase;
            font-size: 11px;
            letter-spacing: 0.05em;
        }

        .ledger-table td {
            padding: 14px 16px;
            border-bottom: 1px solid var(--border-color);
            color: var(--text-main);
        }

        .ledger-table tr:hover {
            background: var(--bg-card);
        }

        .audit-seal-badge {
            background: rgba(139, 92, 246, 0.15);
            color: #C084FC;
            border: 1px solid rgba(139, 92, 246, 0.3);
            padding: 3px 8px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 600;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }

        /* Modal */
        .modal-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.7);
            backdrop-filter: blur(4px);
            z-index: 2000;
            align-items: center;
            justify-content: center;
        }

        .modal-box {
            background: var(--bg-surface);
            border: 1px solid var(--border-highlight);
            border-radius: 14px;
            width: 90%;
            max-width: 520px;
            padding: 24px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
        }

        .modal-title {
            font-size: 17px;
            font-weight: 700;
            margin-bottom: 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .modal-close {
            background: transparent;
            border: none;
            color: var(--text-muted);
            font-size: 20px;
            cursor: pointer;
        }

        .form-group {
            margin-bottom: 14px;
        }

        .form-label {
            display: block;
            font-size: 12px;
            color: var(--text-muted);
            margin-bottom: 6px;
            font-weight: 600;
        }

        .form-control {
            width: 100%;
            background: #0B1120;
            border: 1px solid var(--border-color);
            border-radius: 8px;
            padding: 10px 12px;
            color: #FFFFFF;
            font-size: 13px;
            outline: none;
        }

        .form-control:focus {
            border-color: var(--primary-light);
        }

        .btn-submit {
            width: 100%;
            background: var(--primary);
            color: #FFFFFF;
            border: none;
            border-radius: 8px;
            padding: 12px;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            margin-top: 10px;
            transition: background 0.2s;
        }

        .btn-submit:hover {
            background: var(--primary-dark);
        }

        /* Footer */
        .portal-footer {
            border-top: 1px solid var(--border-color);
            padding: 18px 28px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 12px;
            color: var(--text-dim);
            background: #0A0F1D;
        }

        .footer-links a {
            color: var(--text-muted);
            text-decoration: none;
            margin-left: 14px;
        }

        .footer-links a:hover {
            color: var(--primary-light);
        }
    </style>
</head>
<body>

    <!-- Top Navigation Bar -->
    <header class="topbar">
        <div class="brand-section">
            <div class="brand-logo" id="headerLogo">E</div>
            <div class="brand-titles">
                <h1 id="headerCompanyName">Apex Elevators NCR</h1>
                <p id="headerBrandSubtitle">Apex EleveX Pro • 24x7 Emergency Lift Support: <span id="headerSupportPhone">9811001122</span></p>
            </div>
        </div>

        <div class="top-controls">
            <!-- 1-Click Super Admin Ghost Mode Switcher -->
            <div class="ghost-switcher-container">
                <span class="ghost-badge">🛡️ Ghost Switcher</span>
                <select id="ghostTenantSelect" class="ghost-select" onchange="onGhostTenantChange(this.value)">
                    <?php foreach ($companies as $comp): ?>
                        <option value="<?= htmlspecialchars($comp['tenant_id']) ?>" <?= $comp['tenant_id'] === $defaultTenant ? 'selected' : '' ?>>
                            <?= htmlspecialchars($comp['company_name']) ?> (<?= htmlspecialchars($comp['tenant_id']) ?>)
                        </option>
                    <?php endforeach; ?>
                </select>
            </div>

            <button class="btn-refresh" onclick="reloadDashboardData()">
                <span class="status-dot"></span>
                <span>लाइव सिंक</span>
            </button>

            <button class="btn-refresh" onclick="openNewTicketModal()" style="background: var(--primary); color: #fff; border-color: var(--primary);">
                <span>+ नया ब्रेकडाउन</span>
            </button>
        </div>
    </header>

    <!-- Main Container -->
    <main class="main-container">
        
        <!-- Executive KPI Stat Cards -->
        <section class="kpi-grid">
            <div class="kpi-card">
                <div class="kpi-info">
                    <h4>कुल AMC लिफ्ट साइट्स</h4>
                    <div class="kpi-value" id="kpiTotalLifts">--</div>
                    <div class="kpi-sub">150m जियोफेंस सुरक्षित</div>
                </div>
                <div class="kpi-icon">🏢</div>
            </div>

            <div class="kpi-card kpi-danger">
                <div class="kpi-info">
                    <h4>सक्रिय ब्रेकडाउन (Critical)</h4>
                    <div class="kpi-value" id="kpiActiveBreakdowns">--</div>
                    <div class="kpi-sub">कस्टमर OTP सुरक्षित</div>
                </div>
                <div class="kpi-icon">🚨</div>
            </div>

            <div class="kpi-card kpi-success">
                <div class="kpi-info">
                    <h4>ड्यूटी पर फील्ड टेक्नीशियन</h4>
                    <div class="kpi-value" id="kpiOnlineTechs">--</div>
                    <div class="kpi-sub">लाइव OpenStreetMap राडार</div>
                </div>
                <div class="kpi-icon">📡</div>
            </div>

            <div class="kpi-card kpi-purple">
                <div class="kpi-info">
                    <h4>सुपर एडमिन ऑडिट सील</h4>
                    <div class="kpi-value">100%</div>
                    <div class="kpi-sub">अपरिवर्तनीय लेज़र सुरक्षा</div>
                </div>
                <div class="kpi-icon">🛡️</div>
            </div>
        </section>

        <!-- Live Technician Fleet Radar -->
        <div class="section-header">
            <h2>
                <span>📡 लाइव फील्ड टेक्नीशियन फ्लीट राडार</span>
                <span class="badge-counter" id="fleetCountBadge">0 Online</span>
            </h2>
            <div style="font-size: 12px; color: var(--text-muted);">
                OpenStreetMap लैंडमार्क • बैटरी व पावर स्टेट टेलीमेट्री
            </div>
        </div>

        <section class="fleet-grid" id="fleetGridContainer">
            <div style="grid-column: 1 / -1; text-align: center; padding: 20px; color: var(--text-muted);">
                फ्लीट टेलीमेट्री लोड हो रही है...
            </div>
        </section>

        <!-- Real-Time Breakdown Kanban Board -->
        <div class="section-header">
            <h2>
                <span>🛠️ रियल-टाइम ब्रेकडाउन कानबान बोर्ड</span>
                <span class="badge-counter" id="kanbanTotalBadge">0 Tickets</span>
            </h2>
            <div style="font-size: 12px; color: var(--text-muted);">
                Pending ➔ Assigned ➔ On-Site ➔ Resolved
            </div>
        </div>

        <section class="kanban-board">
            <!-- 1. Pending Column -->
            <div class="kanban-column">
                <div class="kanban-col-header">
                    <div class="col-title col-pending">
                        <span>🕒</span>
                        <span>प्रतीक्षारत (Pending)</span>
                    </div>
                    <span class="badge-counter" id="badgePending">0</span>
                </div>
                <div class="kanban-cards-wrapper" id="colPendingCards"></div>
            </div>

            <!-- 2. Assigned Column -->
            <div class="kanban-column">
                <div class="kanban-col-header">
                    <div class="col-title col-assigned">
                        <span>👤</span>
                        <span>असाइन किया गया (Assigned)</span>
                    </div>
                    <span class="badge-counter" id="badgeAssigned">0</span>
                </div>
                <div class="kanban-cards-wrapper" id="colAssignedCards"></div>
            </div>

            <!-- 3. On-Site Column -->
            <div class="kanban-column">
                <div class="kanban-col-header">
                    <div class="col-title col-onsite">
                        <span>📍</span>
                        <span>साइट पर उपस्थित (On-Site)</span>
                    </div>
                    <span class="badge-counter" id="badgeOnSite">0</span>
                </div>
                <div class="kanban-cards-wrapper" id="colOnSiteCards"></div>
            </div>

            <!-- 4. Resolved Column -->
            <div class="kanban-column">
                <div class="kanban-col-header">
                    <div class="col-title col-resolved">
                        <span>✅</span>
                        <span>सफलतापूर्वक हल (Resolved)</span>
                    </div>
                    <span class="badge-counter" id="badgeResolved">0</span>
                </div>
                <div class="kanban-cards-wrapper" id="colResolvedCards"></div>
            </div>
        </section>

        <!-- Attendance & Audit Ledger Viewer -->
        <div class="section-header">
            <h2>
                <span>📋 उपस्थिति व अपरिवर्तनीय ऑडिट लेज़र</span>
                <span class="badge-counter" id="ledgerCountBadge">Today</span>
            </h2>
            <div style="font-size: 12px; color: var(--text-muted);">
                एंटी-मॉक GPS प्रमाणीकरण • सुपर एडमिन ऑडिट ट्रेल
            </div>
        </div>

        <section class="ledger-wrapper">
            <table class="ledger-table">
                <thead>
                    <tr>
                        <th>टेक्नीशियन</th>
                        <th>तारीख</th>
                        <th>पंच-इन समय</th>
                        <th>पंच-इन लैंडमार्क (OSM GPS)</th>
                        <th>GPS अखंडता</th>
                        <th>ऑडिट सील</th>
                        <th>कार्रवाई</th>
                    </tr>
                </thead>
                <tbody id="ledgerTableBody">
                    <tr>
                        <td colspan="7" style="text-align: center; color: var(--text-muted);">लेज़र रिकॉर्ड लोड हो रहे हैं...</td>
                    </tr>
                </tbody>
            </table>
        </section>

    </main>

    <!-- Modal: Create New Breakdown -->
    <div class="modal-overlay" id="newTicketModal">
        <div class="modal-box">
            <div class="modal-title">
                <span>🚨 नया आपातकालीन ब्रेकडाउन टिकट दर्ज करें</span>
                <button class="modal-close" onclick="closeNewTicketModal()">&times;</button>
            </div>
            <form onsubmit="handleCreateTicket(event)">
                <div class="form-group">
                    <label class="form-label">लिफ्ट साइट चुनें</label>
                    <select id="modalLiftSelect" class="form-control" required></select>
                </div>
                <div class="form-group">
                    <label class="form-label">समस्या / फॉल्ट विवरण (OEM Error Code)</label>
                    <textarea id="modalIssueDesc" class="form-control" rows="3" required placeholder="उदाहरण: Monarch NICE3000+ Err02 Acceleration Overcurrent"></textarea>
                </div>
                <div class="form-group">
                    <label class="form-label">डिस्पैच मोड</label>
                    <select id="modalAutoDispatch" class="form-control">
                        <option value="1">स्मार्ट हैवर्सिन ऑटो-डिस्पैच (निकटतम टेक्नीशियन)</option>
                        <option value="0">मैनुअल डिस्पैच (कॉकपिट द्वारा)</option>
                    </select>
                </div>
                <button type="submit" class="btn-submit">आपातकालीन डिस्पैच आरंभ करें</button>
            </form>
        </div>
    </div>

    <!-- Modal: SuperAdmin Edit Attendance Audit Seal -->
    <div class="modal-overlay" id="auditEditModal">
        <div class="modal-box">
            <div class="modal-title">
                <span>🛡️ सुपर एडमिन उपस्थिति ऑडिट संपादन</span>
                <button class="modal-close" onclick="closeAuditModal()">&times;</button>
            </div>
            <form onsubmit="handleSuperAdminAuditEdit(event)">
                <input type="hidden" id="auditRecordId">
                <div class="form-group">
                    <label class="form-label">संशोधित पंच-इन समय</label>
                    <input type="text" id="auditNewPunchIn" class="form-control" placeholder="YYYY-MM-DD HH:MM:SS" required>
                </div>
                <div class="form-group">
                    <label class="form-label">विधिक ऑडिट संपादन कारण (कम से कम 5 अक्षर)</label>
                    <textarea id="auditReason" class="form-control" rows="3" required placeholder="सुपर एडमिन अंकित चौधरी द्वारा ऑन-साइट आपातकालीन सत्यापन"></textarea>
                </div>
                <button type="submit" class="btn-submit" style="background: var(--purple);">विधिक ऑडिट सील लागू करें</button>
            </form>
        </div>
    </div>

    <!-- Footer -->
    <footer class="portal-footer">
        <div>
            <strong>Ankit EleveX Pro</strong> • एंटरप्राइज मल्टी-टेनेंट कॉर्पोरेट डैशबोर्ड • होस्टेड ऑन Hostinger NVMe SSD
        </div>
        <div class="footer-links">
            <a href="/download.php?app=superadmin">SuperAdmin APK</a>
            <a href="/download.php?app=supervisor">Supervisor APK</a>
            <a href="/download.php?app=technician">Technician APK</a>
            <a href="/download.php?app=owner">Owner APK</a>
            <a href="/download.php?app=gateway">Master Gateway</a>
        </div>
    </footer>

    <!-- Interactive AJAX Engine -->
    <script>
        const API_BASE = '/elevex_gateway';
        const SUPER_ADMIN_PHONE = '8533955333';
        let currentTenantId = '<?= htmlspecialchars($defaultTenant) ?>';
        let availableLifts = [];
        let availableTechs = [];

        // 1. Ghost Mode Switcher Handler
        async function onGhostTenantChange(tenantId) {
            currentTenantId = tenantId;
            try {
                const resp = await fetch(`${API_BASE}/api_auth.php?action=ghost_switch_tenant&caller_phone=${SUPER_ADMIN_PHONE}&target_tenant_id=${tenantId}`);
                const data = await resp.json();
                if (data.success && data.company) {
                    updateBranding(data.company);
                    if (data.fleet_stats) {
                        updateKpis(data.fleet_stats);
                    }
                }
            } catch (err) {
                console.error("Ghost switch error:", err);
            }
            reloadDashboardData();
        }

        function updateBranding(company) {
            document.getElementById('headerCompanyName').textContent = company.company_name;
            document.getElementById('headerBrandSubtitle').innerHTML = `${company.brand_display_name || ''} • 24x7 Emergency Lift Support: <span id="headerSupportPhone">${company.support_phone || ''}</span>`;
            if (company.primary_color) {
                document.documentElement.style.setProperty('--primary', company.primary_color);
            }
        }

        function updateKpis(stats) {
            document.getElementById('kpiTotalLifts').textContent = stats.total_lifts || 0;
            document.getElementById('kpiActiveBreakdowns').textContent = stats.active_breakdowns || 0;
            document.getElementById('kpiOnlineTechs').textContent = stats.online_technicians || 0;
        }

        // 2. Comprehensive Dashboard Reload
        async function reloadDashboardData() {
            await Promise.all([
                loadFleetTelemetry(),
                loadKanbanBreakdowns(),
                loadAttendanceLedger(),
                loadLiftsForTenant()
            ]);
        }

        // 3. Load Fleet Telemetry
        async function loadFleetTelemetry() {
            const container = document.getElementById('fleetGridContainer');
            try {
                const resp = await fetch(`${API_BASE}/api_telemetry.php?action=get_technician_fleet&tenant_id=${currentTenantId}`);
                const data = await resp.json();
                if (!data.success || !data.fleet || data.fleet.length === 0) {
                    container.innerHTML = `<div style="grid-column: 1 / -1; text-align: center; padding: 24px; color: var(--text-muted); background: var(--bg-surface); border-radius: 12px; border: 1px dashed var(--border-color);">
                        इस कंपनी में अभी कोई सक्रिय फील्ड टेक्नीशियन पंजीकृत नहीं हैं।
                    </div>`;
                    document.getElementById('fleetCountBadge').textContent = '0 Technicians';
                    return;
                }

                availableTechs = data.fleet;
                let onlineCount = 0;
                let html = '';

                data.fleet.forEach(tech => {
                    if (tech.is_online == 1) onlineCount++;
                    const pillClass = tech.status_color === 'success' ? 'pill-online' : (tech.status_color === 'warning' ? 'pill-warning' : 'pill-error');
                    const chargingIcon = tech.is_charging == 1 ? '⚡' : '🔋';
                    const initial = (tech.full_name || 'T').charAt(0).toUpperCase();

                    html += `
                        <div class="fleet-card">
                            <div class="fleet-card-top">
                                <div class="tech-profile">
                                    <div class="tech-avatar">${initial}</div>
                                    <div class="tech-meta">
                                        <h3>${tech.full_name}</h3>
                                        <p>📞 <a href="tel:${tech.phone}" style="color: var(--text-muted); text-decoration: none;">${tech.phone}</a></p>
                                    </div>
                                </div>
                                <span class="status-pill ${pillClass}">${tech.status_badge || 'ONLINE'}</span>
                            </div>

                            <div class="fleet-location">
                                <span>📍</span>
                                <div>${tech.location_landmark_text || 'GPS लोकेशन ट्रैकिंग सक्रिय...'}</div>
                            </div>

                            <div class="fleet-card-bottom">
                                <div class="battery-indicator">
                                    <span>${chargingIcon}</span>
                                    <span>${tech.battery_percentage || 100}% बैटरी</span>
                                </div>
                                <div>
                                    ${tech.active_jobs > 0 ? `🚨 <strong>${tech.active_jobs}</strong> सक्रिय जॉब` : `🟢 स्टैंडबाय तैयार`}
                                </div>
                            </div>
                        </div>
                    `;
                });

                container.innerHTML = html;
                document.getElementById('fleetCountBadge').textContent = `${onlineCount} Online / ${data.fleet.length} Total`;
                document.getElementById('kpiOnlineTechs').textContent = onlineCount;
            } catch (err) {
                console.error("Telemetry load error:", err);
            }
        }

        // 4. Load Kanban Breakdowns
        async function loadKanbanBreakdowns() {
            try {
                const resp = await fetch(`${API_BASE}/api_dispatch.php?action=list_breakdowns&tenant_id=${currentTenantId}&role=supervisor`);
                const data = await resp.json();
                
                const colPending = document.getElementById('colPendingCards');
                const colAssigned = document.getElementById('colAssignedCards');
                const colOnSite = document.getElementById('colOnSiteCards');
                const colResolved = document.getElementById('colResolvedCards');

                colPending.innerHTML = '';
                colAssigned.innerHTML = '';
                colOnSite.innerHTML = '';
                colResolved.innerHTML = '';

                let counts = { pending: 0, assigned: 0, on_site: 0, resolved: 0 };

                if (data.success && data.breakdowns) {
                    data.breakdowns.forEach(ticket => {
                        const cardHtml = renderTicketCard(ticket);
                        if (ticket.status === 'pending') {
                            colPending.innerHTML += cardHtml;
                            counts.pending++;
                        } else if (ticket.status === 'assigned' || ticket.status === 'technician_enroute') {
                            colAssigned.innerHTML += cardHtml;
                            counts.assigned++;
                        } else if (ticket.status === 'on_site' || ticket.status === 'in_progress') {
                            colOnSite.innerHTML += cardHtml;
                            counts.on_site++;
                        } else if (ticket.status === 'resolved') {
                            colResolved.innerHTML += cardHtml;
                            counts.resolved++;
                        }
                    });
                }

                // Empty state fallbacks
                if (counts.pending === 0) colPending.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-dim); font-size: 12px;">कोई पेंडिंग टिकट नहीं</div>';
                if (counts.assigned === 0) colAssigned.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-dim); font-size: 12px;">कोई टिकट असाइन नहीं</div>';
                if (counts.on_site === 0) colOnSite.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-dim); font-size: 12px;">कोई टेक्नीशियन साइट पर नहीं</div>';
                if (counts.resolved === 0) colResolved.innerHTML = '<div style="padding: 16px; text-align: center; color: var(--text-dim); font-size: 12px;">आज कोई हल टिकट नहीं</div>';

                document.getElementById('badgePending').textContent = counts.pending;
                document.getElementById('badgeAssigned').textContent = counts.assigned;
                document.getElementById('badgeOnSite').textContent = counts.on_site;
                document.getElementById('badgeResolved').textContent = counts.resolved;
                document.getElementById('kanbanTotalBadge').textContent = `${counts.pending + counts.assigned + counts.on_site} Active`;
                document.getElementById('kpiActiveBreakdowns').textContent = counts.pending + counts.assigned + counts.on_site;
            } catch (err) {
                console.error("Kanban load error:", err);
            }
        }

        function renderTicketCard(t) {
            const hasTech = !!t.technician_name;
            const otpDisplay = t.completion_otp ? `<span class="otp-badge">OTP: ${t.completion_otp}</span>` : '';
            const ratingDisplay = t.customer_rating ? `<span class="rating-stars">${'★'.repeat(t.customer_rating)}${'☆'.repeat(5 - t.customer_rating)}</span>` : '';

            let dispatchAction = '';
            if (t.status === 'pending' && availableTechs.length > 0) {
                const options = availableTechs.map(tech => `<option value="${tech.technician_id}">${tech.full_name}</option>`).join('');
                dispatchAction = `
                    <div style="margin-top: 8px; display: flex; gap: 6px;">
                        <select id="dispatchSelect_${t.id}" style="background: #0B1120; color: #fff; border: 1px solid var(--border-color); border-radius: 6px; font-size: 11px; padding: 4px; flex: 1;">
                            ${options}
                        </select>
                        <button onclick="handleManualDispatch(${t.id})" style="background: var(--primary); color: #fff; border: none; border-radius: 6px; font-size: 11px; font-weight: 700; padding: 4px 8px; cursor: pointer;">डिस्पैच</button>
                    </div>
                `;
            }

            return `
                <div class="ticket-card">
                    <div class="ticket-top">
                        <span class="ticket-number">${t.ticket_number}</span>
                        <span class="controller-tag">${t.controller_brand || 'ELEVEX'}</span>
                    </div>
                    <div class="ticket-building">${t.building_name}</div>
                    <div class="ticket-issue">${t.issue_description}</div>
                    ${hasTech ? `<div style="font-size: 12px; color: var(--primary-light);">👤 <strong>${t.technician_name}</strong> (${t.technician_phone})</div>` : ''}
                    ${t.radar_status_text ? `<div style="font-size: 11px; color: #CBD5E1;">${t.radar_status_text}</div>` : ''}
                    ${dispatchAction}
                    <div class="ticket-footer">
                        ${otpDisplay}
                        ${ratingDisplay}
                        <span>${t.created_at ? t.created_at.substring(11, 16) : ''}</span>
                    </div>
                </div>
            `;
        }

        async function handleManualDispatch(ticketId) {
            const select = document.getElementById(`dispatchSelect_${ticketId}`);
            if (!select) return;
            const techId = select.value;

            try {
                const resp = await fetch(`${API_BASE}/api_dispatch.php?action=manual_dispatch`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        action: 'manual_dispatch',
                        ticket_id: ticketId,
                        technician_id: techId,
                        supervisor_id: 1
                    })
                });
                const res = await resp.json();
                if (res.success) {
                    reloadDashboardData();
                } else {
                    alert(res.error || 'डिस्पैच त्रुटि');
                }
            } catch (err) {
                console.error("Dispatch error:", err);
            }
        }

        // 5. Load Attendance & Audit Ledger
        async function loadAttendanceLedger() {
            const tbody = document.getElementById('ledgerTableBody');
            try {
                const resp = await fetch(`${API_BASE}/api_telemetry.php?action=get_attendance_ledger&tenant_id=${currentTenantId}&date=all`);
                const data = await resp.json();

                if (!data.success || !data.records || data.records.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-dim); padding: 24px;">इस कंपनी में अभी कोई उपस्थिति लेज़र प्रविष्टि नहीं है।</td></tr>`;
                    return;
                }

                let html = '';
                data.records.forEach(r => {
                    const mockStatus = r.is_mock_detected == 1 ? `<span style="color: var(--danger); font-weight: 700;">🚨 MOCK DETECTED</span>` : `<span style="color: var(--success); font-weight: 600;">🛡️ PASS (0% Mock)</span>`;
                    const auditSeal = r.superadmin_edit_reason ? `<span class="audit-seal-badge">🛡️ ${r.superadmin_edit_reason}</span>` : `<span style="color: var(--text-dim); font-size: 11px;">अपरिवर्तनीय मूल रिकॉर्ड</span>`;

                    html += `
                        <tr>
                            <td>
                                <strong>${r.full_name}</strong><br>
                                <span style="font-size: 11px; color: var(--text-dim);">${r.phone}</span>
                            </td>
                            <td>${r.duty_date}</td>
                            <td><strong style="color: var(--primary-light);">${r.punch_in_time ? r.punch_in_time.substring(11, 19) : '--'}</strong></td>
                            <td style="max-width: 320px; font-size: 12px; line-height: 1.4;">${r.punch_in_location_text || 'Site Coordinates'}</td>
                            <td>${mockStatus}</td>
                            <td style="max-width: 280px;">${auditSeal}</td>
                            <td>
                                <button onclick="openAuditModal(${r.id}, '${r.punch_in_time}')" style="background: transparent; border: 1px solid var(--border-color); color: var(--text-muted); border-radius: 6px; padding: 4px 8px; font-size: 11px; cursor: pointer;">
                                    ✏️ ऑडिट सील
                                </button>
                            </td>
                        </tr>
                    `;
                });

                tbody.innerHTML = html;
            } catch (err) {
                console.error("Ledger load error:", err);
            }
        }

        // 6. Load Lifts for current tenant
        async function loadLiftsForTenant() {
            try {
                const resp = await fetch(`${API_BASE}/api_dispatch.php?action=list_lifts&tenant_id=${currentTenantId}`);
                const data = await resp.json();
                if (data.success && data.lifts) {
                    availableLifts = data.lifts;
                    document.getElementById('kpiTotalLifts').textContent = data.lifts.length;
                    
                    const modalSel = document.getElementById('modalLiftSelect');
                    if (modalSel) {
                        modalSel.innerHTML = data.lifts.map(l => `<option value="${l.id}">${l.building_name} (${l.lift_unique_code} - ${l.controller_brand})</option>`).join('');
                    }
                }
            } catch (err) {
                console.error("Lifts load error:", err);
            }
        }

        // 7. Modals
        function openNewTicketModal() {
            document.getElementById('newTicketModal').style.display = 'flex';
        }

        function closeNewTicketModal() {
            document.getElementById('newTicketModal').style.display = 'none';
        }

        async function handleCreateTicket(e) {
            e.preventDefault();
            const liftId = document.getElementById('modalLiftSelect').value;
            const desc = document.getElementById('modalIssueDesc').value;
            const autoDisp = document.getElementById('modalAutoDispatch').value === '1';

            try {
                const resp = await fetch(`${API_BASE}/api_dispatch.php?action=create_breakdown`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        action: 'create_breakdown',
                        tenant_id: currentTenantId,
                        lift_id: liftId,
                        reported_by_id: 1,
                        description: desc,
                        auto_dispatch: autoDisp
                    })
                });
                const res = await resp.json();
                if (res.success) {
                    closeNewTicketModal();
                    document.getElementById('modalIssueDesc').value = '';
                    reloadDashboardData();
                } else {
                    alert(res.error || 'त्रुटि');
                }
            } catch (err) {
                console.error("Create ticket error:", err);
            }
        }

        function openAuditModal(recordId, currentPunchIn) {
            document.getElementById('auditRecordId').value = recordId;
            document.getElementById('auditNewPunchIn').value = currentPunchIn;
            document.getElementById('auditReason').value = 'SuperAdmin Ankit Chaudhary certified on-site emergency maintenance audit';
            document.getElementById('auditEditModal').style.display = 'flex';
        }

        function closeAuditModal() {
            document.getElementById('auditEditModal').style.display = 'none';
        }

        async function handleSuperAdminAuditEdit(e) {
            e.preventDefault();
            const recId = document.getElementById('auditRecordId').value;
            const newPunch = document.getElementById('auditNewPunchIn').value;
            const reason = document.getElementById('auditReason').value;

            try {
                const resp = await fetch(`${API_BASE}/api_telemetry.php?action=superadmin_edit_attendance`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        action: 'superadmin_edit_attendance',
                        caller_phone: SUPER_ADMIN_PHONE,
                        record_id: recId,
                        punch_in_time: newPunch,
                        edit_reason: reason
                    })
                });
                const res = await resp.json();
                if (res.success) {
                    closeAuditModal();
                    loadAttendanceLedger();
                } else {
                    alert(res.error || 'ऑडिट संपादन त्रुटि');
                }
            } catch (err) {
                console.error("Audit edit error:", err);
            }
        }

        // Initialize on page load
        window.addEventListener('DOMContentLoaded', () => {
            reloadDashboardData();
            // 15-second background auto-refresh
            setInterval(() => {
                reloadDashboardData();
            }, 15000);
        });
    </script>
</body>
</html>
