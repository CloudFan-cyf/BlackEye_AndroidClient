package com.example.blackeye

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas

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
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import io.github.sceneview.SceneView
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.collision.HitResult
import io.github.sceneview.math.Position

import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.FileInputStream
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        private const val MAX_FONT_SIZE = 96F
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
            modelNode.position = Position(x = 0f, y = -0.3f, z = -0.5f)

            sceneView.addChildNode(modelNode)
            sceneView.cameraNode.isPositionEditable = true
            sceneView.cameraNode.isRotationEditable = true


            sceneView.onTouchEvent =  { motionEvent: MotionEvent, hitResult: HitResult? ->
                sceneView.renderer.render(sceneView.view)
                sceneView.invalidate()

                false
            }

            token = intent.getStringExtra("TOKEN").toString()
            webSocketClient = WebSocketClientManager.getInstance(token)
            WebSocketClientManager.videoFrameListener = { bytes ->
                runOnUiThread {
                    displayVideoFrame(bytes)
                }
            }
            WebSocketClientManager.imuDataListener = { yaw, pitch, roll ->
                sceneView.onFrame={elapsed ->
                    updateModelView(yaw, pitch, roll)
                    sceneView.invalidate()
                }
            }

            initBatteryIcon()
        }

    override fun onResume() {
        super.onResume()
        sceneView.invalidate()
        sceneView.renderer.render(sceneView.view)
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
            val detectedObjects = runObjectDetection(bitmap)
            drawToSurface(bitmap,detectedObjects)
        } else {
            Log.e("MainActivity", "Failed to decode frame data to Bitmap.")
        }
    }

    private fun drawToSurface(bitmap: Bitmap, detectedObjects: List<DetectedObject>) {
        val canvas = surfaceHolder.lockCanvas()
        if (canvas != null) {
            try {
                val scaleFactor = Math.min(
                    canvas.width.toFloat() / bitmap.width,
                    canvas.height.toFloat() / bitmap.height
                )
                val scaledWidth = (bitmap.width * scaleFactor).toInt()
                val scaledHeight = (bitmap.height * scaleFactor).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                val x = (canvas.width - scaledWidth) / 2.0f
                val y = (canvas.height - scaledHeight) / 2.0f
                canvas.drawColor(Color.WHITE)  // 用白色清空画布以减少闪烁
                canvas.drawBitmap(scaledBitmap, x, y, null)

                // 绘制检测结果
                val paint = Paint()
                paint.color = Color.RED
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4.0f
                paint.textAlign = Paint.Align.LEFT
                for (obj in detectedObjects) {
                    val left = x + (obj.boundingBox.left*scaleFactor).toInt()
                    val top = y + (obj.boundingBox.top*scaleFactor).toInt()
                    val right = x + (obj.boundingBox.right*scaleFactor).toInt()
                    val bottom = y + (obj.boundingBox.bottom*scaleFactor).toInt()
                    canvas.drawRect(left, top, right, bottom, paint)
                    // 绘制标签
                    paint.style = Paint.Style.FILL_AND_STROKE
                    paint.textSize = 20.0f
                    canvas.drawText(obj.label, left, top - 10, paint)
                    paint.style = Paint.Style.STROKE

                }
            } finally {
                if (isCameraAsleep) {
                    canvas.drawColor(Color.WHITE)  // 用白色清空画布以减少闪烁
                }
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun runObjectDetection(bitmap: Bitmap): List<DetectedObject> {
        // Step 1: create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)
        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.5f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this, // the application context
            "efficientdet.tflite", // must be same as the filename in assets folder
            options
        )
        // Step 3: feed given image to the model and print the detection result
        val results = detector.detect(image)
        // Step 4: Parse the detection result and show it
        val resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}, ${category.score.times(100).toInt()}%"

            // Create a data object to display the detection result
            DetectedObject(category.label, it.boundingBox)
        }

        return resultToDisplay
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

    override fun onStop() {
        super.onStop()
        WebSocketClientManager.close()

    }

    override fun onRestart() {
        super.onRestart()
        WebSocketClientManager.getInstance(token)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Close WebSocket connection when the activity is destroyed
        WebSocketClientManager.close()
        sceneView.destroy()
    }
}





