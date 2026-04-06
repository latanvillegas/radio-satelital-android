package com.app.radiosatelital.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.radiosatelital.RadioStation
import kotlinx.coroutines.delay
import java.text.Normalizer

private enum class SearchScope(val label: String) {
    Name("Nombre"),
    Country("País"),
    Region("Región"),
    Genre("Género"),
}

private data class SearchResult(
    val index: Int,
    val station: RadioStation,
    val score: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    stations: List<RadioStation>,
    cardSizeMode: RadioCardSizeMode,
    favorites: Set<Int>,
    onFavoriteClick: (Int) -> Unit,
    onStationSelect: (Int, RadioStation) -> Unit,
) {
    var searchInput by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var selectedScope by rememberSaveable { mutableStateOf(SearchScope.Name) }
    var recentSearches by rememberSaveable { mutableStateOf(listOf<String>()) }

    LaunchedEffect(searchInput) {
        delay(250)
        debouncedQuery = searchInput
    }

    fun saveSearch(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return
        recentSearches = listOf(cleaned) + recentSearches.filterNot { it.equals(cleaned, ignoreCase = true) }
            .take(4)
    }

    val filteredStations = remember(debouncedQuery, stations, selectedScope) {
        if (debouncedQuery.isBlank()) {
            emptyList()
        } else {
            val query = normalizeForSearch(debouncedQuery)
            stations.mapIndexed { index, station -> index to station }
                .mapNotNull { (index, station) ->
                    val result = scoreStationByScope(station, query, selectedScope)
                    if (result == Int.MAX_VALUE) null else SearchResult(index, station, result)
                }
                .sortedWith(compareBy<SearchResult> { it.score }.thenBy { it.station.name.lowercase() })
        }
    }

    val resultCount = filteredStations.size

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Search field
            TextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Buscar emisoras...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { searchInput = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        saveSearch(searchInput)
                        filteredStations.firstOrNull()?.let {
                            onStationSelect(it.index, it.station)
                        }
                    },
                ),
                singleLine = true,
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SearchScope.values().toList()) { scope ->
                    FilterChip(
                        selected = selectedScope == scope,
                        onClick = { selectedScope = scope },
                        label = { Text(scope.label) },
                    )
                }
            }

            if (debouncedQuery.isNotBlank()) {
                Text(
                    text = "$resultCount resultados",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Search results or empty state
            if (debouncedQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Escribe el nombre de una emisora o ubicación",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (recentSearches.isNotEmpty()) {
                            Text(
                                text = "Búsquedas recientes",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(recentSearches) { recent ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { searchInput = recent },
                                        label = { Text(recent) },
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (filteredStations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "No encontramos radios",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Prueba buscar por país o género",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { searchInput = "" }) {
                            Text("Limpiar búsqueda")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        when (cardSizeMode) {
                            RadioCardSizeMode.Compact -> 6.dp
                            RadioCardSizeMode.Normal -> 8.dp
                            RadioCardSizeMode.Large -> 10.dp
                        },
                    ),
                ) {
                    items(
                        items = filteredStations,
                        key = { it.index },
                    ) { result ->
                        SearchResultItem(
                            station = result.station,
                            query = debouncedQuery,
                            isFavorite = favorites.contains(result.index),
                            onFavoriteClick = { onFavoriteClick(result.index) },
                            onClick = {
                                saveSearch(searchInput)
                                onStationSelect(result.index, result.station)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    station: RadioStation,
    query: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .let { it },
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = highlightedText(station.name, query),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = highlightedText(station.locationLabel, query),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (station.genre.isNotBlank()) {
                    Text(
                        text = highlightedText(station.genre, query),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar favorito" else "Agregar favorito",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun highlightedText(text: String, query: String) = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }

    val normalizedSource = normalizeForSearch(text)
    val normalizedQuery = normalizeForSearch(query)
    val start = normalizedSource.indexOf(normalizedQuery)

    if (start < 0) {
        append(text)
        return@buildAnnotatedString
    }

    val end = (start + normalizedQuery.length).coerceAtMost(text.length)
    append(text.substring(0, start))
    pushStyle(
        SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        ),
    )
    append(text.substring(start, end))
    pop()
    append(text.substring(end))
}

private fun scoreStationByScope(station: RadioStation, query: String, scope: SearchScope): Int {
    val name = normalizeForSearch(station.name)
    val country = normalizeForSearch(station.country)
    val region = normalizeForSearch(station.region)
    val genre = normalizeForSearch(station.genre)

    fun scoreField(value: String): Int {
        if (value.startsWith(query)) return 0
        if (value.contains(query)) return 1
        return Int.MAX_VALUE
    }

    return when (scope) {
        SearchScope.Name -> scoreField(name)
        SearchScope.Country -> scoreField(country)
        SearchScope.Region -> scoreField(region)
        SearchScope.Genre -> scoreField(genre)
    }
}

private fun normalizeForSearch(value: String): String {
    val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
    return normalized.replace("\\p{M}+".toRegex(), "").lowercase().trim()
}
