package org.example.requests

import kotlinx.serialization.Serializable

@Serializable
data class CommandRequest(
    val command: String,
    val arguments: List<String>?
)
