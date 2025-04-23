package org.example.requests

import kotlinx.serialization.Serializable

@Serializable
data class CommandRequest<T>(
    val name: String,
    val args: T? = null
)
