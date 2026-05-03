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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.animation.AnimatedVisibility
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Radio
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
                // Public radios section (working / broken)
                val tabTitles = listOf("✅ Funcionando", "⚠️ Caídas")
                var selectedTab by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    adminViewModel.loadPublicRadios()
                }

                val workingRadios = adminViewModel.publicRadios.filter {
                    it.lastStreamStatus != false
                }
                val brokenRadios = adminViewModel.publicRadios.filter {
                    it.lastStreamStatus == false
                }

                TabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> PublicRadioList(
                        radios = workingRadios,
                        onEditLogo = { radio, newLogoUrl ->
                            adminViewModel.updatePublicRadioLogo(radio.id, newLogoUrl)
                        },
                        onTestStream = { url, onResult ->
                            adminViewModel.testPublicRadioStream(url, onResult)
                        },
                        onEditStream = null
                    )
                    1 -> PublicRadioList(
                        radios = brokenRadios,
                        onEditLogo = { radio, newLogoUrl ->
                            adminViewModel.updatePublicRadioLogo(radio.id, newLogoUrl)
                        },
                        onTestStream = { url, onResult ->
                            adminViewModel.testPublicRadioStream(url, onResult)
                        },
                        onEditStream = { radio, newStreamUrl ->
                            adminViewModel.updatePublicRadioStream(radio.id, newStreamUrl)
                        }
                    )
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
 private fun PublicRadioList(
     radios: List<com.app.radiosatelital.data.firebase.CloudRadioDocument>,
     onEditLogo: (com.app.radiosatelital.data.firebase.CloudRadioDocument, String) -> Unit,
     onTestStream: (String, (Boolean) -> Unit) -> Unit,
     onEditStream: ((com.app.radiosatelital.data.firebase.CloudRadioDocument, String) -> Unit)?
) {
     var expandedRadioId by remember { mutableStateOf<String?>(null) }
     var logoInput by remember { mutableStateOf("") }
     var streamInput by remember { mutableStateOf("") }
     var streamTestResult by remember { mutableStateOf<Boolean?>(null) }

     LazyColumn {
         items(radios, key = { it.id }) { radio ->
             androidx.compose.material3.Card(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(horizontal = 16.dp, vertical = 4.dp)
                     .clickable {
                         expandedRadioId = if (expandedRadioId == radio.id)
                             null else radio.id
                         logoInput = radio.logoUrl ?: ""
                         streamInput = radio.streamUrl ?: ""
                         streamTestResult = null
                     },
                 shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
             ) {
                 Column(modifier = Modifier.padding(12.dp)) {
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(12.dp)
                     ) {
                         Box(
                             modifier = Modifier
                                 .size(48.dp)
                                 .clip(RoundedCornerShape(8.dp))
                                 .background(MaterialTheme.colorScheme.surfaceVariant)
                         ) {
                             if (!radio.logoUrl.isNullOrBlank()) {
                                 AsyncImage(
                                     model = radio.logoUrl,
                                     contentDescription = null,
                                     contentScale = ContentScale.Crop,
                                     modifier = Modifier.fillMaxSize()
                                 )
                             } else {
                                 Icon(
                                     Icons.Filled.Radio,
                                     contentDescription = null,
                                     tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                     modifier = Modifier
                                         .size(28.dp)
                                         .align(Alignment.Center)
                                 )
                             }
                         }
                         Column(modifier = Modifier.weight(1f)) {
                             Text(
                                 text = radio.name ?: "Sin nombre",
                                 style = MaterialTheme.typography.bodyLarge,
                                 fontWeight = FontWeight.SemiBold,
                                 maxLines = 1,
                                 overflow = TextOverflow.Ellipsis
                             )
                             Text(
                                 text = radio.country ?: "",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                         }
                         Icon(
                             imageVector = if (expandedRadioId == radio.id)
                                 Icons.Filled.ExpandLess
                             else Icons.Filled.ExpandMore,
                             contentDescription = null
                         )
                     }

                     AnimatedVisibility(visible = expandedRadioId == radio.id) {
                         Column(
                             modifier = Modifier.padding(top = 12.dp),
                             verticalArrangement = Arrangement.spacedBy(8.dp)
                         ) {
                             Text("Logo URL",
                                 style = MaterialTheme.typography.labelMedium)
                             OutlinedTextField(
                                 value = logoInput,
                                 onValueChange = { logoInput = it },
                                 modifier = Modifier.fillMaxWidth(),
                                 placeholder = { Text("https://...") },
                                 singleLine = true,
                                 trailingIcon = {
                                     IconButton(onClick = {
                                         onEditLogo(radio, logoInput)
                                     }) {
                                         Icon(Icons.Filled.Save,
                                             contentDescription = "Guardar logo")
                                     }
                                 }
                             )

                             if (onEditStream != null) {
                                 Text("Stream URL",
                                     style = MaterialTheme.typography.labelMedium)
                                 OutlinedTextField(
                                     value = streamInput,
                                     onValueChange = { streamInput = it },
                                     modifier = Modifier.fillMaxWidth(),
                                     placeholder = { Text("https://...") },
                                     singleLine = true
                                 )
                                 Row(
                                     horizontalArrangement =
                                         Arrangement.spacedBy(8.dp)
                                 ) {
                                     OutlinedButton(
                                         onClick = {
                                             streamTestResult = null
                                             onTestStream(streamInput) {
                                                 streamTestResult = it
                                             }
                                         },
                                         modifier = Modifier.weight(1f)
                                     ) {
                                         Icon(Icons.Filled.PlayCircle, null,
                                             modifier = Modifier.size(18.dp))
                                         Spacer(Modifier.width(4.dp))
                                         Text("Probar")
                                     }
                                     Button(
                                         onClick = {
                                             onEditStream(radio, streamInput)
                                         },
                                         modifier = Modifier.weight(1f),
                                         enabled = streamTestResult == true
                                     ) {
                                         Icon(Icons.Filled.Save, null,
                                             modifier = Modifier.size(18.dp))
                                         Spacer(Modifier.width(4.dp))
                                         Text("Guardar")
                                     }
                                 }
                                 streamTestResult?.let { isOk ->
                                     Row(
                                         verticalAlignment =
                                             Alignment.CenterVertically,
                                         horizontalArrangement =
                                             Arrangement.spacedBy(4.dp)
                                     ) {
                                         Icon(
                                             imageVector = if (isOk)
                                                 Icons.Filled.CheckCircle
                                             else Icons.Filled.Cancel,
                                             contentDescription = null,
                                             tint = if (isOk) Color(0xFF43A047)
                                                    else Color(0xFFE53935),
                                             modifier = Modifier.size(18.dp)
                                         )
                                         Text(
                                             text = if (isOk)
                                                 "Stream activo ✅"
                                             else "Stream caído ❌",
                                             color = if (isOk) Color(0xFF43A047)
                                                     else Color(0xFFE53935),
                                             style =
                                                 MaterialTheme.typography.bodySmall
                                         )
                                     }
                                 }
                             }
                         }
                     }
                 }
             }
         }
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
