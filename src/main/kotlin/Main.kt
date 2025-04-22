package org.example

import IOManager
import org.example.core.*
import java.io.IOException
import java.nio.file.*


fun fileReader(ioManager: IOManager): String {
    val permanentFileName = "Collection.csv"
    val maxAttempts = 3
    var attempts = 0
    val csvRegex = "^[a-zA-Z0-9_\\-() ]+\\.csv$".toRegex()

    fun createOrLoadFile(fileName: String): String? {
        return try {
            val path = Paths.get(fileName)
            when {
                Files.exists(path) -> {
                    ioManager.outputLine("File '$fileName' already exists.")
                    fileName
                }

                Files.notExists(path) -> {
                    Files.createFile(path)
                    ioManager.outputLine("File '$fileName' created successfully.")
                    fileName
                }

                else -> {
                    ioManager.outputLine("Failed to create '$fileName'.")
                    null
                }
            }
        } catch (e: SecurityException) {
            ioManager.outputLine("Security error: ${e.message}")
            null
        } catch (e: IOException) {
            ioManager.outputLine("IO error: ${e.message}")
            null
        } catch (e: FileAlreadyExistsException) {
            ioManager.outputLine("File already exists: ${e.message}")
            fileName
        }

    }

    while (attempts < maxAttempts) {
        ioManager.outputLine("Would you like to:")
        ioManager.outputLine("1. Create new or load existing CSV file")
        ioManager.outputLine("2. Use permanent file ($permanentFileName)")
        print("> ")
        when (ioManager.readLine().trim().lowercase()) {
            "1", "y", "yes", "да" -> {
                while (attempts < maxAttempts) {
                    ioManager.outputLine("Enter new file name (or 'exit' to use permanent file):")
                    print("> ")
                    val input = ioManager.readLine().trim()
                    if (input.equals("exit", ignoreCase = true)) return permanentFileName
                    when {
                        !csvRegex.matches(input) -> {
                            ioManager.outputLine("Invalid name. Allowed: letters, numbers, _-() and .csv extension")
                        }

                        input.equals(permanentFileName, ignoreCase = true) -> {
                            ioManager.outputLine("Cannot use permanent file name for new file")
                        }

                        else -> {
                            createOrLoadFile(input)?.let { return it }
                        }
                    }
                    attempts++
                }
            }

            "2", "n", "no", "нет" -> {
                val path = Paths.get(permanentFileName)
                return when {
                    Files.exists(path) -> permanentFileName
                    else -> {
                        createOrLoadFile(permanentFileName) ?: run {
                            ioManager.outputLine("Fatal error: Cannot create permanent file")
                            throw IllegalStateException("Permanent file creation fail")
                        }
                    }
                }
            }

            else -> {
                ioManager.outputLine("Invalid choice. Please enter 'yes' or 'no'.")
                attempts++
            }
        }
    }
    throw IllegalStateException("Too many invalid attempts, BYE!")
}

fun main() {
    val ioManager = IOManager(
        ConsoleInputManager(),
        ConsoleOutputManager()
    )
    val fileName = fileReader(ioManager)
    CommandProcessor(ioManager, fileName).start()
}