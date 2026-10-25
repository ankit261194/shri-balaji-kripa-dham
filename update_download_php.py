path = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\backend\download.php"
with open(path, "r", encoding="utf-8") as f:
    text = f.read().replace("\r\n", "\n")

old_block = """    $targetFile = '';
    // 1. Check exact manifest version on local server
    $exactFile = "$elevexDir/Ankit_EleveX_v{$latestVer}_Final.apk";
    if (file_exists($exactFile) && filesize($exactFile) > 10000000) {
        $targetFile = $exactFile;
    } else {
        // 2. Fallback: Natural sort of all APKs descending (latest version always first)
        $files = glob("$elevexDir/Ankit_EleveX*.apk");
        if (!empty($files)) {
            natsort($files);
            $files = array_reverse($files);
            foreach ($files as $f) {
                if (filesize($f) > 10000000) {
                    $targetFile = $f;
                    break;
                }
            }
        }
    }

    if (!empty($targetFile) && file_exists($targetFile)) {
        $filesize = filesize($targetFile);
        $filename = basename($targetFile);

        while (ob_get_level()) ob_end_clean();
        header("Content-Type: application/vnd.android.package-archive");
        header("Content-Disposition: attachment; filename=\\"{$filename}\\"");
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

    // High-Speed GitHub Releases Cloud Mirror Fallback (Always Guaranteed & Dynamic)
    header("Location: https://github.com/ankit261194/ankits-liftramban/releases/download/v{$latestVer}/Ankit_EleveX_v{$latestVer}_Final.apk");
    exit;"""

new_block = """    $targetFile = '';
    // 1. Check exact manifest version on local server
    $exactFile = "$elevexDir/Ankit_EleveX_v{$latestVer}_Final.apk";
    if (file_exists($exactFile) && filesize($exactFile) > 10000000) {
        $targetFile = $exactFile;
    }

    // 2. Fallback: Check standard Release APK
    if (empty($targetFile)) {
        $relFile = "$elevexDir/Ankit_EleveX_Release.apk";
        if (file_exists($relFile) && filesize($relFile) > 10000000) {
            $targetFile = $relFile;
        }
    }

    // 3. Fallback: Natural sort of all APKs descending (latest version always first)
    if (empty($targetFile)) {
        $files = glob("$elevexDir/Ankit_EleveX*.apk");
        if (!empty($files)) {
            natsort($files);
            $files = array_reverse($files);
            foreach ($files as $f) {
                if (filesize($f) > 10000000) {
                    $targetFile = $f;
                    break;
                }
            }
        }
    }

    // 4. Fallback: Check root downloads directory
    if (empty($targetFile)) {
        $files = glob(__DIR__ . '/downloads/Ankit_EleveX*.apk');
        if (!empty($files)) {
            natsort($files);
            $files = array_reverse($files);
            foreach ($files as $f) {
                if (filesize($f) > 10000000) {
                    $targetFile = $f;
                    break;
                }
            }
        }
    }

    if (!empty($targetFile) && file_exists($targetFile)) {
        $filesize = filesize($targetFile);
        $filename = "Ankit_EleveX_v{$latestVer}_Final.apk";

        while (ob_get_level()) ob_end_clean();
        header("Content-Type: application/vnd.android.package-archive");
        header("Content-Disposition: attachment; filename=\\"{$filename}\\"");
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

    // Graceful 404 response if no APK is found on server - NEVER redirect to broken GitHub URL
    http_response_code(404);
    header('Content-Type: text/html; charset=utf-8');
    echo "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Update In Progress</title></head><body style='font-family:sans-serif;text-align:center;padding:50px;'><h2>अपडेट फ़ाइल सर्वर पर तैयार हो रही है</h2><p>कृपया 2 मिनट बाद पुनः प्रयास करें।</p></body></html>";
    exit;"""

assert old_block in text, "old_block not found in download.php"
text = text.replace(old_block, new_block, 1)

with open(path, "w", encoding="utf-8", newline="") as f:
    f.write(text.replace("\n", "\r\n"))

print("Updated download.php!")
