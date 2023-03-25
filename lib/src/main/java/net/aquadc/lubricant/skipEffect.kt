@file:JvmName("SkipEffect")
package net.aquadc.lubricant

import android.animation.ValueAnimator
import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.os.Build
import android.view.accessibility.AccessibilityManager


/**
 * A predicate checking if animations are disabled, thus power save is on.
 * Useful to suspend heavyweight visual effects.
 *
 * See `com.android.internal.graphics.drawable.AnimationScaleListDrawable` for the source.
 */
@JvmField val isPowerSave: () -> Boolean =
    { Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled() }

/**
 * A predicate checking if accessibility is enabled.
 * Useful to suspend heavyweight visual effects.
 */
val Context.isAccessibilityEnabled: () -> Boolean
    get() {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager?
        return { am != null && am.isEnabled }
    }

val Context.defaultSkipEffect: () -> Boolean
    @JvmName("defaultSkipEffect") get() {
        val accessibility = isAccessibilityEnabled
        return { isPowerSave() || accessibility() } // 2..16 μs
    }
