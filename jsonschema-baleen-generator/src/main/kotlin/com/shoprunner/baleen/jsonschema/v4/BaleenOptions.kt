package com.shoprunner.baleen.jsonschema.v4

import com.shoprunner.baleen.generator.CoercibleHandlerOption
import com.shoprunner.baleen.generator.Options

data class BaleenOptions(
    override val coercibleHandlerOption: CoercibleHandlerOption = CoercibleHandlerOption.FROM
) : Options
