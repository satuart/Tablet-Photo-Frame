package com.satuart.tabletphotoframe.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldGestureMathTest {

    @Test
    fun computeProgress_isZeroThroughGracePeriod() {
        assertEquals(0f, HoldGestureMath.computeProgress(0L))
        assertEquals(0f, HoldGestureMath.computeProgress(300L))
    }

    @Test
    fun computeProgress_isOneAtThreshold() {
        assertEquals(1f, HoldGestureMath.computeProgress(1600L))
    }

    @Test
    fun computeProgress_isHalfAtMidpoint() {
        assertEquals(0.5f, HoldGestureMath.computeProgress(950L), 0.001f)
    }

    @Test
    fun computeProgress_clampsAboveThreshold() {
        assertEquals(1f, HoldGestureMath.computeProgress(5000L))
    }

    @Test
    fun computeDimFactor_isFullBrightnessAtZeroProgress() {
        assertEquals(1f, HoldGestureMath.computeDimFactor(0f))
    }

    @Test
    fun computeDimFactor_isMinDimAtFullProgress() {
        assertEquals(HoldGestureMath.MIN_DIM_FACTOR, HoldGestureMath.computeDimFactor(1f))
    }

    @Test
    fun computeDimFactor_isLinearAtMidpoint() {
        assertEquals(0.81f, HoldGestureMath.computeDimFactor(0.5f), 0.001f)
    }

    @Test
    fun exceedsDrift_falseUnderTolerance() {
        assertFalse(HoldGestureMath.exceedsDrift(10f, 10f, 40f))
    }

    @Test
    fun exceedsDrift_falseAtExactTolerance() {
        assertFalse(HoldGestureMath.exceedsDrift(40f, 0f, 40f))
    }

    @Test
    fun exceedsDrift_trueOverTolerance() {
        assertTrue(HoldGestureMath.exceedsDrift(30f, 30f, 40f))
    }
}
