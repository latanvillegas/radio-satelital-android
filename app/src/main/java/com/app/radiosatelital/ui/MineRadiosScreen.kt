package com.app.radiosatelital.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.radiosatelital.RadioStation
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
    onPlayStation: (List<RadioStation>, Int) -> Unit,
    onSaveStation: (UserRadioStation, Int?) -> Unit,
    onDeleteStation: (Int) -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    BackHandler(enabled = showEditor) {
        showEditor = false
    }

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

        if (stations.isEmpty()) {
            Text(
                text = "Aun no agregaste radios personales",
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
                itemsIndexed(stations) { index, station ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(text = station.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${station.country} · ${station.region}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(onClick = {
                                    val catalog = stations.map { it.toRadioStation() }
                                    onPlayStation(catalog, index)
                                }) {
                                    Text("Reproducir")
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingIndex = index
                                        showEditor = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                    }
                                    IconButton(onClick = { onDeleteStation(index) }) {
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

    if (showEditor) {
        AddEditRadioDialog(
            initial = editingIndex?.let { stations[it] },
            onDismiss = { showEditor = false },
            onSave = { station ->
                onSaveStation(station, editingIndex)
                showEditor = false
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
