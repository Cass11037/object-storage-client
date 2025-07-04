package org.example.core

import kotlinx.serialization.encodeToString
import java.net.DatagramSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.example.requests.FullCommandRequest
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketTimeoutException
import org.example.requests.CommandRequestInterface
import org.example.requests.*

class Client (
    private val serverAdress: String,
    private val serverPort: Int
)
{
    private val socket = DatagramSocket()

    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        serializersModule = SerializersModule {
            polymorphic(CommandRequestInterface::class) {
                subclass(NoArgs::class, NoArgs.serializer())
                subclass(IdRequest::class, IdRequest.serializer())
                subclass(AddRequest::class, AddRequest.serializer())
                subclass(AddIfMaxRequest::class, AddIfMaxRequest.serializer())
                subclass(AddIfMinRequest::class, AddIfMinRequest.serializer())
                subclass(UpdateIdRequest::class, UpdateIdRequest.serializer())
                subclass(FilterByEnginePowerRequest::class, FilterByEnginePowerRequest.serializer())
            }
        }
    }
    private var maxRetries : Int
    private var buf = ByteArray(65535)
    init {
        socket.soTimeout = 5000
        maxRetries = 3
    }
    fun sendRequest(request: String) : String {
        var attempt = 0
        while (attempt < maxRetries) {

            try {
                buf = request.toByteArray()
                var packet = DatagramPacket(buf, buf.size, InetAddress.getByName(serverAdress), serverPort);
                socket.send(packet)
                packet = DatagramPacket(buf, buf.size)
                socket.receive(packet)
                return String(packet.data, 0, packet.length).trim()
            } catch (e: SocketTimeoutException) {
                attempt++
                if (attempt == maxRetries) {
                    throw RuntimeException("Server did not respond within the timeout period.")
                }
            } catch (e: Exception) {
                throw RuntimeException("Network error: ${e.message}")
            }
        }
        throw RuntimeException("Unknown error")
    }
    inline fun <reified T> sendRequest(obj: Any): T {
        val jsonRequest = json.encodeToString(obj)
        val jsonResponse = sendRequest(jsonRequest)
        return json.decodeFromString(jsonResponse)
    }
    inline fun <reified T> sendCommand(command: String, arguments: CommandRequestInterface): T {
        val request = FullCommandRequest(command, arguments)
        val jsonRequest = json.encodeToString(request)
        val jsonResponse = sendRequest(jsonRequest)
        return json.decodeFromString(jsonResponse)
    }



    fun close() {
        socket.close()
    }
}