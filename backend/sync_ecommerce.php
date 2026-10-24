<?php
// ==============================================================================
// AuraMarket Multi-Vendor Commerce Platform - Autonomous Cloud Asset Deployer
// Strictly syncs ecommerce-demo files directly from GitHub into /ecommerce-demo/
// Leaves root domain & temple files 100% untouched.
// ==============================================================================

ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");
header("Pragma: no-cache");

$repoRawBase = "https://raw.githubusercontent.com/ankit261194/ecommerce-demo/main/ecommerce-demo/";
$targetBaseDir = __DIR__ . '/ecommerce-demo';

if (!is_dir($targetBaseDir)) {
    @mkdir($targetBaseDir, 0755, true);
}

$files = [
    "index.html",
    "manifest.json",
    "robots.txt",
    "sitemap.xml",
    "sw.js",
    "HOW_TO_UPLOAD_TO_CPANEL.txt",
    "api/.htaccess",
    "api/data.json",
    "api/index.php",
    "assets/BentoGrid-BEJ22lZW.js",
    "assets/CameraBarcodeScannerModal-Dyfq1uX7.js",
    "assets/CourierDashboard-hjx-FSWu.js",
    "assets/CustomerStorefront-Dx5coxKB.js",
    "assets/DemoHub-Cw7VLFNo.js",
    "assets/InstallWizard-BiDaQRq8.js",
    "assets/InventoryAlertsModal-Lx9qvVnc.js",
    "assets/SlideOverDrawer-DmmRCURG.js",
    "assets/SuperAdminDashboard-jLlnk_Fa.js",
    "assets/TaxInvoiceModal-Dah49oJR.js",
    "assets/VendorPortal-CqiNBG2w.js",
    "assets/VendorPublicStoreModal-BlHi8YcW.js",
    "assets/feature-crossborder-recs-CTZThTUK.js",
    "assets/feature-escrow-engine-qfCiB9Pb.js",
    "assets/feature-fraud-engine-DK1sCz6c.js",
    "assets/feature-tax-compliance-CAVEt73r.js",
    "assets/feature-vendor-subscription-DttKQRQ3.js",
    "assets/feature-video-shopping-DhTnMjOy.js",
    "assets/index-CZAMOnsB.js",
    "assets/index-CiqTCk71.css",
    "assets/vendor-misc-nwr-sXqd.js",
    "assets/vendor-network-BcNqe6-g.js",
    "assets/vendor-react-rxyxaCp0.js"
];

$updated = [];
$failed = [];

function fetchRemote($url) {
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 20);
        curl_setopt($ch, CURLOPT_USERAGENT, "AuraMarket-Deployer/1.0");
        $res = curl_exec($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        if ($code === 200 && $res !== false) return $res;
    }
    return @file_get_contents($url);
}

foreach ($files as $rel) {
    $remoteUrl = $repoRawBase . $rel . "?t=" . time();
    $content = fetchRemote($remoteUrl);
    if ($content !== false && strlen($content) > 0) {
        $dest = $targetBaseDir . '/' . $rel;
        $d = dirname($dest);
        if (!is_dir($d)) {
            @mkdir($d, 0755, true);
        }
        if (@file_put_contents($dest, $content) !== false) {
            $updated[] = $rel . " (" . strlen($content) . " bytes)";
        } else {
            $failed[] = $rel . " (write failed)";
        }
    } else {
        $failed[] = $rel . " (fetch failed from " . $remoteUrl . ")";
    }
}

if (function_exists('opcache_reset')) {
    @opcache_reset();
}

echo json_encode([
    "success" => count($failed) === 0,
    "status" => "AURA_MARKET_DEMO_SYNC_COMPLETE",
    "message" => "AuraMarket e-commerce demo assets successfully deployed to /ecommerce-demo/",
    "synced_count" => count($updated),
    "failed_count" => count($failed),
    "updated" => $updated,
    "failed" => $failed,
    "demo_url" => "https://shribalajikripadham.online/ecommerce-demo/",
    "timestamp" => time()
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
