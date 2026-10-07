package com.example.shribalajikripadham.ui.home.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * Pro-Tier Dedicated Ashram Information Tab.
 * Clear, dignified details regarding Guruji, timings, rules, and helpline.
 */
@Composable
fun AshramTabContent(
    isHindi: Boolean,
    settings: AshramSettings,
    currentTheme: SacredTheme,
    onNavigateToTravelGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Ashram Overview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, currentTheme.cardBorderColor.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚩", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isHindi) {
                        "परम पूज्य गुरुदेव तेजवीर सिंह जी महाराज के पावन सानिध्य में संकट मोचन श्री बालाजी महाराज, प्रेतराज सरकार एवं भैरव बाबा का दिव्य धाम।"
                    } else {
                        "Divine abode of Shri Balaji Maharaj, Pretraj Sarkar & Bhairav Baba under the grace of Param Pujya Gurudev Tejveer Singh Ji Maharaj."
                    },
                    fontSize = 12.5.sp,
                    color = Color(0xFF374151),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isHindi) "📍 ग्राम डूँगरा जाट, जिला बुलन्दशहर (उत्तर प्रदेश)" else "📍 Gram Dungra Jaat, Bulandshahr (U.P.)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
            }
        }

        // 2. Darbar & Aarti Timings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, currentTheme.cardBorderColor.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isHindi) "⏰ पावन आरती व दरबार समय" else "⏰ Sacred Timings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = currentTheme.primaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                TimingRow("मंगलवार दरबार (बुलन्दशहर)", "प्रातः 9:00 बजे से दोपहर 2:00 बजे तक")
                TimingRow("रविवार दरबार (डूँगरा जाट)", "प्रातः 8:00 बजे से प्रभु इच्छा तक")
                TimingRow("संध्या आरती", "सायं 7:00 बजे प्रतिदिन")
            }
        }

        // 3. Contact & Helpline
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, currentTheme.cardBorderColor.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isHindi) "📞 आश्रम सेवा व संपर्क" else "📞 Ashram Helpline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = currentTheme.primaryColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val phone = settings.contactPhone.ifBlank { "9100100251" }
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "कॉल करने में असमर्थ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "📞 कॉल करें" else "📞 Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToTravelGuide,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(if (isHindi) "🗺️ मार्ग गाइड ➔" else "🗺️ Route ➔", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingRow(label: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF4B5563))
        Text(text = time, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
    }
}
