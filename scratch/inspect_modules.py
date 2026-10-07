with open(r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'val allAdminModules' in line or 'allAdminModules =' in line:
        print(f'Line {i+1}:')
        for j in range(i, min(len(lines), i+80)):
            safe = lines[j].rstrip().encode('ascii', errors='replace').decode('ascii')
            print(f'{j+1}: {safe}')
        break
