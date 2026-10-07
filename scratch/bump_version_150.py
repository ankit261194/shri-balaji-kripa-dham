import json
import re

# 1. app/build.gradle.kts
gradle_path = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\app\build.gradle.kts"
with open(gradle_path, "r", encoding="utf-8") as f:
    gradle_content = f.read()

gradle_content = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 150', gradle_content)
gradle_content = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "2.65.00"', gradle_content)

with open(gradle_path, "w", encoding="utf-8") as f:
    f.write(gradle_content)
print("Updated app/build.gradle.kts to versionCode = 150, versionName = 2.65.00")

# 2. JSON version files
v_data = {
    "latest_version_code": 150,
    "version_code": 150,
    "latest_version_name": "2.65.00",
    "version_name": "2.65.00",
    "update_notes_hindi": "v2.65.00 (Build 150): 🪔 संपूर्ण 18 पावन आरती व चालीसा ग्रंथ संग्रह। 👑 प्रो-लेवल सुपर एडमिन एक्जीक्यूटिव कमांड डेक (~75dp सुगठित), आश्रम बस सेवा व अर्जी मास्टर कंट्रोल, होम स्क्रीन पर सीधा एक्टिव टोकन कार्ड, अग्रिम रविवार टोकन बुकिंग व वैकल्पिक सेल्फी फोटो।",
    "update_notes_english": "v2.65.00 (Build 150): 🪔 Complete 18 Sacred Aartis & Chalisas. 👑 Pro-Level Super Admin Executive Command Deck, Ashram Bus & Arzi Master Controls, Direct Devotee Active Token Card on Home, Advance Sunday Token Booking & Optional Selfie Photo.",
    "apk_url": "https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk",
    "webhook_url": "https://script.google.com/macros/s/AKfycbzz_gaO2sIkKE1oApOK3HY__VA-wO3228WcXxmFU--Y4PH8ow47G13vPSrfUV_QqpG4/exec",
    "is_force_update": True
}

json_paths = [
    r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\version.json",
    r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\app\src\main\assets\version.json",
    r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\backend\version.json",
    r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\app_update.json"
]

for p in json_paths:
    with open(p, "w", encoding="utf-8") as f:
        json.dump(v_data, f, indent=2, ensure_ascii=False)
    print(f"Updated {p}")

print("All version manifests bumped to Build 150 successfully!")
