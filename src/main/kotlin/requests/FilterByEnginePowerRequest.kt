package org.example.requests

import kotlinx.serialization.Serializable

@Serializable
data class FilterByEnginePowerRequest (val power: Int)