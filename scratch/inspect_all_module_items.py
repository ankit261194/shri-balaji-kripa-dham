with open(r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminHubComponents.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, l in enumerate(lines[:550]):
    if 'AdminHubModuleItem(' in l:
        title = lines[i+4].strip().encode('ascii', errors='replace').decode('ascii')
        cat = lines[i+6].strip().encode('ascii', errors='replace').decode('ascii')
        print(f'Line {i+1}: {title} | {cat}')
