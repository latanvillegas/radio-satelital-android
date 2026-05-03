package com.app.radiosatelital.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.defaultStations
import com.app.radiosatelital.ui.components.BottomNavigationBar
import com.app.radiosatelital.ui.components.MiniPlayerBar
import com.app.radiosatelital.ui.components.NavigationDrawerContent
import com.app.radiosatelital.ui.components.RadioListItem
import com.app.radiosatelital.ui.theme.AppThemeMode
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    coordinator: PlaybackCoordinator,
    themeMode: AppThemeMode,
    layoutMode: RadioLayoutMode,
    cardSizeMode: RadioCardSizeMode,
    animationsEnabled: Boolean,
    statusBarVisible: Boolean,
    onThemeChange: (AppThemeMode) -> Unit,
    onLayoutModeChange: (RadioLayoutMode) -> Unit,
    onCardSizeModeChange: (RadioCardSizeMode) -> Unit,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    onStatusBarVisibleChange: (Boolean) -> Unit,
    onResetAppearance: () -> Unit,
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
    val liveListenersByUrl = cloudState.liveListenersByUrl
    val baseCatalog = remember(cloudState.publicRadios) {
        (defaultStations + cloudState.publicRadios).distinctBy { it.url }
    }

    var favorites by remember { mutableStateOf(setOf<Int>()) }
    val userStations = remember { mutableStateListOf<UserRadioStation>() }
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME,
    ) {
        composable(AppRoutes.HOME) {
            HomeRootScreen(
                navController = navController,
                coordinator = coordinator,
                uiState = uiState,
                animationsEnabled = animationsEnabled,
                layoutMode = layoutMode,
                cardSizeMode = cardSizeMode,
                baseCatalog = baseCatalog,
                liveListenersByUrl = liveListenersByUrl,
                userStations = userStations,
                favorites = favorites,
                onFavoritesChange = { favorites = it },
                openShare = { shareApp(context) },
            )
        }

        composable(AppRoutes.SEARCH) {
            SearchScreen(
                stations = baseCatalog,
                cardSizeMode = cardSizeMode,
                favorites = favorites,
                onFavoriteClick = { index ->
                    favorites = if (favorites.contains(index)) favorites - index else favorites + index
                },
                onStationSelect = { index, station ->
                    coordinator.play(index, station, baseCatalog)
                    navController.navigate(AppRoutes.PLAYER) {
                        popUpTo(AppRoutes.SEARCH) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                currentTheme = themeMode,
                layoutMode = layoutMode,
                cardSizeMode = cardSizeMode,
                animationsEnabled = animationsEnabled,
                statusBarVisible = statusBarVisible,
                onThemeChange = onThemeChange,
                onLayoutModeChange = onLayoutModeChange,
                onCardSizeModeChange = onCardSizeModeChange,
                onAnimationsEnabledChange = onAnimationsEnabledChange,
                onStatusBarVisibleChange = onStatusBarVisibleChange,
                onResetAppearance = onResetAppearance,
                onOpenAdminModeration = {
                    navController.navigate(AppRoutes.ADMIN_MODERATION) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.navigateUp() },
            )
        }

        composable(AppRoutes.ADMIN_MODERATION) {
            AdminModerationScreen(
                onBack = { navController.navigateUp() },
            )
        }

        composable(AppRoutes.ABOUT) {
            AboutScreen(
                appVersion = appVersion,
                onBack = { navController.navigateUp() },
            )
        }

        composable(AppRoutes.MINE_RADIOS) {
            MineRadiosScreen(
                stations = userStations,
                cloudMessage = cloudState.infoMessage,
                submissionStatusByUrl = cloudState.submissionStatusByUrl,
                onPlayStation = { catalog, index ->
                    catalog.getOrNull(index)?.let { station ->
                        coordinator.play(index, station, catalog)
                        navController.navigate(AppRoutes.PLAYER)
                    }
                },
                onSaveStation = { station, editIndex ->
                    if (editIndex == null) {
                        userStations.add(station)
                        cloudViewModel.submitUserRadio(station)
                    } else if (editIndex in userStations.indices) {
                        userStations[editIndex] = station
                        // Cuando un usuario corrige un enlace caido, también se envia a moderacion.
                        cloudViewModel.submitUserRadio(station)
                    }
                },
                onDeleteStation = { index ->
                    if (index in userStations.indices) userStations.removeAt(index)
                },
            )
        }

        composable(AppRoutes.PLAYER) {
            val selectedIndex = uiState.selectedStationId?.toIntOrNull() ?: -1
            PlayerScreen(
                uiState = uiState,
                isFavorite = favorites.contains(selectedIndex),
                onBack = { navController.navigateUp() },
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
                onRetry = coordinator::retryCurrentStation,
                onNext = coordinator::next,
                onVolumeChange = coordinator::setVolume,
                onDataSaverToggle = coordinator::setDataSaverMode,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeRootScreen(
    navController: NavHostController,
    coordinator: PlaybackCoordinator,
    uiState: RadioUiState,
    animationsEnabled: Boolean,
    layoutMode: RadioLayoutMode,
    cardSizeMode: RadioCardSizeMode,
    baseCatalog: List<RadioStation>,
    liveListenersByUrl: Map<String, Int>,
    userStations: List<UserRadioStation>,
    favorites: Set<Int>,
    onFavoritesChange: (Set<Int>) -> Unit,
    openShare: () -> Unit,
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val topBarScrollBehavior = if (animationsEnabled) {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }
    val isSmallScreen = LocalConfiguration.current.screenWidthDp < 360
    var showExitConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            showExitConfirm -> showExitConfirm = false
            else -> showExitConfirm = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerContent(
                    onAboutClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(AppRoutes.ABOUT)
                        }
                    },
                    onShareClick = {
                        scope.launch {
                            drawerState.close()
                            openShare()
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
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color(0xFF1E88E5))
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(AppRoutes.SEARCH) }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = Color(0xFF1E88E5))
                        }
                        IconButton(onClick = { coordinator.setTab(HomeTab.Favorites) }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favoritos", tint = Color(0xFFE53935))
                        }
                        IconButton(onClick = { navController.navigate(AppRoutes.SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color(0xFF546E7A))
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
                            onOpenPlayer = { navController.navigate(AppRoutes.PLAYER) { launchSingleTop = true } },
                            onPrevious = coordinator::previous,
                            onPlayPause = coordinator::togglePlayback,
                            onNext = coordinator::next,
                            onShare = { shareStation(context, station) },
                        )
                    }
                    BottomNavigationBar(
                        currentTab = uiState.currentTab,
                        onTabSelected = { tab ->
                            if (tab == HomeTab.Mine) {
                                navController.navigate(AppRoutes.MINE_RADIOS)
                            } else {
                                coordinator.setTab(tab)
                            }
                        },
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
                    val catalog = (baseCatalog + userStations.map { it.toRadioStation() }).distinctBy { it.url }
                    CountryExplorerScreen(
                        stations = catalog,
                        selectedStationUrl = uiState.selectedStation?.url,
                        cardSizeMode = cardSizeMode,
                        onStationClick = { index, station -> coordinator.play(index, station, catalog) },
                    )
                } else {
                    val filteredIndices = when (uiState.currentTab) {
                        HomeTab.Music -> baseCatalog.indices.toList()
                        HomeTab.Favorites -> baseCatalog.indices.filter { favorites.contains(it) }
                        HomeTab.Mine -> emptyList()
                        HomeTab.Country -> emptyList()
                    }.sortedBy { baseCatalog[it].name.lowercase() }

                    val verticalSpacing = when (cardSizeMode) {
                        RadioCardSizeMode.Compact -> 6.dp
                        RadioCardSizeMode.Normal -> 8.dp
                        RadioCardSizeMode.Large -> 10.dp
                    }
                    val useTwoColumns = layoutMode == RadioLayoutMode.TwoRows &&
                        !(cardSizeMode == RadioCardSizeMode.Large && isSmallScreen)

                    if (useTwoColumns) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                        ) {
                            items(
                                count = filteredIndices.size,
                                key = { filteredIndices[it] },
                            ) { displayIndex ->
                                val index = filteredIndices[displayIndex]
                                val station = baseCatalog[index]
                                RadioListItem(
                                    station = station,
                                    cardSizeMode = cardSizeMode,
                                    selected = uiState.selectedStation?.url == station.url,
                                    liveListeners = liveListenersByUrl[station.url] ?: 0,
                                    isFavorite = favorites.contains(index),
                                    onFavoriteClick = {
                                        onFavoritesChange(
                                            if (favorites.contains(index)) favorites - index else favorites + index,
                                        )
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
                            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                        ) {
                            items(
                                count = filteredIndices.size,
                                key = { filteredIndices[it] },
                            ) { displayIndex ->
                                val index = filteredIndices[displayIndex]
                                val station = baseCatalog[index]
                                RadioListItem(
                                    station = station,
                                    cardSizeMode = cardSizeMode,
                                    selected = uiState.selectedStation?.url == station.url,
                                    liveListeners = liveListenersByUrl[station.url] ?: 0,
                                    isFavorite = favorites.contains(index),
                                    onFavoriteClick = {
                                        onFavoritesChange(
                                            if (favorites.contains(index)) favorites - index else favorites + index,
                                        )
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

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Salir") },
            text = { Text("¿Deseas salir de la aplicación?") },
            confirmButton = {
                TextButton(onClick = { (context as? Activity)?.finish() }) {
                    Text("Salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

private fun shareApp(context: android.content.Context) {
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

private fun shareStation(context: android.content.Context, station: com.app.radiosatelital.RadioStation) {
    val shareText = buildString {
        append("Escucha ")
        append(station.name)
        append(" en vivo con Radio Satelital")
        append("\n\n")
        append("App: Radio Satelital")
        append("\n")
        append("https://play.google.com/store/apps/details?id=")
        append(context.packageName)
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartir radio"))
}
