package com.app.radiosatelital.data.firebase

import com.app.radiosatelital.RadioStation
import com.google.firebase.firestore.DocumentSnapshot

const val STATUS_PENDING = "pending"
const val STATUS_APPROVED = "approved"

data class CloudRadioDocument(
    val id: String,
    val name: String,
    val streamUrl: String,
    val country: String,
    val region: String,
    val districtOrCity: String,
    val continent: String,
    val genre: String,
    val description: String,
    val logoUrl: String?,
    val createdBy: String,
    val status: String,
)

fun CloudRadioDocument.toRadioStation(): RadioStation {
    val composedRegion = listOf(region, districtOrCity)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    return RadioStation(
        name = name,
        country = country,
        region = composedRegion,
        url = streamUrl,
        logoUrl = logoUrl,
        genre = genre,
    )
}

fun DocumentSnapshot.toCloudRadioDocumentOrNull(): CloudRadioDocument? {
    val id = getString("id") ?: id
    val name = getString("name") ?: return null
    val streamUrl = getString("streamUrl") ?: return null
    val country = getString("country") ?: return null
    val region = getString("region") ?: ""
    val districtOrCity = getString("districtOrCity") ?: ""
    val continent = getString("continent") ?: ""
    val genre = getString("genre") ?: ""
    val description = getString("description") ?: ""
    val logoUrl = getString("logoUrl")
    val createdBy = getString("createdBy") ?: ""
    val status = getString("status") ?: STATUS_PENDING

    return CloudRadioDocument(
        id = id,
        name = name,
        streamUrl = streamUrl,
        country = country,
        region = region,
        districtOrCity = districtOrCity,
        continent = continent,
        genre = genre,
        description = description,
        logoUrl = logoUrl,
        createdBy = createdBy,
        status = status,
    )
}
