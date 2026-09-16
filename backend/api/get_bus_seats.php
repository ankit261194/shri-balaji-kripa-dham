<?php
if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} else {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");

$yatraDate = trim($_GET['yatra_date'] ?? date('Y-m-d'));
$pdo = getDB();

try {
    $stmt = $pdo->prepare("SELECT * FROM bus_seats WHERE yatra_date = :yatra_date ORDER BY seat_number ASC");
    $stmt->execute([':yatra_date' => $yatraDate]);
    $seats = $stmt->fetchAll();

    echo json_encode([
        "success" => true,
        "yatra_date" => $yatraDate,
        "total_booked" => count($seats),
        "seats" => $seats
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
