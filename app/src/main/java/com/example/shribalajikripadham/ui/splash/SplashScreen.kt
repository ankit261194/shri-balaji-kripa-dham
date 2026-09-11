package com.example.shribalajikripadham.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.R
import com.example.shribalajikripadham.theme.*

@Composable
fun SplashScreen(
    onEnterDarbar: () -> Unit,
    isHindi: Boolean,
    onToggleLanguage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5C001E), // Deep Maroon
                        Color(0xFF9E2A00),
                        Color(0xFFE65100), // Sacred Saffron
                        Color(0xFFFFB300)  // Gold
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Language Toggle at Top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onToggleLanguage,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isHindi) "English" else "हिंदी",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sacred Divine Portrait Picture with Golden Aura
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GoldLight,
                                GoldSecondary,
                                Color(0xFFC68400)
                            )
                        )
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Shri Balaji Maharaj Divine Portrait",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Invocation
            Text(
                text = "॥ श्री हनुमते नमः ॥",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = if (isHindi) "श्री बालाजी कृपा धाम" else "Shri Balaji Kripa Dham",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle / Address
            Text(
                text = if (isHindi) "डूँगरा जाट, बुलन्दशहर (उ.प्र.)" else "Dungra Jaat, Bulandshahr (U.P.)",
                fontSize = 17.sp,
                color = Color(0xFFFFF8E1),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Guruji Badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHindi) "परम पूज्य गुरुजी तेजवीर सिंह जी" else "Param Pujya Guruji Tejveer Singh Ji",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaroonAccent,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "॥ संकट कटे मिटे सब पीरा, जो सुमिरै हनुमत बलबीरा ॥" else "|| Sankat Kate Mite Sab Peera, Jo Sumirai Hanumat Balbeera ||",
                        fontSize = 13.sp,
                        color = SaffronDark,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi)
                            "भूत-प्रेत व मानसिक समस्याओं का इलाज पूर्णतः निःशुल्क (FREE) किया जाता है।"
                        else
                            "Treatment for spiritual affliction & mental distress is provided 100% FREE.",
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Enter Button
            Button(
                onClick = onEnterDarbar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFDF8),
                    contentColor = MaroonAccent
                ),
                shape = RoundedCornerShape(27.dp),
                elevation = ButtonDefaults.buttonElevation(6.dp)
            ) {
                Text(
                    text = if (isHindi) "दरबार प्रवेश करें  ➔" else "Enter Darbar  ➔",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Developer: Ankit Chaudhary (Anti Gravity)",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
