/**
 * ============================================================================
 * श्री बालाजी कृपा धाम (Shri Balaji Kripa Dham) - Master Cloud Ledger Webhook
 * ============================================================================
 * निर्देश (Instructions):
 * 1. अपनी नई या मौजूदा Google Spreadsheet खोलें।
 * 2. मेन्यू में 'Extensions' (एक्सटेंशन) -> 'Apps Script' पर जाएं।
 * 3. पुराना कोड हटाकर यह पूरा कोड पेस्ट करें और Ctrl+S (Save) दबाएं।
 * 4. ऊपर दाएँ कोने में 'Deploy' -> 'New deployment' पर क्लिक करें।
 * 5. 'Select type' में 'Web app' चुनें।
 * 6. 'Execute as' में 'Me' और 'Who has access' में 'Anyone' चुनें।
 * 7. 'Deploy' दबाएं और 'Web app URL' को कॉपी करके ऐप के सुपर-एडमिन पैनल में डाल दें।
 * 
 * विशेषताएँ (Features):
 * 1. Sheet 1: 'Sunday_Tokens' - क्रमवार 1, 2, 3... टोकन, कभी डिलीट नहीं, लाइव स्टेटस
 * 2. Sheet 2: 'Dharmashala_Payments' - कमरा बुकिंग, दान व रसीद बही-खाता
 * 3. Sheet 3: 'Arzi_Box_Ledger' - अर्जी पेटिका, बड़ी-छोटी अर्जी व नकद/ऑनलाइन हिसाब
 * 4. Sheet 4: 'Daily_Expenses' - आश्रम दैनिक खर्च व रसीदें
 * 5. Sheet 5: 'Devotee_Registry' - केंद्रीय भक्त सूची, फोन व शहर
 * 6. LockService: 50 टोकन/मिनट पर भी 0% डुप्लीकेट गारंटी
 * ============================================================================
 */

function doPost(e) {
  var lock = LockService.getScriptLock();
  try {
    // 15 seconds concurrency queue lock
    lock.waitLock(15000);

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var rawText = e.postData ? e.postData.contents : '{}';
    var data = JSON.parse(rawText);
    var action = data.action || 'RECORD_TOKEN';
    var timeStr = new Date().toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' });

    // ------------------------------------------------------------------------
    // 1. ATOMIC SEQUENTIAL TOKEN GENERATION (LockService Guaranteed 1,2,3...)
    // ------------------------------------------------------------------------
    if (action === 'GENERATE_ATOMIC_TOKEN') {
      var tokenSheet = getOrCreateSheet(ss, 'Sunday_Tokens', [
        'टोकन नं (Token No)', 'दिनांक (Date)', 'समय (Time)', 'भक्त / मरीज का नाम (Patient Name)',
        'मोबाइल नंबर (Phone)', 'शहर / गाँव (City)', 'पंजीकरणकर्ता (Registered By)',
        'दूरी किमी (Distance Km)', 'स्थिति (Status)', 'फोटो स्थिति (Photo)', 'रद्द का कारण (Cancel Reason)'
      ], '#B71C1C');

      var targetDate = data.darbar_date || getTodayDateStr();
      var rows = tokenSheet.getDataRange().getValues();
      var maxToken = 0;

      for (var i = 1; i < rows.length; i++) {
        var rDate = String(rows[i][1]).trim();
        if (rDate === targetDate) {
          var tNum = Number(rows[i][0]) || 0;
          if (tNum > maxToken) maxToken = tNum;
        }
      }

      var nextToken = maxToken + 1;

      tokenSheet.appendRow([
        nextToken,
        targetDate,
        data.time_str || timeStr,
        data.patient_name || '',
        String(data.phone_number || ''),
        data.city || '',
        data.registered_by || 'APP',
        data.distance_km != null ? data.distance_km : 0,
        'WAITING',
        data.has_photo ? '📸 संलग्न (Yes)' : '— बिना फोटो',
        ''
      ]);

      formatTokenRow(tokenSheet, tokenSheet.getLastRow());

      return jsonResponse({
        status: 'SUCCESS',
        token_number: nextToken,
        darbar_date: targetDate,
        message: 'टोकन #' + nextToken + ' सफलतापूर्वक जारी हुआ!'
      });
    }

    // ------------------------------------------------------------------------
    // 2. RECORD SINGLE TOKEN
    // ------------------------------------------------------------------------
    if (action === 'RECORD_TOKEN') {
      var tokenSheet = getOrCreateSheet(ss, 'Sunday_Tokens', [
        'टोकन नं (Token No)', 'दिनांक (Date)', 'समय (Time)', 'भक्त / मरीज का नाम (Patient Name)',
        'मोबाइल नंबर (Phone)', 'शहर / गाँव (City)', 'पंजीकरणकर्ता (Registered By)',
        'दूरी किमी (Distance Km)', 'स्थिति (Status)', 'फोटो स्थिति (Photo)', 'रद्द का कारण (Cancel Reason)'
      ], '#B71C1C');

      var tNum = Number(data.token_number) || (tokenSheet.getLastRow());
      tokenSheet.appendRow([
        tNum,
        data.darbar_date || getTodayDateStr(),
        data.time_str || timeStr,
        data.patient_name || '',
        String(data.phone_number || ''),
        data.city || '',
        data.registered_by || 'APP',
        data.distance_km != null ? data.distance_km : 0,
        data.status || 'WAITING',
        data.has_photo ? '📸 संलग्न (Yes)' : '— बिना फोटो',
        ''
      ]);

      formatTokenRow(tokenSheet, tokenSheet.getLastRow());

      return jsonResponse({
        status: 'SUCCESS',
        token_number: tNum,
        message: 'टोकन #' + tNum + ' Google Sheet में दर्ज हुआ!'
      });
    }

    // ------------------------------------------------------------------------
    // 3. BATCH TOKENS (Register Scan OCR)
    // ------------------------------------------------------------------------
    if (action === 'BATCH_TOKENS' && Array.isArray(data.tokens)) {
      var tokenSheet = getOrCreateSheet(ss, 'Sunday_Tokens', [
        'टोकन नं (Token No)', 'दिनांक (Date)', 'समय (Time)', 'भक्त / मरीज का नाम (Patient Name)',
        'मोबाइल नंबर (Phone)', 'शहर / गाँव (City)', 'पंजीकरणकर्ता (Registered By)',
        'दूरी किमी (Distance Km)', 'स्थिति (Status)', 'फोटो स्थिति (Photo)', 'रद्द का कारण (Cancel Reason)'
      ], '#B71C1C');

      var batch = data.tokens;
      for (var b = 0; b < batch.length; b++) {
        var t = batch[b];
        tokenSheet.appendRow([
          Number(t.token_number) || '',
          t.darbar_date || getTodayDateStr(),
          t.time_str || timeStr,
          t.patient_name || '',
          String(t.phone_number || ''),
          t.city || '',
          t.registered_by || 'REGISTER_SCAN',
          t.distance_km != null ? t.distance_km : 0,
          t.status || 'WAITING',
          t.has_photo ? '📸 संलग्न' : '📄 रजिस्टर प्रविष्टि',
          ''
        ]);
        formatTokenRow(tokenSheet, tokenSheet.getLastRow());
      }

      return jsonResponse({
        status: 'SUCCESS',
        count: batch.length,
        message: batch.length + ' टोकन रजिस्टर से दर्ज हुए!'
      });
    }

    // ------------------------------------------------------------------------
    // 4. UPDATE TOKEN STATUS (NEVER DELETE, AUDIT COMPLIANT)
    // ------------------------------------------------------------------------
    if (action === 'UPDATE_TOKEN_STATUS') {
      var tokenSheet = ss.getSheetByName('Sunday_Tokens');
      if (!tokenSheet) return jsonResponse({ status: 'ERROR', message: 'Sunday_Tokens sheet not found' });

      var rows = tokenSheet.getDataRange().getValues();
      var targetDate = String(data.darbar_date || '').trim();
      var targetNum = Number(data.token_number);
      var newStatus = data.new_status || 'COMPLETED';
      var reason = data.cancel_reason || '';
      var found = false;

      for (var r = 1; r < rows.length; r++) {
        var rowNum = Number(rows[r][0]);
        var rowDate = String(rows[r][1]).trim();
        if (rowNum === targetNum && (!targetDate || rowDate === targetDate)) {
          var statusDisplay = (newStatus === 'COMPLETED') ? '✅ दर्शन सम्पन्न' :
                              (newStatus === 'CANCELLED') ? '❌ रद्द (CANCELLED)' : newStatus;
          tokenSheet.getRange(r + 1, 9).setValue(statusDisplay);
          if (reason) {
            tokenSheet.getRange(r + 1, 11).setValue(reason + ' [' + timeStr + ']');
          }
          found = true;
          break;
        }
      }

      return jsonResponse({
        status: found ? 'SUCCESS' : 'NOT_FOUND',
        message: found ? 'टोकन #' + targetNum + ' का स्टेटस अपडेट हुआ: ' + newStatus : 'टोकन नहीं मिला'
      });
    }

    // ------------------------------------------------------------------------
    // 5. DHARMASHALA & DONATION PAYMENTS (Financial Ledger)
    // ------------------------------------------------------------------------
    if (action === 'RECORD_PAYMENT') {
      var paySheet = getOrCreateSheet(ss, 'Dharmashala_Payments', [
        'रसीद / पेमेंट आईडी', 'दिनांक व समय', 'भक्त / यात्री का नाम', 'मोबाइल नंबर',
        'प्रयोजन (कमरा / दान / यात्रा)', 'राशि (₹)', 'माध्यम (UPI / नकद)',
        'ट्रांजेक्शन आईडी (UTR)', 'सत्यापनकर्ता (Verified By)', 'टिप्पणी (Notes)'
      ], '#1B5E20');

      paySheet.appendRow([
        data.payment_id || ('PAY_' + Date.now()),
        timeStr,
        data.devotee_name || '',
        String(data.devotee_phone || ''),
        data.purpose || 'धर्मशाला कमरा',
        Number(data.amount) || 0,
        data.payment_mode || 'UPI_QR',
        data.transaction_id || '',
        data.verified_by || 'SUPER_ADMIN',
        data.notes || ''
      ]);

      var lr = paySheet.getLastRow();
      paySheet.getRange(lr, 1).setHorizontalAlignment('center');
      paySheet.getRange(lr, 2).setHorizontalAlignment('center');
      paySheet.getRange(lr, 6).setHorizontalAlignment('right').setFontWeight('bold');

      return jsonResponse({
        status: 'SUCCESS',
        message: 'पेमेंट रसीद ₹' + (data.amount || 0) + ' दर्ज हुई!'
      });
    }

    // ------------------------------------------------------------------------
    // 6. ARZI DISTRIBUTION LEDGER (अर्जी पेटिका हिसाब)
    // ------------------------------------------------------------------------
    if (action === 'RECORD_ARZI') {
      var arziSheet = getOrCreateSheet(ss, 'Arzi_Box_Ledger', [
        'दिनांक व समय', 'दरबार तारीख', 'भक्त का नाम', 'मोबाइल नंबर',
        'बड़ी अर्जी (संख्या)', 'छोटी अर्जी (संख्या)', 'कुल राशि (₹)',
        'भुगतान स्थिति', 'भुगतान माध्यम', 'काटने वाला सेवादार', 'टिप्पणी'
      ], '#E65100');

      arziSheet.appendRow([
        timeStr,
        data.darbar_date || getTodayDateStr(),
        data.devotee_name || '',
        String(data.phone_number || ''),
        Number(data.big_arzi_qty) || 0,
        Number(data.small_arzi_qty) || 0,
        Number(data.total_amount) || 0,
        data.is_paid ? '✅ प्राप्त (PAID)' : '⏳ बाकी (UNPAID)',
        data.payment_mode || 'CASH',
        data.recorded_by || 'SUPER_ADMIN',
        data.notes || ''
      ]);

      var alr = arziSheet.getLastRow();
      arziSheet.getRange(alr, 1).setHorizontalAlignment('center');
      arziSheet.getRange(alr, 2).setHorizontalAlignment('center');
      arziSheet.getRange(alr, 7).setHorizontalAlignment('right').setFontWeight('bold');

      return jsonResponse({
        status: 'SUCCESS',
        message: 'अर्जी हिसाब दर्ज हुआ!'
      });
    }

    // ------------------------------------------------------------------------
    // 7. DAILY EXPENSES (आश्रम दैनिक खर्च)
    // ------------------------------------------------------------------------
    if (action === 'RECORD_EXPENSE') {
      var expSheet = getOrCreateSheet(ss, 'Daily_Expenses', [
        'दिनांक व समय', 'खर्च तारीख', 'मद / शीर्षक', 'श्रेणी (Category)',
        'राशि (₹)', 'दर्जकर्ता (Added By)', 'रसीद फोटो लिंक', 'टिप्पणी'
      ], '#C2185B');

      expSheet.appendRow([
        timeStr,
        data.expense_date || getTodayDateStr(),
        data.title || '',
        data.category || 'सामान्य',
        Number(data.amount) || 0,
        data.added_by || 'SUPER_ADMIN',
        data.receipt_uri || '',
        data.notes || ''
      ]);

      var elr = expSheet.getLastRow();
      expSheet.getRange(elr, 1).setHorizontalAlignment('center');
      expSheet.getRange(elr, 5).setHorizontalAlignment('right').setFontWeight('bold');

      return jsonResponse({
        status: 'SUCCESS',
        message: 'दैनिक खर्च ₹' + (data.amount || 0) + ' दर्ज हुआ!'
      });
    }

    // ------------------------------------------------------------------------
    // 8. DEVOTEE REGISTRY (Universal Central Directory)
    // ------------------------------------------------------------------------
    if (action === 'UPSERT_DEVOTEE') {
      var regSheet = getOrCreateSheet(ss, 'Devotee_Registry', [
        'मोबाइल नंबर (Phone)', 'भक्त का नाम (Name)', 'शहर / गाँव (City)',
        'कुल फेरे (Visits)', 'अंतिम दर्शन तारीख (Last Visit)', 'पंजीकरणकर्ता'
      ], '#4A148C');

      var phoneStr = String(data.phone_number || '').trim();
      var rows = regSheet.getDataRange().getValues();
      var foundRow = -1;
      var curVisits = 0;

      for (var r = 1; r < rows.length; r++) {
        if (String(rows[r][0]).trim() === phoneStr) {
          foundRow = r + 1;
          curVisits = Number(rows[r][3]) || 1;
          break;
        }
      }

      if (foundRow > 0) {
        if (data.patient_name) regSheet.getRange(foundRow, 2).setValue(data.patient_name);
        if (data.city) regSheet.getRange(foundRow, 3).setValue(data.city);
        regSheet.getRange(foundRow, 4).setValue(curVisits + 1);
        regSheet.getRange(foundRow, 5).setValue(timeStr);
      } else {
        regSheet.appendRow([
          phoneStr,
          data.patient_name || '',
          data.city || '',
          1,
          timeStr,
          data.registered_by || 'APP'
        ]);
      }

      return jsonResponse({
        status: 'SUCCESS',
        message: 'भक्त ' + (data.patient_name || '') + ' रजिस्ट्री में सिंक हुआ!'
      });
    }

    return jsonResponse({
      status: 'WARNING',
      message: 'Unrecognized action: ' + action
    });

  } catch (err) {
    return jsonResponse({
      status: 'ERROR',
      message: err.toString()
    });
  } finally {
    lock.releaseLock();
  }
}

function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var paramAction = (e && e.parameter && e.parameter.action) ? e.parameter.action : 'get_tokens';

    if (paramAction === 'get_summary') {
      var tokenSheet = ss.getSheetByName('Sunday_Tokens');
      var paySheet = ss.getSheetByName('Dharmashala_Payments');

      var totalTokens = tokenSheet ? Math.max(0, tokenSheet.getLastRow() - 1) : 0;
      var totalPayments = 0;
      var totalPaymentsCount = 0;

      if (paySheet && paySheet.getLastRow() > 1) {
        var pRows = paySheet.getRange(2, 6, paySheet.getLastRow() - 1, 1).getValues();
        for (var p = 0; p < pRows.length; p++) {
          totalPayments += (Number(pRows[p][0]) || 0);
          totalPaymentsCount++;
        }
      }

      return jsonResponse({
        status: 'SUCCESS',
        spreadsheet_name: ss.getName(),
        total_tokens: totalTokens,
        total_payments_amount: totalPayments,
        total_payments_count: totalPaymentsCount,
        last_updated: new Date().toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' })
      });
    }

    var sheet = ss.getSheetByName('Sunday_Tokens') || ss.getSheets()[0];
    var rows = sheet.getDataRange().getValues();
    var tokens = [];
    var requestedDate = (e && e.parameter && e.parameter.date) ? e.parameter.date : '';

    for (var i = 1; i < rows.length; i++) {
      var row = rows[i];
      var rowDate = String(row[1]);
      if (requestedDate && rowDate !== requestedDate) continue;

      tokens.push({
        token_number: Number(row[0]) || 0,
        darbar_date: String(row[1]),
        time_str: String(row[2]),
        patient_name: String(row[3]),
        phone_number: String(row[4]),
        city: String(row[5]),
        registered_by: String(row[6]),
        distance_km: Number(row[7]) || 0,
        status: String(row[8]) || 'WAITING',
        cancel_reason: String(row[10] || '')
      });
    }

    return jsonResponse({
      status: 'SUCCESS',
      date: requestedDate,
      count: tokens.length,
      tokens: tokens
    });

  } catch (err) {
    return jsonResponse({
      status: 'ERROR',
      message: err.toString()
    });
  }
}

function getOrCreateSheet(ss, sheetName, headers, headerColor) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow(headers);
    var hr = sheet.getRange(1, 1, 1, headers.length);
    hr.setFontWeight('bold');
    hr.setBackground(headerColor || '#0D47A1');
    hr.setFontColor('#FFFFFF');
    hr.setHorizontalAlignment('center');
    sheet.setFrozenRows(1);
  }
  return sheet;
}

function formatTokenRow(sheet, rowIdx) {
  try {
    sheet.getRange(rowIdx, 1).setHorizontalAlignment('center').setFontWeight('bold');
    sheet.getRange(rowIdx, 2).setHorizontalAlignment('center');
    sheet.getRange(rowIdx, 3).setHorizontalAlignment('center');
    sheet.getRange(rowIdx, 7).setHorizontalAlignment('center');
    sheet.getRange(rowIdx, 9).setHorizontalAlignment('center');
  } catch (e) {}
}

function getTodayDateStr() {
  var d = new Date();
  var year = d.getFullYear();
  var month = ('0' + (d.getMonth() + 1)).slice(-2);
  var day = ('0' + d.getDate()).slice(-2);
  return year + '-' + month + '-' + day;
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}