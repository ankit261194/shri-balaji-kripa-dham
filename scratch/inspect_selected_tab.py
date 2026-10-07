with open(r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'selectedTab' in line:
        print(f'{i+1}: {line.strip()[:100]}')
