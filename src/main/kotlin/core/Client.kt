package org.example.core

import kotlinx.serialization.encodeToString
import java.net.DatagramSocket
import kotlinx.serialization.json.Json
import org.example.model.Vehicle
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketTimeoutException

class Client (
    private val serverAdress: String,
    private val serverPort: Int
)
{
    private val socket = DatagramSocket()
    var json = Json {ignoreUnknownKeys = true}
    private var buf = ByteArray(65535)
    init {
        socket.soTimeout = 5000
    }
    fun sendRequest(request: String) : String {
        return try {
        buf = request.toByteArray()
        var packet = DatagramPacket(buf, buf.size, InetAddress.getByName(serverAdress), serverPort);
        socket.send(packet)
        packet = DatagramPacket(buf, buf.size)
        socket.receive(packet)
       String(packet.data, 0, packet.length).trim() }
        catch (e: SocketTimeoutException) {
            throw RuntimeException("Server did not respond within the timeout period.")
        } catch (e: Exception) {
            throw RuntimeException("Network error: ${e.message}")
        }
    }
    inline fun <reified T> sendRequest(obj: Any): T {
        val jsonRequest = json.encodeToString(obj)
        val jsonResponse = sendRequest(jsonRequest)
        return json.decodeFromString(jsonResponse)
    }
    inline fun <reified T> sendCommand(command: String, arguments: List<String>? = null): T {
        val request = CommandRequest(command, arguments)
        val jsonRequest = json.encodeToString(request)
        val jsonResponse = sendRequest(jsonRequest)
        return json.decodeFromString(jsonResponse)
    }

    fun close() {
        socket.close()
    }
}