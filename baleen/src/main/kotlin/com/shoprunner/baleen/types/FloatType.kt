package com.shoprunner.baleen.types

import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.DataTrace
import com.shoprunner.baleen.ValidationError
import com.shoprunner.baleen.ValidationInfo
import com.shoprunner.baleen.ValidationResult
import java.math.BigDecimal

class FloatType(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY) : NumericType(min, max) {
    override fun name() = "float"

    override fun isInRange(value: Number): Boolean {
        val decimal = BigDecimal(value.toString())
        val isInfinite = value == Float.NEGATIVE_INFINITY || value == Float.POSITIVE_INFINITY
        val isInRange = decimal >= -Float.MAX_VALUE.toBigDecimal() && decimal <= Float.MAX_VALUE.toBigDecimal()
        return isInfinite || isInRange
    }
}