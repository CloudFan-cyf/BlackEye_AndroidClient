package com.example.blackeye

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import android.graphics.Color
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.example.blackeye.WebSocketClientManager


class MainActivity : AppCompatActivity() {

    private lateinit var videoSurface: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var batteryIcon: ImageView
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set the orientation to landscape
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        videoSurface = findViewById(R.id.videoSurface)
        surfaceHolder = videoSurface.holder
        batteryIcon = findViewById(R.id.batteryIcon)

        val token = intent.getStringExtra("TOKEN")
        val webSocketClient = WebSocketClientManager.getInstance(token)
        WebSocketClientManager.videoFrameListener = { bytes ->
            runOnUiThread {
                displayVideoFrame(bytes)
            }
        }

        // Initialize joystick and battery icon
//        initJoystick()
        initBatteryIcon()

        // Connect to WebSocket server
        connectWebSocket(token)
    }

//    private fun initJoystick() {
//        val joystick: JoystickView = findViewById(R.id.joystick)
//        joystick.setOnMoveListener(object : JoystickView.JoystickListener {
//            override fun onMove(angle: Int, strength: Int) {
//                // Implement your joystick control logic here
//                // Send control commands to ESP32
//                sendControlSignalToESP32(angle, strength)
//            }
//        })
//    }

    // Make sure to handle back press for Drawer
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun initBatteryIcon() {
        // Implement your battery icon logic here
        // Update battery icon based on the battery level received from ESP32
        updateBatteryIcon(100) // Example: set battery level to 100%
    }

    private fun updateBatteryIcon(batteryLevel: Int) {
        // Update battery icon based on the battery level
        val resId = when {
            batteryLevel > 90 -> R.drawable.battery_full
            batteryLevel > 70 -> R.drawable.battery_75
            batteryLevel > 25 -> R.drawable.battery_25
            batteryLevel > 5 -> R.drawable.battery_empty
            else -> R.drawable.battery_empty
        }
        val drawable: Drawable = resources.getDrawable(resId, null)
        batteryIcon.setImageDrawable(drawable)
    }

    private fun sendControlSignalToESP32(angle: Int, strength: Int) {
        // Implement the WebSocket logic to send control signals to ESP32
        val controlMessage = "servo1:$angle,servo2:$strength"
        webSocketClient.send(controlMessage)
    }

    private fun connectWebSocket(token: String?) {
        val uri = URI("ws://49.233.216.82:5901/user")
        val headers = mapOf("Sec-WebSocket-Protocol" to token)
        webSocketClient = object : WebSocketClient(uri, headers) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // WebSocket connection opened
            }

            override fun onMessage(message: String?) {
                // Handle text messages from the server
                if (message?.startsWith("battery:") == true) {
                    val batteryLevel = message.split(":")[1].toInt()
                    runOnUiThread {
                        updateBatteryIcon(batteryLevel)
                    }
                }
            }

            override fun onMessage(bytes: ByteBuffer) {
                // 第一个字节是数据类型
                val dataType = bytes[0]
                if (dataType.toInt() == 0x01) { // 数据类型为视频帧
                    // 跳过第一个字节读取剩余的 JPEG 数据
                    val jpegBytes = ByteArray(bytes.remaining() - 1)
                    bytes.position(1) // 忽略第一个字节

                    bytes[jpegBytes, 0, jpegBytes.size]

                    // 显示视频帧
                    displayVideoFrame(ByteBuffer.wrap(jpegBytes))
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // Handle WebSocket close event
            }

            override fun onError(ex: Exception?) {
                // Handle WebSocket error event
            }
        }
        webSocketClient.connect()
    }

    private fun displayVideoFrame(frameData: ByteBuffer) {
        // Implement logic to display the video frame on the SurfaceView
        // 将ByteBuffer转换为字节数组
        val byteArray = ByteArray(frameData.remaining())
        frameData.get(byteArray)

        // 使用BitmapFactory解码字节数组为Bitmap
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        // 检查Bitmap是否有效
        if (bitmap != null) {
            drawToSurface(bitmap)
        } else {
            Log.e("MainActivity", "Failed to decode frame data to Bitmap.")
        }
    }

    private fun drawToSurface(bitmap: Bitmap) {
        val canvas = surfaceHolder.lockCanvas()
        if (canvas != null) {
            try {
                if (canvas != null) {
                    // 计算最大可能的显示尺寸，保持长宽比
                    val scaleFactor = Math.min(
                        canvas.width.toFloat() / bitmap.width,
                        canvas.height.toFloat() / bitmap.height
                    )
                    val scaledWidth = (bitmap.width * scaleFactor).toInt()
                    val scaledHeight = (bitmap.height * scaleFactor).toInt()

                    // 创建缩放后的图像
                    val scaledBitmap =
                        Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

                    // 计算绘图坐标，使图像居中
                    val x = (canvas.width - scaledWidth) / 2.0f
                    val y = (canvas.height - scaledHeight) / 2.0f
                    canvas.drawColor(Color.WHITE)  // 用白色清空画布以减少闪烁

                    canvas.drawBitmap(scaledBitmap, x, y, null)
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // Close WebSocket connection when the activity is destroyed
        webSocketClient.close()
    }
}





