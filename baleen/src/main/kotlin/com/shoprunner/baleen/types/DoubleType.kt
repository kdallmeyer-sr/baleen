package com.shoprunner.baleen.types

import java.math.BigDecimal

class DoubleType(min: Double = Double.NEGATIVE_INFINITY, max: Double = Double.POSITIVE_INFINITY) : NumericType(min, max) {
    override fun name() = "double"

    override fun isInRange(value: Number): Boolean {
        val decimal = BigDecimal(value.toString())
        val isInfinite = value == Double.NEGATIVE_INFINITY || value == Double.POSITIVE_INFINITY
        val isInRange = decimal >= -Double.MIN_VALUE.toBigDecimal() && decimal <= Double.MAX_VALUE.toBigDecimal()
        return isInfinite || isInRange
    }
}