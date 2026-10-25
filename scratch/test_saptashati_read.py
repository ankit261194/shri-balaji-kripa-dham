import re

with open('app/src/main/java/com/example/shribalajikripadham/data/sacred/DurgaSaptashatiData.kt', 'r', encoding='utf-8') as f:
    text = f.read()

titles = re.findall(r'title\s*=\s*"([^"]+)"', text)
for t in titles:
    print(t.encode('ascii', 'replace').decode())
