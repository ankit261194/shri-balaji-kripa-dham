<?php
// ==============================================================================
// श्री बालाजी कृपा धाम - दुर्गा सप्तशती 17 अध्याय व स्तोत्र सर्वर माइग्रेशन इंजन
// Server-Side Fast CDN Migration Engine: Fetches directly to Hostinger storage
// ==============================================================================

set_time_limit(600);
ini_set('memory_limit', '256M');
header('Content-Type: application/json; charset=utf-8');

$uploadDir = __DIR__ . '/../uploads/audio/';
if (!is_dir($uploadDir)) {
    @mkdir($uploadDir, 0755, true);
}

$ds_map = [
    "durga_saptashati_kavach" => [
        "file" => "sbkd_ds_00_kavach.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/05%20Devi%20Kavacham.mp3"
    ],
    "durga_saptashati_argala" => [
        "file" => "sbkd_ds_00_argala.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/06%20Argala%20Stotram.mp3"
    ],
    "durga_saptashati_keelak" => [
        "file" => "sbkd_ds_00_keelak.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/07%20Keelakam.mp3"
    ],
    "durga_saptashati_ch1" => [
        "file" => "sbkd_ds_01_ch1.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%201%20Pradamodhyayha.mp3"
    ],
    "durga_saptashati_ch2" => [
        "file" => "sbkd_ds_02_ch2.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%202%20Divitiyodhyayaha.mp3"
    ],
    "durga_saptashati_ch3" => [
        "file" => "sbkd_ds_03_ch3.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%203%20Tritiyodhyayaha.mp3"
    ],
    "durga_saptashati_ch4" => [
        "file" => "sbkd_ds_04_ch4.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%204%20Chaturthodhayayaha.mp3"
    ],
    "durga_saptashati_ch5" => [
        "file" => "sbkd_ds_05_ch5.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%205%20Panchamodhyayaha.mp3"
    ],
    "durga_saptashati_ch6" => [
        "file" => "sbkd_ds_06_ch6.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%206%20Shashtodhyayaha.mp3"
    ],
    "durga_saptashati_ch7" => [
        "file" => "sbkd_ds_07_ch7.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%207%20Saptamodhyayaha.mp3"
    ],
    "durga_saptashati_ch8" => [
        "file" => "sbkd_ds_08_ch8.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%208%20Ashtamodhyayaha.mp3"
    ],
    "durga_saptashati_ch9" => [
        "file" => "sbkd_ds_09_ch9.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter%209%20Navamodhyayaha.mp3"
    ],
    "durga_saptashati_ch10" => [
        "file" => "sbkd_ds_10_ch10.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter10%20Dasamodhyayaha.mp3"
    ],
    "durga_saptashati_ch11" => [
        "file" => "sbkd_ds_11_ch11.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter11%20Ekadhasadhyayaha.mp3"
    ],
    "durga_saptashati_ch12" => [
        "file" => "sbkd_ds_12_ch12.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter12%20Dwadhasodhyayaha.mp3"
    ],
    "durga_saptashati_ch13" => [
        "file" => "sbkd_ds_13_ch13.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Chapter13%20Triodasadhyayaha.mp3"
    ],
    "durga_saptashati_kshama" => [
        "file" => "sbkd_ds_14_kshama.mp3",
        "url" => "https://archive.org/download/DeviMahatmyamRecitationAudio/Kshama%20Prathana%20Stotram.mp3"
    ]
];

$keyFilter = $_GET['key'] ?? '';
$results = [];

foreach ($ds_map as $key => $info) {
    if ($keyFilter && $key !== $keyFilter) continue;

    $targetPath = $uploadDir . $info['file'];
    $cdnUrl = "https://shribalajikripadham.online/uploads/audio/" . $info['file'];

    // Check if already downloaded and valid
    if (file_exists($targetPath) && filesize($targetPath) > 500000) {
        $results[$key] = [
            "status" => "ALREADY_EXISTS",
            "file" => $info['file'],
            "size" => filesize($targetPath),
            "size_mb" => round(filesize($targetPath) / (1024 * 1024), 2) . " MB",
            "cdn_url" => $cdnUrl
        ];
        continue;
    }

    // Stream download directly from source
    $ctx = stream_context_create([
        'http' => [
            'method' => 'GET',
            'header' => "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)\r\nAccept: */*\r\n",
            'timeout' => 120
        ]
    ]);

    $in = @fopen($info['url'], 'rb', false, $ctx);
    if (!$in) {
        $results[$key] = [
            "status" => "ERROR_OPENING_SOURCE",
            "source_url" => $info['url']
        ];
        continue;
    }

    $out = @fopen($targetPath . '.tmp', 'wb');
    if (!$out) {
        @fclose($in);
        $results[$key] = ["status" => "ERROR_OPENING_DESTINATION"];
        continue;
    }

    $bytes = stream_copy_to_stream($in, $out);
    @fclose($in);
    @fclose($out);

    if ($bytes > 500000) {
        rename($targetPath . '.tmp', $targetPath);
        $results[$key] = [
            "status" => "DOWNLOADED",
            "file" => $info['file'],
            "size" => $bytes,
            "size_mb" => round($bytes / (1024 * 1024), 2) . " MB",
            "cdn_url" => $cdnUrl
        ];
    } else {
        @unlink($targetPath . '.tmp');
        $results[$key] = [
            "status" => "INCOMPLETE_DOWNLOAD",
            "bytes" => $bytes
        ];
    }
}

echo json_encode([
    "success" => true,
    "total_processed" => count($results),
    "results" => $results
], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
