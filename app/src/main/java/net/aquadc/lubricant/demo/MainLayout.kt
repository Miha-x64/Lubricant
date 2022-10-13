package net.aquadc.lubricant.demo

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import net.aquadc.lubricant.blur.ViewBlurDrawable
import net.aquadc.lubricant.blur.blurDrawable
import net.aquadc.lubricant.view.PostEffectRecyclerView
import kotlin.math.max

class MainLayout(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    private var top = 0
    private var bottom = 0
    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            top = max(top, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            bottom = max(bottom, insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            getChildAt(0).setPadding(0, top, 0, bottom)
            requestLayout()
            WindowInsetsCompat.CONSUMED
        }
    }

    lateinit var statusBlur: ViewBlurDrawable
    lateinit var contentBlur: ViewBlurDrawable
    lateinit var navBlur: ViewBlurDrawable
    override fun onFinishInflate() {
        super.onFinishInflate()
        val list = getChildAt(0) as PostEffectRecyclerView
        statusBlur = list.blurDrawable(20.dp)
        contentBlur = list.blurDrawable(0)
        navBlur = list.blurDrawable(20.dp)

        getChildAt(1).background =
            LayerDrawable(arrayOf(statusBlur, ColorDrawable(0x40FFFFFF)))

        getChildAt(2).background =
            contentBlur

        getChildAt(3).background =
            LayerDrawable(arrayOf(navBlur, ColorDrawable(0x40000000)))
    }
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        getChildAt(0).layout(l, t, r, b)
        getChildAt(1).layout(l, t, r, t + top)
        getChildAt(2).layout(l, t + top, r, b - bottom)
        contentBlur.srcOffsetY = top
        getChildAt(3).layout(l, b - bottom, r, b)
        navBlur.srcOffsetY = b - bottom
    }

}
