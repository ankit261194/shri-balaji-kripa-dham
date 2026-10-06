<?php
// ==============================================================================
// श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट) - स्वायत्त क्लाउड डेटाबेस बैकअप इंजन
// Automated Cloud MySQL Database Backup & Rolling Disaster Recovery Engine
// Generates full SQL/GZIP dumps, rotates 30-day snapshots, and protects data.
// ==============================================================================

if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} else {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-SBKD-API-KEY, x-sbkd-api-key, X-SBKD-ADMIN-TOKEN");
header("Cache-Control: no-store, no-cache, must-revalidate, max-age=0");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Security: Verify API Auth or Secret Key
$apiKey = $_SERVER['HTTP_X_SBKD_API_KEY'] ?? $_SERVER['HTTP_AUTHORIZATION'] ?? $_GET['key'] ?? $_POST['key'] ?? '';
$isAdmin = false;

if ($apiKey === SBKD_API_SECRET) {
    $isAdmin = true;
} else if (function_exists('verifyApiAuth')) {
    $auth = verifyApiAuth(false);
    if ($auth) $isAdmin = true;
}

if (!$isAdmin) {
    http_response_code(401);
    echo json_encode(["success" => false, "error" => "अनधिकृत अनुरोध: मान्य API कुंजी अनिवार्य है।"], JSON_UNESCAPED_UNICODE);
    exit;
}

$pdo = getDB();
if (!$pdo) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => "Database connection unavailable"], JSON_UNESCAPED_UNICODE);
    exit;
}

// 1. Vault Storage Directory & Security Lock (.htaccess)
$vaultDir = __DIR__ . '/../vault';
if (!is_dir($vaultDir)) {
    @mkdir($vaultDir, 0755, true);
}
$htaccessFile = $vaultDir . '/.htaccess';
if (!file_exists($htaccessFile)) {
    @file_put_contents($htaccessFile, "Order deny,allow\nDeny from all\n");
}
$snapshotsDir = $vaultDir . '/snapshots';
if (!is_dir($snapshotsDir)) {
    @mkdir($snapshotsDir, 0755, true);
}

$action = strtolower(trim($_GET['action'] ?? $_POST['action'] ?? 'backup'));

/**
 * Generates a complete MySQL .sql dump string for all database tables.
 */
function generateFullSqlDump($pdo) {
    $tables = [
        'ashram_settings',
        'tokens',
        'devotee_face_profiles',
        'sevadars',
        'donors',
        'expenses',
        'parchas',
        'admin_sessions',
        'app_queries',
        'havan_bookings',
        'devotee_notifications',
        'daily_darshan',
        'bus_seats',
        'dharamshala_bookings',
        'ashram_statuses'
    ];

    $sql = "-- ====================================================================\n";
    $sql .= "-- श्री बालाजी कृपा धाम (ग्राम डूँगरा जाट, बुलन्दशहर) - संपूर्ण डेटाबेस बैकअप\n";
    $sql .= "-- Generated at: " . date('Y-m-d H:i:s') . " (IST)\n";
    $sql .= "-- Engine: Hostinger MySQL Cloud Backup Engine\n";
    $sql .= "-- ====================================================================\n\n";
    $sql .= "SET FOREIGN_KEY_CHECKS=0;\n";
    $sql .= "SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';\n";
    $sql .= "SET NAMES utf8mb4;\n\n";

    $stats = [];

    foreach ($tables as $table) {
        try {
            // Check if table exists
            $check = $pdo->query("SHOW TABLES LIKE '{$table}'");
            if (!$check || $check->rowCount() === 0) continue;

            // Get Create Table SQL
            $createStmt = $pdo->query("SHOW CREATE TABLE `{$table}`");
            $createRow = $createStmt->fetch(PDO::FETCH_NUM);
            if ($createRow && isset($createRow[1])) {
                $sql .= "-- Table structure for `{$table}`\n";
                $sql .= "DROP TABLE IF EXISTS `{$table}`;\n";
                $sql .= $createRow[1] . ";\n\n";
            }

            // Get Data Rows
            $dataStmt = $pdo->query("SELECT * FROM `{$table}`");
            $rows = $dataStmt->fetchAll(PDO::FETCH_ASSOC);
            $count = count($rows);
            $stats[$table] = $count;

            if ($count > 0) {
                $sql .= "-- Data dumping for `{$table}` ({$count} records)\n";
                $cols = array_keys($rows[0]);
                $colNames = implode('`, `', $cols);

                foreach (array_chunk($rows, 50) as $chunk) {
                    $sql .= "INSERT INTO `{$table}` (`{$colNames}`) VALUES\n";
                    $valuesArr = [];
                    foreach ($chunk as $row) {
                        $escapedValues = array_map(function($val) use ($pdo) {
                            if ($val === null) return 'NULL';
                            return $pdo->quote($val);
                        }, array_values($row));
                        $valuesArr[] = "(" . implode(', ', $escapedValues) . ")";
                    }
                    $sql .= implode(",\n", $valuesArr) . ";\n";
                }
                $sql .= "\n";
            }
        } catch (Exception $e) {
            $stats[$table] = "Error: " . $e->getMessage();
        }
    }

    $sql .= "SET FOREIGN_KEY_CHECKS=1;\n";
    $sql .= "-- Backup Completed Successfully --\n";

    return ['sql' => $sql, 'stats' => $stats];
}

/**
 * Cleans up backups older than 30 days.
 */
function pruneOldSnapshots($dir, $retentionDays = 30) {
    $now = time();
    $pruned = [];
    $files = glob($dir . '/*.*');
    if ($files) {
        foreach ($files as $f) {
            if (is_file($f) && basename($f) !== '.htaccess') {
                if (($now - filemtime($f)) > ($retentionDays * 86400)) {
                    @unlink($f);
                    $pruned[] = basename($f);
                }
            }
        }
    }
    return $pruned;
}

try {
    switch ($action) {
        case 'backup':
        case 'create':
            $dumpResult = generateFullSqlDump($pdo);
            $sqlContent = $dumpResult['sql'];
            $stats = $dumpResult['stats'];

            $dateTag = date('Y-m-d_His');
            $fileName = "sbkd_db_backup_{$dateTag}.sql";
            $gzFileName = "sbkd_db_backup_{$dateTag}.sql.gz";

            $gzPath = $snapshotsDir . '/' . $gzFileName;
            $rawPath = $snapshotsDir . '/' . $fileName;

            // Compress using gzencode if available
            $isCompressed = false;
            if (function_exists('gzencode')) {
                $compressed = gzencode($sqlContent, 9);
                if ($compressed !== false) {
                    file_put_contents($gzPath, $compressed, LOCK_EX);
                    $isCompressed = true;
                    $targetFile = $gzFileName;
                    $targetSize = filesize($gzPath);
                }
            }

            if (!$isCompressed) {
                file_put_contents($rawPath, $sqlContent, LOCK_EX);
                $targetFile = $fileName;
                $targetSize = filesize($rawPath);
            }

            // Prune snapshots older than 30 days
            $pruned = pruneOldSnapshots($snapshotsDir, 30);

            echo json_encode([
                "success" => true,
                "status" => "BACKUP_COMPLETED",
                "message" => "संपूर्ण डेटाबेस बैकअप सफलतापूर्वक सुरक्षित हो गया!",
                "backup_file" => $targetFile,
                "size_bytes" => $targetSize,
                "size_formatted" => round($targetSize / 1024, 2) . " KB",
                "is_compressed" => $isCompressed,
                "records_backed_up" => $stats,
                "retention_policy" => "30 Days Rolling",
                "pruned_old_backups" => $pruned,
                "timestamp" => time()
            ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
            break;

        case 'list':
            $files = glob($snapshotsDir . '/*.*');
            $list = [];
            if ($files) {
                foreach ($files as $f) {
                    if (is_file($f) && basename($f) !== '.htaccess') {
                        $list[] = [
                            "file" => basename($f),
                            "size_bytes" => filesize($f),
                            "size_formatted" => round(filesize($f) / 1024, 2) . " KB",
                            "created_at" => date('Y-m-d H:i:s', filemtime($f)),
                            "age_days" => round((time() - filemtime($f)) / 86400, 1)
                        ];
                    }
                }
                // Sort newest first
                usort($list, function($a, $b) {
                    return strcmp($b['created_at'], $a['created_at']);
                });
            }

            echo json_encode([
                "success" => true,
                "count" => count($list),
                "snapshots" => $list
            ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
            break;

        case 'download':
            $reqFile = basename(trim($_GET['file'] ?? ''));
            if (empty($reqFile)) {
                // Find latest backup
                $files = glob($snapshotsDir . '/*.*');
                if (!$files) {
                    http_response_code(404);
                    echo json_encode(["success" => false, "error" => "कोई बैकअप फाइल उपलब्ध नहीं है।"]);
                    exit;
                }
                usort($files, function($a, $b) { return filemtime($b) - filemtime($a); });
                $reqFile = basename($files[0]);
            }

            $filePath = $snapshotsDir . '/' . $reqFile;
            if (!file_exists($filePath)) {
                http_response_code(404);
                echo json_encode(["success" => false, "error" => "बैकअप फाइल नहीं मिली: $reqFile"]);
                exit;
            }

            while (ob_get_level()) ob_end_clean();
            header('Content-Type: application/octet-stream');
            header('Content-Disposition: attachment; filename="' . $reqFile . '"');
            header('Content-Length: ' . filesize($filePath));
            header('Cache-Control: no-cache, must-revalidate');
            header('Pragma: no-cache');
            readfile($filePath);
            exit;

        default:
            http_response_code(400);
            echo json_encode(["success" => false, "error" => "अज्ञात क्रिया (Unknown action: $action)"]);
            break;
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => "बैकअप त्रुटि: " . $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
