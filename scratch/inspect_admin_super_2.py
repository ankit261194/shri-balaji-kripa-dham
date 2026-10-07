with open(r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i in range(1600, 1750):
    safe = lines[i].rstrip().encode('ascii', errors='replace').decode('ascii')
    print(f'{i+1}: {safe}')
