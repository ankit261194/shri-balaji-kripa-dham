<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - आधिकारिक APK डाउनलोड सेवा
// ==============================================================================

$currentVersion = "v2.44.0";
$apkFileName = "ShriBalajiKripaDham-v2.44.0.apk";
$localApkPath = __DIR__ . "/" . $apkFileName;
$altLocalApk = __DIR__ . "/app.apk";
$altReleaseApk = __DIR__ . "/ShriBalajiKripaDham-release.apk";
$fallbackGithubUrl = "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/{$currentVersion}/{$apkFileName}";

// If APK exists locally on server with valid size (> 20MB)
if (file_exists($localApkPath) && filesize($localApkPath) > 20000000) {
    header('Content-Description: File Transfer');
    header('Content-Type: application/vnd.android.package-archive');
    header('Content-Disposition: attachment; filename="' . $apkFileName . '"');
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    header('Content-Length: ' . filesize($localApkPath));
    readfile($localApkPath);
    exit;
} elseif (file_exists($altLocalApk) && filesize($altLocalApk) > 20000000) {
    header('Content-Description: File Transfer');
    header('Content-Type: application/vnd.android.package-archive');
    header('Content-Disposition: attachment; filename="' . $apkFileName . '"');
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    header('Content-Length: ' . filesize($altLocalApk));
    readfile($altLocalApk);
    exit;
} elseif (file_exists($altReleaseApk) && filesize($altReleaseApk) > 20000000) {
    header('Content-Description: File Transfer');
    header('Content-Type: application/vnd.android.package-archive');
    header('Content-Disposition: attachment; filename="' . $apkFileName . '"');
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    header('Content-Length: ' . filesize($altReleaseApk));
    readfile($altReleaseApk);
    exit;
} else {
    // 302 Redirect to High-Speed GitHub Release CDN
    header("Location: " . $fallbackGithubUrl, true, 302);
    exit;
}
