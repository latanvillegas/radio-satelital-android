package com.app.radiosatelital.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.radiosatelital.data.firebase.CloudRadioDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationScreen(
    onBack: () -> Unit,
) {
    val adminViewModel: AdminModerationViewModel = viewModel()
    val state = adminViewModel.uiState
    var editingRadio by remember { mutableStateOf<CloudRadioDocument?>(null) }
    var showExitPrompt by remember { mutableStateOf(false) }

    val requestExit: () -> Unit = {
        if (state.isAdminLoggedIn) {
            showExitPrompt = true
        } else {
            onBack()
        }
    }

    BackHandler {
        requestExit()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Moderación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            adminViewModel.logoutAdmin()
                            onBack()
                        },
                        enabled = !state.isBusy,
                    ) {
                        Text("Cerrar sesión")
                    }
                },
            )
        },
    ) { paddingValues ->

    if (showExitPrompt) {
        AlertDialog(
            onDismissRequest = { showExitPrompt = false },
            title = { Text("Salir de administrador") },
            text = { Text("¿Quieres dejar iniciada tu sesión de administrador?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitPrompt = false
                        onBack()
                    },
                ) {
                    Text("Sí, dejar iniciada")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showExitPrompt = false
                            adminViewModel.logoutAdmin()
                            onBack()
                        },
                    ) {
                        Text("Cerrar sesión y salir")
                    }
                    TextButton(onClick = { showExitPrompt = false }) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Pendientes: ${state.pendingRadios.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Radios pendientes de verificar",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            if (state.pendingRadios.isEmpty()) {
                Text(
                    text = "No hay radios pendientes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.pendingRadios, key = { it.id }) { pending ->
                        PendingRadioModerationItem(
                            radio = pending,
                            busy = state.isBusy,
                            onEdit = { editingRadio = pending },
                            onTestStream = { adminViewModel.testStream(pending.streamUrl) },
                            onApprove = { adminViewModel.approveRadio(pending) },
                            onReject = { adminViewModel.rejectRadio(pending.id) },
                        )
                    }
                }
            }

            if (!state.infoMessage.isNullOrBlank()) {
                Text(
                    text = state.infoMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.infoMessage.startsWith("No se pudo")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }

    editingRadio?.let { pending ->
        EditPendingRadioDialog(
            initialRadio = pending,
            busy = state.isBusy,
            onDismiss = { editingRadio = null },
            onSave = { edited ->
                adminViewModel.updatePendingRadio(edited)
                editingRadio = null
            },
            onTest = { streamUrl ->
                adminViewModel.testStream(streamUrl)
            },
        )
    }
}

@Composable
private fun PendingRadioModerationItem(
    radio: CloudRadioDocument,
    busy: Boolean,
    onEdit: () -> Unit,
    onTestStream: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    androidx.compose.material3.Surface(
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onEdit, enabled = !busy) {
                    Text("Editar")
                }
                TextButton(onClick = onTestStream, enabled = !busy) {
                    Text("Probar stream")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
private fun EditPendingRadioDialog(
    initialRadio: CloudRadioDocument,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (CloudRadioDocument) -> Unit,
    onTest: (String) -> Unit,
) {
    var name by remember(initialRadio.id) { mutableStateOf(initialRadio.name) }
    var streamUrl by remember(initialRadio.id) { mutableStateOf(initialRadio.streamUrl) }
    var country by remember(initialRadio.id) { mutableStateOf(initialRadio.country) }
    var region by remember(initialRadio.id) { mutableStateOf(initialRadio.region) }
    var district by remember(initialRadio.id) { mutableStateOf(initialRadio.districtOrCity) }
    var genre by remember(initialRadio.id) { mutableStateOf(initialRadio.genre) }
    var description by remember(initialRadio.id) { mutableStateOf(initialRadio.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar radio pendiente") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Stream URL") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("País") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = region,
                        onValueChange = { region = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Región") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ciudad / Distrito") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Género") },
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Descripción") },
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onTest(streamUrl) },
                    enabled = !busy,
                ) {
                    Text("Probar")
                }
                Button(
                    onClick = {
                        onSave(
                            initialRadio.copy(
                                name = name,
                                streamUrl = streamUrl,
                                country = country,
                                region = region,
                                districtOrCity = district,
                                genre = genre,
                                description = description,
                            ),
                        )
                    },
                    enabled = !busy,
                ) {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Cancelar")
            }
        },
    )
}
