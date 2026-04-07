package com.app.radiosatelital.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.data.firebase.STATUS_APPROVED
import com.app.radiosatelital.data.firebase.STATUS_PENDING
import com.app.radiosatelital.data.firebase.STATUS_REJECTED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class UserRadioStation(
    val name: String,
    val streamUrl: String,
    val country: String,
    val region: String,
    val city: String,
    val continent: String,
    val genre: String,
    val description: String,
    val logoUrl: String,
)

private enum class LinkCheckStatus {
    Unknown,
    Active,
    Down,
}

fun UserRadioStation.toRadioStation(): RadioStation {
    val location = listOf(region, city).filter { it.isNotBlank() }.joinToString(" · ")
    val sanitizedLogo = sanitizeUserProvidedLogoUrl(logoUrl)
    return RadioStation(
        name = name,
        country = country,
        region = location,
        url = streamUrl,
        logoUrl = sanitizedLogo,
        faviconUrl = null,
        homepageUrl = null,
        genre = genre,
    )
}

@Composable
fun MineRadiosScreen(
    stations: List<UserRadioStation>,
    cloudMessage: String?,
    submissionStatusByUrl: Map<String, String>,
    onPlayStation: (List<RadioStation>, Int) -> Unit,
    onSaveStation: (UserRadioStation, Int?) -> Unit,
    onDeleteStation: (Int) -> Unit,
) {
    val filterAll = "Todas"
    val filterActive = "Activas"
    val filterDown = "Caidas"
    val filterUnknown = "Sin probar"

    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var checkingIndex by remember { mutableStateOf<Int?>(null) }
    var checkResultMessage by remember { mutableStateOf<String?>(null) }
    var localSaveMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf(filterAll) }
    var deleteIndexPending by remember { mutableStateOf<Int?>(null) }
    val linkStatusByUrl = remember { mutableStateMapOf<String, LinkCheckStatus>() }
    val scope = rememberCoroutineScope()

    val filteredStations = stations.mapIndexed { index, station -> index to station }
        .filter { (_, station) ->
            val status = linkStatusByUrl[station.streamUrl] ?: LinkCheckStatus.Unknown
            val matchesStatus = when (selectedStatusFilter) {
                filterActive -> status == LinkCheckStatus.Active
                filterDown -> status == LinkCheckStatus.Down
                filterUnknown -> status == LinkCheckStatus.Unknown
                else -> true
            }
            val query = searchQuery.trim()
            val matchesSearch = query.isBlank() || listOf(
                station.name,
                station.country,
                station.region,
                station.city,
            ).any { value -> value.contains(query, ignoreCase = true) }
            matchesStatus && matchesSearch
        }

    BackHandler(enabled = showEditor) {
        showEditor = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Mis radios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Button(onClick = {
                editingIndex = null
                showEditor = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(text = "Agregar", modifier = Modifier.padding(start = 6.dp))
            }
        }

        if (!cloudMessage.isNullOrBlank()) {
            Text(
                text = cloudMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!checkResultMessage.isNullOrBlank()) {
            val isError = checkResultMessage.orEmpty().startsWith("Enlace caido")
            Text(
                text = checkResultMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
            )
        }

        if (!localSaveMessage.isNullOrBlank()) {
            Text(
                text = localSaveMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32),
            )
        }

        Text(
            text = "Si una radio se cae, edita su URL de streaming para corregirla y enviarla a moderacion.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar en Mis radios") },
            singleLine = true,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(filterAll, filterActive, filterDown, filterUnknown)) { option ->
                FilterChip(
                    selected = selectedStatusFilter == option,
                    onClick = { selectedStatusFilter = option },
                    label = { Text(option) },
                )
            }
        }

        if (stations.isEmpty()) {
            Text(
                text = "Aun no agregaste radios personales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (filteredStations.isEmpty()) {
            Text(
                text = "No hay radios para el filtro actual",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = filteredStations,
                    key = { (index, _) -> index },
                ) { (index, station) ->
                    val submissionStatus = submissionStatusByUrl[station.streamUrl]?.lowercase().orEmpty()
                    val cardContainerColor = when (submissionStatus) {
                        STATUS_APPROVED -> Color(0xFF2E7D32).copy(alpha = 0.06f)
                        STATUS_PENDING -> Color(0xFFB26A00).copy(alpha = 0.06f)
                        STATUS_REJECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(text = station.name, style = MaterialTheme.typography.titleMedium)
                            val linkStatus = linkStatusByUrl[station.streamUrl] ?: LinkCheckStatus.Unknown
                            Surface(
                                color = when (linkStatus) {
                                    LinkCheckStatus.Active -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                                    LinkCheckStatus.Down -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    LinkCheckStatus.Unknown -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = when (linkStatus) {
                                        LinkCheckStatus.Active -> "Activa"
                                        LinkCheckStatus.Down -> "Caida"
                                        LinkCheckStatus.Unknown -> "Sin probar"
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (linkStatus) {
                                        LinkCheckStatus.Active -> Color(0xFF2E7D32)
                                        LinkCheckStatus.Down -> MaterialTheme.colorScheme.error
                                        LinkCheckStatus.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = "${station.country} · ${station.region}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            submissionStatusByUrl[station.streamUrl]?.let { status ->
                                val statusLabel: String
                                val statusColor: Color
                                val statusIcon = when (status.lowercase()) {
                                    STATUS_PENDING -> {
                                        statusLabel = "Pendiente"
                                        statusColor = Color(0xFFB26A00)
                                        Icons.Filled.HourglassTop
                                    }
                                    STATUS_APPROVED -> {
                                        statusLabel = "Aceptada"
                                        statusColor = Color(0xFF2E7D32)
                                        Icons.Filled.CheckCircle
                                    }
                                    STATUS_REJECTED -> {
                                        statusLabel = "Rechazada"
                                        statusColor = MaterialTheme.colorScheme.error
                                        Icons.Filled.Cancel
                                    }
                                    else -> {
                                        statusLabel = status.replaceFirstChar { it.uppercase() }
                                        statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        Icons.Filled.HourglassTop
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = statusColor.copy(alpha = 0.12f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                                    ) {
                                        Text(
                                            text = "Estado: $statusLabel",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    androidx.compose.material3.Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = statusColor,
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                checkingIndex = index
                                                val isValid = validateStreamingUrl(station.streamUrl)
                                                checkingIndex = null
                                                linkStatusByUrl[station.streamUrl] = if (isValid) {
                                                    LinkCheckStatus.Active
                                                } else {
                                                    LinkCheckStatus.Down
                                                }
                                                checkResultMessage = if (isValid) {
                                                    "Enlace activo: ${station.name}"
                                                } else {
                                                    "Enlace caido o no reproducible: ${station.name}"
                                                }
                                            }
                                        },
                                        enabled = checkingIndex == null,
                                    ) {
                                        Text(if (checkingIndex == index) "Probando..." else "Probar enlace")
                                    }

                                    TextButton(onClick = {
                                        val catalog = stations.map { it.toRadioStation() }
                                        onPlayStation(catalog, index)
                                    }) {
                                        Text("Reproducir")
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingIndex = index
                                        showEditor = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Corregir enlace")
                                    }
                                    IconButton(onClick = { deleteIndexPending = index }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
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

    if (showEditor) {
        AddEditRadioDialog(
            initial = editingIndex?.let { stations[it] },
            onDismiss = { showEditor = false },
            onSave = { station ->
                val oldUrl = editingIndex?.let { index -> stations.getOrNull(index)?.streamUrl }
                onSaveStation(station, editingIndex)
                if (oldUrl != null && oldUrl != station.streamUrl) {
                    linkStatusByUrl.remove(oldUrl)
                }
                linkStatusByUrl[station.streamUrl] = LinkCheckStatus.Unknown
                localSaveMessage = if (editingIndex == null) {
                    "Guardado local y enviado a moderacion"
                } else {
                    "Cambios guardados localmente y enviados a moderacion"
                }
                showEditor = false
            },
        )
    }

    if (deleteIndexPending != null) {
        val pendingIndex = deleteIndexPending ?: -1
        val pendingName = stations.getOrNull(pendingIndex)?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { deleteIndexPending = null },
            title = { Text("Eliminar radio") },
            text = { Text("¿Seguro que deseas eliminar \"$pendingName\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteStation(pendingIndex)
                    deleteIndexPending = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteIndexPending = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun AddEditRadioDialog(
    initial: UserRadioStation?,
    onDismiss: () -> Unit,
    onSave: (UserRadioStation) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var streamUrl by remember { mutableStateOf(initial?.streamUrl ?: "") }
    var country by remember { mutableStateOf(initial?.country ?: "") }
    var region by remember { mutableStateOf(initial?.region ?: "") }
    var city by remember { mutableStateOf(initial?.city ?: "") }
    var continent by remember { mutableStateOf(initial?.continent ?: "") }
    var genre by remember { mutableStateOf(initial?.genre ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var logoUrl by remember { mutableStateOf(initial?.logoUrl ?: "") }

    var validating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Agregar radio" else "Editar radio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                OutlinedTextField(value = streamUrl, onValueChange = { streamUrl = it }, label = { Text("URL streaming") })
                OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("País") })
                OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Región") })
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Distrito o ciudad") })
                OutlinedTextField(value = continent, onValueChange = { continent = it }, label = { Text("Continente") })
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Género") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción (opcional)") })
                OutlinedTextField(value = logoUrl, onValueChange = { logoUrl = it }, label = { Text("Logo URL (opcional)") })
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        errorMessage = null
                        if (name.isBlank() || streamUrl.isBlank() || country.isBlank() || region.isBlank() || city.isBlank() || continent.isBlank() || genre.isBlank()) {
                            errorMessage = "Completa los campos obligatorios"
                            return@launch
                        }

                        validating = true
                        val valid = validateStreamingUrl(streamUrl)
                        validating = false
                        if (!valid) {
                            errorMessage = "El streaming no responde o no es reproducible"
                            return@launch
                        }

                        val validatedLogo = sanitizeUserProvidedLogoUrl(logoUrl)
                        if (logoUrl.isNotBlank() && validatedLogo == null) {
                            errorMessage = "Logo URL invalido. Usa una URL http/https real"
                            return@launch
                        }

                        onSave(
                            UserRadioStation(
                                name = name.trim(),
                                streamUrl = streamUrl.trim(),
                                country = country.trim(),
                                region = region.trim(),
                                city = city.trim(),
                                continent = continent.trim(),
                                genre = genre.trim(),
                                description = description.trim(),
                                logoUrl = validatedLogo.orEmpty(),
                            ),
                        )
                    }
                },
                enabled = !validating,
            ) {
                Text(if (validating) "Validando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !validating) {
                Text("Cancelar")
            }
        },
    )
}

private fun sanitizeUserProvidedLogoUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return null
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (host == "localhost" || host == "127.0.0.1") return null
    return uri.toString()
}

private suspend fun validateStreamingUrl(url: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Range", "bytes=0-512")
            setRequestProperty("User-Agent", "RadioSatelital/1.0")
            instanceFollowRedirects = true
        }
        connection.connect()
        val code = connection.responseCode
        val contentType = connection.contentType.orEmpty().lowercase()
        val hasResponse = code in 200..299 || code == 206
        val streamLike = contentType.contains("audio") ||
            contentType.contains("mpegurl") ||
            contentType.contains("octet-stream")
        val readable = runCatching { connection.inputStream.use { it.read() >= -1 } }.getOrDefault(false)
        connection.disconnect()
        hasResponse && (streamLike || readable)
    }.getOrDefault(false)
}
