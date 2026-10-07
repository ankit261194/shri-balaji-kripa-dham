# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/example/shribalajikripadham/ui/admin/AdminDashboardScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update refreshData to initialize havanCostInput and havanRulesInput
target_refresh = 'customSundayTokenCustomNotice = s.sundayTokenCustomNotice'
replacement_refresh = '''customSundayTokenCustomNotice = s.sundayTokenCustomNotice
            havanCostInput = s.havanEstimatedCost.toString()
            havanRulesInput = s.havanRulesNotice'''

if target_refresh in content:
    content = content.replace(target_refresh, replacement_refresh, 1)
    print("1. refreshData havan inputs updated")
else:
    print("WARNING: target_refresh not found")

# 2. In allowedTabs, remove dead tabs
dead_tab_1 = '''            if (isSuper || (admin.canManageYatra && settings.isBusBookingLive)) {
                allowedTabs.add(if (isHindi) "बस बुकिंग लेजर" else "Bus Ledger")
            }'''
content = content.replace(dead_tab_1, '// (Bus Ledger removed)', 1)

dead_tab_2 = '''            if (isSuper || admin.canManageUiControl) {
                allowedTabs.add(if (isHindi) "UI बॉक्स कंट्रोल" else "UI Control")
            }
            if (isSuper || admin.canManageWebsite) {
                allowedTabs.add(if (isHindi) "🌐 वेबसाइट लाइव एडिटर" else "Website Live Editor")
                allowedTabs.add(if (isHindi) "🌐 वेबसाइट व CMS" else "Website & CMS")
            }'''
content = content.replace(dead_tab_2, '// (UI Box & Website CMS removed)', 1)

dead_tab_3 = '''            if (isSuper || admin.canManageDistances) {
                allowedTabs.add(if (isHindi) "कस्टम दूरियाँ" else "Distances")
            }'''
content = content.replace(dead_tab_3, '// (City Distances removed)', 1)

dead_tab_4 = 'allowedTabs.add(if (isHindi) "ऐप कस्टमाइजर" else "Customizer")'
content = content.replace(dead_tab_4, '// (Customizer removed)', 1)
print("2. allowedTabs purged of dead controls")

# 3. In PublicServiceMatrixTab:
# Remove Section 2 Bus card
idx_s2_start = content.find('// SECTION 2: 60-SEATER LUXURY BUS CONTROL (SUPER ADMIN DIRECT CONTROL)')
idx_s3_start = content.find('// SECTION 3: ASHRAM UPI QR CODE & PAYMENT GATEWAY CONTROL (SUPER ADMIN DIRECT CONTROL)')

if idx_s2_start != -1 and idx_s3_start != -1 and idx_s2_start < idx_s3_start:
    content = content[:idx_s2_start] + '// (Section 2 Bus & Section 2.5 Dharamshala removed)\n        ' + content[idx_s3_start:]
    print("3. Section 2 & 2.5 removed from PublicServiceMatrixTab")
else:
    print("WARNING: Section 2 / Section 3 markers not matched")

# Comment out Aarti switch
target_aarti_switch = 'ServiceSwitchRow("⏰ आरती व दरबार समय सारणी (Aarti & Darbar Timings Card)", isAartiTimings, onAartiTimingsChange)'
if target_aarti_switch in content:
    content = content.replace(target_aarti_switch, '// Aarti Timings switch removed', 1)
    print("4. Aarti Timings switch removed")
else:
    print("WARNING: target_aarti_switch not found")

# Remove Section 5 Yatra Diary card
idx_s5_start = content.find('// SECTION 5: YATRA EXPENSE DIARY PRIVACY (SUPER ADMIN ONLY BY DEFAULT)')
idx_btn_save = content.find('Spacer(modifier = Modifier.height(16.dp))\n                Button(\n                    onClick = {\n                        onSave()\n                        showSaveConfirmationDialog = true')

if idx_s5_start != -1 and idx_btn_save != -1 and idx_s5_start < idx_btn_save:
    content = content[:idx_s5_start] + '// (Section 5 Yatra Diary removed)\n                ' + content[idx_btn_save:]
    print("5. Section 5 removed from PublicServiceMatrixTab")
else:
    print("WARNING: Section 5 / Save button markers not matched")

# Remove showRoomManagementDialog
idx_room_dlg = content.find('if (showRoomManagementDialog) {')
idx_save_confirm = content.find('if (showSaveConfirmationDialog) {')

if idx_room_dlg != -1 and idx_save_confirm != -1 and idx_room_dlg < idx_save_confirm:
    content = content[:idx_room_dlg] + '// (showRoomManagementDialog removed)\n    ' + content[idx_save_confirm:]
    print("6. showRoomManagementDialog removed")
else:
    print("WARNING: showRoomManagementDialog / showSaveConfirmationDialog markers not matched")

with open('app/src/main/java/com/example/shribalajikripadham/ui/admin/AdminDashboardScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("AdminDashboardScreen.kt edits applied successfully!")
