package org.example.requests

import kotlinx.serialization.Serializable
import org.example.model.Vehicle

@Serializable
data class AddRequest (val vehicle: Vehicle)
