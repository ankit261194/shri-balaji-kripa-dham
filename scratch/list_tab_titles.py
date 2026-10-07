import re

with open(r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminHubComponents.kt', 'r', encoding='utf-8') as f:
    content = f.read()

items = re.findall(r'tabTitle\s*=\s*(?:if\s*\(isHindi\)\s*)?"([^"]+)"', content)
print('Modules found in AdminHubComponents.kt:', len(items))
for idx, it in enumerate(items, 1):
    safe = it.encode('ascii', errors='replace').decode('ascii')
    print(f'{idx}: {safe}')
