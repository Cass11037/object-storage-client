package org.example.requests

import kotlinx.serialization.Serializable
import org.example.model.Vehicle

@Serializable
data class UpdateIdRequest (val id: Int, val newVehicle: Vehicle)