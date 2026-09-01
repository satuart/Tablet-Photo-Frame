package com.satuart.tabletphotoframe.util

object NightModeMath {

    const val NIGHT_DIM_FACTOR = 0.35f

    fun currentDimFactor(enabled: Boolean, startMinute: Int, endMinute: Int, nowMinute: Int): Float =
        if (enabled && isWithinWindow(startMinute, endMinute, nowMinute)) NIGHT_DIM_FACTOR else 1f

    fun isWithinWindow(startMinute: Int, endMinute: Int, nowMinute: Int): Boolean = when {
        startMinute == endMinute -> false
        startMinute < endMinute -> nowMinute in startMinute until endMinute
        else -> nowMinute >= startMinute || nowMinute < endMinute
    }
}
