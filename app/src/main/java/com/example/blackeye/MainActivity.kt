package com.example.blackeye

import android.content.Intent
import com.example.blackeye.WebSocketClientManager
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import org.java_websocket.client.WebSocketClient

import java.nio.ByteBuffer
import android.graphics.Color
import android.view.MotionEvent
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.collision.HitResult


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

        private lateinit var videoSurface: SurfaceView
        private lateinit var surfaceHolder: SurfaceHolder
        private lateinit var batteryIcon: ImageView
        private lateinit var webSocketClient :WebSocketClient
        private lateinit var drawerLayout: DrawerLayout
        private lateinit var navigationLayout : NavigationView
        private var isCameraAsleep: Boolean = false  // 初始状态设置为已唤醒
        private lateinit var toolbar: Toolbar
        private lateinit var sceneView: SceneView
        private lateinit var modelNode: ModelNode
        private lateinit var token:String


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)

            drawerLayout = findViewById(R.id.drawer_layout)
            drawerLayout = findViewById(R.id.drawer_layout)
            navigationLayout = findViewById(R.id.navigation_view)
            navigationLayout.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_camera_sleep -> {
                        toggleCameraState()
                        true
                    }
                    R.id.nav_account -> {
                        val intent = Intent(this, AccountManagementActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_quit -> {
                        // Handle application quitting
                        showExitConfirmation()
                        true
                    }
                    else -> false
                }
            }
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

            sceneView = findViewById(R.id.sceneView)


            val modelFile = "models/3D.glb"

            val modelInstance = sceneView.modelLoader.createModelInstance(modelFile)
            modelNode = ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 1.0f,
            )
            modelNode.scale = Scale(20f)
            modelNode.isTouchable = false
            sceneView.addChildNode(modelNode)
            sceneView.cameraNode.isPositionEditable = true
            sceneView.cameraNode.isRotationEditable = true

//            sceneView.onFrame = { elapsed ->
//                Log.d(TAG, "onFrame: $elapsed")
//            }
            sceneView.onTouchEvent =  { motionEvent: MotionEvent, hitResult: HitResult? ->
                Log.d("TEST", "$motionEvent, $hitResult")
                modelNode.rotation = Rotation(y = -45.0f)
                sceneView.invalidate()

                true
            }






            token = intent.getStringExtra("TOKEN").toString()
            webSocketClient = WebSocketClientManager.getInstance(token)
            WebSocketClientManager.videoFrameListener = { bytes ->
                runOnUiThread {
                    displayVideoFrame(bytes)
                }
            }
            WebSocketClientManager.imuDataListener = { yaw, pitch, roll ->
                updateModelView(yaw, pitch, roll)
            }

            initBatteryIcon()



        }

    override fun onResume() {
        super.onResume()
        sceneView.invalidate()


    }


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
                if(isCameraAsleep){
                    canvas.drawColor(Color.WHITE)  // 用白色清空画布以减少闪烁

                }
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun updateModelView(yaw: Float, pitch: Float, roll: Float) {
        // 设置模型的旋转，需要将角度转换为弧度
        // 保证这里的yaw, pitch, roll是以弧度为单位
        modelNode.rotation = Rotation(yaw,pitch,roll)

    }

    private fun toggleCameraState() {
        if (isCameraAsleep) {
            sendCameraCommand("wake")
            navigationLayout.menu.findItem(R.id.nav_camera_sleep).apply {
                title = "Put Camera to Sleep"
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_sleep)
            }
        } else {
            sendCameraCommand("sleep")
            navigationLayout.menu.findItem(R.id.nav_camera_sleep).apply {
                title = "Wake up Camera"
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_wake)
            }
        }
        isCameraAsleep = !isCameraAsleep
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Comfirm") // 设置对话框标题
            .setMessage("Are you really want to quit?") // 设置对话框消息提示
            .setPositiveButton("Yes") { dialog, which ->
                // 用户确认退出应用
                finishAffinity() // 结束所有Activity，适用于API 16以上
            }
            .setNegativeButton("No") { dialog, which ->
                // 用户取消退出，关闭对话框，不做任何事情
                dialog.dismiss()
            }
            .show() // 显示对话框
    }


    private fun sendCameraCommand(command: String) {
        // Assuming WebSocketClientManager is already set up and connected

        WebSocketClientManager.sendMessage(command)
    }






    override fun onDestroy() {
        super.onDestroy()
        // Close WebSocket connection when the activity is destroyed
        WebSocketClientManager.close()
        sceneView.destroy()
    }
}





