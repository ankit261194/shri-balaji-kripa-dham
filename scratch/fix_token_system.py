import os
import sys

# 1. Update TokenRegistrationScreen.kt
target_reg = r"C:\Users\hp\.gemini\antigravity\scratch\shri_balaji_kripa_dham\app\src\main\java\com\example\shribalajikripadham\ui\token\TokenRegistrationScreen.kt"

with open(target_reg, "r", encoding="utf-8") as f:
    reg_code = f.read()

# Make photo optional:
# Find:
old_photo_check = '''                                if (capturedBitmap == null && capturedPhotoUri.isNullOrBlank()) {
                                    errorMessage = if (isHindi) "कृपया टोकन हेतु भक्त की फोटो खींचें या नीचे गैलरी से चुनें।" else "Please take or pick devotee photo."
                                    return@Button
                                }'''

new_photo_check = '''                                // Devotee photo is optional - proceeds smoothly with or without selfie'''

if old_photo_check in reg_code:
    reg_code = reg_code.replace(old_photo_check, new_photo_check, 1)
    print("Made photo optional in TokenRegistrationScreen.kt")
else:
    print("Could not find old_photo_check directly, looking for partial")
    pos = reg_code.find("capturedBitmap == null && capturedPhotoUri.isNullOrBlank()")
    print("Position:", pos)

# Allow non-Sunday advance registration:
# Find:
old_non_sunday = '''                                    is SundayScheduleState.NonSunday -> {
                                        scheduleAlertTitle = if (isHindi) "📅 टोकन केवल रविवार को मिलते हैं" else "📅 Tokens Only On Sunday"
                                        scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                        showScheduleAlertDialog = true
                                        errorMessage = scheduleAlertMessage
                                        return@Button
                                    }'''

new_non_sunday = '''                                    is SundayScheduleState.NonSunday -> {
                                        // Devotees can register Advance Tokens for the upcoming Sunday!
                                        // Will automatically issue for currentSchedule.nextSundayDateStr
                                    }'''

if old_non_sunday in reg_code:
    reg_code = reg_code.replace(old_non_sunday, new_non_sunday, 1)
    print("Enabled Advance Sunday token booking on Non-Sunday!")
else:
    print("Could not find old_non_sunday directly")

# Allow registration during countdown:
old_countdown = '''                                    is SundayScheduleState.CountdownActive -> {
                                        scheduleAlertTitle = if (isHindi) "⏳ टोकन उल्टी गिनती जारी है" else "⏳ Countdown Active"
                                        scheduleAlertMessage = if (isHindi)
                                            "रविवार टोकन पंजीकरण में शेष समय: ${SundayTokenScheduleHelper.formatCountdownHindi(currentSchedule.remainingMillis)} [ ${SundayTokenScheduleHelper.formatCountdown(currentSchedule.remainingMillis)} ]।\\n\\nटोकन ${currentSchedule.formattedTarget} स्वतः खुल जाएंगे। कृपया उस समय पुनः प्रयास करें।"
                                        else
                                            "Tokens open in: ${SundayTokenScheduleHelper.formatCountdown(currentSchedule.remainingMillis)} (Will open automatically at ${currentSchedule.formattedTarget})."
                                        showScheduleAlertDialog = true
                                        errorMessage = scheduleAlertMessage
                                        return@Button
                                    }'''

new_countdown = '''                                    is SundayScheduleState.CountdownActive -> {
                                        // Open for immediate advance booking during countdown!
                                    }'''

if old_countdown in reg_code:
    reg_code = reg_code.replace(old_countdown, new_countdown, 1)
    print("Enabled token booking during countdown!")
else:
    print("Could not find old_countdown directly")

# Allow registration during SundayBeforeStart:
old_before_start = '''                                    is SundayScheduleState.SundayBeforeStart -> {
                                        scheduleAlertTitle = if (isHindi) "⏳ टोकन सुबह 8:00 बजे से मिलेंगे" else "⏳ Opens at 8:00 AM"
                                        scheduleAlertMessage = if (isHindi) currentSchedule.messageHindi else currentSchedule.messageEnglish
                                        showScheduleAlertDialog = true
                                        errorMessage = scheduleAlertMessage
                                        return@Button
                                    }'''

new_before_start = '''                                    is SundayScheduleState.SundayBeforeStart -> {
                                        // Open for registration on Sunday morning
                                    }'''

if old_before_start in reg_code:
    reg_code = reg_code.replace(old_before_start, new_before_start, 1)
    print("Enabled Sunday morning registration!")
else:
    print("Could not find old_before_start directly")

with open(target_reg, "w", encoding="utf-8") as f:
    f.write(reg_code)

print("Saved TokenRegistrationScreen.kt successfully!")
