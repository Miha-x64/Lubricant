package net.aquadc.lubricant.demo

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.lubricant.blurDrawable
import net.aquadc.lubricant.view.PostEffectRecyclerView
import kotlin.math.max

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(FLAG_LAYOUT_NO_LIMITS)
        window.decorView.systemUiVisibility =
            SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val list = findViewById<PostEffectRecyclerView>(R.id.list)
        list.solidColor = (window.decorView.background as ColorDrawable).color
        list.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            private val icons = intArrayOf( // https://www.iconfinder.com/iconsets/hawcons
                R.drawable.ic_document_cloud,
                R.drawable.ic_document_sql,
                R.drawable.ic_error_cloud,
            )
            override fun getItemCount(): Int = icons.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(object : ImageView(parent.context) {
                    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                        val size = MeasureSpec.getSize(widthMeasureSpec)
                        setMeasuredDimension(size, size)
                    }
                }) {}
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as ImageView).setImageResource(icons[position])
            }
        }

        val status = findViewById<View>(R.id.status)
        status.background = LayerDrawable(arrayOf(list.blurDrawable(20.dp), ColorDrawable(0x40FFFFFF)))

        val nav = findViewById<View>(R.id.nav)
        val navBlur = list.blurDrawable(20.dp)
        nav.background = LayerDrawable(arrayOf(navBlur, ColorDrawable(0x40000000)))
        nav.viewTreeObserver.addOnGlobalLayoutListener {
            navBlur.srcOffsetY = nav.top
        }

        var top = 0
        var bottom = 0
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            top = max(top, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            bottom = max(bottom, insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            status.layoutParams = status.layoutParams.also { it.height = top }
            nav.layoutParams = nav.layoutParams.also { it.height = bottom }
            list.setPadding(0, top, 0, bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}