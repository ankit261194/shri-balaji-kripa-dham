import sys
import re
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/DurgaSaptashatiData.kt', 'r', encoding='utf-8') as f:
    content = f.read()

parts = content.split('object DurgaSaptashatiSampurnaAdhyayData')
nitya = parts[0]
adhyay = parts[1]

print("--- NITYA PATH VERSES ---")
for num, title in re.findall(r'number\s*=\s*(\d+),\s*title\s*=\s*"([^"]+)"', nitya):
    print(f"Verse {num}: {title}")

print("\n--- SAMPURNA ADHYAY CHAPTERS ---")
for num, title in re.findall(r'number\s*=\s*(\d+),\s*title\s*=\s*"([^"]+)"', adhyay):
    print(f"Chapter {num}: {title}")
