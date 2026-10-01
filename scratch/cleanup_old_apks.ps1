# Automated Cleanup Script for Shri Balaji Kripa Dham APKs
# Keeps ONLY the latest release APK and purges all older versions across the project.

$projectRoot = Split-Path -Parent $PSScriptRoot
$releaseApkPath = Join-Path $projectRoot "backend\downloads\ShriBalajiKripaDham-release.apk"
$buildApkPath = Join-Path $projectRoot "app\build\outputs\apk\release\app-release.apk"

$allApks = Get-ChildItem -Path $projectRoot -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue

$deletedCount = 0
$deletedBytes = 0

foreach ($apk in $allApks) {
    if ($apk.FullName -ne $releaseApkPath -and $apk.FullName -ne $buildApkPath) {
        $deletedBytes += $apk.Length
        Remove-Item $apk.FullName -Force -ErrorAction SilentlyContinue
        $deletedCount++
    }
}

$mb = [math]::Round($deletedBytes / 1MB, 2)
Write-Host "Cleanup complete: Removed $deletedCount obsolete APK(s) ($mb MB freed)."
