file_path = r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove showRoomManagementDialog declaration
old_room_var = "    var showRoomManagementDialog by remember { mutableStateOf(false) }"
if old_room_var in content:
    content = content.replace(old_room_var, "    // showRoomManagementDialog removed (feature not in main app)", 1)
    print("1. showRoomManagementDialog declaration removed.")
else:
    print("1. WARNING: showRoomManagementDialog not found.")

# 2. Remove Section 2 (Bus) and Section 2.5 (Dharamshala)
bus_start = content.find("        // SECTION 2: 60-SEATER LUXURY BUS CONTROL (SUPER ADMIN DIRECT CONTROL)")
upi_start = content.find("        // SECTION 3: ASHRAM UPI QR CODE & PAYMENT GATEWAY CONTROL (SUPER ADMIN DIRECT CONTROL)")

if bus_start != -1 and upi_start != -1 and bus_start < upi_start:
    content = content[:bus_start] + "// (Bus and Dharamshala controls removed: features not present in main app)\n\n" + content[upi_start:]
    print("2. Bus and Dharamshala sections removed from PublicServiceMatrixTab.")
else:
    print(f"2. WARNING: Could not find bus_start ({bus_start}) or upi_start ({upi_start}).")

# 3. Remove Aarti timings row from Section 4
import re
aarti_pattern = r'ServiceSwitchRow\([^)]*Aarti & Darbar Timings Card[^)]*\)\s*\n'
match = re.search(aarti_pattern, content)
if match:
    content = content[:match.start()] + "// (Aarti timings card toggle removed: aarti timings disabled in main app)\n" + content[match.end():]
    print("3. Aarti timings row removed from Section 4.")
else:
    print("3. WARNING: Aarti timings row pattern not matched.")

# 4. Remove Section 5 (Yatra diary) and room management dialogs up to showSaveConfirmationDialog
yatra_start = content.find("        // SECTION 5: YATRA EXPENSE DIARY PRIVACY (SUPER ADMIN ONLY BY DEFAULT)")
save_dlg_start = content.find("        if (showSaveConfirmationDialog) {")

if yatra_start != -1 and save_dlg_start != -1 and yatra_start < save_dlg_start:
    content = content[:yatra_start] + "// (Yatra diary privacy & room dialogs removed)\n\n" + content[save_dlg_start:]
    print("4. Yatra diary and room dialogs removed.")
else:
    print(f"4. WARNING: Could not find yatra_start ({yatra_start}) or save_dlg_start ({save_dlg_start}).")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("File updated successfully.")
