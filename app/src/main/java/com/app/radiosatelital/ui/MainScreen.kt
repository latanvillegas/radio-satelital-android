package com.app.radiosatelital.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.layout.WindowInsets
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.defaultStations
import com.app.radiosatelital.ui.components.BottomNavigationBar
import com.app.radiosatelital.ui.components.MiniPlayerBar
import com.app.radiosatelital.ui.components.RadioListItem
import com.app.radiosatelital.ui.components.NavigationDrawerContent
import com.app.radiosatelital.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    coordinator: PlaybackCoordinator,
    themeMode: AppThemeMode,
    layoutMode: RadioLayoutMode,
    onThemeChange: (AppThemeMode) -> Unit,
    onLayoutModeChange: (RadioLayoutMode) -> Unit,
) {
    val context = LocalContext.current
    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        }.getOrDefault("1.0")
    }
    val uiState = coordinator.uiState
    val cloudViewModel: RadioCloudViewModel = viewModel()
    val cloudState = cloudViewModel.uiState
    val baseCatalog = remember(cloudState.publicRadios) {
        (defaultStations + cloudState.publicRadios).distinctBy { it.url }
    }
    var showPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(setOf<Int>()) }
    val userStations = remember { mutableStateListOf<UserRadioStation>() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val scope = rememberCoroutineScope()

    if (showSettings) {
        SettingsScreen(
            currentTheme = themeMode,
            layoutMode = layoutMode,
            onThemeChange = onThemeChange,
            onLayoutModeChange = onLayoutModeChange,
            onBack = { showSettings = false },
        )
        return
    }

    if (showSearch) {
        SearchScreen(
            stations = baseCatalog,
            favorites = favorites,
            onFavoriteClick = { index ->
                favorites = if (favorites.contains(index)) {
                    favorites - index
                } else {
                    favorites + index
                }
            },
            onStationSelect = { index: Int, station: RadioStation ->
                coordinator.play(index, station, baseCatalog)
                showPlayer = true
            },
            onClose = { showSearch = false },
        )
        return
    }

    if (showPlayer && uiState.selectedStation != null) {
        val selectedIndex = uiState.selectedStationId?.toIntOrNull() ?: -1
        PlayerScreen(
            uiState = uiState,
            isFavorite = favorites.contains(selectedIndex),
            onBack = { showPlayer = false },
            onFavoriteClick = {
                if (selectedIndex >= 0) {
                    favorites = if (favorites.contains(selectedIndex)) {
                        favorites - selectedIndex
                    } else {
                        favorites + selectedIndex
                    }
                }
            },
            onPrevious = coordinator::previous,
            onPlayPause = coordinator::togglePlayback,
            onNext = coordinator::next,
            onVolumeChange = coordinator::setVolume,
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerContent(
                    onAboutClick = {
                        scope.launch {
                            drawerState.close()
                            showAbout = true
                        }
                    },
                    onShareClick = {
                        scope.launch {
                            drawerState.close()
                            val appId = context.packageName
                            val shareText = buildString {
                                append("Escucha radios en vivo con Radio Satelital")
                                append("\n\n")
                                append("App: Radio Satelital")
                                append("\n")
                                append("https://play.google.com/store/apps/details?id=")
                                append(appId)
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir app"))
                        }
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Radio Satelital",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
                        IconButton(onClick = { coordinator.setTab(HomeTab.Favorites) }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favoritos")
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                        }
                    },
                    scrollBehavior = topBarScrollBehavior,
                )
            },
            bottomBar = {
                Column {
                    uiState.selectedStation?.let { station ->
                        MiniPlayerBar(
                            station = station,
                            artistName = uiState.nowPlayingArtist,
                            songTitle = uiState.nowPlayingTitle,
                            isPlaying = uiState.playbackState is RadioPlaybackState.Playing,
                            onOpenPlayer = { showPlayer = true },
                            onPrevious = coordinator::previous,
                            onPlayPause = coordinator::togglePlayback,
                            onNext = coordinator::next,
                        )
                    }
                    BottomNavigationBar(
                        currentTab = uiState.currentTab,
                        onTabSelected = coordinator::setTab,
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.currentTab == HomeTab.Country) {
                    CountryExplorerScreen(
                        stations = (baseCatalog + userStations.map { it.toRadioStation() }).distinctBy { it.url },
                        selectedStationUrl = uiState.selectedStation?.url,
                        favorites = favorites,
                        onFavoriteClick = { index ->
                            favorites = if (favorites.contains(index)) favorites - index else favorites + index
                        },
                        onStationClick = { index, station ->
                            val catalog = (baseCatalog + userStations.map { it.toRadioStation() }).distinctBy { it.url }
                            coordinator.play(index, station, catalog)
                        },
                    )
                } else if (uiState.currentTab == HomeTab.Mine) {
                    MineRadiosScreen(
                        stations = userStations,
                        cloudMessage = cloudState.infoMessage,
                        onPlayStation = { catalog, index ->
                            catalog.getOrNull(index)?.let { station ->
                                coordinator.play(index, station, catalog)
                            }
                        },
                        onSaveStation = { station, editIndex ->
                            if (editIndex == null) {
                                userStations.add(station)
                                cloudViewModel.submitUserRadio(station)
                            } else if (editIndex in userStations.indices) {
                                userStations[editIndex] = station
                            }
                        },
                        onDeleteStation = { index ->
                            if (index in userStations.indices) {
                                userStations.removeAt(index)
                            }
                        },
                    )
                } else {
                    val filteredIndices = when (uiState.currentTab) {
                        HomeTab.Music -> baseCatalog.indices.toList()
                        HomeTab.Favorites -> baseCatalog.indices.filter { favorites.contains(it) }
                        HomeTab.Mine -> emptyList()
                        HomeTab.Country -> emptyList()
                    }

                    if (layoutMode == RadioLayoutMode.TwoRows) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                count = filteredIndices.size,
                                key = { filteredIndices[it] },
                            ) { displayIndex ->
                                val index = filteredIndices[displayIndex]
                                val station = baseCatalog[index]
                                RadioListItem(
                                    station = station,
                                    selected = uiState.selectedStation?.url == station.url,
                                    isFavorite = favorites.contains(index),
                                    onFavoriteClick = {
                                        favorites = if (favorites.contains(index)) favorites - index else favorites + index
                                    },
                                    onClick = { coordinator.play(index, station, baseCatalog) },
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                count = filteredIndices.size,
                                key = { filteredIndices[it] },
                            ) { displayIndex ->
                                val index = filteredIndices[displayIndex]
                                val station = baseCatalog[index]
                                RadioListItem(
                                    station = station,
                                    selected = uiState.selectedStation?.url == station.url,
                                    isFavorite = favorites.contains(index),
                                    onFavoriteClick = {
                                        favorites = if (favorites.contains(index)) favorites - index else favorites + index
                                    },
                                    onClick = { coordinator.play(index, station, baseCatalog) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Acerca de") },
            text = {
                Text(
                    "Radio Satelital\n" +
                        "Version: $appVersion\n\n" +
                        "Escucha radios en vivo desde una sola aplicacion.\n" +
                        "Desarrollador: Radio Satelital Team",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Cerrar")
                }
            },
        )
    }
}
