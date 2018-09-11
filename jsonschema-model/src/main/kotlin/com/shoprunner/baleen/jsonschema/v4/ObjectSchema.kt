package com.shoprunner.baleen.jsonschema.v4

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class ObjectSchema(
    val required: List<String>?,
    val additionalProperties: Boolean?,
    @JsonDeserialize(contentUsing = JsonSchemaDeserializer::class)
    val properties: Map<String, JsonSchema>? = null,
    @JsonDeserialize(contentUsing = JsonSchemaDeserializer::class)
    val patternProperties: Map<String, JsonSchema>? = null

) : JsonSchema() {
    val type = JsonType.`object`
}