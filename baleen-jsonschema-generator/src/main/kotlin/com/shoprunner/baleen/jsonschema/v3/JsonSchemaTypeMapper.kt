package com.shoprunner.baleen.jsonschema.v3

import com.fasterxml.jackson.module.jsonSchema.JsonSchema
import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.generator.TypeMapper

typealias JsonSchemaTypeMapper = TypeMapper<BaleenType, JsonSchema, JsonSchemaOptions>
