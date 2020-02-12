package com.shoprunner.baleen.jsonschema.v4

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.shoprunner.baleen.Baleen
import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.DataDescription
import com.shoprunner.baleen.generator.BaseGenerator
import com.shoprunner.baleen.generator.TypeMapper
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.BooleanType
import com.shoprunner.baleen.types.EnumType
import com.shoprunner.baleen.types.InstantType
import com.shoprunner.baleen.types.IntegerType
import com.shoprunner.baleen.types.MapType
import com.shoprunner.baleen.types.NumericType
import com.shoprunner.baleen.types.OccurrencesType
import com.shoprunner.baleen.types.StringConstantType
import com.shoprunner.baleen.types.StringType
import com.shoprunner.baleen.types.UnionType
import com.squareup.kotlinpoet.FileSpec
import java.io.File
import java.net.URL

/**
 * Given a JsonSchema, generate basic Baleen descriptions.
 */
object BaleenGenerator2 : BaseGenerator<JsonSchema, BaleenType, BaleenOptions> {

    private val mapper = jacksonObjectMapper()

    override fun defaultTypeMapper(
        typeMapper: TypeMapper<JsonSchema, BaleenType, BaleenOptions>,
        source: JsonSchema,
        options: BaleenOptions
    ): BaleenType {
        return when (source) {
            is AnyOf -> unionType(source.anyOf, typeMapper, options)
            is ArraySchema -> OccurrencesType(typeMapper(source, options))
            is BooleanSchema -> BooleanType()
            is IntegerSchema -> IntegerType(source.minimum, source.maximum)
            is MapSchema -> MapType(StringType(), typeMapper(source.additionalProperties, options))
            is NumberSchema -> NumericType(source.minimum, source.maximum)
            is ObjectReference -> source.`$ref`.toDataDescription()
            is OneOf -> unionType(source.oneOf, typeMapper, options)
            is StringSchema -> when {
                source.enum != null -> {
                    if (source.enum!!.size > 1) {
                        val enumName = "Enum${source.enum!!.map { it.capitalize().first() }.joinToString("")}"
                        EnumType(enumName, source.enum!!)
                    } else if (source.enum!!.size == 1) {
                        StringConstantType(source.enum!!.first())
                    } else {
                        throw Exception("Enum should have at least 1 value")
                    }
                }
                source.format == StringFormats.`date-time` -> InstantType()
                else -> StringType(source.minLength ?: 0, source.maxLength ?: Int.MAX_VALUE)
            }
            is ObjectSchema -> mapObjectSchema(source,  typeMapper, options)
            else -> throw IllegalArgumentException("json type ${source::class.simpleName} not supported")
        }
    }

    private fun String.toDataDescription(): DataDescription {
        val ref = replaceFirst("#/definitions/", "")
        val id = ref.replaceFirst("record:", "")
        return if (id.contains(".")) {
            val namespace = id.substringBeforeLast(".")
            val name = id.substringAfterLast(".")
            Baleen.describe(name, namespace)
        } else {
            Baleen.describe(id)
        }
    }

    private fun unionType(
        subSchemas: List<JsonSchema>,
        typeMapper: TypeMapper<JsonSchema, BaleenType, BaleenOptions>,
        options: BaleenOptions
    ): BaleenType {
        val unions = subSchemas.filterNot { it is NullSchema }.map { typeMapper(it, options) }
        val baleenType = if (unions.size == 1) unions.first() else UnionType(*unions.toTypedArray())
        return if (subSchemas.any { it is NullSchema }) {
            AllowsNull(baleenType)
        } else {
            baleenType
        }
    }

    fun mapObjectSchema(objectSchema: ObjectSchema, typeMapper: TypeMapper<JsonSchema, BaleenType, BaleenOptions>, options: BaleenOptions): DataDescription {
        val dataDescription = objectSchema.id!!.toDataDescription()
        objectSchema.properties.forEach { attrName, attrSchema ->
            val isRequired = objectSchema.required?.contains(attrName) == true
            val attrType = typeMapper(attrSchema, options)

            dataDescription.attr(attrName, attrType, required = isRequired)
        }
    }

    fun encode(schema: RootJsonSchema): List<FileSpec> {
        return if (schema.definitions != null) {
            schema.definitions!!.map { (record, objectSchema) ->
                val (namespace, name) = BaleenGenerator.getNamespaceAndName(record)
                encode(namespace, name, objectSchema)
            }
        } else if (schema.type == JsonType.`object`) {
            val (namespace, name) = BaleenGenerator.getNamespaceAndName(schema)
            val objectSchema = ObjectSchema(
                schema.required,
                schema.additionalProperties,
                schema.properties ?: emptyMap())
            listOf(encode(namespace, name, objectSchema))
        } else {
            emptyList()
        }
    }

    fun String.parseJsonSchema(): RootJsonSchema {
        return mapper.readValue(this, RootJsonSchema::class.java)
    }

    fun File.parseJsonSchema(): RootJsonSchema {
        return mapper.readValue(this, RootJsonSchema::class.java)
    }

    fun URL.parseJsonSchema(): RootJsonSchema {
        return mapper.readValue(this, RootJsonSchema::class.java)
    }
}
