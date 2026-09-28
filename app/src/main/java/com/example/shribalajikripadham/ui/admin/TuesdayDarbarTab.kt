package com.example.shribalajikripadham.ui.admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.network.HostingerCentralSyncManager
import com.example.shribalajikripadham.data.repository.AshramRepository
import com.example.shribalajikripadham.hardware.GeofenceLocationManager
import com.example.shribalajikripadham.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuesdayDarbarTab(
    isHindi: Boolean,
    settings: AshramSettings,
    onSettingsUpdated: (AshramSettings) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AshramRepository(context) }
    val scope = rememberCoroutineScope()

    // Form fields for Tuesday Darbar
    var isEnabled by remember { mutableStateOf(settings.isTuesdayDarbarEnabled) }
    var darbarName by remember { mutableStateOf(settings.tuesdayDarbarName.ifBlank { "श्री बालाजी कृपा धाम (मंगलवार दरबार, बुलन्दशहर)" }) }
    var darbarAddress by remember { mutableStateOf(settings.tuesdayDarbarAddress.ifBlank { "बुलन्दशहर, उत्तर प्रदेश" }) }

    var latitudeStr by remember { mutableStateOf(if (settings.tuesdayLatitude != 0.0) settings.tuesdayLatitude.toString() else "28.4089") }
    var longitudeStr by remember { mutableStateOf(if (settings.tuesdayLongitude != 0.0) settings.tuesdayLongitude.toString() else "77.8498") }
    var radiusStr by remember { mutableStateOf(settings.tuesdayAllowedRadiusMeters.toInt().toString()) }
    var outstationKmStr by remember { mutableStateOf(settings.tuesdayOutstationMinDistanceKm.toInt().toString()) }

    var darbarTimings by remember { mutableStateOf(settings.tuesdayDarbarTimings.ifBlank { "प्रत्येक मंगलवार प्रातः 8:00 AM से सायं 5:00 PM तक" }) }
    var serviceMode by remember { mutableStateOf(settings.tuesdayTokenServiceMode.ifBlank { "AUTO_TUESDAY" }) }

    var currentServingToken by remember { mutableIntStateOf(settings.tuesdayCurrentServingToken) }
    var runningTokenNumber by remember { mutableIntStateOf(settings.tuesdayRunningTokenNumber) }
    var tokenNotice by remember { mutableStateOf(settings.tuesdayTokenNotice) }

    var isCapturingGps by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaroonPrimary),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚩", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "मंगलवार दरबार नियंत्रण (बुलन्दशहर)" else "Tuesday Darbar Control (Bulandshahr)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = GoldLight
                        )
                        Text(
                            text = if (isHindi) "सुपर एडमिन नियंत्रण • टोकन, जीपीएस व वेबसाइट सिंक" else "Super Admin • Token, GPS & Website Sync",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 2. Master Toggle Switch Card
        Card(
            colors = CardDefaults.cardColors(containerColor = if (isEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, if (isEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnabled)
                            (if (isHindi) "🟢 मंगलवार दरबार सक्रिय (ON)" else "🟢 Tuesday Darbar Active (ON)")
                        else
                            (if (isHindi) "🔴 मंगलवार दरबार बंद (OFF)" else "🔴 Tuesday Darbar Paused (OFF)"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isEnabled) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isEnabled)
                            (if (isHindi) "भक्तों के लिए ऐप व वेबसाइट पर मंगलवार टोकन व काउंटर प्रदर्शित होगा।" else "Tuesday tokens visible on app & website.")
                        else
                            (if (isHindi) "मंगलवार दरबार सेवा पूर्णतः स्थगित है। ऐप व वेबसाइट से छिपी रहेगी।" else "Tuesday Darbar is completely paused."),
                        fontSize = 11.5.sp,
                        color = Color.DarkGray
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2E7D32),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFC62828)
                    )
                )
            }
        }

        // 3. Venue Name & Address Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isHindi) "📍 स्थान एवं पता विवरण" else "📍 Venue & Address Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaroonPrimary
                )

                OutlinedTextField(
                    value = darbarName,
                    onValueChange = { darbarName = it },
                    label = { Text(if (isHindi) "मंगलवार दरबार का नाम *" else "Tuesday Darbar Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = darbarAddress,
                    onValueChange = { darbarAddress = it },
                    label = { Text(if (isHindi) "दरबार का पूरा पता (बुलन्दशहर) *" else "Full Address (Bulandshahr) *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // 4. GPS Coordinates & 1-Click Current Location Capture
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isHindi) "🌐 जीपीएस लोकेशन व दायरा (Geofence)" else "🌐 GPS Location & Geofence",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaroonPrimary
                )

                Text(
                    text = if (isHindi)
                        "जब आप बुलन्दशहर दरबार स्थल पर हों, तो नीचे दिए गए 1-क्लिक बटन को दबाएं। ऐप स्वतः आपके वर्तमान स्थान के अक्षांश और देशांतर सेव कर लेगा।"
                    else
                        "When physically at Bulandshahr venue, tap the 1-click button below to capture exact GPS coordinates.",
                    fontSize = 11.5.sp,
                    color = Color.DarkGray
                )

                Button(
                    onClick = {
                        isCapturingGps = true
                        GeofenceLocationManager.requestFreshLocation(context) { loc ->
                            isCapturingGps = false
                            if (loc != null) {
                                latitudeStr = String.format(Locale.US, "%.7f", loc.latitude)
                                longitudeStr = String.format(Locale.US, "%.7f", loc.longitude)
                                val resolvedCity = GeofenceLocationManager.resolveVillageAndCity(context, loc.latitude, loc.longitude)
                                if (resolvedCity.isNotBlank() && darbarAddress.isBlank()) {
                                    darbarAddress = resolvedCity
                                }
                                Toast.makeText(
                                    context,
                                    if (isHindi) "✅ वर्तमान लोकेशन सेट हुई! (परिशुद्धता: ${loc.accuracy.toInt()}m)" else "✅ GPS coordinates captured! (acc: ${loc.accuracy.toInt()}m)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    if (isHindi) "⚠️ लोकेशन प्राप्त नहीं हुई। कृपया GPS चालू करें।" else "⚠️ Could not acquire GPS.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCapturingGps
                ) {
                    Text(
                        text = if (isCapturingGps)
                            (if (isHindi) "⏳ जीपीएस सिग्नल खोज रहे हैं..." else "⏳ Acquiring GPS...")
                        else
                            (if (isHindi) "📍 मेरी वर्तमान GPS लोकेशन से सेट करें (1-क्लिक)" else "📍 Set to My Current GPS (1-Click)"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = latitudeStr,
                        onValueChange = { latitudeStr = it },
                        label = { Text("अक्षांश (Latitude)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = longitudeStr,
                        onValueChange = { longitudeStr = it },
                        label = { Text("देशांतर (Longitude)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = radiusStr,
                        onValueChange = { radiusStr = it },
                        label = { Text(if (isHindi) "परिसर दायरा (मीटर)" else "Radius (meters)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = outstationKmStr,
                        onValueChange = { outstationKmStr = it },
                        label = { Text(if (isHindi) "आउटस्टेशन दूरी (किमी)" else "Outstation Min (km)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 5. Timings & Token Service Mode Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isHindi) "⏰ समय एवं टोकन वितरण मोड" else "⏰ Timings & Token Service Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaroonPrimary
                )

                OutlinedTextField(
                    value = darbarTimings,
                    onValueChange = { darbarTimings = it },
                    label = { Text(if (isHindi) "मंगलवार दरबार का समय" else "Tuesday Darbar Timings") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = if (isHindi) "टोकन सेवा मोड चुनें:" else "Select Token Service Mode:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )

                val modes = listOf(
                    Triple("AUTO_TUESDAY", "स्वतः मंगलवार मोड", "सोमवार 8 PM काउंटडाउन ➔ मंगलवार 8 AM-5 PM खुला"),
                    Triple("FORCE_OPEN", "हमेशा खुला (Force Open)", "तत्काल टोकन वितरण चालू"),
                    Triple("FORCE_CLOSED", "स्थगित (Force Closed)", "टोकन वितरण पूर्णतः बंद")
                )

                modes.forEach { (modeKey, title, desc) ->
                    val isSelected = serviceMode == modeKey
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)),
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) SaffronPrimary else Color.LightGray),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        onClick = { serviceMode = modeKey }
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { serviceMode = modeKey },
                                colors = RadioButtonDefaults.colors(selectedColor = SaffronPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) MaroonPrimary else Color.Black)
                                Text(desc, fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }

        // 6. Live Token Counter & Notice Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isHindi) "🔢 लाइव टोकन काउंटर (ऐप व वेबसाइट)" else "🔢 Live Token Counter (App & Website)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaroonPrimary
                )

                Text(
                    text = if (isHindi)
                        "यह काउंटर सीधे वेबसाइट shribalajikripadham.online और ऐप पर लाइव दिखता है।"
                    else
                        "This counter is shown live on website and app.",
                    fontSize = 11.5.sp,
                    color = Color.DarkGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "वर्तमान में चल रहा टोकन:" else "Current Serving Token:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "#$currentServingToken",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { if (currentServingToken > 0) currentServingToken -= 1 },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-1", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Button(
                            onClick = { currentServingToken += 1 },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("+1", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentServingToken.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v != null && v >= 0) currentServingToken = v
                        },
                        label = { Text("चल रहा टोकन (Serving)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = runningTokenNumber.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v != null && v >= 0) runningTokenNumber = v
                        },
                        label = { Text("कुल जारी टोकन (Issued)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = tokenNotice,
                    onValueChange = { tokenNotice = it },
                    label = { Text(if (isHindi) "मंगलवार टोकन विशेष सूचना (वेबसाइट पर दिखेगी)" else "Tuesday Token Notice") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Status Message Banner
        if (statusMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage!!,
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // 7. Save & Cloud Sync Button
        Button(
            onClick = {
                isSaving = true
                statusMessage = null
                scope.launch {
                    try {
                        val parsedLat = latitudeStr.toDoubleOrNull() ?: 28.4089
                        val parsedLng = longitudeStr.toDoubleOrNull() ?: 77.8498
                        val parsedRadius = radiusStr.toDoubleOrNull() ?: 200.0
                        val parsedOutstation = outstationKmStr.toDoubleOrNull() ?: 30.0

                        val updatedSettings = settings.copy(
                            isTuesdayDarbarEnabled = isEnabled,
                            tuesdayDarbarName = darbarName.trim(),
                            tuesdayDarbarAddress = darbarAddress.trim(),
                            tuesdayLatitude = parsedLat,
                            tuesdayLongitude = parsedLng,
                            tuesdayAllowedRadiusMeters = parsedRadius,
                            tuesdayOutstationMinDistanceKm = parsedOutstation,
                            tuesdayDarbarTimings = darbarTimings.trim(),
                            tuesdayTokenServiceMode = serviceMode,
                            tuesdayCurrentServingToken = currentServingToken,
                            tuesdayRunningTokenNumber = runningTokenNumber,
                            tuesdayTokenNotice = tokenNotice.trim()
                        )

                        // 1. Update SQLite
                        repository.updateSettings(updatedSettings)
                        repository.persistCurrentSettingsToAllLayers()

                        // 2. Sync to Hostinger Live MySQL & Website
                        var networkSynced = false
                        withContext(Dispatchers.IO) {
                            try {
                                networkSynced = HostingerCentralSyncManager.updateFullLiveConfig(updatedSettings).first
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        onSettingsUpdated(updatedSettings)
                        statusMessage = if (isHindi)
                            if (networkSynced) "✅ मंगलवार सेटिंग्स सुरक्षित एवं वेबसाइट पर लाइव सिंक हो गईं!"
                            else "✅ मंगलवार सेटिंग्स फोन में सुरक्षित हो गईं! (वेबसाइट सिंक बैकग्राउंड में होगी)"
                        else
                            "✅ Tuesday Darbar settings successfully saved!"
                        Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        statusMessage = "❌ त्रुटि: ${e.localizedMessage ?: "अज्ञात समस्या"}"
                        Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isSaving
        ) {
            Text(
                text = if (isSaving)
                    (if (isHindi) "⏳ सुरक्षित व सिंक हो रहा है..." else "⏳ Saving & Syncing...")
                else
                    (if (isHindi) "💾 मंगलवार सेटिंग्स सुरक्षित करें एवं वेबसाइट पर सिंक करें" else "💾 Save & Sync to Website"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
