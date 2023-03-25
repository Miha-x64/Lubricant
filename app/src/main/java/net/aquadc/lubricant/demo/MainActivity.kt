package net.aquadc.lubricant.demo

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import net.aquadc.lubricant.blur.ViewBlurDrawable
import net.aquadc.lubricant.view.PostEffectRecyclerView

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
            // https://www.iconfinder.com/iconsets/hawcons
            private val icons = intArrayOf(
                R.drawable.ic_document_cloud,
                R.drawable.ic_document_sql,
                R.drawable.ic_error_cloud,
            ).map { VectorDrawableCompat.create(resources, it, null) }
            override fun getItemCount(): Int = icons.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
                DumbViewHolder(SquareImageView(parent.context)).also { vh ->
                    vh.itemView.setOnClickListener {
                        val pos = vh.bindingAdapterPosition
                        if (pos >= 0) {
                            val contentBlur = findViewById<MainLayout>(R.id.root).contentBlur
                            AlertDialog.Builder(it.context)
                                .setView(SquareImageView(it.context).also {
                                    it.setImageDrawable(icons[pos]!!.constantState!!.newDrawable())
                                })
                                .setOnDismissListener {
                                    ObjectAnimator.ofInt(contentBlur, ViewBlurDrawable.RADIUS, 20.dp, 0).setDuration(200).start()
                                }
                                .show()
                            ObjectAnimator.ofInt(contentBlur, ViewBlurDrawable.RADIUS, 0, 20.dp).setDuration(200).start()
                        }
                    }
                }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as ImageView).setImageDrawable(icons[position])
            }
        }
    }

    class DumbViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class SquareImageView(context: Context) : ImageView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val size = MeasureSpec.getSize(widthMeasureSpec)
            setMeasuredDimension(size, size)
        }
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}
