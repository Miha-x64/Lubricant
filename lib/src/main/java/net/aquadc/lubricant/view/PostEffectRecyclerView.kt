package net.aquadc.lubricant.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewParent
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.lubricant.PostEffect
import net.aquadc.lubricant.invalidateChildInParent

open class PostEffectRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs), PostEffectView {

    private var _solidColor: Int = 0
    @Suppress("DEPRECATION") final override fun getSolidColor(): Int =
        if (background?.opacity == PixelFormat.OPAQUE) 1 else _solidColor
    override fun setSolidColor(@ColorInt color: Int) {
        _solidColor = if (Color.alpha(color) == 255) color else 0
    }

    override var postEffect: PostEffect? = null

    @CallSuper override fun onDescendantInvalidated(child: View, target: View) {
        super.onDescendantInvalidated(child, target)
        postEffect?.takeIf { !it.isDirty }?.onInvalidated(child)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @CallSuper override fun invalidateChildInParent(location: IntArray?, dirty: Rect?): ViewParent? {
        @Suppress("DEPRECATION") val parent = super.invalidateChildInParent(location, dirty)
        postEffect?.invalidateChildInParent(this)
        return parent
    }

    override fun invalidateDrawable(drawable: Drawable) {
        super.invalidateDrawable(drawable)
        postEffect?.takeIf { !it.isDirty && verifyDrawable(drawable) }?.onInvalidated(null)
    }

    @CallSuper override fun invalidate() {
        super.invalidate()
        postEffect?.takeIf { !it.isDirty }?.onInvalidated(null)
    }

    final override fun draw(c: Canvas) {
        postEffect?.clipOut(c)
        super.draw(c)
    }

    final override fun drawFully(canvas: Canvas) {
        super.draw(canvas)
    }
    final override fun requestRedraw() {
        super.invalidate()
    }
}
