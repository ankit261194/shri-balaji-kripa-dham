path = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\backend\sync_elevex_apk.php"
with open(path, "r", encoding="utf-8") as f:
    text = f.read().replace("\r\n", "\n")

old_code = """// 1. Status Check
if (isset($_GET['status'])) {
    $exists = file_exists($targetFile);
    $size = $exists ? filesize($targetFile) : 0;
    echo json_encode([
        "success" => true,
        "filename" => $apkFilename,
        "exists" => $exists,
        "size" => $size,
        "size_mb" => round($size / (1024 * 1024), 2) . " MB",
        "last_modified" => $exists ? date("Y-m-d H:i:s", filemtime($targetFile)) : null
    ]);
    exit;
}"""

new_code = """// 1. Status Check
if (isset($_GET['status'])) {
    $exists = file_exists($targetFile);
    $size = $exists ? filesize($targetFile) : 0;
    $allFiles = glob($targetDir . '/*');
    echo json_encode([
        "success" => true,
        "filename" => $apkFilename,
        "targetDir" => $targetDir,
        "targetFile" => $targetFile,
        "exists" => $exists,
        "size" => $size,
        "size_mb" => round($size / (1024 * 1024), 2) . " MB",
        "last_modified" => $exists ? date("Y-m-d H:i:s", filemtime($targetFile)) : null,
        "all_files" => $allFiles ? array_map(function($f) {
            return basename($f) . ' (' . round(filesize($f)/(1024*1024), 2) . ' MB)';
        }, $allFiles) : []
    ]);
    exit;
}"""

assert old_code in text, "old_code not found"
text = text.replace(old_code, new_code, 1)

with open(path, "w", encoding="utf-8", newline="") as f:
    f.write(text.replace("\n", "\r\n"))

print("Updated sync_elevex_apk.php!")
