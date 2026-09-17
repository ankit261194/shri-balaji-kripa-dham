<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक हाई-स्पीड APK डाउनलोड सेवा
// ==============================================================================

$currentVersion = "v2.44.0";
$apkFileName = "ShriBalajiKripaDham-v2.44.0.apk";
$githubCdnUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/{$currentVersion}/{$apkFileName}";

// Redirect directly to GitHub Global High-Speed CDN (500Mbps+) for instant download
header("HTTP/1.1 302 Found");
header("Location: " . $githubCdnUrl);
header("Cache-Control: no-cache, must-revalidate");
exit;
