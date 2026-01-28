package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

data class GeocodingResponseDto(
    val results: List<GeocodingResultDto>?,
    val status: String?
)

data class GeocodingResultDto(
    @SerializedName("formatted_address") val formattedAddress: String?
)
