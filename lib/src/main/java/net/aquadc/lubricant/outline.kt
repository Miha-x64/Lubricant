@file:JvmName("OutlineShapes")
@file:Suppress("FunctionName")
package net.aquadc.lubricant.outline

import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.Size

/**
 * We need something to describe the shape of post-effect.
 * Outline is write-only (and SDK 21), it cannot be read from without using private APIs.
 * Shape can be drawn but we need clipping.
 *
 * As a bonus, we're stateless.
 *
 * @see android.graphics.Outline
 * @see android.graphics.drawable.shapes.Shape
 */
interface OutlineShape {
    fun clip(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int)
    fun clipOut(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getOutline(outline: Outline, left: Int, top: Int, right: Int, bottom: Int)
}

@JvmField val RectOutline = object : OutlineShape {
    override fun clip(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int) {
        canvas.clipRect(left, top, right, bottom)
    }
    override fun clipOut(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutRect(left, top, right, bottom)
        } else {
            @Suppress("DEPRECATION")
            canvas.clipRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), Region.Op.DIFFERENCE)
        }
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getOutline(outline: Outline, left: Int, top: Int, right: Int, bottom: Int) {
        outline.setRect(left, top, right, bottom)
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun RoundRectOutline(@Px radius: Float): OutlineShape {
    require(radius.isFinite() && radius >= 0) { radius }
    return if (radius == 0f) RectOutline else RoundRectShapeOutline(radius, radius, null)
}
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun RoundRectOutline(@Px rx: Float, @Px ry: Float): OutlineShape {
    require(rx.isFinite() && rx >= 0) { rx }
    require(ry.isFinite() && ry >= 0) { ry }
    return if (rx == 0f && ry == 0f) RectOutline else RoundRectShapeOutline(rx, ry, null)
}
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun RoundRectOutline(@Px @Size(8) vararg radii: Float): OutlineShape {
    require(radii.size == 8) { radii.contentToString() }
    var zero = true
    for (i in 0 until 8) {
        require(radii[i].isFinite() && radii[i] >= 0f) { radii.contentToString() }
        zero = zero && radii[i] == 0f
    }
    return when {
        zero -> RectOutline
        radii[0] == radii[2] && radii[2] == radii[4] && radii[4] == radii[6] &&
            radii[1] == radii[3] && radii[3] == radii[5] && radii[5] == radii[7] ->
            RoundRectShapeOutline(radii[0], radii[1], null) // this gives better outline if rx == ry
        else -> RoundRectShapeOutline(Float.NaN, Float.NaN, radii)
    }
}
@RequiresApi(Build.VERSION_CODES.LOLLIPOP) // Lazy ass.
private class RoundRectShapeOutline(       // Implement your own path winding for ancient devices if you ever need this.
    private val rx: Float,
    private val ry: Float,
    private val radii: FloatArray?,
) : OutlineShape {
    private val bounds = Rect()
    private val path = Path()
    private fun set(left: Int, top: Int, right: Int, bottom: Int) {
        if (bounds.left != left || bounds.top != top || bounds.right != right || bounds.bottom != bottom) {
            bounds.set(left, top, right, bottom)
            path.rewind()
            if (radii == null) path.addRoundRect(
                left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), rx, ry, Path.Direction.CW
            ) else path.addRoundRect(
                left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), radii, Path.Direction.CW
            )
        }
    }
    override fun clip(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int) {
        set(left, top, right, bottom)
        canvas.clipPath(path)
    }
    override fun clipOut(canvas: Canvas, left: Int, top: Int, right: Int, bottom: Int) {
        set(left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(path)
        } else {
            @Suppress("DEPRECATION")
            canvas.clipPath(path, Region.Op.DIFFERENCE)
        }
    }
    override fun getOutline(outline: Outline, left: Int, top: Int, right: Int, bottom: Int) {
        if (radii == null && rx == ry) outline.setRoundRect(left, top, right, bottom, rx)
        else {
            set(left, top, right, bottom)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                outline.setPath(path)
            } else {
                @Suppress("DEPRECATION")
                outline.setConvexPath(path)
            }
        }
    }
}
