<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");

require_once __DIR__ . '/config.php';

$pdo = getDbConnection();
if (!$pdo) {
    echo json_encode(["error" => "No DB"]);
    exit;
}

$tables = [
    'elevex_companies',
    'elevex_users',
    'elevex_lifts_amc',
    'elevex_amc_contracts',
    'elevex_breakdowns',
    'elevex_attendance_ledger',
    'elevex_spare_parts',
    'elevex_erection_projects'
];

$output = [];
foreach ($tables as $t) {
    try {
        $stmt = $pdo->query("SELECT * FROM $t");
        $output[$t] = [
            'count' => $stmt->rowCount(),
            'rows' => $stmt->fetchAll(PDO::FETCH_ASSOC)
        ];
    } catch (Exception $e) {
        $output[$t] = ['error' => $e->getMessage()];
    }
}

echo json_encode($output, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
