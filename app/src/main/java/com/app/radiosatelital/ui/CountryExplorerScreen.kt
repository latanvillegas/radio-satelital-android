package com.app.radiosatelital.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.ui.components.RadioListItem

private const val ALL_FILTER = "Todos"

@Composable
fun CountryExplorerScreen(
    stations: List<RadioStation>,
    selectedStationUrl: String?,
    cardSizeMode: RadioCardSizeMode,
    favorites: Set<Int>,
    onFavoriteClick: (Int) -> Unit,
    onStationClick: (Int, RadioStation) -> Unit,
) {
    val indexedStations = remember(stations) { stations.mapIndexed { index, station -> index to station } }

    var selectedContinent by remember { mutableStateOf(ALL_FILTER) }
    var selectedRegion by remember { mutableStateOf(ALL_FILTER) }
    var selectedCountry by remember { mutableStateOf(ALL_FILTER) }
    var selectedCity by remember { mutableStateOf(ALL_FILTER) }

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
        selectedCity == ALL_FILTER || cityFromRegion(station.region) == selectedCity
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterSection(
            title = "Continente",
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
            options = countryOptions,
            selected = selectedCountry,
            onSelected = {
                selectedCountry = it
                selectedCity = ALL_FILTER
            },
        )

        FilterSection(
            title = "Ciudad",
            options = cityOptions,
            selected = selectedCity,
            onSelected = { selectedCity = it },
        )

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
            items(
                items = filteredStations,
                key = { (index, _) -> index },
            ) { (index, station) ->
                RadioListItem(
                    station = station,
                    cardSizeMode = cardSizeMode,
                    selected = selectedStationUrl == station.url,
                    isFavorite = favorites.contains(index),
                    onFavoriteClick = { onFavoriteClick(index) },
                    onClick = { onStationClick(index, station) },
                )
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
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

private fun cityFromRegion(region: String): String? {
    if (region.isBlank()) return null
    return region.split(",").firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

private fun continentFor(country: String): String = when (country) {
    "Perú", "Argentina", "Venezuela", "Colombia", "México", "Guatemala", "Ecuador", "El Salvador", "Chile", "Bolivia", "Costa Rica", "Puerto Rico" -> "America Latina"
    "EE.UU" -> "America del Norte"
    else -> "Otros"
}
