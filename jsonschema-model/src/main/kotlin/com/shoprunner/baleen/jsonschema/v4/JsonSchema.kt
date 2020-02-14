package com.shoprunner.baleen.jsonschema.v4

abstract class JsonSchema(var description: String? = null, var default: Any? = null)

fun <T : JsonSchema> T.withDescription(description: String?): T = this.apply { this.description = description }

fun <T : JsonSchema> T.withDefault(default: Any?): T = this.apply { this.default = default }
