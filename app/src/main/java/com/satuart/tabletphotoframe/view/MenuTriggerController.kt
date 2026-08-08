package com.satuart.tabletphotoframe.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.satuart.tabletphotoframe.R
import com.satuart.tabletphotoframe.databinding.OverlayMenuBinding
import com.satuart.tabletphotoframe.util.HoldGestureMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Owns the press-and-hold gesture on the fullscreen slideshow, its ring/dim feedback,
 * and the resulting hidden settings menu overlay (show/dismiss/auto-dismiss/back-press).
 */
class MenuTriggerController(
    private val activity: AppCompatActivity,
    private val rootView: View,
    private val imageView: ImageView,
    private val holdRingView: HoldRingView,
    private val menuOverlayBinding: OverlayMenuBinding,
    private val isPausedProvider: () -> Boolean,
    private val onOpenSettings: () -> Unit,
    private val onTogglePause: () -> Unit,
    private val onRefresh: () -> Unit,
) {

    private enum class Phase { IDLE, PRESSING, MENU }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val driftTolerancePx = activity.resources.getDimension(R.dimen.hold_drift_tolerance)

    private val menuOverlayRoot: View = menuOverlayBinding.root
    private val textPauseResume = menuOverlayBinding.textPauseResume
    private val rowSettings = menuOverlayBinding.rowSettings
    private val rowPauseResume = menuOverlayBinding.rowPauseResume
    private val rowRefresh = menuOverlayBinding.rowRefresh

    private var phase = Phase.IDLE
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var startX = 0f
    private var startY = 0f
    private var currentDimFactor = 1f

    private var holdAnimator: ValueAnimator? = null
    private var fadeAnimator: ValueAnimator? = null
    private var autoDismissJob: Job? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = dismissMenu()
    }

    init {
        rootView.setOnTouchListener { _, event -> onRootTouch(event) }
        menuOverlayRoot.setOnClickListener { dismissMenu() }
        rowSettings.setOnClickListener {
            dismissMenu()
            onOpenSettings()
        }
        rowPauseResume.setOnClickListener {
            onTogglePause()
            dismissMenu()
        }
        rowRefresh.setOnClickListener {
            dismissMenu()
            onRefresh()
        }
        activity.onBackPressedDispatcher.addCallback(activity, backCallback)
    }

    private fun onRootTouch(event: MotionEvent): Boolean {
        if (phase == Phase.MENU) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (phase != Phase.IDLE) return true
                activePointerId = event.getPointerId(0)
                startX = event.x
                startY = event.y
                startHold(startX, startY)
            }

            MotionEvent.ACTION_POINTER_DOWN -> cancelHold()

            MotionEvent.ACTION_MOVE -> {
                if (phase != Phase.PRESSING) return true
                val index = event.findPointerIndex(activePointerId)
                if (index < 0) return true
                val x = event.getX(index)
                val y = event.getY(index)
                holdRingView.updateContact(x, y)
                if (HoldGestureMath.exceedsDrift(x - startX, y - startY, driftTolerancePx)) {
                    cancelHold()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelHold()
        }
        return true
    }

    private fun startHold(x: Float, y: Float) {
        phase = Phase.PRESSING
        holdRingView.updateContact(x, y)
        holdAnimator?.cancel()
        holdAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = HoldGestureMath.HOLD_THRESHOLD_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val elapsed = (animator.animatedFraction * HoldGestureMath.HOLD_THRESHOLD_MS).toLong()
                onHoldTick(elapsed)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (phase == Phase.PRESSING) commitOpen()
                }
            })
            start()
        }
    }

    private fun onHoldTick(elapsedMs: Long) {
        if (phase != Phase.PRESSING) return
        val progress = HoldGestureMath.computeProgress(elapsedMs)
        applyDim(HoldGestureMath.computeDimFactor(progress))
        holdRingView.updateProgress(progress, if (progress > 0f) 1f else 0f)
    }

    private fun cancelHold() {
        if (phase != Phase.PRESSING) return
        phase = Phase.IDLE
        activePointerId = MotionEvent.INVALID_POINTER_ID
        holdAnimator?.cancel()
        fadeToIdle()
    }

    private fun fadeToIdle() {
        fadeAnimator?.cancel()
        val startDim = currentDimFactor
        val startRingAlpha = if (currentDimFactor < 1f) 1f else 0f
        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = HoldGestureMath.CANCEL_FADE_MS
            addUpdateListener { animator ->
                val t = animator.animatedFraction
                applyDim(startDim + (1f - startDim) * t)
                holdRingView.updateProgress(0f, startRingAlpha * (1f - t))
            }
            start()
        }
    }

    private fun commitOpen() {
        phase = Phase.MENU
        activePointerId = MotionEvent.INVALID_POINTER_ID
        holdAnimator?.cancel()
        holdRingView.updateProgress(0f, 0f)
        vibrate()
        updatePauseResumeLabel()
        showMenuOverlay()
    }

    private fun showMenuOverlay() {
        menuOverlayRoot.visibility = View.VISIBLE
        menuOverlayRoot.alpha = 0f
        menuOverlayRoot.animate().alpha(1f).setDuration(HoldGestureMath.MENU_TRANSITION_MS).start()
        animateDimTo(HoldGestureMath.MENU_DIM_FACTOR, HoldGestureMath.MENU_TRANSITION_MS)
        backCallback.isEnabled = true
        scheduleAutoDismiss()
    }

    private fun dismissMenu() {
        if (phase != Phase.MENU) return
        phase = Phase.IDLE
        autoDismissJob?.cancel()
        backCallback.isEnabled = false
        menuOverlayRoot.animate()
            .alpha(0f)
            .setDuration(HoldGestureMath.MENU_TRANSITION_MS)
            .withEndAction { menuOverlayRoot.visibility = View.GONE }
            .start()
        animateDimTo(1f, HoldGestureMath.MENU_TRANSITION_MS)
    }

    private fun animateDimTo(target: Float, durationMs: Long) {
        fadeAnimator?.cancel()
        val start = currentDimFactor
        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val t = animator.animatedFraction
                applyDim(start + (target - start) * t)
            }
            start()
        }
    }

    private fun applyDim(factor: Float) {
        currentDimFactor = factor
        val matrix = ColorMatrix(
            floatArrayOf(
                factor, 0f, 0f, 0f, 0f,
                0f, factor, 0f, 0f, 0f,
                0f, 0f, factor, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        imageView.colorFilter = ColorMatrixColorFilter(matrix)
    }

    private fun updatePauseResumeLabel() {
        textPauseResume.setText(
            if (isPausedProvider()) R.string.menu_resume_slideshow else R.string.menu_pause_slideshow
        )
    }

    private fun scheduleAutoDismiss() {
        autoDismissJob?.cancel()
        autoDismissJob = scope.launch {
            delay(HoldGestureMath.AUTO_DISMISS_MS)
            dismissMenu()
        }
    }

    private fun vibrate() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    fun destroy() {
        holdAnimator?.cancel()
        fadeAnimator?.cancel()
        scope.cancel()
    }
}
