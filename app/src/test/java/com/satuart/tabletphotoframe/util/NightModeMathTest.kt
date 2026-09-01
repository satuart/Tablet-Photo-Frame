package com.satuart.tabletphotoframe.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightModeMathTest {

    @Test
    fun currentDimFactor_isFullBrightnessWhenDisabled() {
        assertEquals(1f, NightModeMath.currentDimFactor(enabled = false, startMinute = 1320, endMinute = 420, nowMinute = 0))
    }

    @Test
    fun currentDimFactor_isDimmedInsideWrappedWindow() {
        assertEquals(
            NightModeMath.NIGHT_DIM_FACTOR,
            NightModeMath.currentDimFactor(enabled = true, startMinute = 1320, endMinute = 420, nowMinute = 0),
        )
    }

    @Test
    fun currentDimFactor_isFullBrightnessOutsideWrappedWindow() {
        assertEquals(1f, NightModeMath.currentDimFactor(enabled = true, startMinute = 1320, endMinute = 420, nowMinute = 720))
    }

    @Test
    fun isWithinWindow_handlesSameDayWindow() {
        assertTrue(NightModeMath.isWithinWindow(60, 120, 90))
        assertFalse(NightModeMath.isWithinWindow(60, 120, 30))
    }

    @Test
    fun isWithinWindow_handlesMidnightWraparound() {
        assertTrue(NightModeMath.isWithinWindow(1320, 420, 1350))
        assertTrue(NightModeMath.isWithinWindow(1320, 420, 100))
        assertFalse(NightModeMath.isWithinWindow(1320, 420, 800))
    }

    @Test
    fun isWithinWindow_isFalseWhenStartEqualsEnd() {
        assertFalse(NightModeMath.isWithinWindow(600, 600, 600))
    }
}
