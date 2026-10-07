with open(r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminDashboardScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Check for bus section
bus_hdr = "// SECTION 2: 60-SEATER LUXURY BUS CONTROL"
upi_hdr = "// SECTION 3: ASHRAM UPI QR CODE & PAYMENT GATEWAY CONTROL"
print("Bus header found:", bus_hdr in content)
print("UPI header found:", upi_hdr in content)

# Check for aarti timings row
aarti_row = 'ServiceSwitchRow("आरती व दरबार समय विवरण (Aarti & Darbar Timings Card)", isAartiTimings, onAartiTimingsChange)'
print("Aarti row found:", aarti_row in content)

# Check for yatra diary and room dialogs
yatra_hdr = "// SECTION 5: YATRA EXPENSE DIARY PRIVACY"
save_dlg = "if (showSaveConfirmationDialog) {"
print("Yatra header found:", yatra_hdr in content)
print("Save dialog found:", save_dlg in content)
