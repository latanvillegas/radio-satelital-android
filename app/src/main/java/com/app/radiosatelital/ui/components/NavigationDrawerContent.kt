package com.app.radiosatelital.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NavigationDrawerContent(
    onAboutClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Text(
            text = "Radio Satelital",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        // Menu items
        NavigationDrawerItem(
            label = { Text("Acerca de") },
            icon = { Icon(Icons.Filled.Info, contentDescription = "Acerca de") },
            selected = false,
            onClick = {
                onAboutClick()
            },
        )

        NavigationDrawerItem(
            label = { Text("Compartir") },
            icon = { Icon(Icons.Filled.Share, contentDescription = "Compartir") },
            selected = false,
            onClick = {
                onShareClick()
            },
        )
    }
}
