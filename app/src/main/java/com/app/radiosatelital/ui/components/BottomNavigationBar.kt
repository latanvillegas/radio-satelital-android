package com.app.radiosatelital.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.app.radiosatelital.ui.HomeTab

@Composable
fun BottomNavigationBar(currentTab: HomeTab, onTabSelected: (HomeTab) -> Unit) {
    NavigationBar {
        HomeTab.entries.forEach { tab ->
            val icon = when (tab) {
                HomeTab.Music -> Icons.Filled.LibraryMusic
                HomeTab.Favorites -> Icons.Filled.Star
                HomeTab.Country -> Icons.Filled.Public
                HomeTab.Mine -> Icons.Filled.Tune
            }
            val tintColor = when (tab) {
                HomeTab.Music -> Color(0xFF1E88E5)
                HomeTab.Favorites -> Color(0xFFFDD835)
                HomeTab.Country -> Color(0xFF43A047)
                HomeTab.Mine -> Color(0xFFFB8C00)
            }
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        icon,
                        contentDescription = tab.label,
                        tint = tintColor
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Unspecified,
                    unselectedIconColor = Color.Unspecified
                ),
            )
        }
    }
}
