package com.example.shribalajikripadham.ui.home.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.theme.SacredTheme

enum class ProHomeTab(
    val titleHindi: String,
    val titleEnglish: String,
    val icon: String
) {
    DARSHAN("दर्शन", "Darshan", "🏠"),
    TOKEN("टोकन", "Token", "🎟️"),
    BHAKTI("भक्ति", "Bhakti", "📿"),
    ASHRAM("आश्रम", "Ashram", "ℹ️")
}

/**
 * Pro-Tier Material 3 Sacred Bottom Navigation Bar.
 * Clean, lightweight 4-tab bar providing frictionless navigation.
 */
@Composable
fun ProBottomNavBar(
    selectedTab: ProHomeTab,
    onTabSelected: (ProHomeTab) -> Unit,
    isHindi: Boolean,
    currentTheme: SacredTheme,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = currentTheme.surfaceLight,
        tonalElevation = 6.dp,
        modifier = modifier
    ) {
        ProHomeTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Text(
                        text = tab.icon,
                        fontSize = if (isSelected) 20.sp else 18.sp
                    )
                },
                label = {
                    Text(
                        text = if (isHindi) tab.titleHindi else tab.titleEnglish,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) currentTheme.primaryColor else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = currentTheme.primaryColor,
                    selectedTextColor = currentTheme.primaryColor,
                    indicatorColor = currentTheme.primaryColor.copy(alpha = 0.12f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
