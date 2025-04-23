package org.example.requests

import kotlinx.serialization.Serializable
import org.example.model.Vehicle


@Serializable
data class UpdateArguments(val id: Long, val vehicle: Vehicle)

