package com.shoprunner.baleen.version2

import com.shoprunner.baleen.DataTrace
import com.shoprunner.baleen.ValidationError
import com.shoprunner.baleen.ValidationInfo
import com.shoprunner.baleen.ValidationResult
import com.shoprunner.baleen.annotation.DataTest

@DataTest(isExtension = true)
fun Dog.assertAtLeast3Legs(dataTrace: DataTrace): Iterable<ValidationResult> {
    val numLegs = this.numLegs
    return when {
        numLegs == null -> emptyList()
        numLegs >= 3 -> listOf(ValidationInfo(dataTrace, "The number of legs is at least 3", numLegs))
        else -> listOf(ValidationError(dataTrace, "The number of legs is less than 3", numLegs))
    }
}

@DataTest
fun assertNotNamedBob(dog: Dog, dataTrace: DataTrace): Sequence<ValidationResult> {
    val name = dog.name
    return when {
        name == "Bob" -> sequenceOf(ValidationError(dataTrace, "The name should not be Bob", name))
        else -> sequenceOf(ValidationInfo(dataTrace, "The name is not Bob", name))
    }
}
