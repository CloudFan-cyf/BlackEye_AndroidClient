package com.example.blackeye
import android.util.Log
import androidx.compose.material3.contentColorFor
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer



object WebSocketClientManager {
    private var webSocketClient: WebSocketClient? = null
    private val serverUri = URI("ws://49.233.216.82:5901/user")
    var videoFrameListener: ((ByteBuffer) -> Unit)? = null
    var imuDataListener: ((Float, Float, Float) -> Unit)? = null  // 使用三个Float参数的Lambda
    var connectState : Boolean = false

    fun getInstance(token: String?): WebSocketClient {
        if (webSocketClient == null || !connectState) {
            connectWebSocket(token)
        }
        return webSocketClient!!
    }

    private fun connectWebSocket(token: String?) {
        val headers = mapOf("Sec-WebSocket-Protocol" to token)
        webSocketClient = object : WebSocketClient(serverUri, headers) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket connection opened")
                connectState = true
            }

            override fun onMessage(message: String?) {
                println("Received text message: $message")
            }

            override fun onMessage(bytes: ByteBuffer) {
                val dataType = bytes[0]
                if(dataType.toInt() == 0x01 ){
                    handleVideoFrame(bytes)
                }
                else if (dataType.toInt() == 0x03){//imu data
                    handleIMUData(bytes)

                }

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("WebSocket closed with exit code $code additional info: $reason")
                connectState = false
            }

            override fun onError(ex: Exception?) {
                println("WebSocket connection error: ${ex?.message}")
                connectState = false
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
            videoFrameListener?.invoke(ByteBuffer.wrap(jpegBytes))
        }

    }

    private fun handleIMUData(bytes: ByteBuffer) {
        // 跳过第一个字节（数据类型）
        bytes.position(1)

        // 获取剩余的数据，转换为字符串
        val remaining = ByteArray(bytes.remaining())
        bytes.get(remaining)
        val dataString = String(remaining, Charsets.UTF_8)

        // 数据格式假定为 "yaw,pitch,roll"
        val parts = dataString.split(',')
        if (parts.size == 3) {
            val yaw = parts[0].toFloat()   // 将字符串转换为浮点数
            val pitch = parts[1].toFloat()
            val roll = parts[2].toFloat()
            imuDataListener?.invoke(yaw, pitch, roll)
        }
        else{
            Log.e("IMUData", "Received invalid data: $dataString")
        }
    }

    fun sendMessage(message: String) {
        webSocketClient?.send(message)
    }
    fun close(){
        connectState = false
        webSocketClient?.close()
    }
}
