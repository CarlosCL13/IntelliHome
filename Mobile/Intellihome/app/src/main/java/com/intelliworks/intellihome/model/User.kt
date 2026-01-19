package com.intelliworks.intellihome.model

data class User(
    val id: Int,
    val username: String,
    val fingerprintEnabled: Boolean
)