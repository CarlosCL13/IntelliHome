package com.intelliworks.intellihome.data.model

import com.google.gson.annotations.SerializedName

data class ArrendamientoResponseDto(
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("id") val id: Int? = null
)