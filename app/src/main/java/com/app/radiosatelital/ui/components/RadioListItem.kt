package com.app.radiosatelital.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.ui.RadioCardSizeMode
import com.app.radiosatelital.ui.locationLabel

@Composable
fun RadioListItem(
    station: RadioStation,
    cardSizeMode: RadioCardSizeMode,
    selected: Boolean,
    liveListeners: Int,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    showFavoriteAction: Boolean = true,
) {
    val sizing = rememberRadioItemSizing(cardSizeMode)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 0.5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sizing.minHeight)
                .padding(horizontal = sizing.horizontalPadding, vertical = sizing.verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(sizing.contentSpacing),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!station.logoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        AsyncImage(
                            model = station.logoUrl,
                            contentDescription = "${station.name} logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.Radio,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = sizing.titleStyle,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = station.locationLabel,
                    style = sizing.subtitleStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (liveListeners > 0) {
                    Text(
                        text = "${liveListeners.coerceAtLeast(0)} escuchando",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (showFavoriteAction) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(sizing.favoriteTouchSize),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar favorito" else "Agregar favorito",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(sizing.favoriteIconSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberRadioItemSizing(mode: RadioCardSizeMode): RadioItemSizing {
    return when (mode) {
        RadioCardSizeMode.Compact -> RadioItemSizing(
            minHeight = 60.dp,
            horizontalPadding = 12.dp,
            verticalPadding = 8.dp,
            contentSpacing = 10.dp,
            logoContainerSize = 36.dp,
            logoIconSize = 20.dp,
            favoriteTouchSize = 40.dp,
            favoriteIconSize = 20.dp,
            titleStyle = MaterialTheme.typography.titleSmall,
            subtitleStyle = MaterialTheme.typography.labelMedium,
        )

        RadioCardSizeMode.Normal -> RadioItemSizing(
            minHeight = 68.dp,
            horizontalPadding = 14.dp,
            verticalPadding = 12.dp,
            contentSpacing = 12.dp,
            logoContainerSize = 44.dp,
            logoIconSize = 24.dp,
            favoriteTouchSize = 48.dp,
            favoriteIconSize = 24.dp,
            titleStyle = MaterialTheme.typography.titleMedium,
            subtitleStyle = MaterialTheme.typography.bodySmall,
        )

        RadioCardSizeMode.Large -> RadioItemSizing(
            minHeight = 82.dp,
            horizontalPadding = 16.dp,
            verticalPadding = 14.dp,
            contentSpacing = 14.dp,
            logoContainerSize = 52.dp,
            logoIconSize = 28.dp,
            favoriteTouchSize = 54.dp,
            favoriteIconSize = 26.dp,
            titleStyle = MaterialTheme.typography.titleLarge,
            subtitleStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}

private data class RadioItemSizing(
    val minHeight: androidx.compose.ui.unit.Dp,
    val horizontalPadding: androidx.compose.ui.unit.Dp,
    val verticalPadding: androidx.compose.ui.unit.Dp,
    val contentSpacing: androidx.compose.ui.unit.Dp,
    val logoContainerSize: androidx.compose.ui.unit.Dp,
    val logoIconSize: androidx.compose.ui.unit.Dp,
    val favoriteTouchSize: androidx.compose.ui.unit.Dp,
    val favoriteIconSize: androidx.compose.ui.unit.Dp,
    val titleStyle: androidx.compose.ui.text.TextStyle,
    val subtitleStyle: androidx.compose.ui.text.TextStyle,
)
