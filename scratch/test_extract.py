import sys
import re
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/DurgaSaptashatiData.kt', 'r', encoding='utf-8') as f:
    content = f.read()

parts = content.split('object DurgaSaptashatiSampurnaAdhyayData')
nitya_part = parts[0]
rem = parts[1].split('object HanumanVeerRakshaMantraData')
adhyay_part = rem[0]

def extract_from_section(section_text, num):
    pattern = rf'number\s*=\s*{num},.*?title\s*=\s*"([^"]*)".*?devanagariText\s*=\s*"""(.*?)""".*?hindiMeaning\s*=\s*"(.*?)"'
    match = re.search(pattern, section_text, re.DOTALL)
    if match:
        title = match.group(1).strip()
        dev = match.group(2).strip()
        meaning = match.group(3).strip()
        return title, dev + "\n\n॥ हिंदी भावार्थ ॥\n" + meaning
    return None, None

print("Checking Nitya Stotras:")
for n in [2, 3, 4, 5]:
    title, full_text = extract_from_section(nitya_part, n)
    print(f"Verse {n}: {title} (Length: {len(full_text)} chars)")

print("\nChecking 13 Chapters:")
for ch in range(1, 14):
    title, full_text = extract_from_section(adhyay_part, ch)
    print(f"Chapter {ch}: {title} (Length: {len(full_text)} chars)")
