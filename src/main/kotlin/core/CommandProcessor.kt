package org.example.core

import IOManager
import org.example.requests.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class CommandProcessor(
    private val client: Client,
    private val ioManager: IOManager,
) {
    private lateinit var vehicleReader: VehicleReader
    private val commandArgBuilders: Map<String, () -> Any?> = mapOf(
        "add" to {
            val reader = VehicleReader(ioManager)
            AddRequest(reader.readVehicle())
        },
        "add_if_max" to {
            val reader = VehicleReader(ioManager)
            AddIfMaxRequest(reader.readVehicle())
        },
        "add_if_min" to {
            val reader = VehicleReader(ioManager)
            AddIfMinRequest(reader.readVehicle())
        },
        "clear" to {
            NoArgs
        },
        "filter_by_engine_power" to {
            ioManager.outputLine("Enter min engine power:")
            val power = ioManager.readLine().toIntOrNull()
                ?: throw IllegalArgumentException("Invalid engine power")
            FilterByEnginePowerRequest(power)
        },
        "info" to {
            NoArgs
        },
        "min_by_name" to {
            NoArgs
        },
        "remove_any_by_engine_power" to {
            ioManager.outputLine("Enter engine power:")
            val power = ioManager.readLine().toIntOrNull()
                ?: throw IllegalArgumentException("Invalid engine power")
            FilterByEnginePowerRequest(power)
        },
        "remove_by_id" to {
            ioManager.outputLine("Enter ID:")
            val id = ioManager.readLine().toIntOrNull()
                ?: throw IllegalArgumentException("Invalid ID")
            IdRequest(id)
        },
        "remove_first" to {
            NoArgs
        },
        "show" to {
            NoArgs
        },
        "save" to {
            NoArgs
        },
        "update_id" to {
            ioManager.outputLine("Enter ID:")
            val id = ioManager.readLine().toIntOrNull()
                ?: throw IllegalArgumentException("Invalid ID")
            val reader = VehicleReader(ioManager)
            UpdateIdRequest(id, reader.readVehicle())
        }
    )

    private val maxRecursionDepth = 5
    private var recursionDepth = 0
    private val executedScripts =
        mutableSetOf<String>() // protection against recursion & may be a file reading in the file

    fun start() {
        ioManager.outputLine("Transport manager 3000")
        try {
            val noObject = NoArgs
            val helpResponse: List<String> = client.sendCommand("help", noObject )
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

    /*private fun loadCommands(): Set<String> {
        return try {
            client.sendRequest("get_commands" to "")
        } catch (e: Exception) {
            ioManager.error("Failed to load commands: ${e.message}")
            emptySet()
        }
    }*/

    private fun processCommand(input: String) {
        val parts = input.split("\\s+".toRegex(), 2)
        val commandName = parts[0]
        try {
            val response = if (commandArgBuilders.containsKey(commandName)) {
                val args = commandArgBuilders[commandName]?.invoke()
                client.sendCommand<String, Any>(commandName, args)
            } else {
                // Если команды нет в мапе, обрабатываем как обычную команду с аргументами
                client.sendCommand<String, List<String>>(commandName, parts.drop(1))
            }
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

        val path = Paths.get(nameOfFile) //TODO норм или нет
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
}