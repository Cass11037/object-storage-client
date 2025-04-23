package org.example.core

import kotlinx.serialization.Serializable

@Serializable
class CommandRequest(
    val name: String,
    val args: List<String>?
) {

}