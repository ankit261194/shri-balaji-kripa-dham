/**
 * ============================================================================
 * श्री बालाजी कृपा धाम (Shri Balaji Kripa Dham) - Google Sheets Master Webhook
 * ============================================================================
 * निर्देश (Instructions):
 * 1. अपनी Google Spreadsheet में 'Extensions' -> 'Apps Script' पर जाएं।
 * 2. यह पूरा कोड पेस्ट कर दें और 'Save' (Ctrl+S) करें।
 * 3. 'Deploy' -> 'Manage deployments' -> 'Edit' -> 'New version' -> 'Deploy' करें।
 * 
 * सुविधाएँ (Features):
 * 1. Sheet 1 (Sunday_Tokens): टोकन रियल-टाइम सिंक व बैच टोकन सिंक
 * 2. Sheet 2 (Devotee_Registry): भक्तों का केंद्रीय नाम, नंबर, शहर व फेस कोड सिंक
 * 3. Sheet 3 (App_Devices_Presence): सक्रिय फोन, फोन मॉडल, किसने खोला व लाइव टेलीमेट्री
 * ============================================================================
 */

function doPost(e) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000); // Concurrency lock up to 10s

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var data = JSON.parse(e.postData.contents);

    // ACTION 1: DEVICE HEARTBEAT & LIVE PRESENCE TELEMETRY
    if (data.action === "DEVICE_HEARTBEAT") {
      var devSheet = ss.getSheetByName("App_Devices_Presence");
      if (!devSheet) {
        devSheet = ss.insertSheet("App_Devices_Presence");
        devSheet.appendRow([
          "डिवाइस आईडी (Device ID)",
          "फोन मॉडल (Device Model)",
          "उपयोगकर्ता / भक्त (User Name)",
          "मोबाइल नंबर (Phone)",
          "शहर / गाँव (City)",
          "ऐप वर्शन (Version)",
          "अंतिम सक्रियता (Last Seen)",
          "कुल बार खोला (Open Count)"
        ]);
        var dhr = devSheet.getRange("A1:H1");
        dhr.setFontWeight("bold");
        dhr.setBackground("#0D47A1");
        dhr.setFontColor("#FFFFFF");
        dhr.setHorizontalAlignment("center");
        devSheet.setFrozenRows(1);
      }

      var devIdStr = String(data.device_id || "").trim();
      var rows = devSheet.getDataRange().getValues();
      var foundRow = -1;
      var curCount = 0;

      for (var r = 1; r < rows.length; r++) {
        if (String(rows[r][0]).trim() === devIdStr) {
          foundRow = r + 1;
          curCount = Number(rows[r][7]) || 0;
          break;
        }
      }

      var timeStr = new Date().toLocaleString("en-IN", { timeZone: "Asia/Kolkata" });

      if (foundRow > 0) {
        // Update existing device row
        if (data.device_model) devSheet.getRange(foundRow, 2).setValue(data.device_model);
        if (data.user_name) devSheet.getRange(foundRow, 3).setValue(data.user_name);
        if (data.phone_number) devSheet.getRange(foundRow, 4).setValue(String(data.phone_number));
        if (data.city) devSheet.getRange(foundRow, 5).setValue(data.city);
        if (data.app_version) devSheet.getRange(foundRow, 6).setValue(data.app_version);
        devSheet.getRange(foundRow, 7).setValue(timeStr);
        devSheet.getRange(foundRow, 8).setValue(curCount + 1);
      } else {
        // Insert new device row
        devSheet.appendRow([
          devIdStr,
          data.device_model || "Android Device",
          data.user_name || "अतिथि भक्त (Guest)",
          String(data.phone_number || ""),
          data.city || "",
          data.app_version || "2.9.0",
          timeStr,
          1
        ]);
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: "SUCCESS",
        message: "Device heartbeat recorded for " + (data.device_model || devIdStr)
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // ACTION 2: UP-SERT DEVOTEE PROFILE (Universal Registry)
    if (data.action === "UPSERT_DEVOTEE") {
      var regSheet = ss.getSheetByName("Devotee_Registry");
      if (!regSheet) {
        regSheet = ss.insertSheet("Devotee_Registry");
        regSheet.appendRow([
          "मोबाइल नंबर (Phone)",
          "भक्त का नाम (Name)",
          "शहर / गाँव (City)",
          "फेस कोड (Face Vector Base64)",
          "फोटो स्थिति (Photo)",
          "पंजीकरणकर्ता (Registered By)",
          "अंतिम दिनांक (Last Seen)"
        ]);
        var hr = regSheet.getRange("A1:G1");
        hr.setFontWeight("bold");
        hr.setBackground("#4A148C");
        hr.setFontColor("#FFFFFF");
        hr.setHorizontalAlignment("center");
        regSheet.setFrozenRows(1);
      }

      var phoneStr = String(data.phone_number || "").trim();
      var rows = regSheet.getDataRange().getValues();
      var foundRow = -1;

      for (var r = 1; r < rows.length; r++) {
        if (String(rows[r][0]).trim() === phoneStr) {
          foundRow = r + 1;
          break;
        }
      }

      var hasPhoto = (data.has_photo === true) || (data.face_vector_b64 && data.face_vector_b64.length > 20);
      var timeStr = new Date().toLocaleString("en-IN", { timeZone: "Asia/Kolkata" });

      if (foundRow > 0) {
        if (data.patient_name) regSheet.getRange(foundRow, 2).setValue(data.patient_name);
        if (data.city) regSheet.getRange(foundRow, 3).setValue(data.city);
        if (data.face_vector_b64) regSheet.getRange(foundRow, 4).setValue(data.face_vector_b64);
        regSheet.getRange(foundRow, 5).setValue(hasPhoto ? "📸 हाँ (Yes)" : "— बिना फोटो");
        if (data.registered_by) regSheet.getRange(foundRow, 6).setValue(data.registered_by);
        regSheet.getRange(foundRow, 7).setValue(timeStr);
      } else {
        regSheet.appendRow([
          phoneStr,
          data.patient_name || "",
          data.city || "",
          data.face_vector_b64 || "",
          hasPhoto ? "📸 हाँ (Yes)" : "— बिना फोटो",
          data.registered_by || "APP",
          timeStr
        ]);
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: "SUCCESS",
        message: "Devotee " + (data.patient_name || "") + " synced to Registry!"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // ACTION 3: BATCH TOKENS (Paper Register OCR & Sequential Issuance)
    var sheet = ss.getSheetByName("Sunday_Tokens") || ss.getSheets()[0];
    if (sheet.getLastRow() === 0) {
      sheet.appendRow([
        "टोकन नं (Token No)",
        "दिनांक (Date)",
        "समय (Time)",
        "भक्त / मरीज का नाम (Patient Name)",
        "मोबाइल नंबर (Phone)",
        "शहर / गाँव (City)",
        "पंजीकरणकर्ता (Registered By)",
        "दूरी किमी (Distance Km)",
        "स्थिति (Status)",
        "फोटो स्थिति (Photo)"
      ]);
      var headerRange = sheet.getRange("A1:J1");
      headerRange.setFontWeight("bold");
      headerRange.setBackground("#B71C1C");
      headerRange.setFontColor("#FFFFFF");
      headerRange.setHorizontalAlignment("center");
      sheet.setFrozenRows(1);
    }

    if (data.action === "BATCH_TOKENS" && Array.isArray(data.tokens)) {
      var batchList = data.tokens;
      for (var b = 0; b < batchList.length; b++) {
        var t = batchList[b];
        sheet.appendRow([
          t.token_number || "",
          t.darbar_date || "",
          t.time_str || "",
          t.patient_name || "",
          String(t.phone_number || ""),
          t.city || "",
          t.registered_by || "REGISTER_SCAN",
          t.distance_km != null ? t.distance_km : 0,
          t.status || "WAITING",
          t.has_photo ? "📸 संलग्न" : "— बिना फोटो (रजिस्टर)"
        ]);
        var lr = sheet.getLastRow();
        sheet.getRange(lr, 1).setHorizontalAlignment("center").setFontWeight("bold");
        sheet.getRange(lr, 2).setHorizontalAlignment("center");
        sheet.getRange(lr, 3).setHorizontalAlignment("center");
        sheet.getRange(lr, 7).setHorizontalAlignment("center");
        sheet.getRange(lr, 9).setHorizontalAlignment("center");
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: "SUCCESS",
        count: batchList.length,
        message: batchList.length + " tokens recorded from Register Scan!"
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // DEFAULT ACTION: RECORD SINGLE NEW TOKEN
    sheet.appendRow([
      data.token_number || "",
      data.darbar_date || "",
      data.time_str || "",
      data.patient_name || "",
      String(data.phone_number || ""),
      data.city || "",
      data.registered_by || "",
      data.distance_km != null ? data.distance_km : 0,
      data.status || "WAITING",
      data.has_photo ? "📸 संलग्न (Yes)" : "— बिना फोटो"
    ]);

    // Format new row
    var lastRow = sheet.getLastRow();
    sheet.getRange(lastRow, 1).setHorizontalAlignment("center").setFontWeight("bold");
    sheet.getRange(lastRow, 2).setHorizontalAlignment("center");
    sheet.getRange(lastRow, 3).setHorizontalAlignment("center");
    sheet.getRange(lastRow, 7).setHorizontalAlignment("center");
    sheet.getRange(lastRow, 9).setHorizontalAlignment("center");

    return ContentService.createTextOutput(JSON.stringify({
      status: "SUCCESS",
      message: "Token " + (data.token_number || "") + " recorded in Google Sheet successfully!"
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "ERROR",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  } finally {
    lock.releaseLock();
  }
}

function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();

    // GET 1: GET ACTIVE DEVICES PRESENCE (Super Admin Handset Tracking)
    if (e && e.parameter && e.parameter.action === "get_devices") {
      var devSheet = ss.getSheetByName("App_Devices_Presence");
      if (!devSheet) {
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          total: 0,
          active_today: 0,
          devices: []
        })).setMimeType(ContentService.MimeType.JSON);
      }

      var rows = devSheet.getDataRange().getValues();
      var devices = [];
      var todayStr = new Date().toLocaleDateString("en-IN", { timeZone: "Asia/Kolkata" });
      var activeTodayCount = 0;

      for (var k = 1; k < rows.length; k++) {
        var dr = rows[k];
        if (!dr[0]) continue;
        var lastSeen = String(dr[6] || "");
        var isToday = lastSeen.indexOf(todayStr) !== -1;
        if (isToday) activeTodayCount++;

        devices.push({
          device_id: String(dr[0]),
          device_model: String(dr[1] || "Unknown"),
          user_name: String(dr[2] || ""),
          phone_number: String(dr[3] || ""),
          city: String(dr[4] || ""),
          app_version: String(dr[5] || "2.9.0"),
          last_seen: lastSeen,
          open_count: Number(dr[7]) || 1
        });
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: "SUCCESS",
        total: devices.length,
        active_today: activeTodayCount,
        devices: devices
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // GET 2: DEVOTEE REGISTRY FOR UNIVERSAL AUTO-COMPLETE & FACE SYNC
    if (e && e.parameter && e.parameter.action === "get_registry") {
      var regSheet = ss.getSheetByName("Devotee_Registry");
      if (!regSheet) {
        return ContentService.createTextOutput(JSON.stringify({
          status: "SUCCESS",
          devotees: []
        })).setMimeType(ContentService.MimeType.JSON);
      }

      var dRows = regSheet.getDataRange().getValues();
      var devotees = [];
      for (var j = 1; j < dRows.length; j++) {
        var dr = dRows[j];
        if (!dr[0] && !dr[1]) continue;
        devotees.push({
          phone_number: String(dr[0] || "").trim(),
          patient_name: String(dr[1] || "").trim(),
          city: String(dr[2] || "").trim(),
          face_vector_b64: String(dr[3] || "").trim(),
          has_photo: String(dr[4] || "").indexOf("हाँ") !== -1
        });
      }

      return ContentService.createTextOutput(JSON.stringify({
        status: "SUCCESS",
        count: devotees.length,
        devotees: devotees
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // GET 3: DEFAULT: FETCH TOKENS
    var sheet = ss.getSheetByName("Sunday_Tokens") || ss.getSheets()[0];
    var rows = sheet.getDataRange().getValues();
    var tokens = [];
    var requestedDate = (e && e.parameter && e.parameter.date) ? e.parameter.date : "";

    for (var i = 1; i < rows.length; i++) {
      var row = rows[i];
      var rowDate = String(row[1]);
      if (requestedDate && rowDate !== requestedDate) {
        continue;
      }
      tokens.push({
        token_number: Number(row[0]) || 0,
        darbar_date: String(row[1]),
        time_str: String(row[2]),
        patient_name: String(row[3]),
        phone_number: String(row[4]),
        city: String(row[5]),
        registered_by: String(row[6]),
        distance_km: Number(row[7]) || 0,
        status: String(row[8]) || "WAITING"
      });
    }

    return ContentService.createTextOutput(JSON.stringify({
      status: "SUCCESS",
      date: requestedDate,
      count: tokens.length,
      tokens: tokens
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "ERROR",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
