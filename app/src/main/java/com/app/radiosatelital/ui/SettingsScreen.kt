package com.app.radiosatelital.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.radiosatelital.data.firebase.CloudRadioDocument
import com.app.radiosatelital.ui.theme.AppThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    layoutMode: RadioLayoutMode,
    cardSizeMode: RadioCardSizeMode,
    animationsEnabled: Boolean,
    onThemeChange: (AppThemeMode) -> Unit,
    onLayoutModeChange: (RadioLayoutMode) -> Unit,
    onCardSizeModeChange: (RadioCardSizeMode) -> Unit,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    onResetAppearance: () -> Unit,
    onBack: () -> Unit,
) {
    val adminViewModel: AdminModerationViewModel = viewModel()
    val adminState = adminViewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var adminEmailInput by remember(adminState.adminEmail) { mutableStateOf(adminState.adminEmail) }
    var adminPasswordInput by remember { mutableStateOf("") }
    val trimmedAdminEmail = adminEmailInput.trim()
    val emailHasFormat = trimmedAdminEmail.contains("@") && trimmedAdminEmail.contains(".")
    val adminEmailError = when {
        trimmedAdminEmail.isBlank() -> "Ingresa el correo administrador"
        !emailHasFormat -> "Formato de correo invalido"
        else -> null
    }
    val adminPasswordError = if (adminPasswordInput.isBlank()) "Ingresa la contrasena" else null
    val canLoginAdmin = !adminState.isBusy && adminEmailError == null && adminPasswordError == null

    LaunchedEffect(adminState.infoMessage) {
        val message = adminState.infoMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            adminViewModel.clearInfoMessage()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
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
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ThemePreviewCard(currentTheme = currentTheme)

            SettingsSectionCard(
                title = "Tema",
                subtitle = "Selecciona el estilo visual de la app",
            ) {
                ThemeOption(
                    label = AppThemeMode.PureWhite.label,
                    selected = currentTheme == AppThemeMode.PureWhite,
                    previewColor = Color.White,
                    onSelect = { onThemeChange(AppThemeMode.PureWhite) },
                )
                ThemeOption(
                    label = AppThemeMode.PureBlackAmoled.label,
                    selected = currentTheme == AppThemeMode.PureBlackAmoled,
                    previewColor = Color.Black,
                    onSelect = { onThemeChange(AppThemeMode.PureBlackAmoled) },
                )
                ThemeOption(
                    label = AppThemeMode.PureBlue.label,
                    selected = currentTheme == AppThemeMode.PureBlue,
                    previewColor = Color(0xFF001A33),
                    onSelect = { onThemeChange(AppThemeMode.PureBlue) },
                )
                ThemeOption(
                    label = AppThemeMode.IslandGlass.label,
                    selected = currentTheme == AppThemeMode.IslandGlass,
                    previewColor = Color(0xFFDCEFFD),
                    onSelect = { onThemeChange(AppThemeMode.IslandGlass) },
                )
            }

            SettingsSectionCard(
                title = "Apariencia",
                subtitle = "Personaliza vista y tamaño de radios",
            ) {
                Text(
                    text = "Vista de radios",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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

                Button(
                    onClick = {
                        onResetAppearance()
                        scope.launch {
                            snackbarHostState.showSnackbar("Apariencia restablecida")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = null)
                    Text(text = "Restablecer apariencia", modifier = Modifier.padding(start = 8.dp))
                }
            }

            SettingsSectionCard(
                title = "Animaciones",
                subtitle = "Activa o desactiva animaciones de la aplicación",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = "Animaciones de interfaz",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (animationsEnabled) "Activadas" else "Desactivadas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = animationsEnabled,
                        onCheckedChange = onAnimationsEnabledChange,
                    )
                }
            }

            SettingsSectionCard(
                title = "Administrador",
                subtitle = "Moderación de radios enviadas por usuarios",
            ) {

            if (!adminState.isAdminLoggedIn) {
                if (adminState.isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                OutlinedTextField(
                    value = adminEmailInput,
                    onValueChange = { adminEmailInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo administrador") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = adminEmailError != null,
                    supportingText = {
                        if (adminEmailError != null) {
                            Text(adminEmailError)
                        }
                    },
                    enabled = !adminState.isBusy,
                    singleLine = true,
                )

                OutlinedTextField(
                    value = adminPasswordInput,
                    onValueChange = { adminPasswordInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contrasena") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = adminPasswordError != null,
                    supportingText = {
                        if (adminPasswordError != null) {
                            Text(adminPasswordError)
                        }
                    },
                    enabled = !adminState.isBusy,
                    singleLine = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            Log.d(
                                "AdminLogin",
                                "[SettingsScreen] Click Ingresar. email='${trimmedAdminEmail}' passwordLength=${adminPasswordInput.length}",
                            )
                            adminViewModel.loginAsAdmin(
                                email = trimmedAdminEmail,
                                password = adminPasswordInput,
                            )
                        },
                        enabled = canLoginAdmin,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (adminState.isBusy) "Ingresando..." else "Ingresar")
                    }

                    TextButton(
                        onClick = { adminViewModel.sendPasswordReset(trimmedAdminEmail) },
                        enabled = !adminState.isBusy && adminEmailError == null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Restablecer clave")
                    }
                }
            } else {
                Text(
                    text = "Sesion de administrador activa",
                    style = MaterialTheme.typography.bodyMedium,
                )

                TextButton(
                    onClick = { adminViewModel.logoutAdmin() },
                    enabled = !adminState.isBusy,
                ) {
                    Text("Cerrar sesion")
                }

                Text(
                    text = "Radios pendientes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                if (adminState.pendingRadios.isEmpty()) {
                    Text(
                        text = "No hay radios pendientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    adminState.pendingRadios.forEach { pending ->
                        PendingRadioModerationItem(
                            radio = pending,
                            busy = adminState.isBusy,
                            onApprove = { adminViewModel.approveRadio(pending) },
                            onReject = { adminViewModel.rejectRadio(pending.id) },
                        )
                    }
                }
            }

            }

            SettingsSectionCard(
                title = "Información",
                subtitle = "Detalles y ayuda rápida",
            ) {
                Text(
                    text = "Radio Satelital v1.0",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Tip: si cambias de tema, la vista previa se actualiza al instante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!adminState.infoMessage.isNullOrBlank()) {
                Text(
                    text = adminState.infoMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (adminState.infoMessage.startsWith("No se pudo")) {
                        Color(0xFFB3261E)
                    } else if (adminState.infoMessage.startsWith("Sesion") || adminState.infoMessage.startsWith("Se envio")) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(currentTheme: AppThemeMode) {
    val previewBackground = when (currentTheme) {
        AppThemeMode.PureWhite -> Color(0xFFFFFFFF)
        AppThemeMode.PureBlackAmoled -> Color(0xFF000000)
        AppThemeMode.PureBlue -> Color(0xFF001A33)
        AppThemeMode.IslandGlass -> Color(0xFFDCEFFD)
    }
    val previewContent = when (currentTheme) {
        AppThemeMode.PureWhite -> Color(0xFF0D141C)
        AppThemeMode.PureBlackAmoled -> Color(0xFFF2F2F2)
        AppThemeMode.PureBlue -> Color(0xFFEAF3FF)
        AppThemeMode.IslandGlass -> Color(0xFF0B1E2A)
    }

    SettingsSectionCard(
        title = "Vista previa",
        subtitle = "Así se ve la app con el tema seleccionado",
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = previewBackground,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(previewContent.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${currentTheme.label} activo",
                        color = previewContent,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Reproductor, Mis radios y listas usan este estilo",
                        color = previewContent.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                content()
            },
        )
    }
}

@Composable
private fun PendingRadioModerationItem(
    radio: CloudRadioDocument,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = radio.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = radio.streamUrl, style = MaterialTheme.typography.bodySmall)
            Text(
                text = listOf(radio.country, radio.region, radio.districtOrCity)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove, enabled = !busy) {
                    Text("Aprobar")
                }
                TextButton(onClick = onReject, enabled = !busy) {
                    Text("Rechazar")
                }
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (selected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "ACTIVO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
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
}
