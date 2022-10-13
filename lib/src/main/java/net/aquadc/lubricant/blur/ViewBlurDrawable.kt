package net.aquadc.lubricant.blur

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.IntProperty
import android.util.Property
import android.view.View
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import net.aquadc.lubricant.PostEffect
import net.aquadc.lubricant.blur.StackBlur.stackBlur
import net.aquadc.lubricant.outline.OutlineShape
import net.aquadc.lubricant.outline.RectOutline
import net.aquadc.lubricant.view.PostEffectRecyclerView
import net.aquadc.lubricant.view.PostEffectView

class ViewBlurDrawable(
    private val source: PostEffectView,
    @Px radius: Int,
    horizontalScroll: Boolean,
    verticalScroll: Boolean,
    outline: OutlineShape = RectOutline,
    downscale: Int,
    blur: StackBlur = stackBlur(),
    private val clipOut: Boolean = true,
) : Drawable(), PostEffect {

    var outline: OutlineShape = outline
        set(value) {
            field = value
            bndsChg()
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getOutline(outline: Outline) {
        val bnds = bounds
        this.outline.getOutline(outline, bnds.left, bnds.top, bnds.right, bnds.bottom)
        outline.alpha = Color.alpha(source.getSolidColor()) / 255f
    }

    var srcOffsetX: Int = 0
        set(value) {
            if (field != value) {
                field = value
                blur.invalidate()
                bndsChg()
            }
        }

    var srcOffsetY: Int = 0
        set(value) {
            if (field != value) {
                field = value
                blur.invalidate()
                bndsChg()
            }
        }

    private fun bndsChg() {
        if (useful()) {
            if (clipOut)
                source.requestRedraw()
            invalidateSelf()
        }
    }

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val blur = DynamicBlur(blur, radius, downscale, horizontalScroll, verticalScroll) { c ->
//        var time = System.nanoTime()
        c.translate(-srcOffsetX.toFloat(), -srcOffsetY.toFloat())

        source.drawFully(c)
//        time = System.nanoTime() - time
//        Log.i("ViewBlurDrawable", "drawn views into buffer in ${time / 1000} us")
        true
    }

    var radius: Int
        get() = blur.radius
        set(value) {
            val u = useful()
            blur.radius = value
            if (clipOut && u != useful()) {
                source.requestRedraw() // we're either appeared or disappeared, start or stop clipping source
            }
            if (blur.isDirty) {
                invalidateSelf()
            }
        }

    override fun draw(canvas: Canvas) {
        if (!useful())
            return
        val bnds = bounds
        outline.clip(canvas, bnds.left, bnds.top, bnds.right, bnds.bottom)
        blur.draw(
            canvas,
            bnds.left, bnds.top,
            bnds.width(), bnds.height(),
            source.getSolidColor(),
            paint,
        )
    }

    override fun getAlpha(): Int = paint.alpha
    override fun setAlpha(alpha: Int) {
        if (paint.alpha != alpha) {
            val u = useful()
            paint.alpha = alpha
            if (u != useful())
                source.requestRedraw()
            invalidateSelf()
        }
    }

    override fun getColorFilter(): ColorFilter? = paint.colorFilter
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("OVERRIDE_DEPRECATION") override fun getOpacity(): Int =
        if (Color.alpha(source.getSolidColor()) == 255) PixelFormat.OPAQUE
        else PixelFormat.TRANSLUCENT

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        val oldU = useful()
        val old = bounds
        val chg = old.left != left || old.top != top || old.right != right || old.bottom != bottom
        val oldW = old.width()
        val oldH = old.height()
        super.setBounds(left, top, right, bottom)
        val newU = useful()
        if (clipOut && (oldU != newU || (newU && chg)))
            source.requestRedraw()
        if (bounds.width() > oldW || bounds.height() > oldH) {
            invalidateSelf()
        }
    }

    override val isDirty: Boolean
        get() = blur.isDirty

    private val rect = Rect()
    override fun onInvalidated(child: View?): Boolean {
        if (child == null || rect.setIntersect(rect.also(child::getHitRect), bounds)) {
            blur.invalidate()
            invalidateSelf()
            return true
        }
        return false
    }

    override fun clipOut(canvas: Canvas) {
        if (!clipOut) return

        // We don't need our view to draw post-effect-affected area.
        // This is super important without solidColor where blur overlay is transparent;
        // otherwise, just gonna shrink drawing area and save cycles.
        val bnds = bounds
        if (bnds.isEmpty || srcOffsetX >= source.getWidth() || srcOffsetY >= source.getHeight() || radius == 0 || paint.alpha != 255)
            return
        val right = srcOffsetX + bnds.width()
        val bottom = srcOffsetY + bnds.height()
        outline.clipOut(canvas, srcOffsetX, srcOffsetY, right, bottom)
    }

    private fun useful(): Boolean =
// isVisible == false when our callback/View is absent, INVISIBLE, GONE, or detached.
// This check would save some miserable cycles in rare scenarios but break some existing screen-shooting code, skip it.
        !bounds.isEmpty && srcOffsetX < source.getWidth() && srcOffsetY < source.getHeight() &&
            blur.radius > 0 && paint.alpha > 0

    companion object {
        @JvmField val RADIUS: Property<ViewBlurDrawable, Int> =
            intProperty("radius", { radius }, { radius = it })
        @JvmField val SRC_OFFSET_X: Property<ViewBlurDrawable, Int> =
            intProperty("srcOffsetX", { srcOffsetX }, { srcOffsetX = it })
        @JvmField val SRC_OFFSET_Y: Property<ViewBlurDrawable, Int> =
            intProperty("srcOffsetY", { srcOffsetY }, { srcOffsetY = it })

        private inline fun intProperty(
            name: String,
            crossinline get: ViewBlurDrawable.() -> Int,
            crossinline set: ViewBlurDrawable.(Int) -> Unit,
        ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            object : IntProperty<ViewBlurDrawable>(name) {
                override fun get(obj: ViewBlurDrawable): Int = obj.get()
                override fun setValue(obj: ViewBlurDrawable, value: Int) { obj.set(value) }
            }
        else
            object : Property<ViewBlurDrawable, Int>(Int::class.java, name) {
                override fun get(obj: ViewBlurDrawable): Int = obj.get()
                override fun set(obj: ViewBlurDrawable, value: Int) { obj.set(value) }
            }
    }
}

fun PostEffectRecyclerView.blurDrawable(
    @Px radius: Int,
    downscale: Int = (resources.displayMetrics.density + .5f).toInt(),
    outline: OutlineShape = RectOutline,
    blur: StackBlur = stackBlur(),
    clipOut: Boolean = true,
): ViewBlurDrawable {
    return ViewBlurDrawable(
        this,
        radius,
        (layoutManager ?: throw NullPointerException("Set RecyclerView#layoutManager first")).canScrollHorizontally(),
        layoutManager!!.canScrollVertically(),
        outline,
        downscale,
        blur,
        clipOut,
    ).also { this += it }
}
