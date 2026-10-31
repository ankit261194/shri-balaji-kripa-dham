<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

header('Content-Type: text/plain; charset=utf-8');

try {
    include __DIR__ . '/../download.php';
} catch (Throwable $e) {
    echo "Caught: " . $e->getMessage() . " in " . $e->getFile() . " on line " . $e->getLine();
}
