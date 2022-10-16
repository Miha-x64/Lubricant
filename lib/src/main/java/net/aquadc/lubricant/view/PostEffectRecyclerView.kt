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
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.lubricant.PostEffect
import net.aquadc.lubricant.removeReferent
import java.lang.ref.WeakReference

open class PostEffectRecyclerView : RecyclerView, PostEffectView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var _solidColor: Int = 0
    @Suppress("DEPRECATION") final override fun getSolidColor(): Int =
        if (background?.opacity == PixelFormat.OPAQUE) 1 else _solidColor
    override fun setSolidColor(@ColorInt color: Int) {
        _solidColor = if (Color.alpha(color) == 255) color else 0
    }

    private var effects: ArrayList<WeakReference<PostEffect>>? = null
    override fun plusAssign(effect: PostEffect) {
        (effects ?: ArrayList<WeakReference<PostEffect>>().also { effects = it }).add(WeakReference(effect))
    }
    override fun minusAssign(effect: PostEffect) {
        effects?.removeReferent(effect)
    }

    @CallSuper override fun onDescendantInvalidated(child: View, target: View) {
        super.onDescendantInvalidated(child, target)
        effects?.onInvalidated(child)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @CallSuper override fun invalidateChildInParent(location: IntArray?, dirty: Rect?): ViewParent? {
        @Suppress("DEPRECATION") val parent = super.invalidateChildInParent(location, dirty)
        effects?.invalidateChildInParent(this)
        return parent
    }

    override fun invalidateDrawable(drawable: Drawable) {
        super.invalidateDrawable(drawable)
        if (verifyDrawable(drawable))
            effects?.onInvalidated(null)
    }
    @CallSuper override fun invalidate() {
        super.invalidate()
        effects?.onInvalidated(null)
    }
    final override fun draw(c: Canvas) {
        effects?.clipOut(c)
        super.draw(c)
    }
    final override fun drawFully(canvas: Canvas) {
        super.draw(canvas)
    }
    final override fun requestRedraw() {
        super.invalidate()
    }
}
