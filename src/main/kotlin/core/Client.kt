package org.example.core

import com.sun.org.apache.bcel.internal.util.Args
import kotlinx.serialization.encodeToString
import java.net.DatagramSocket
import kotlinx.serialization.json.Json
import org.example.requests.CommandRequest
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlinx.serialization.serializer

class Client (
    private val serverAdress: String,
    private val serverPort: Int
)
{
    private val socket = DatagramSocket()
    var json = Json {ignoreUnknownKeys = true}
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
    inline fun <reified T, reified Args> sendCommand(command: String, arguments: Args? = null): T {
        val request = CommandRequest(command, arguments)
        val jsonRequest = json.encodeToString(request)
        val jsonResponse = sendRequest(jsonRequest)
        return json.decodeFromString(jsonResponse)
    }



    fun close() {
        socket.close()
    }
}