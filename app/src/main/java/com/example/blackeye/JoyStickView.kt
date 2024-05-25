package com.example.blackeye

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class JoystickView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var innerCirclePaint: Paint = Paint()
    private var outerCirclePaint: Paint = Paint()
    private var baseCenterX: Float = 0f  // 基底圆心X坐标
    private var baseCenterY: Float = 0f  // 基底圆心Y坐标
    private var handleX: Float = 0f      // 小圆中心X坐标
    private var handleY: Float = 0f      // 小圆中心Y坐标
    private var baseRadius: Float = 0f   // 基底圆半径
    private var handleRadius: Float = 0f // 小圆半径
    private var initialCenterX: Float = 0f
    private var initialCenterY: Float = 0f
    private var joystickCallback: JoystickListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var shouldSendData = false



    init {
        innerCirclePaint.color = Color.DKGRAY
        innerCirclePaint.style = Paint.Style.FILL_AND_STROKE

        outerCirclePaint.color = Color.LTGRAY
        outerCirclePaint.style = Paint.Style.FILL_AND_STROKE
        updateRunnable = Runnable {
            if (shouldSendData) {
                val percentX = 100 * (handleX - baseCenterX + baseRadius) / (2 * baseRadius)
                val percentY = 100 * ((baseCenterY + baseRadius) - handleY) / (2 * baseRadius)
                WebSocketClientManager.sendMessage("H:$percentX V:$percentY")
                shouldSendData = false // 重置标志
            }
            updateRunnable?.let { handler.postDelayed(it, 300) } // 重新调度Runnable，100毫秒后再次执行
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        baseCenterX = (width / 2).toFloat()
        baseCenterY = (height / 2).toFloat()
        handleX = baseCenterX  // 初始化时，小圆位于中心
        handleY = baseCenterY
        baseRadius = (min(width, height) / 3).toFloat()
        handleRadius = (min(width, height) / 8).toFloat()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制基底圆，位置固定
        canvas.drawCircle(baseCenterX, baseCenterY, baseRadius, outerCirclePaint)
        // 绘制可移动的小圆
        canvas.drawCircle(handleX, handleY, handleRadius, innerCirclePaint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dx = event.x - baseCenterX
        val dy = event.y - baseCenterY
        val distance = sqrt(dx.pow(2) + dy.pow(2))
        val angle = atan2(dy, dx)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                updateRunnable?.let { handler.postDelayed(it, 100) } // 在第一次按下时开始调度Runnable
                // 以下处理逻辑与原来相同
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                if (distance < baseRadius) {
                    handleX = event.x
                    handleY = event.y
                } else {
                    handleX = (cos(angle) * baseRadius + baseCenterX).toFloat()
                    handleY = (sin(angle) * baseRadius + baseCenterY).toFloat()
                }
                shouldSendData = true
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                updateRunnable?.let { handler.removeCallbacks(it) } // 停止发送数据
                handleX = baseCenterX
                handleY = baseCenterY
                invalidate()
            }
        }
        return true
    }



    interface JoystickListener {
        fun onJoystickMoved(percentX: Float, percentY: Float)

    }


}