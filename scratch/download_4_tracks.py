import subprocess
import os

tracks = [
    ("aaj_mangalwar", "https://www.youtube.com/watch?v=t2-JFDkk_Yg"),
    ("meri_naav_chali", "https://www.youtube.com/watch?v=FBLu_YfD_5A"),
    ("hanuman_shabar_mantra", "https://www.youtube.com/watch?v=kYJ5cW5o1H0"),
    ("satyanarayan_aarti_laxmi_ramana", "https://www.youtube.com/watch?v=P94mKqVI9IM")
]

for name, url in tracks:
    out_file = f"scratch/{name}.m4a"
    print(f"Downloading {name} from {url}...")
    cmd = [
        "yt-dlp",
        "-f", "140/ba[ext=m4a]/ba",
        "-o", out_file,
        url
    ]
    res = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
    if os.path.exists(out_file) and os.path.getsize(out_file) > 100000:
        print(f"SUCCESS: {name} downloaded ({os.path.getsize(out_file)/(1024*1024):.2f} MB)")
    else:
        print(f"FAILED {name}: {res.stderr}")
