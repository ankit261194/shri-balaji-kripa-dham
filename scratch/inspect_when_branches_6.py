with open(r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i in range(2993, 3300):
    line = lines[i]
    if 'currentTabTitle ==' in line or 'else ->' in line:
        safe = line.rstrip().encode('ascii', errors='replace').decode('ascii')
        print(f'{i+1}: {safe}')
