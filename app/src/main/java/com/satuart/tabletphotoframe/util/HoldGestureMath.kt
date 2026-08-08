package com.satuart.tabletphotoframe.util

object HoldGestureMath {

    const val GRACE_MS = 300L
    const val HOLD_THRESHOLD_MS = 1600L
    const val CANCEL_FADE_MS = 200L
    const val MENU_TRANSITION_MS = 250L
    const val AUTO_DISMISS_MS = 10_000L
    const val MIN_DIM_FACTOR = 0.62f
    const val MENU_DIM_FACTOR = 0.40f

    fun computeProgress(elapsedMs: Long): Float {
        if (elapsedMs <= GRACE_MS) return 0f
        val progress = (elapsedMs - GRACE_MS).toFloat() / (HOLD_THRESHOLD_MS - GRACE_MS).toFloat()
        return progress.coerceIn(0f, 1f)
    }

    fun computeDimFactor(progress: Float): Float =
        1f - progress.coerceIn(0f, 1f) * (1f - MIN_DIM_FACTOR)

    fun exceedsDrift(dx: Float, dy: Float, tolerancePx: Float): Boolean =
        (dx * dx + dy * dy) > (tolerancePx * tolerancePx)
}
