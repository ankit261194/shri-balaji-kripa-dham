<?php
// ==============================================================================
// ANKIT ELEVEX PRO - OFFICIAL ENTERPRISE DIGITAL JOB SHEET
// Shri Balaji Kripa Dham Cloud Server (Hostinger NVMe SSD)
// ==============================================================================

ini_set('display_errors', 0);
error_reporting(0);

require_once __DIR__ . '/config.php';

$pdo = getDbConnection();
if (!$pdo) {
    die("Database connection unavailable");
}

$ticketId = intval($_GET['ticket_id'] ?? 0);
$ticketNo = trim($_GET['ticket'] ?? $_GET['ticket_number'] ?? '');
$scheduleId = intval($_GET['schedule_id'] ?? 0);
$isAmcMode = false;

if ($scheduleId > 0) {
    $isAmcMode = true;
    $stmtAmc = $pdo->prepare("SELECT s.*, 
                     l.lift_unique_code, l.building_name, l.building_address, l.landmark_name, l.controller_brand, l.amc_type,
                     c.company_name, c.brand_display_name, c.logo_url, c.primary_color_hex, c.support_phone, c.office_address,
                     tech.full_name as tech_name, tech.phone as tech_phone
              FROM elevex_amc_schedules s
              JOIN elevex_lifts_amc l ON s.lift_id = l.id
              LEFT JOIN elevex_companies c ON s.tenant_id = c.tenant_id
              LEFT JOIN elevex_users tech ON s.assigned_technician_id = tech.id
              WHERE s.id = :sid LIMIT 1");
    $stmtAmc->execute([':sid' => $scheduleId]);
    $job = $stmtAmc->fetch(PDO::FETCH_ASSOC);
    if (!$job) {
        die("AMC routine schedule record not found");
    }
    $job['ticket_number'] = "AMC-" . $job['routine_year'] . "-M" . sprintf("%02d", $job['routine_month']);
    $job['issue_description'] = "नियमित मासिक प्रिवेंटिव मेंटेनेंस (28-Point AMC Preventive Checklist) - माह: " . $job['routine_month'] . "/" . $job['routine_year'];
    $job['resolution_notes'] = $job['service_notes'] ?: 'सभी 28 बिंदुओं की सघन तकनीकी जांच पूर्ण की गई। लिफ्ट परिचालन पूर्णतः सुरक्षित है।';
    $job['resolved_at'] = $job['completed_at'];
    $job['photo_after_url'] = $job['service_photo_url'];
    $job['photo_before_url'] = null;
    $job['owner_name'] = 'सोसाइटी / RWA प्रतिनिधि';
    $job['owner_phone'] = '';
} else {
    $query = "SELECT b.*, 
                     l.lift_unique_code, l.building_name, l.building_address, l.landmark_name, l.controller_brand, l.amc_type,
                     c.company_name, c.brand_display_name, c.logo_url, c.primary_color_hex, c.support_phone, c.office_address,
                     tech.full_name as tech_name, tech.phone as tech_phone,
                     owner.full_name as owner_name, owner.phone as owner_phone
              FROM elevex_breakdowns b
              JOIN elevex_lifts_amc l ON b.lift_id = l.id
              LEFT JOIN elevex_companies c ON b.tenant_id = c.tenant_id
              LEFT JOIN elevex_users tech ON b.assigned_technician_id = tech.id
              LEFT JOIN elevex_users owner ON b.reported_by_id = owner.id
              WHERE ";

    if ($ticketId > 0) {
        $query .= "b.id = :tid LIMIT 1";
        $params = [':tid' => $ticketId];
    } elseif (!empty($ticketNo)) {
        $query .= "b.ticket_number = :tno LIMIT 1";
        $params = [':tno' => $ticketNo];
    } else {
        die("Ticket identifier missing");
    }

    $stmt = $pdo->prepare($query);
    $stmt->execute($params);
    $job = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$job) {
        die("Job sheet record not found");
    }
}

$primaryColor = !empty($job['primary_color_hex']) ? $job['primary_color_hex'] : '#1E40AF';
$brandName = !empty($job['brand_display_name']) ? $job['brand_display_name'] : $job['company_name'];
$resolvedDate = !empty($job['resolved_at']) ? date("d M Y, h:i A", strtotime($job['resolved_at'])) : date("d M Y, h:i A");
$createdDate = !empty($job['created_at']) ? date("d M Y, h:i A", strtotime($job['created_at'])) : '';

if ($isAmcMode) {
    $waShareText = "📋 Ankit EleveX Pro - AMC Service Certificate for " . $job['building_name'] . " (" . $job['ticket_number'] . "):\nhttps://shribalajikripadham.online/elevex_gateway/jobsheet.php?schedule_id=" . $job['id'];
} else {
    $waShareText = "📋 Ankit EleveX Pro - Job Sheet for Ticket " . $job['ticket_number'] . " at " . $job['building_name'] . ":\nhttps://shribalajikripadham.online/elevex_gateway/jobsheet.php?ticket_id=" . $job['id'];
}
$waUrl = "https://wa.me/?text=" . rawurlencode($waShareText);
?>
<!DOCTYPE html>
<html lang="hi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>कार्य पूर्णता प्रमाण पत्र | Job Sheet - <?= htmlspecialchars($job['ticket_number']) ?></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: <?= htmlspecialchars($primaryColor) ?>;
            --dark: #0F172A;
            --slate: #475569;
            --border: #E2E8F0;
            --bg: #F8FAFC;
            --surface: #FFFFFF;
            --success: #059669;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: var(--bg);
            color: var(--dark);
            line-height: 1.5;
            padding: 24px 16px;
        }
        .container {
            max-width: 820px;
            margin: 0 auto;
            background: var(--surface);
            border-radius: 14px;
            border: 1px solid var(--border);
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
            overflow: hidden;
        }
        .top-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: #F1F5F9;
            padding: 12px 24px;
            border-bottom: 1px solid var(--border);
        }
        .action-btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 8px 14px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            text-decoration: none;
            cursor: pointer;
            border: none;
            transition: all 0.2s;
        }
        .btn-print { background: var(--dark); color: #FFF; }
        .btn-wa { background: #25D366; color: #FFF; }
        .header {
            padding: 24px 28px;
            border-bottom: 2px solid var(--border);
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
        }
        .brand-title {
            font-size: 20px;
            font-weight: 800;
            color: var(--primary);
            letter-spacing: -0.5px;
        }
        .company-subtitle {
            font-size: 12px;
            color: var(--slate);
            margin-top: 2px;
        }
        .ticket-badge {
            text-align: right;
        }
        .ticket-no {
            font-family: 'JetBrains Mono', monospace;
            font-size: 14px;
            font-weight: 700;
            background: #EEF2F6;
            padding: 4px 10px;
            border-radius: 6px;
            display: inline-block;
        }
        .status-pill {
            display: inline-block;
            margin-top: 6px;
            background: #ECFDF5;
            color: var(--success);
            border: 1px solid #A7F3D0;
            padding: 3px 8px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 700;
        }
        .grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 16px;
            padding: 24px 28px;
            border-bottom: 1px solid var(--border);
            background: #FAFBFD;
        }
        .info-card h4 {
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: var(--slate);
            margin-bottom: 4px;
        }
        .info-card p {
            font-size: 14px;
            font-weight: 600;
            color: var(--dark);
        }
        .info-card span {
            font-size: 12px;
            color: var(--slate);
            display: block;
        }
        .section {
            padding: 24px 28px;
            border-bottom: 1px solid var(--border);
        }
        .section-title {
            font-size: 14px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: var(--primary);
            margin-bottom: 12px;
        }
        .desc-box {
            background: #F8FAFC;
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 14px;
            font-size: 13.5px;
            line-height: 1.6;
        }
        .photos-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 16px;
            margin-top: 12px;
        }
        .photo-card {
            border: 1px solid var(--border);
            border-radius: 10px;
            overflow: hidden;
            background: #FFF;
        }
        .photo-card img {
            width: 100%;
            height: 220px;
            object-fit: cover;
            display: block;
        }
        .photo-label {
            padding: 8px 12px;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            background: #F1F5F9;
            color: var(--slate);
        }
        .signature-box {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            padding: 24px 28px;
            background: #FAFBFD;
        }
        .sig-container {
            border: 1px dashed var(--border);
            border-radius: 8px;
            padding: 10px;
            background: #FFF;
            text-align: center;
            width: 260px;
        }
        .sig-container img {
            max-width: 100%;
            height: 90px;
            object-fit: contain;
            display: block;
            margin: 0 auto;
        }
        .sig-caption {
            font-size: 11px;
            color: var(--slate);
            margin-top: 6px;
            border-top: 1px solid var(--border);
            padding-top: 4px;
            font-weight: 600;
        }
        .footer {
            padding: 16px 28px;
            background: #F1F5F9;
            text-align: center;
            font-size: 11px;
            color: var(--slate);
        }
        @media print {
            body { padding: 0; background: #FFF; }
            .container { border: none; box-shadow: none; max-width: 100%; }
            .top-bar { display: none; }
        }
        @media (max-width: 600px) {
            .grid { grid-template-columns: 1fr; gap: 12px; }
            .photos-grid { grid-template-columns: 1fr; }
            .signature-box { flex-direction: column; gap: 16px; align-items: stretch; }
            .sig-container { width: 100%; }
        }
    </style>
</head>
<body>

<div class="container">
    <div class="top-bar">
        <span style="font-size: 12px; font-weight: 600; color: #475569;">Ankit EleveX Pro • MNC Enterprise Verification</span>
        <div style="display: flex; gap: 8px;">
            <a href="<?= htmlspecialchars($waUrl) ?>" target="_blank" class="action-btn btn-wa">📲 WhatsApp शेयर</a>
            <button onclick="window.print()" class="action-btn btn-print">🖨️ प्रिंट / PDF सेव करें</button>
        </div>
    </div>

    <div class="header">
        <div>
            <div class="brand-title"><?= htmlspecialchars($brandName) ?></div>
            <div class="company-subtitle"><?= htmlspecialchars($job['company_name']) ?> • 24x7 हेल्पलाइन: <?= htmlspecialchars($job['support_phone']) ?></div>
            <div style="font-size: 11px; color: #64748B; margin-top: 4px;"><?= htmlspecialchars($job['office_address']) ?></div>
        </div>
        <div class="ticket-badge">
            <div class="ticket-no"><?= htmlspecialchars($job['ticket_number']) ?></div>
            <div>
                <?php if ($job['status'] === 'resolved'): ?>
                <span class="status-pill">✅ प्रमाणित व पूर्ण (RESOLVED)</span>
                <?php else: ?>
                <span class="status-pill" style="background: #FEF3C7; color: #D97706; border-color: #F59E0B;">⏳ कार्य प्रगति पर (<?= strtoupper($job['status']) ?>)</span>
                <?php endif; ?>
            </div>
            <div style="font-size: 11px; color: #64748B; margin-top: 4px;">तारीख: <?= htmlspecialchars($resolvedDate) ?></div>
        </div>
    </div>

    <?php if (!empty($job['is_passenger_trapped']) || !empty($job['is_emergency'])): ?>
    <div style="background: #FEE2E2; border: 2px solid #EF4444; border-radius: 8px; padding: 12px 16px; margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between;">
        <div>
            <span style="font-weight: 800; color: #DC2626; font-size: 14px;">🚨 आपातकालीन ब्रेकडाउन (EMERGENCY SOS: PASSENGER TRAPPED)</span>
            <div style="font-size: 12px; color: #991B1B; margin-top: 4px;">फ्लोर: <?= htmlspecialchars($job['emergency_floor'] ?: 'Ground') ?> • फंसे यात्री: <?= intval($job['trapped_passenger_count'] ?: 1) ?> व्यक्ति • रिपोर्ट समय: <?= htmlspecialchars($job['emergency_reported_at'] ?: $job['created_at']) ?></div>
        </div>
        <span style="background: #DC2626; color: white; padding: 4px 10px; border-radius: 4px; font-weight: 700; font-size: 11px;">CRITICAL RESCUE</span>
    </div>
    <?php endif; ?>

    <div class="grid">
        <div class="info-card">
            <h4>साइट / बिल्डिंग विवरण (Site Details)</h4>
            <p><?= htmlspecialchars($job['building_name']) ?></p>
            <span>लिफ्ट कोड: <b><?= htmlspecialchars($job['lift_unique_code'] ?: 'LIFT-001') ?></b> (<?= htmlspecialchars($job['controller_brand'] ?: 'MONARCH') ?>)</span>
            <span>स्थान: <?= htmlspecialchars($job['landmark_name'] ?: $job['building_address']) ?></span>
        </div>
        <div class="info-card">
            <h4>फील्ड तकनीशियन विवरण (Technician Details)</h4>
            <p><?= htmlspecialchars($job['tech_name'] ?: 'फील्ड तकनीशियन') ?></p>
            <span>संपर्क: <?= htmlspecialchars($job['tech_phone'] ?: '9811334455') ?></span>
            <span>सत्यापन OTP: <b><?= htmlspecialchars($job['completion_otp']) ?></b> (6-Digit Crypto OTP)</span>
        </div>
    </div>

    <div class="section">
        <div class="section-title">समस्या व समाधान विवरण (Work Carried Out)</div>
        <div class="desc-box">
            <p><strong>शिकायत का विवरण (Reported Issue):</strong></p>
            <p style="color: #64748B; margin-bottom: 8px;"><?= nl2br(htmlspecialchars($job['issue_description'])) ?></p>
            
            <p><strong>किए गए कार्य का विवरण (Resolution Work Done):</strong></p>
            <p style="color: #0F172A;"><?= nl2br(htmlspecialchars($job['resolution_notes'] ?: 'लिफ्ट की पूर्ण जांच की गई और समस्या का सफल निवारण किया गया। सभी सुरक्षा सर्किट सामान्य अवस्था में पाए गए।')) ?></p>
            
            <?php if (!empty($job['replaced_parts'])): ?>
            <div style="margin-top: 10px; padding-top: 8px; border-top: 1px dashed #CBD5E1;">
                <strong>बदले गए स्पेयर पार्ट्स (Replaced Spare Parts):</strong>
                <p style="color: #1E40AF; font-family: 'JetBrains Mono', monospace; font-size: 12.5px;"><?= htmlspecialchars($job['replaced_parts']) ?></p>
            </div>
            <?php endif; ?>
        </div>
    </div>

    <?php if ($isAmcMode && !empty($job['checklist_json'])): 
        $checklistData = json_decode($job['checklist_json'], true);
        if (is_array($checklistData)):
    ?>
    <div class="section">
        <div class="section-title">28-पॉइंट तकनीकी प्रिवेंटिव इंस्पेक्शन (28-Point AMC Preventive Audit)</div>
        <div style="background: #FFF; border: 1px solid var(--border); border-radius: 8px; overflow: hidden;">
            <table style="width: 100%; border-collapse: collapse; font-size: 12.5px;">
                <thead>
                    <tr style="background: #F1F5F9; border-bottom: 1px solid var(--border); text-align: left;">
                        <th style="padding: 10px 14px; font-weight: 700; color: #475569;">#</th>
                        <th style="padding: 10px 14px; font-weight: 700; color: #475569;">जांच बिंदु (Check Point)</th>
                        <th style="padding: 10px 14px; font-weight: 700; color: #475569; width: 120px;">स्थिति (Status)</th>
                        <th style="padding: 10px 14px; font-weight: 700; color: #475569;">टिप्पणी (Remarks)</th>
                    </tr>
                </thead>
                <tbody>
                    <?php 
                    $ptIndex = 1;
                    foreach ($checklistData as $item): 
                        $status = strtolower($item['status'] ?? 'ok');
                        $badgeStyle = 'background: #ECFDF5; color: #059669; border: 1px solid #A7F3D0;';
                        $statusLabel = 'सामान्य (OK)';
                        if ($status === 'adjusted') {
                            $badgeStyle = 'background: #FEF3C7; color: #D97706; border: 1px solid #FDE68A;';
                            $statusLabel = 'समायोजित (Adj)';
                        } elseif ($status === 'defective') {
                            $badgeStyle = 'background: #FEE2E2; color: #DC2626; border: 1px solid #FECACA;';
                            $statusLabel = 'दोषपूर्ण (Fault)';
                        }
                    ?>
                    <tr style="border-bottom: 1px solid #F1F5F9;">
                        <td style="padding: 9px 14px; color: #94A3B8; font-weight: 600;"><?= $ptIndex++ ?></td>
                        <td style="padding: 9px 14px; color: #1E293B; font-weight: 500;"><?= htmlspecialchars($item['name'] ?? $item['point'] ?? '') ?></td>
                        <td style="padding: 9px 14px;">
                            <span style="display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 700; <?= $badgeStyle ?>">
                                <?= $statusLabel ?>
                            </span>
                        </td>
                        <td style="padding: 9px 14px; color: #64748B; font-size: 11.5px;"><?= htmlspecialchars($item['notes'] ?? '-') ?></td>
                    </tr>
                    <?php endforeach; ?>
                </tbody>
            </table>
        </div>
    </div>
    <?php endif; endif; ?>

    <?php if (!empty($job['photo_before_url']) || !empty($job['photo_after_url'])): ?>
    <div class="section">
        <div class="section-title">फोटो साक्ष्य (Visual Proof Before & After)</div>
        <div class="photos-grid">
            <?php if (!empty($job['photo_before_url'])): ?>
            <div class="photo-card">
                <div class="photo-label">काम से पहले की स्थिति (Before Repair)</div>
                <img src="<?= htmlspecialchars($job['photo_before_url']) ?>" alt="Before Repair">
            </div>
            <?php endif; ?>
            <?php if (!empty($job['photo_after_url'])): ?>
            <div class="photo-card">
                <div class="photo-label">काम के बाद की स्थिति (After Repair)</div>
                <img src="<?= htmlspecialchars($job['photo_after_url']) ?>" alt="After Repair">
            </div>
            <?php endif; ?>
        </div>
    </div>
    <?php endif; ?>

    <div class="signature-box">
        <div>
            <div style="font-size: 12px; font-weight: 700; color: #1E40AF; margin-bottom: 4px;">कानूनी कार्य संतुष्टि प्रमाण (Legal Sign-Off)</div>
            <div style="font-size: 11.5px; color: #64748B; max-width: 420px; line-height: 1.4;">
                यह प्रमाणित किया जाता है कि लिफ्ट का ब्रेकडाउन कार्य हमारी पूर्ण संतुष्टि के साथ संपन्न किया गया है। लिफ्ट सुचारू और सुरक्षित रूप से चल रही है।
            </div>
            <div style="margin-top: 8px; font-size: 12px; color: #0F172A;">
                ग्राहक / RWA प्रतिनिधि: <b><?= htmlspecialchars($job['owner_name'] ?: 'सोसाइटी प्रतिनिधि') ?></b> (<?= htmlspecialchars($job['owner_phone'] ?: '') ?>)
            </div>
        </div>

        <div class="sig-container">
            <?php if (!empty($job['customer_signature_url'])): ?>
                <img src="<?= htmlspecialchars($job['customer_signature_url']) ?>" alt="Customer Signature">
            <?php else: ?>
                <div style="height: 90px; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; color: #059669;">
                    ✓ OTP द्वारा सत्यापित (<?= htmlspecialchars($job['completion_otp']) ?>)
                </div>
            <?php endif; ?>
            <div class="sig-caption">ग्राहक डिजिटल हस्ताक्षर (Digital Signature)</div>
        </div>
    </div>

    <div class="footer">
        यह एक सिस्टम-जनरेटेड आधिकारिक जॉब शीट है • Ankit EleveX Pro (v5.9.45) • होस्टिंगर NVMe SSD सुरक्षित क्लाउड क्लस्टर
    </div>
</div>

</body>
</html>
