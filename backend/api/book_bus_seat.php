<?php
if (file_exists(__DIR__ . '/../config/db.php')) {
    require_once __DIR__ . '/../config/db.php';
} else {
    require_once __DIR__ . '/config/db.php';
}

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$input = json_decode(file_get_contents('php://input'), true) ?: $_POST;

$yatraDate = trim($input['yatra_date'] ?? date('Y-m-d'));
$seatNumber = intval($input['seat_number'] ?? 0);
$passengerName = trim($input['passenger_name'] ?? '');
$passengerPhone = trim($input['passenger_phone'] ?? '');
$passengerGender = trim($input['passenger_gender'] ?? 'पुरुष');
$passengerAge = intval($input['passenger_age'] ?? 30);
$seatStatus = trim($input['seat_status'] ?? 'BOOKED');
$paymentStatus = trim($input['payment_status'] ?? 'PAID');
$fareAmount = floatval($input['fare_amount'] ?? 1500.0);
$bookedBy = trim($input['booked_by'] ?? 'APP');
$createdAt = intval($input['created_at'] ?? (time() * 1000));

if ($seatNumber <= 0 || empty($passengerName) || empty($passengerPhone)) {
    http_response_code(400);
    echo json_encode(["success" => false, "error" => "सीट नंबर, यात्री का नाम और मोबाइल नंबर अनिवार्य हैं।"], JSON_UNESCAPED_UNICODE);
    exit;
}

$pdo = getDB();

try {
    $stmt = $pdo->prepare("INSERT INTO bus_seats (
        seat_number, yatra_date, passenger_name, passenger_phone, passenger_gender, passenger_age, seat_status, payment_status, fare_amount, booked_by, created_at
    ) VALUES (
        :seat, :yatra_date, :name, :phone, :gender, :age, :status, :pay_status, :fare, :booked_by, :created_at
    ) ON DUPLICATE KEY UPDATE
        passenger_name = VALUES(passenger_name),
        passenger_phone = VALUES(passenger_phone),
        passenger_gender = VALUES(passenger_gender),
        passenger_age = VALUES(passenger_age),
        seat_status = VALUES(seat_status),
        payment_status = VALUES(payment_status),
        fare_amount = VALUES(fare_amount),
        booked_by = VALUES(booked_by)");

    $stmt->execute([
        ':seat' => $seatNumber,
        ':yatra_date' => $yatraDate,
        ':name' => $passengerName,
        ':phone' => $passengerPhone,
        ':gender' => $passengerGender,
        ':age' => $passengerAge,
        ':status' => $seatStatus,
        ':pay_status' => $paymentStatus,
        ':fare' => $fareAmount,
        ':booked_by' => $bookedBy,
        ':created_at' => $createdAt
    ]);

    echo json_encode([
        "success" => true,
        "seat_number" => $seatNumber,
        "yatra_date" => $yatraDate,
        "passenger_name" => $passengerName,
        "message" => "सीट नंबर $seatNumber ($passengerName) सफलतापूर्वक बुक हो गई!"
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "error" => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}
