package com.shoprunner.baleen.version2

import com.shoprunner.baleen.annotation.DataDescription
import javax.validation.constraints.NotNull

/**
 * The dog data class
 */
@DataDescription("Dog", "com.shoprunner.data.dogs")
data class Dog(
    /** The dog name */
    @NotNull var name: String,
    /**
     * The number of legs
     * the dog has
     */
    var numLegs: Int?
)
