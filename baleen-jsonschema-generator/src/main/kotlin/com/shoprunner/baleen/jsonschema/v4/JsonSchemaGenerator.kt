package com.shoprunner.baleen.jsonschema.v4

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.DataDescription
import com.shoprunner.baleen.NoDefault
import com.shoprunner.baleen.generator.BaseGenerator
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.BooleanType
import com.shoprunner.baleen.types.CoercibleType
import com.shoprunner.baleen.types.DoubleType
import com.shoprunner.baleen.types.EnumType
import com.shoprunner.baleen.types.FloatType
import com.shoprunner.baleen.types.InstantType
import com.shoprunner.baleen.types.IntType
import com.shoprunner.baleen.types.IntegerType
import com.shoprunner.baleen.types.LongType
import com.shoprunner.baleen.types.MapType
import com.shoprunner.baleen.types.NumericType
import com.shoprunner.baleen.types.OccurrencesType
import com.shoprunner.baleen.types.StringConstantType
import com.shoprunner.baleen.types.StringType
import com.shoprunner.baleen.types.TimestampMillisType
import com.shoprunner.baleen.types.UnionType
import java.io.File
import java.nio.file.Path

object JsonSchemaGenerator : BaseGenerator<BaleenType, JsonSchema, JsonSchemaOptions> {
    private fun DataDescription.getId(): String =
        if (this.nameSpace.isNotBlank()) "${this.nameSpace}.${this.name}" else this.name

    private fun ObjectSchema.asObjectReference(): ObjectReference {
        val ref = "record:$id"
        return ObjectReference("#/definitions/$ref")
    }

    private fun JsonSchema.asObjectReferenceOrSelf(): JsonSchema =
        when (this) {
            is ObjectSchema -> asObjectReference()
            else -> this
        }

    private fun DataDescription.encodeObjectSchema(typeMapper: JsonSchemaTypeMapper, options: JsonSchemaOptions): ObjectSchema {
        val dataDescription = this
        val requiredProperties = dataDescription.attrs.filter { it.required }.map { it.name }
        val objectSchema = ObjectSchema(
            id = dataDescription.getId(),
            required = if (requiredProperties.isNotEmpty()) requiredProperties else null,
            additionalProperties = options.withAdditionalAttributes,
            properties = dataDescription.attrs.map {
                val subType = typeMapper(it.type, options).asObjectReferenceOrSelf()
                val subTypeSchema = subType.apply {
                    description = if (it.markdownDescription.isNotBlank()) it.markdownDescription else null
                    default = when (it.default) {
                        NoDefault -> null
                        null -> Null
                        else -> it.default
                    }
                }
                it.name to subTypeSchema
            }.toMap()
        ).apply {
            description =
                if (dataDescription.markdownDescription.isNotBlank()) dataDescription.markdownDescription else null
        }
        return objectSchema
    }

    override fun defaultTypeMapper(
        typeMapper: JsonSchemaTypeMapper,
        source: BaleenType,
        options: JsonSchemaOptions
    ): JsonSchema {
        return when (source) {
            is DataDescription ->
                source.encodeObjectSchema(typeMapper, options)
            is AllowsNull<*> -> {
                val subSchema = typeMapper(source.type, options)
                if (subSchema is OneOf) {
                    OneOf(listOf(NullSchema()) + subSchema.oneOf)
                } else {
                    OneOf(listOf(NullSchema(), subSchema.asObjectReferenceOrSelf()))
                }
            }
            is BooleanType -> BooleanSchema
            is CoercibleType<*, *> -> typeMapper(source.toSubType(options.coercibleHandlerOption), options)
            is DoubleType -> NumberSchema(
                maximum = source.max.takeIf { it.isFinite() }?.toBigDecimal(),
                minimum = source.min.takeIf { it.isFinite() }?.toBigDecimal()
            )
            is IntType -> IntegerSchema(
                maximum = source.max.toBigInteger(),
                minimum = source.min.toBigInteger()
            )
            is IntegerType -> IntegerSchema(
                maximum = source.max,
                minimum = source.min
            )
            is EnumType -> StringSchema(
                enum = source.enum
            )
            is MapType -> {
                if (source.keyType !is StringType) {
                    throw Exception("Map keys can only be String")
                }
                val subSchema = typeMapper(source.valueType, options).asObjectReferenceOrSelf()
                MapSchema(additionalProperties = subSchema)
            }
            is FloatType -> NumberSchema(
                maximum = source.max.takeIf { it.isFinite() }?.toBigDecimal(),
                minimum = source.min.takeIf { it.isFinite() }?.toBigDecimal()
            )
            is InstantType -> DateTimeSchema()
            is LongType -> IntegerSchema(
                maximum = source.max.toBigInteger(),
                minimum = source.min.toBigInteger()
            )
            is NumericType -> NumberSchema(
                maximum = source.max,
                minimum = source.min
            )
            is OccurrencesType -> {
                val subSchema = typeMapper(source.memberType, options).asObjectReferenceOrSelf()
                ArraySchema(items = subSchema)
            }
            is StringType -> StringSchema(
                maxLength = source.max,
                minLength = source.min
            )
            is StringConstantType -> StringSchema(
                enum = listOf(source.constant)
            )
            is TimestampMillisType -> DateTimeSchema()
            is UnionType -> {
                val subSchemas = source.types
                    .map { typeMapper(it, options).asObjectReferenceOrSelf() }
                    .distinct()

                if (subSchemas.size == 1) {
                    subSchemas.first()
                } else {
                    OneOf(subSchemas)
                }
            }
            else -> throw Exception("No mapping is defined for ${source.name()} to JsonSchema")
        }
    }

    fun encode(dataDescription: DataDescription, options: JsonSchemaOptions = JsonSchemaOptions(), typeMapper: JsonSchemaTypeMapper = JsonSchemaGenerator::defaultTypeMapper): RootJsonSchema {
        val id = dataDescription.getId()
        val ref = "#/definitions/record:$id"
        val schema = "http://json-schema.org/draft-04/schema"

        val types = dataDescription.attrs.getDataDescriptions(setOf(dataDescription), options)
        val schemas = types.map { typeMapper(it, options) }
            .filterIsInstance<ObjectSchema>()
            .map { "record:${it.id}" to it }
            .toMap()

        return RootJsonSchema(id, schemas.toSortedMap(), ref, schema)
    }

    fun encodeAsSelfDescribing(dataDescription: DataDescription, version: String, namespace: String = dataDescription.nameSpace, options: JsonSchemaOptions = JsonSchemaOptions(), typeMapper: JsonSchemaTypeMapper = ::defaultTypeMapper): RootJsonSchema {
        val selfDescribingSchema = "http://iglucentral.com/schemas/com.snowplowananalytics.self-desc/schema/jsonschema/1-0-0"

        val rootSchema = encode(dataDescription, options, typeMapper)

        return RootJsonSchema(
                rootSchema.id,
                rootSchema.definitions,
                rootSchema.`$ref`,
                selfDescribingSchema,
                SelfDescribing(
                        namespace,
                        dataDescription.name,
                        version
                ))
    }

    fun RootJsonSchema.writeTo(directory: File, prettyPrint: Boolean = false): File {
        val id = this.id
        val schemaFile = if (id != null) {
            val name = id.replace(".", "/")
            File(directory, "$name.schema.json")
        } else {
            File(directory, "UNNAMED.schema.json")
        }

        schemaFile.parentFile.mkdirs()

        if (prettyPrint) {
            ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(schemaFile, this)
        } else {
            ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writeValue(schemaFile, this)
        }
        return directory
    }

    fun RootJsonSchema.writeTo(directory: Path, prettyPrint: Boolean = false): Path {
        return this.writeTo(directory.toFile(), prettyPrint).toPath()
    }

    fun RootJsonSchema.writeTo(out: Appendable, prettyPrint: Boolean = false): Appendable {
        if (prettyPrint) {
            out.append(ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this))
        } else {
            out.append(ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writeValueAsString(this))
        }
        return out
    }
}
