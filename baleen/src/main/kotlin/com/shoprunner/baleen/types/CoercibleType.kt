package com.shoprunner.baleen.types

import com.shoprunner.baleen.BaleenType

abstract class CoercibleType(
    val coercedFromType: BaleenType,
    val coercedToType: BaleenType
) : BaleenType