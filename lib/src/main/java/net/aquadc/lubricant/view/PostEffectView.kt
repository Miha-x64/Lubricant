package net.aquadc.lubricant.view

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.Px
import net.aquadc.lubricant.PostEffect
import net.aquadc.lubricant.forEachReferent
import net.aquadc.lubricant.invalidateChildInParent
import java.lang.ref.WeakReference

/**
 * Background aware, invalidation tracking, postEffect-enabled View, Drawable or whatever.
 */
interface PostEffectView {

    @ColorInt fun getSolidColor(): Int
    fun setSolidColor(@ColorInt color: Int)

    @Px fun getWidth(): Int
    @Px fun getHeight(): Int

    operator fun plusAssign(effect: PostEffect)
    operator fun minusAssign(effect: PostEffect)

    fun drawFully(canvas: Canvas)
    fun requestRedraw()

}

internal fun ArrayList<WeakReference<PostEffect>>.onInvalidated(child: View?) {
    forEachReferent { it.onInvalidated(child) }
}
internal fun ArrayList<WeakReference<PostEffect>>.invalidateChildInParent(parent: ViewGroup) {
    forEachReferent { it.invalidateChildInParent(parent) }
}
internal fun ArrayList<WeakReference<PostEffect>>.clipOut(canvas: Canvas) {
    forEachReferent { it.clipOut(canvas) }
}
