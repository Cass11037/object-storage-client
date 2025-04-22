package org.example.core

import IOManager
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class CommandProcessor(
    private val client: Client,
    private val ioManager: IOManager,
) {
    private lateinit var vehicleReader: VehicleReader
    private var commands =   mutableSetOf<String>()
    private val maxRecursionDepth = 5
    private var recursionDepth = 0
    private val executedScripts =
        mutableSetOf<String>() // protection against recursion & may be a file reading in the file

    fun start() {
        if (commands.isEmpty()) commands = loadCommands().toMutableSet()
        ioManager.outputLine("Transport manager 3000")
        try {
            val helpResponse: List<String> = client.sendRequest("help" to "")
            ioManager.outputLine("Available commands: ${helpResponse.joinToString(", ")}")
        }  catch (e: Exception) {
            ioManager.error("Failed to fetch help information: ${e.message}")
        }
        while (true) {
            ioManager.outputInline("> ")
            val input = ioManager.readLine().trim()
            val executeScriptRegex = "^execute_script\\s.+\$".toRegex()
            when {
                input == "exit" -> break
                executeScriptRegex.matches(input) -> executeScript(input)
                input.isEmpty() -> continue
                else -> processCommand(input)
            }
        }
    }

    private fun loadCommands(): Set<String> {
        return try {
            client.sendRequest("get_commands" to "")
        } catch (e: Exception) {
            ioManager.error("Failed to load commands: ${e.message}")
            emptySet()
        }
    }

    private fun processCommand(input: String) {
        val parts = input.split("\\s+".toRegex())
        if (command.getName() == "execute_script") {
            if (parts.isEmpty()) {
                ioManager.outputLine("Error: The file name is not specified.")
                return
            }
            executeScript(parts[0])
            return
        }
        try {
           val commandName = parts[0]
            val args = parts.drop(1).joinToString { " " }
            val response - cl
        } catch (e: Exception) {
            ioManager.outputLine("Error executing command: ${e.message}")
        }
    }

    private fun executeScript(input: String) {
        val parts = input.split("\\s+".toRegex())
        if (parts.size < 2) {
            ioManager.error("Syntax: execute_script <filename>")
            return
        }

        val filename = parts[1]
        if (filename in executedScripts) {
            ioManager.error("Recursion detected: $filename")
            return
        }

        if (recursionDepth >= maxRecursionDepth) {
            throw StackOverflowError("Max script recursion depth ($maxRecursionDepth) exceeded")
        }

        val path = Paths.get(filename) //TODO норм или нет
        if (!Files.exists(path)) {
            ioManager.error("File not found: $filename")
            return
        }

        if (!Files.isReadable(path)) {
            ioManager.error("Access denied: $filename")
            return
        }

        recursionDepth++
        executedScripts.add(filename)
        try {
            processScriptFile(path)
        } catch (e: Exception) {
            ioManager.error("Script error: ${e.message}")
        } finally {
            executedScripts.remove(filename)
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
                    when {
                        line.startsWith("add", ignoreCase = true) -> processAddCommandInScript()
                        else -> processCommand(line)
                    }
                }
            }
        } finally {
            ioManager.setInput(originalInput)
        }
    }

    private fun processAddCommandInScript() {
        val vehicleData = mutableListOf<String>()
        while (ioManager.hasNextLine() && vehicleData.size < 7) {
            val line = ioManager.readLine().trim()
            if (line.isNotEmpty()) {
                vehicleData.add(line)
            }
        }
        if (vehicleData.size == 7) {
            val fullCommand = "add\n${vehicleData.joinToString("\n")}"
            processCommand(fullCommand)
        } else {
            ioManager.error("Неполные данные для команды add в скрипте")
        }
    }
    fun getCommands(): Set<String> {
        return commands
    }
    fun setCommands(com: Set<String>) {
         commands=com.toMutableSet()
    }
}