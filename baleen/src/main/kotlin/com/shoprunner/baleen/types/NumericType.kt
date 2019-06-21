package com.shoprunner.baleen.types

import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.DataTrace
import com.shoprunner.baleen.ValidationError
import com.shoprunner.baleen.ValidationResult
import java.math.BigDecimal

open class NumericType(
    val min: Number? = null,
    val max: Number? = null
): BaleenType {
    override fun name() = "numeric"

    override fun validate(dataTrace: DataTrace, value: Any?): Sequence<ValidationResult> =
        when {
            value == null -> sequenceOf(ValidationError(dataTrace, "is null", value))
            value !is Number -> sequenceOf(ValidationError(dataTrace, "is not a number", value))
            !isInRange(value) -> sequenceOf(ValidationError(dataTrace, "is not in range for ${name()}", value))
            min != null && BigDecimal(value.toString()) < BigDecimal(min.toString()) -> sequenceOf(ValidationError(dataTrace, "is less than $min", value))
            max != null && BigDecimal(value.toString()) > BigDecimal(max.toString()) -> sequenceOf(ValidationError(dataTrace, "is greater than $max", value))
            else -> emptySequence()
        }

    open fun isInRange(value: Number): Boolean = true
}