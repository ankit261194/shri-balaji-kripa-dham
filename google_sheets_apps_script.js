/**
 * ============================================================================
 * श्री बालाजी कृपा धाम (Shri Balaji Kripa Dham) - Google Sheets Token Webhook
 * ============================================================================
 * निर्देश (Instructions):
 * 1. अपनी Google Drive में एक नई Google Spreadsheet बनाएं और नाम दें: "Shri_Balaji_Kripa_Dham_Tokens"
 * 2. ऊपर मेन्यू में 'Extensions' -> 'Apps Script' पर क्लिक करें।
 * 3. पुराना कोड हटाकर यह पूरा कोड पेस्ट कर दें और 'Save' (Ctrl+S) करें।
 * 4. ऊपर 'Deploy' -> 'New deployment' पर क्लिक करें।
 * 5. 'Select type' में 'Web app' चुनें:
 *    - Description: "Balaji Token Webhook"
 *    - Execute as: "Me"
 *    - Who has access: "Anyone" (ताकि ऐप टोकन भेज सके)
 * 6. 'Deploy' पर क्लिक करें और मिली 'Web app URL' को कॉपी कर लें।
 * 7. वह URL ऐप के एडमिन पैनल में "Google Sheet वेबहुक लिंक" में पेस्ट कर दें।
 * ============================================================================
 */

function doPost(e) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000); // Wait up to 10s for concurrency lock

    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    
    // Auto-create royal header row if empty
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
        "स्थिति (Status)"
      ]);
      var headerRange = sheet.getRange("A1:I1");
      headerRange.setFontWeight("bold");
      headerRange.setBackground("#B71C1C");
      headerRange.setFontColor("#FFFFFF");
      headerRange.setHorizontalAlignment("center");
      sheet.setFrozenRows(1);
    }

    var data = JSON.parse(e.postData.contents);

    sheet.appendRow([
      data.token_number || "",
      data.darbar_date || "",
      data.time_str || "",
      data.patient_name || "",
      String(data.phone_number || ""),
      data.city || "",
      data.registered_by || "",
      data.distance_km != null ? data.distance_km : 0,
      data.status || "WAITING"
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
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var rows = sheet.getDataRange().getValues();
    var tokens = [];
    var requestedDate = (e && e.parameter && e.parameter.date) ? e.parameter.date : "";

    // Skip header row
    for (var i = 1; i < rows.length; i++) {
      var row = rows[i];
      var rowDate = String(row[1]);
      if (requestedDate && rowDate !== requestedDate) {
        continue; // Filter by date if requested
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
      tokens: tokens,
      count: tokens.length
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "ERROR",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
