package com.android.example.cameraxbasic.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var dimenRect = Size(1,1);
    private var dimenWin = Size(1,1)
    private var rotRect = 0
    private var rectList = mutableListOf<Rect>()
    private var matRect = Matrix()
    private var paint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 10f
        style = Paint.Style.STROKE
    }
    private fun recalcmat() {
        matRect = Matrix().apply {
            setScale((if (rotRect==270) -1 else 1)*dimenWin.width/dimenRect.width.toFloat(),
                     dimenWin.height/dimenRect.height.toFloat())
            preRotate(rotRect.toFloat())
            postTranslate(
                    dimenWin.width.toFloat(),
                    if (rotRect==270) dimenWin.height.toFloat() else 0f)
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        dimenWin = Size(w,h)
        recalcmat()
    }
    fun setSize(s: Size, r: Int) {
        Log.d("CameraXBasic","setSize $s $r")
        rotRect = r
        dimenRect = if (r%180==0) s else Size(s.height,s.width)
        recalcmat()
    }
    fun add(rect: Rect) {
        rectList.add(rect)
        invalidate()
    }
    fun clear() {
        if (rectList.size>0) {
            rectList.clear()
            invalidate()
        }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var rectf = RectF()
        rectList.forEach {
            rectf.set(it)
            matRect.mapRect(rectf)
            // Log.d("CameraXBasic","drawRect ${rectfTemp.toShortString()}")
            canvas.drawRoundRect(rectf,rectf.width()/2,rectf.height()/2,paint)
        }
    }
}
