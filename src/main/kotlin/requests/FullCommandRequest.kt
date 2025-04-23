package org.example.requests

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
data class FullCommandRequest(
    val commandName: String,
    @Polymorphic val arguments: CommandRequestInterface? = null
)

