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

// Clean old assets if exists to prevent stale chunks
$assetsDir = $targetBaseDir . '/assets';
if (is_dir($assetsDir)) {
    foreach (glob($assetsDir . '/*') as $f) {
        if (is_file($f)) @unlink($f);
    }
}

$files = [
    "api/.htaccess",
    "api/data.json",
    "api/index.php",
    "assets/BentoGrid-7cxG5yvw.js",
    "assets/CameraBarcodeScannerModal-1qBMXYUg.js",
    "assets/CourierDashboard-D5e7zbnp.js",
    "assets/CustomerStorefront-DMQ7yWJC.js",
    "assets/DemoHub-CbNEiEe_.js",
    "assets/feature-crossborder-recs-BoSLzOf3.js",
    "assets/feature-escrow-engine-6fWU7g8V.js",
    "assets/feature-fraud-engine-DlAMle6S.js",
    "assets/feature-tax-compliance-DRD7YHOB.js",
    "assets/feature-vendor-subscription-BGLmVyHO.js",
    "assets/feature-video-shopping-3x1k_4L0.js",
    "assets/index-bPvF7c_u.js",
    "assets/index-CiqTCk71.css",
    "assets/InstallWizard-BayutogC.js",
    "assets/InventoryAlertsModal-BzwZc38J.js",
    "assets/SlideOverDrawer-CJR7freH.js",
    "assets/SuperAdminDashboard-MXEFXfs9.js",
    "assets/TaxInvoiceModal-COR3oBBV.js",
    "assets/vendor-misc-nwr-sXqd.js",
    "assets/vendor-network-BcNqe6-g.js",
    "assets/vendor-react-C9PhKaO_.js",
    "assets/VendorPortal-vk1iNpeE.js",
    "assets/VendorPublicStoreModal-DX6y6k2X.js",
    "files.json",
    "HOW_TO_UPLOAD_TO_CPANEL.txt",
    "index.html",
    "manifest.json",
    "robots.txt",
    "sitemap.xml",
    "sw.js"
];

$updated = [];
$failed = [];

function fetchRemote($url) {
    if (function_exists('curl_init')) {
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 25);
        curl_setopt($ch, CURLOPT_USERAGENT, "AuraMarket-Deployer/2.0");
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
