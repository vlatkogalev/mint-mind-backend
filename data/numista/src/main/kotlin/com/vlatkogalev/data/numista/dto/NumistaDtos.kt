package com.vlatkogalev.data.numista.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NumistaTypesSearchResponse(
    @SerialName("types")
    val types: List<NumistaTypeSummary> = emptyList(),
)

@Serializable
data class NumistaTypeSummary(
    @SerialName("id")
    val id: Long,
    @SerialName("title")
    val title: String? = null,
    @SerialName("country")
    val country: NumistaCountry? = null,
    @SerialName("issuer")
    val issuer: NumistaCountry? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("year_start")
    val yearStart: Int? = null,
    @SerialName("year_end")
    val yearEnd: Int? = null,
)

@Serializable
data class NumistaCountry(
    @SerialName("name")
    val name: String? = null,
)

@Serializable
data class NumistaTypeDetail(
    @SerialName("id")
    val id: Long,
    @SerialName("url")
    val url: String? = null,
    @SerialName("composition")
    val composition: NumistaComposition? = null,
    @SerialName("weight")
    val weight: Double? = null,
    @SerialName("size")
    val size: Double? = null,
    @SerialName("obverse")
    val obverse: NumistaSide? = null,
    @SerialName("reverse")
    val reverse: NumistaSide? = null,
    @SerialName("comments")
    val comments: String? = null,
    @SerialName("obverse_thumbnail")
    val obverseThumbnail: String? = null,
    @SerialName("catalogue_codes")
    val catalogueCodes: List<NumistaCatalogueCode> = emptyList(),
)

@Serializable
data class NumistaComposition(
    val text: String? = null,
)

@Serializable
data class NumistaSide(
    val description: String? = null,
)

@Serializable
data class NumistaCatalogueCode(
    val catalogue: NumistaCatalogue? = null,
    val code: String? = null,
)

@Serializable
data class NumistaCatalogue(
    val name: String? = null,
)
