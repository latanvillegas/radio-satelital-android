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
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(),
            )
        }
    }
}
