package com.example.shribalajikripadham.ui.token

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.model.AshramSettings
import com.example.shribalajikripadham.data.model.Token
import com.example.shribalajikripadham.theme.*
import com.example.shribalajikripadham.util.TokenPdfExporter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PremiumRoyalTokenCard(
    token: Token,
    settings: AshramSettings,
    isHindi: Boolean,
    savedImageUri: Uri? = null,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedTimestamp = sdfDate.format(Date(token.createdAt))

    // Royal Golden-Saffron Gradient Brushes
    val goldGradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFD4AF37), // Metallic Gold
            Color(0xFFFFDF73), // Brilliant Light Gold
            Color(0xFFFF8C00), // Deep Saffron Orange
            Color(0xFFD4AF37)
        )
    )

    val royalHeaderBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF800000), // Deep Royal Maroon
            Color(0xFF4A0000)  // Imperial Dark Burgundy
        )
    )

    val parchmentBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFDF8),
            Color(0xFFFFF9EE),
            Color(0xFFFFF3E0)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .border(BorderStroke(4.dp, goldGradientBrush), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(parchmentBgBrush)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP ROYAL HEADER: Sanstha Name + Official Logo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.5.dp, Color(0xFFFFD700)), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(royalHeaderBrush)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🚩", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "॥ श्री हनुमते नमः ॥",
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🚩", fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = settings.ashramName.ifEmpty { "श्री बालाजी कृपा धाम, डूँगरा जाट" },
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = settings.address.ifEmpty { "ग्राम डूँगरा जाट, जिला बुलन्दशहर (उत्तर प्रदेश)" },
                            color = Color(0xFFFFE082),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. SUB-HEADER: Param Pujya Guruji Tejveer Singh Ji + Photo/Icon
            Surface(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFFA000)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFCC80))
                            .border(BorderStroke(1.5.dp, Color(0xFFE65100)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "परम पूज्य गुरुजी तेजवीर सिंह जी",
                            color = Color(0xFF800000),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHindi) "के पावन सानिध्य में रविवार दिव्य दरबार" else "Sunday Divya Darbar Pass",
                            color = Color(0xFF5D4037),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. CORE DATA FIELD: LARGE / BOLD TOKEN NUMBER
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, goldGradientBrush),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHindi) "॥ आपका अधिकृत दर्शन टोकन ॥" else "Official Darbar Token Number",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF795548)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "#${token.tokenNumber}",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD84315),
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "स्थिति: कतार में सक्रिय (WAITING)" else "STATUS: ACTIVE IN QUEUE",
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. CORE DATA FIELDS TABLE: Name, Mobile, Coming From, Timestamp
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    TokenDetailRow(
                        label = if (isHindi) "भक्त का नाम (Name):" else "Devotee Name:",
                        value = token.patientName,
                        isBold = true,
                        valueColor = Color(0xFF800000)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF0F0F0))

                    TokenDetailRow(
                        label = if (isHindi) "मोबाइल नंबर (Mobile):" else "Mobile Number:",
                        value = token.phoneNumber,
                        valueColor = Color.Black
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF0F0F0))

                    TokenDetailRow(
                        label = if (isHindi) "निवासी पता (Resident Of):" else "Resident Of / From:",
                        value = token.originAddress.ifEmpty { token.city },
                        isBold = true,
                        valueColor = SaffronPrimary
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF0F0F0))

                    TokenDetailRow(
                        label = if (isHindi) "गंतव्य स्थान (Destination):" else "Destination:",
                        value = token.destinationAddress,
                        valueColor = Color(0xFF800000)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF0F0F0))

                    TokenDetailRow(
                        label = if (isHindi) "कुल सड़क दूरी (Distance):" else "Total Road Distance:",
                        value = if (token.distanceKm >= 0f) "%.1f किमी (KM)".format(token.distanceKm) else if (isHindi) "आश्रम परिसर (स्थानीय)" else "Inside Campus",
                        isBold = true,
                        valueColor = Color(0xFFE65100)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF0F0F0))

                    TokenDetailRow(
                        label = if (isHindi) "टोकन जारी समय (Issued):" else "Generation Time:",
                        value = formattedTimestamp,
                        valueColor = Color(0xFF424242)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF0F0F0))

                    TokenDetailRow(
                        label = if (isHindi) "दरबार दिनांक (Date):" else "Darbar Date:",
                        value = token.darbarDate.ifEmpty { "Upcoming Sunday" },
                        valueColor = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1-Tap PDF Receipt Download & Share Button
            Button(
                onClick = {
                    TokenPdfExporter.shareSingleTokenReceipt(context, token, settings)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📄", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "टोकन रसीद PDF डाउनलोड / शेयर करें" else "Download / Share Token PDF Receipt",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. AUTOMATIC GALLERY SAVE CONFIRMATION BANNER
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🖼️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isHindi)
                                "यह टोकन कार्ड आपकी फोटो गैलरी (Gallery) में स्वतः सुरक्षित हो गया है!"
                            else
                                "Auto-Saved directly into your Mobile Photo Gallery (Pictures)!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF795548)
                        )
                        Text(
                            text = if (isHindi) "अलग से डाउनलोड करने की आवश्यकता नहीं है।" else "No manual download required.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. ACTION BUTTONS: View in Gallery, Share, Back to Home
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View in Gallery Button
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                if (savedImageUri != null) {
                                    setDataAndType(savedImageUri, "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } else {
                                    type = "image/*"
                                }
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback to gallery main app
                            try {
                                val galleryIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_GALLERY)
                                }
                                context.startActivity(galleryIntent)
                            } catch (ignored: Exception) {}
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF800000)),
                    border = BorderStroke(1.dp, Color(0xFF800000))
                ) {
                    Text(if (isHindi) "📂 गैलरी में देखें" else "View Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Share Button
                Button(
                    onClick = {
                        val shareText = """
                            🚩 श्री बालाजी कृपा धाम, डूँगरा जाट 🚩
                            परम पूज्य गुरुजी तेजवीर सिंह जी
                            ---------------------------------
                            रविवार दर्शन टोकन क्रमांक: #${token.tokenNumber}
                            भक्त का नाम: ${token.patientName}
                            आगमन स्थान: ${token.city}
                            टोकन जारी समय: $formattedTimestamp
                            दरबार तिथि: ${token.darbarDate}
                            ---------------------------------
                            भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क इलाज।
                        """.trimIndent()

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "श्री बालाजी कृपा धाम टोकन #${token.tokenNumber}")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            if (savedImageUri != null) {
                                putExtra(Intent.EXTRA_STREAM, savedImageUri)
                                type = "image/png"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "टोकन शेयर करें"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text(if (isHindi) "📤 शेयर करें" else "Share Token", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Back to Home Button
            Button(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaroonAccent)
            ) {
                Text(if (isHindi) "मुख्य पृष्ठ पर लौटें (Back to Home)" else "Back to Home", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TokenDetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color.DarkGray
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
