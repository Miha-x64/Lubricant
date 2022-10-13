package net.aquadc.lubricant

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup


interface PostEffect {

    val isDirty: Boolean

    /**
     * Receive invalidation notification.
     * @param child which child was invalidated, or null, if it was the whole view
     * @return whether invalidation was propagated
     */
    fun onInvalidated(child: View?): Boolean

    /**
     * Avoid rendering the part affected by this effect.
     */
    fun clipOut(canvas: Canvas)
}

fun PostEffect.invalidateChildInParent(parent: ViewGroup) {
    if (isDirty) return
    for (i in 0 until parent.childCount)
        if (parent.getChildAt(i).let { it.isDirty && onInvalidated(it) })
            break
}
