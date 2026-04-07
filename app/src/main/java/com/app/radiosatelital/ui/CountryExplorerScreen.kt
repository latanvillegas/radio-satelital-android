package com.app.radiosatelital.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.ui.components.RadioListItem
import com.app.radiosatelital.ui.locationLabel

private const val ALL_FILTER = "Todos"

@Composable
fun CountryExplorerScreen(
    stations: List<RadioStation>,
    selectedStationUrl: String?,
    cardSizeMode: RadioCardSizeMode,
    onStationClick: (Int, RadioStation) -> Unit,
) {
    val indexedStations = remember(stations) { stations.mapIndexed { index, station -> index to station } }

    var selectedContinent by remember { mutableStateOf(ALL_FILTER) }
    var selectedRegion by remember { mutableStateOf(ALL_FILTER) }
    var selectedCountry by remember { mutableStateOf(ALL_FILTER) }
    var selectedCity by remember { mutableStateOf(ALL_FILTER) }
    var searchQuery by remember { mutableStateOf("") }

    val continentOptions = buildList {
        add(ALL_FILTER)
        addAll(indexedStations.map { (_, station) -> continentFor(station.country) }.distinct().sorted())
    }

    val continentFiltered = indexedStations.filter { (_, station) ->
        selectedContinent == ALL_FILTER || continentFor(station.country) == selectedContinent
    }

    val regionOptions = buildList {
        add(ALL_FILTER)
        addAll(
            continentFiltered.mapNotNull { (_, station) -> station.region.takeIf { it.isNotBlank() } }
                .distinct()
                .sorted(),
        )
    }

    if (selectedRegion !in regionOptions) selectedRegion = ALL_FILTER

    val regionFiltered = continentFiltered.filter { (_, station) ->
        selectedRegion == ALL_FILTER || station.region == selectedRegion
    }

    val countryOptions = buildList {
        add(ALL_FILTER)
        addAll(regionFiltered.map { (_, station) -> station.country }.distinct().sorted())
    }

    if (selectedCountry !in countryOptions) selectedCountry = ALL_FILTER

    val countryFiltered = regionFiltered.filter { (_, station) ->
        selectedCountry == ALL_FILTER || station.country == selectedCountry
    }

    val cityOptions = buildList {
        add(ALL_FILTER)
        addAll(countryFiltered.mapNotNull { (_, station) -> cityFromRegion(station.region) }.distinct().sorted())
    }

    if (selectedCity !in cityOptions) selectedCity = ALL_FILTER

    val filteredStations = countryFiltered.filter { (_, station) ->
        val matchesCity = selectedCity == ALL_FILTER || cityFromRegion(station.region) == selectedCity
        val matchesSearch = searchQuery.isBlank() || stationMatchesSearch(station, searchQuery)
        matchesCity && matchesSearch
    }

    val isSearching = searchQuery.isNotBlank()

    val groupedSearchStations = if (isSearching) {
        filteredStations
            .groupBy { (_, station) -> cityFromRegion(station.region) ?: "Sin ciudad" }
            .toSortedMap()
    } else {
        emptyMap()
    }

    val activeRoute = listOf(selectedContinent, selectedRegion, selectedCountry, selectedCity)
        .filter { it != ALL_FILTER }
        .joinToString(" > ")
        .ifBlank { "Todos" }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "País",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!isSearching) {
                    Text(
                        text = "Ruta activa: $activeRoute",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Mostrando ${filteredStations.size} de ${indexedStations.size} radios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar radio o ciudad") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (isSearching) {
                            searchQuery = ""
                        } else {
                            selectedContinent = ALL_FILTER
                            selectedRegion = ALL_FILTER
                            selectedCountry = ALL_FILTER
                            selectedCity = ALL_FILTER
                            searchQuery = ""
                        }
                    }) {
                        Text(if (isSearching) "Limpiar búsqueda" else "Limpiar filtros")
                    }
                }
            }
        }

        if (!isSearching) {
            FilterSection(
                title = "Continente",
                count = continentOptions.count { it != ALL_FILTER },
                options = continentOptions,
                selected = selectedContinent,
                onSelected = {
                    selectedContinent = it
                    selectedRegion = ALL_FILTER
                    selectedCountry = ALL_FILTER
                    selectedCity = ALL_FILTER
                },
            )

            FilterSection(
                title = "Region",
                count = regionOptions.count { it != ALL_FILTER },
                options = regionOptions,
                selected = selectedRegion,
                onSelected = {
                    selectedRegion = it
                    selectedCountry = ALL_FILTER
                    selectedCity = ALL_FILTER
                },
            )

            FilterSection(
                title = "Pais",
                count = countryOptions.count { it != ALL_FILTER },
                options = countryOptions,
                selected = selectedCountry,
                onSelected = {
                    selectedCountry = it
                    selectedCity = ALL_FILTER
                },
            )

            FilterSection(
                title = "Ciudad",
                count = cityOptions.count { it != ALL_FILTER },
                options = cityOptions,
                selected = selectedCity,
                onSelected = { selectedCity = it },
            )
        }

        if (filteredStations.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "No hay radios para este filtro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Prueba limpiar filtros o vuelve a una ciudad anterior.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(
                    when (cardSizeMode) {
                        RadioCardSizeMode.Compact -> 6.dp
                        RadioCardSizeMode.Normal -> 8.dp
                        RadioCardSizeMode.Large -> 10.dp
                    },
                ),
            ) {
                if (isSearching) {
                    items(
                        items = groupedSearchStations.entries.toList(),
                        key = { entry -> entry.key },
                    ) { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "${entry.key} (${entry.value.size})",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            entry.value.forEach { (index, station) ->
                                CountrySearchRadioItem(
                                    station = station,
                                    selected = selectedStationUrl == station.url,
                                    searchQuery = searchQuery,
                                    onClick = { onStationClick(index, station) },
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredStations,
                        key = { (index, _) -> index },
                    ) { (index, station) ->
                        RadioListItem(
                            station = station,
                            cardSizeMode = cardSizeMode,
                            selected = selectedStationUrl == station.url,
                            liveListeners = 0,
                            isFavorite = false,
                            onFavoriteClick = {},
                            showFavoriteAction = false,
                            onClick = { onStationClick(index, station) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountrySearchRadioItem(
    station: RadioStation,
    selected: Boolean,
    searchQuery: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Radio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = highlightedText(station.name, searchQuery),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = highlightedText(station.locationLabel, searchQuery),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    val source = text
    val target = query.trim()
    val start = source.indexOf(target, ignoreCase = true)
    if (start < 0) {
        append(source)
        return@buildAnnotatedString
    }
    val end = (start + target.length).coerceAtMost(source.length)
    append(source.substring(0, start))
    pushStyle(
        SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        ),
    )
    append(source.substring(start, end))
    pop()
    append(source.substring(end))
}

@Composable
private fun FilterSection(
    title: String,
    count: Int,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                )
            }
        }
    }
}

private fun stationMatchesSearch(station: RadioStation, query: String): Boolean {
    val lowerQuery = query.trim().lowercase()
    if (lowerQuery.isBlank()) return true
    return listOf(
        station.name,
        station.country,
        station.region,
        cityFromRegion(station.region).orEmpty(),
        continentFor(station.country),
        station.locationLabel,
    ).any { value -> value.contains(lowerQuery, ignoreCase = true) }
}

private fun cityFromRegion(region: String): String? {
    if (region.isBlank()) return null
    return region.split(",").firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

private fun continentFor(country: String): String = when (country) {
    "Perú", "Argentina", "Venezuela", "Colombia", "México", "Guatemala", "Ecuador", "El Salvador", "Chile", "Bolivia", "Costa Rica", "Puerto Rico" -> "America Latina"
    "EE.UU" -> "America del Norte"
    else -> "Otros"
}
