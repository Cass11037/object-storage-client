package org.example.core

import IOManager
import org.example.requests.CommandRequest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CommandProcessor(
    private val client: Client,
    private val ioManager: IOManager,
) {
    private val maxRecursionDepth = 5
    private var recursionDepth = 0
    private val executedScripts = mutableSetOf<String>()

    fun start() {
        ioManager.outputLine("Transport manager 3000")
        try {
            val helpRequest = CommandRequest("help", emptyList())
            val helpResponse: List<String> = client.sendCommand(helpRequest)
            ioManager.outputLine("Available commands: ${helpResponse.joinToString(", ")}")
        } catch (e: Exception) {
            ioManager.error("Failed to fetch help information: ${e.message}")
        }

        while (true) {
            ioManager.outputInline("> ")
            val input = ioManager.readLine().trim()
            val executeScriptRegex = "^execute_script\\s.+$".toRegex()
            when {
                input == "exit" -> break
                executeScriptRegex.matches(input) -> executeScript(input.substringAfter(" "))
                input.isEmpty() -> continue
                else -> processCommand(input)
            }
        }
    }

    private fun processCommand(input: String) {
        val parts = input.split("\\s+").filter { it.isNotBlank() }
        if (parts.isEmpty()) return

        val commandName = parts[0]
        val args = if (parts.size > 1) parts.subList(1, parts.size) else emptyList()

        try {
            val request = CommandRequest(commandName, args)
            val response: String = client.sendCommand(request)
            ioManager.outputLine(response)
        } catch (e: Exception) {
            ioManager.outputLine("Error executing command: ${e.message}")
        }
    }

    private fun executeScript(nameOfFile: String) {
        if (nameOfFile in executedScripts) {
            ioManager.error("Recursion detected: $nameOfFile")
            return
        }

        if (recursionDepth >= maxRecursionDepth) {
            throw StackOverflowError("Max script recursion depth ($maxRecursionDepth) exceeded")
        }

        val path = Paths.get(nameOfFile)
        if (!Files.exists(path)) {
            ioManager.error("File not found: $nameOfFile")
            return
        }

        if (!Files.isReadable(path)) {
            ioManager.error("Access denied: $nameOfFile")
            return
        }

        recursionDepth++
        executedScripts.add(nameOfFile)
        try {
            processScriptFile(path)
        } catch (e: Exception) {
            ioManager.error("Script error: ${e.message}")
        } finally {
            executedScripts.remove(nameOfFile)
            recursionDepth--
        }
    }

    private fun processScriptFile(path: Path) {
        val originalInput = ioManager.getInput()
        val scriptInput = object : InputManager {
            private val reader = Files.newBufferedReader(path)
            override fun readLine(): String? = reader.readLine()
            override fun hasInput(): Boolean = reader.ready()
        }
        ioManager.setInput(scriptInput)

        try {
            while (ioManager.hasNextLine()) {
                val line = ioManager.readLine().trim() ?: continue
                if (line.isNotEmpty()) {
                    ioManager.outputLine("[Script]> $line")
                    processCommand(line)
                }
            }
        } finally {
            ioManager.setInput(originalInput)
        }
    }
}