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
    onNavigateToTravelGuide: () -> Unit = {},
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

        // 2. Darbar Timings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, currentTheme.cardBorderColor.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isHindi) "⏰ पावन दरबार समय" else "⏰ Sacred Darbar Timings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = currentTheme.primaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                TimingRow(
                    label = if (isHindi) "रविवार दरबार (डूँगरा जाट)" else "Sunday Darbar (Dungra Jaat)",
                    time = settings.darbarTimings.ifBlank { if (isHindi) "प्रातः 8:00 बजे से प्रभु इच्छा तक" else "8:00 AM onwards" }
                )

                // मंगलवार दरबार केवल तभी दिखेगा जब सुपर एडमिन ने मास्टर स्विच चालू किया हो
                if (settings.isTuesdayDarbarEnabled) {
                    TimingRow(
                        label = if (isHindi) "मंगलवार दरबार (बुलन्दशहर)" else "Tuesday Darbar (Bulandshahr)",
                        time = settings.tuesdayDarbarTimings.ifBlank { if (isHindi) "प्रातः 9:00 बजे से दोपहर 2:00 बजे तक" else "9:00 AM - 2:00 PM" }
                    )
                }
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
                        onClick = {
                            val addr = settings.address.ifBlank { "Shri Balaji Kripa Dham Dungra Jat Bulandshahr" }
                            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(addr))
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                            } catch (e: Exception) {
                                Toast.makeText(context, "मानचित्र खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(if (isHindi) "📍 गूगल मैप्स ➔" else "📍 Directions ➔", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val channelUrl = if (settings.whatsappChannelUrl.isNotBlank() && !settings.whatsappChannelUrl.contains("chat.whatsapp.com")) {
                            settings.whatsappChannelUrl
                        } else {
                            "https://whatsapp.com/channel/0029VaCZJTmJ3jv2UwiMtY1w"
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(channelUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, if (isHindi) "व्हाट्सएप खोलने में असमर्थ" else "Unable to open WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHindi) "💬 आधिकारिक व्हाट्सएप चैनल फॉलो करें ➔" else "💬 Follow Official WhatsApp Channel ➔",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
