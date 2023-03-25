package net.aquadc.lubricant.blur

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.annotation.Px
import kotlin.math.max
import kotlin.math.sign


class DynamicBlur(
    private val blur: StackBlur,
    @Px radius: Int,
    private val downscale: Int,
    private val horizontalScroll: Boolean,
    private val verticalScroll: Boolean,
    private val draw: (Canvas) -> Boolean,
) {
    var radius: Int = 0
        @Px get
        set(@Px value) {
            require(value >= 0)
            val newScaledRadius = max(value / downscale, value.sign)
            if (scaledRadius != newScaledRadius) {
                scaledRadius = newScaledRadius
                isDirty = true
            }
            field = value
        }
    private var scaledRadius = -1
    init {
        require(downscale > 0)
        this.radius = radius
    }

    private var blurredWidth = 0
    private var blurredHeight = 0

    private var blurBitmap: Bitmap? = null
    private val blurCanvas = Canvas()
    var isDirty = true
        private set
    private var drawn = false
    fun invalidate() {
        isDirty = true
    }

    private val blurSrc = Rect()
    private val blurDst = Rect()
    fun draw(on: Canvas, dstLeft: Int, dstTop: Int, blurWidth: Int, blurHeight: Int, @ColorInt solidColor: Int = 0, paint: Paint? = null) {
        if (isDirty || blurredWidth < blurWidth || blurredHeight < blurHeight) {
            blurredWidth = blurWidth
            blurredHeight = blurHeight
            reblur(blurWidth, blurHeight, solidColor)
        }
        if (drawn) {
            val insetH = if (horizontalScroll) scaledRadius else 0
            val insetV = if (verticalScroll) scaledRadius else 0
            blurSrc.set(insetH, insetV, insetH + blurWidth / downscale, insetV + blurHeight / downscale)
            blurDst.set(dstLeft, dstTop, dstLeft + blurWidth, dstTop + blurHeight)
            on.drawBitmap(blurBitmap!!, blurSrc, blurDst, paint)
        }
    }

    private fun reblur(width: Int, height: Int, solidColor: Int) {
        isDirty = false
        if (scaledRadius == 0) {
            drawn = false
            return
        }

        // if scrollable, fix sharp blur edges by adding blur radius before and after
        val insetH = if (horizontalScroll) scaledRadius else 0
        val insetV = if (verticalScroll) scaledRadius else 0
        val scaledW = insetH + width / downscale + insetH
        val scaledH = insetV + height / downscale + insetV

        configure(scaledW, scaledH)
        if (solidColor != 1) { // special value meaning "gonna fill it whole"
            blurBitmap!!.eraseColor(solidColor)
        }

        blurCanvas.apply {
            val save = save()
            // The bitmap left from the previous time could be significantly bigger.
            // Clipping avoids extra drawing thanks to quickReject() checks in Android SDK.
            clipRect(0, 0, scaledW, scaledH)
            translate(insetH.toFloat(), insetV.toFloat())
            scale(1f / downscale, 1f / downscale)
            drawn = draw(this)
            restoreToCount(save)
            if (drawn) {
                blur(solidColor, scaledRadius)
            }
        }
    }
//    private var sum = 0L
//    private var times = 0
    private fun blur(solidColor: Int, radius: Int) {
//        var time = System.nanoTime()
        if (Color.alpha(solidColor) == 255)
            blur.blurRgb(blurBitmap!!, radius)
        else
            blur.blurArgb(blurBitmap!!, radius)
        /*time = System.nanoTime() - time
        sum += time
        times++
        android.util.Log.i(
            "DynamicBlur",
            "$this(${blurBitmap!!.width}x${blurBitmap!!.height} r$radius) in ${time/1000} μs, avg($times) ${sum/times/1000} μs",
        )*/
    }

    private fun configure(width: Int, height: Int) {
        val bitmap = blurBitmap
        if (bitmap == null || ((bitmap.width != width || bitmap.height != height) && !tryReconfigure(width, height))) {
            bitmap?.recycle()
            blurBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            blurCanvas.setBitmap(blurBitmap)
        }
    }
    private fun tryReconfigure(width: Int, height: Int) =
            blurBitmap!!.allocationByteCount >= (width * height * 4) &&
                try { blurBitmap!!.reconfigure(width, height, Bitmap.Config.ARGB_8888); true }
                catch (e: IllegalArgumentException) { false }

}
