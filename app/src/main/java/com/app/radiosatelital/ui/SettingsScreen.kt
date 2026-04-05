package com.app.radiosatelital.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.radiosatelital.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    layoutMode: RadioLayoutMode,
    cardSizeMode: RadioCardSizeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    onLayoutModeChange: (RadioLayoutMode) -> Unit,
    onCardSizeModeChange: (RadioCardSizeMode) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tema section
            Text(
                text = "Tema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Blanco puro
            ThemeOption(
                label = AppThemeMode.PureWhite.label,
                selected = currentTheme == AppThemeMode.PureWhite,
                previewColor = androidx.compose.ui.graphics.Color.White,
                onSelect = { onThemeChange(AppThemeMode.PureWhite) },
            )

            // Negro puro AMOLED
            ThemeOption(
                label = AppThemeMode.PureBlackAmoled.label,
                selected = currentTheme == AppThemeMode.PureBlackAmoled,
                previewColor = androidx.compose.ui.graphics.Color.Black,
                onSelect = { onThemeChange(AppThemeMode.PureBlackAmoled) },
            )

            // Info section
            Text(
                text = "Vista de radios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            ThemeOption(
                label = RadioLayoutMode.OneRow.label,
                selected = layoutMode == RadioLayoutMode.OneRow,
                previewColor = MaterialTheme.colorScheme.surface,
                onSelect = { onLayoutModeChange(RadioLayoutMode.OneRow) },
            )

            ThemeOption(
                label = RadioLayoutMode.TwoRows.label,
                selected = layoutMode == RadioLayoutMode.TwoRows,
                previewColor = MaterialTheme.colorScheme.surfaceVariant,
                onSelect = { onLayoutModeChange(RadioLayoutMode.TwoRows) },
            )

            Text(
                text = "Tamaño de radios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            ThemeOption(
                label = RadioCardSizeMode.Compact.label,
                selected = cardSizeMode == RadioCardSizeMode.Compact,
                previewColor = MaterialTheme.colorScheme.surface,
                onSelect = { onCardSizeModeChange(RadioCardSizeMode.Compact) },
            )

            ThemeOption(
                label = RadioCardSizeMode.Normal.label,
                selected = cardSizeMode == RadioCardSizeMode.Normal,
                previewColor = MaterialTheme.colorScheme.surfaceVariant,
                onSelect = { onCardSizeModeChange(RadioCardSizeMode.Normal) },
            )

            ThemeOption(
                label = RadioCardSizeMode.Large.label,
                selected = cardSizeMode == RadioCardSizeMode.Large,
                previewColor = MaterialTheme.colorScheme.surface,
                onSelect = { onCardSizeModeChange(RadioCardSizeMode.Large) },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Información",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Radio Satelital v1.0",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    previewColor: androidx.compose.ui.graphics.Color,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .background(
                            color = previewColor,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                        )
                        .padding(8.dp)
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
