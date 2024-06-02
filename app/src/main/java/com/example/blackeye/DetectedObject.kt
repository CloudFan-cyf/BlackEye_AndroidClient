package com.example.blackeye

import android.graphics.RectF

data class DetectedObject(
    val label: String,         // 检测对象的类别
    //val confidence: Float,    // 置信度
    val boundingBox: RectF    // 边界框
)
