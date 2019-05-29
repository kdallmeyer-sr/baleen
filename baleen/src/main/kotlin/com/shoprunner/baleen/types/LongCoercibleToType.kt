package com.shoprunner.baleen.types

import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.DataTrace
import com.shoprunner.baleen.ValidationError
import com.shoprunner.baleen.ValidationResult

open class LongCoercibleToType<out T : BaleenType>(type: T, private val converter: (Long) -> Any?) : CoercibleType(LongType(), type) {
    override fun name() = "long coercible to ${coercedToType.name()}"

    override fun validate(dataTrace: DataTrace, value: Any?): Sequence<ValidationResult> =
            when (value) {
                null -> coercedToType.validate(dataTrace, value)
                !is Long -> sequenceOf(ValidationError(dataTrace, "is not a long", value))
                else -> {
                    val newType = converter(value)
                    if (newType == null) {
                        sequenceOf(ValidationError(dataTrace, "could not be parsed to ${coercedToType.name()}.", value))
                    } else {
                        coercedToType.validate(dataTrace, newType)
                    }
                }
            }
}