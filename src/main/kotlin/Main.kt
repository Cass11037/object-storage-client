package org.example

import IOManager
import org.example.core.*

fun main() {
    val ioManager = IOManager(
        ConsoleInputManager(),
        ConsoleOutputManager()
    )

    val client = Client("localhost", 9999) // замените на нужный адрес/порт сервера

    val commandProcessor = CommandProcessor(client, ioManager)
    commandProcessor.start()

    client.close()
}
