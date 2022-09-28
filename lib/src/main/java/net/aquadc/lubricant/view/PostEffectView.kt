package net.aquadc.lubricant.view

import android.graphics.Canvas
import androidx.annotation.ColorInt
import androidx.annotation.Px
import net.aquadc.lubricant.PostEffect

/**
 * Background aware, invalidation tracking, postEffect-enabled View, Drawable or whatever.
 */
interface PostEffectView {

    @ColorInt fun getSolidColor(): Int
    fun setSolidColor(@ColorInt color: Int)

    @Px fun getWidth(): Int
    @Px fun getHeight(): Int

    var postEffect: PostEffect?

    fun drawFully(canvas: Canvas)
    fun requestRedraw()

}
