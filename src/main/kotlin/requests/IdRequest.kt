package org.example.requests

import kotlinx.serialization.Serializable

@Serializable
data class IdRequest(val id: Int) : CommandRequestInterface()

