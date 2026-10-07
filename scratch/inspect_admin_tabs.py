with open(r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Search for Tab titles in AdminDashboardScreen
tab_lines = []
for i, line in enumerate(lines[:1200]):
    if 'Tab(' in line or 'ScrollableTabRow' in line or 'TabRow' in line or 'tabs =' in line or 'listOf(' in line:
        tab_lines.append((i+1, line.strip()))

for l in tab_lines[:40]:
    print(f'{l[0]}: {l[1]}')
