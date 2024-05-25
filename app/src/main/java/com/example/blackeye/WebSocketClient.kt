package com.example.blackeye
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

object WebSocketClientManager {
    private var webSocketClient: WebSocketClient? = null
    private val serverUri = URI("ws://49.233.216.82:5901/user")
    var videoFrameListener: ((ByteBuffer) -> Unit)? = null

    fun getInstance(token: String?): WebSocketClient {
        if (webSocketClient == null) {
            connectWebSocket(token)
        }
        return webSocketClient!!
    }

    private fun connectWebSocket(token: String?) {
        val headers = mapOf("Sec-WebSocket-Protocol" to token)
        webSocketClient = object : WebSocketClient(serverUri, headers) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket connection opened")
            }

            override fun onMessage(message: String?) {
                println("Received text message: $message")
            }

            override fun onMessage(bytes: ByteBuffer) {
                handleVideoFrame(bytes)
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("WebSocket closed with exit code $code additional info: $reason")
            }

            override fun onError(ex: Exception?) {
                println("WebSocket connection error: ${ex?.message}")
            }
        }
        webSocketClient?.connect()
    }

    private fun handleVideoFrame(bytes: ByteBuffer) {
        // 第一个字节是数据类型
        val dataType = bytes[0]
        if (dataType.toInt() == 0x01) { // 数据类型为视频帧
            // 跳过第一个字节读取剩余的 JPEG 数据
            val jpegBytes = ByteArray(bytes.remaining() - 1)
            bytes.position(1) // 忽略第一个字节

            bytes[jpegBytes, 0, jpegBytes.size]

            // 显示视频帧
        }
        videoFrameListener?.invoke(bytes)
    }
}
