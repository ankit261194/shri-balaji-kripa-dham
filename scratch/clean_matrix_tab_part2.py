file_path = r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
skip_yatra = False
for i, line in enumerate(lines):
    if 'onAartiTimingsChange)' in line and 'ServiceSwitchRow' in line:
        new_lines.append("                // (Aarti timings card toggle removed: aarti timings disabled in main app)\n")
        print(f"Removed Aarti timings toggle at line {i+1}")
        continue
    if '// SECTION 5: YATRA EXPENSE DIARY PRIVACY' in line:
        skip_yatra = True
        print(f"Started skipping Yatra & rooms from line {i+1}")
        continue
    if skip_yatra and 'if (showSaveConfirmationDialog) {' in line:
        skip_yatra = False
        print(f"Stopped skipping at line {i+1} (showSaveConfirmationDialog)")
        new_lines.append(line)
        continue
    if not skip_yatra:
        new_lines.append(line)

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print("Remaining matrix items cleaned successfully.")
