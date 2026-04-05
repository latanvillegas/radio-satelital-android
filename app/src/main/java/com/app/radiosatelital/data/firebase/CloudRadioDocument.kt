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
    val faviconUrl: String?,
    val homepageUrl: String?,
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
        logoUrl = logoUrl?.trim()?.takeIf { it.isNotBlank() },
        faviconUrl = faviconUrl?.trim()?.takeIf { it.isNotBlank() },
        homepageUrl = homepageUrl?.trim()?.takeIf { it.isNotBlank() },
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
    val faviconUrl = getString("favicon") ?: getString("faviconUrl")
    val homepageUrl = getString("homepage") ?: getString("homepageUrl")
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
        faviconUrl = faviconUrl,
        homepageUrl = homepageUrl,
        createdBy = createdBy,
        status = status,
    )
}
