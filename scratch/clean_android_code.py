import os

base_dir = r'C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham'

# 1. SevadarDirectoryManager.kt
mgr_path = os.path.join(base_dir, r'app\src\main\java\com\example\shribalajikripadham\data\repository\SevadarDirectoryManager.kt')
with open(mgr_path, 'r', encoding='utf-8') as f:
    code = f.read()

# Replace getDefaultSevadars and nullOrBlank check
old_block = """    fun getDefaultSevadars(): List<AshramSevadarContact> {
        return listOf(
            AshramSevadarContact(
                id = "ashram_helpline_main",
                name = "श्री बालाजी कृपा धाम (आधिकारिक हेल्पलाइन)",
                department = "सामान्य आश्रम सहायता",
                roleTitleHindi = "मुख्य आश्रम सेवादार",
                phoneNumber = "9100100251",
                whatsappNumber = "9100100251",
                isAvailable = true,
                description = "धाम पता, नियम, मंगलवार/रविवार दरबार समय व संपूर्ण आधिकारिक जानकारी"
            )
        )
    }

    fun getAllSevadars(context: Context): List<AshramSevadarContact> {
        val prefs = getPrefs(context)
        val rawJson = prefs.getString(KEY_SEVADARS, null)
        if (rawJson.isNullOrBlank()) {
            val defaults = getDefaultSevadars()
            saveSevadars(context, defaults)
            return defaults
        }"""

new_block = """    fun getDefaultSevadars(): List<AshramSevadarContact> {
        return emptyList()
    }

    fun getAllSevadars(context: Context): List<AshramSevadarContact> {
        val prefs = getPrefs(context)
        val rawJson = prefs.getString(KEY_SEVADARS, null)
        if (rawJson.isNullOrBlank()) {
            return emptyList()
        }"""

# Normalize \r\n to \n for replacement
code_norm = code.replace('\r\n', '\n')
old_norm = old_block.replace('\r\n', '\n')
new_norm = new_block.replace('\r\n', '\n')

if old_norm in code_norm:
    code_norm = code_norm.replace(old_norm, new_norm)
    with open(mgr_path, 'w', encoding='utf-8') as f:
        f.write(code_norm)
    print("SevadarDirectoryManager.kt updated!")
else:
    print("SevadarDirectoryManager.kt old_block not found!")

# 2. AshramSevadarContact.kt
c_path = os.path.join(base_dir, r'app\src\main\java\com\example\shribalajikripadham\data\model\AshramSevadarContact.kt')
with open(c_path, 'r', encoding='utf-8') as f:
    c_code = f.read()
c_code = c_code.replace('json.optString("phone", json.optString("phoneNumber", "9100100251"))', 'json.optString("phone", json.optString("phoneNumber", ""))')
with open(c_path, 'w', encoding='utf-8') as f:
    f.write(c_code)
print("AshramSevadarContact.kt updated!")

# 3. AdminHelpdeskTab.kt
a_path = os.path.join(base_dir, r'app\src\main\java\com\example\shribalajikripadham\ui\admin\AdminHelpdeskTab.kt')
with open(a_path, 'r', encoding='utf-8') as f:
    a_code = f.read()
a_code = a_code.replace('phoneNumber = "9100100251"', 'phoneNumber = ""')
a_code = a_code.replace('whatsappNumber = "9100100251"', 'whatsappNumber = ""')
with open(a_path, 'w', encoding='utf-8') as f:
    f.write(a_code)
print("AdminHelpdeskTab.kt updated!")

# 4. AshramTabContent.kt
t_path = os.path.join(base_dir, r'app\src\main\java\com\example\shribalajikripadham\ui\home\components\AshramTabContent.kt')
with open(t_path, 'r', encoding='utf-8') as f:
    t_code = f.read()
t_code = t_code.replace('settings.contactPhone.ifBlank { "9100100251" }', 'settings.contactPhone.ifBlank { "8171064598" }')
with open(t_path, 'w', encoding='utf-8') as f:
    f.write(t_code)
print("AshramTabContent.kt updated!")

# 5. SevadarHelpdeskDialog.kt
h_path = os.path.join(base_dir, r'app\src\main\java\com\example\shribalajikripadham\ui\feedback\SevadarHelpdeskDialog.kt')
with open(h_path, 'r', encoding='utf-8') as f:
    h_code = f.read()
h_code = h_code.replace('prefs.getString("user_phone", "")?.takeIf { it.isNotBlank() } ?: "9100100251"', 'prefs.getString("user_phone", "")?.takeIf { it.isNotBlank() } ?: "8171064598"')
h_code = h_code.replace('prefs.getString("user_phone", "")?.takeIf { it.isNotBlank() } ?: "9100100000"', 'prefs.getString("user_phone", "") ?: ""')
with open(h_path, 'w', encoding='utf-8') as f:
    f.write(h_code)
print("SevadarHelpdeskDialog.kt updated!")

# 6. InAppVoiceCallDialog.kt
v_path = os.path.join(base_dir, r'app\src\main\java\com\example\shribalajikripadham\ui\feedback\InAppVoiceCallDialog.kt')
with open(v_path, 'r', encoding='utf-8') as f:
    v_code = f.read()
v_code = v_code.replace('callerPhone: String = "9100100251"', 'callerPhone: String = "8171064598"')
with open(v_path, 'w', encoding='utf-8') as f:
    f.write(v_code)
print("InAppVoiceCallDialog.kt updated!")
