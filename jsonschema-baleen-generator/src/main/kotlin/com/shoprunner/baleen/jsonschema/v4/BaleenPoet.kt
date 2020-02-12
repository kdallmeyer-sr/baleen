package com.shoprunner.baleen.jsonschema.v4

import com.shoprunner.baleen.AttributeDescription
import com.shoprunner.baleen.Baleen
import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.DataDescription
import com.shoprunner.baleen.NoDefault
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.BooleanType
import com.shoprunner.baleen.types.DoubleType
import com.shoprunner.baleen.types.FloatType
import com.shoprunner.baleen.types.IntegerType
import com.shoprunner.baleen.types.MapType
import com.shoprunner.baleen.types.NumericType
import com.shoprunner.baleen.types.OccurrencesType
import com.shoprunner.baleen.types.UnionType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import java.time.Instant
import java.time.LocalDateTime

fun DataDescription.toFileSpec(): FileSpec =
    FileSpec.builder(this.nameSpace, this.name)
        .addProperty(this.toPropertySpec())
        .build()


fun DataDescription.toPropertySpec(): PropertySpec =
    PropertySpec.builder(name, DataDescription::class)
        .addModifiers(KModifier.PUBLIC)
        .addDataDescription(this)
        .build()

fun PropertySpec.Builder.addDataDescription(dataDescription: DataDescription): PropertySpec.Builder =
        this.addKdoc(dataDescription.markdownDescription)
        .initializer(CodeBlock.builder()
            .beginControlFlow("%M(%S, %S, %S)",
                MemberName(Baleen::class.asClassName(), "describe"),
                dataDescription.name,
                dataDescription.nameSpace,
                dataDescription.markdownDescription)
            .addAttributeDescriptions(dataDescription.attrs)
            .build())

fun CodeBlock.Builder.addAttributeDescriptions(attrs: List<AttributeDescription>): CodeBlock.Builder = apply {
    attrs.forEach { this.addAttributeDescription(it) }
}

typealias BaleenPoetTypeMapper = (CodeBlock.Builder, BaleenType) -> CodeBlock.Builder

fun CodeBlock.Builder.addAttributeDescription(attr: AttributeDescription, typeMapper: BaleenPoetTypeMapper = ::defaultTypeMapper): CodeBlock.Builder = apply {
    // create attribute
    add("it.attr(\n")
    indent()
    // name
    add("name = %S,\n", attr.name)
    // type
    add("type = ")
    typeMapper(this, attr.type)
    // markdownDescription
    if (attr.markdownDescription.isNotBlank()) {
        add(",\nmarkdownDescription = %S", attr.markdownDescription)
    }
    // required
    if (attr.required) {
        add(",\nrequired = %L", attr.required)
    }
    // default
    if (attr.default != NoDefault) {
        add(",\ndefault = ")
        addDefaultValue(attr.type, attr.default)
    }
    unindent()
    add(")")
}

fun recursiveTypeMapper(typeMapper: BaleenPoetTypeMapper): BaleenPoetTypeMapper {
    return { codeBlockBuilder, baleenType -> defaultTypeMapper(codeBlockBuilder, baleenType, typeMapper) }
}

fun defaultTypeMapper(codeBlockBuilder: CodeBlock.Builder, baleenType: BaleenType): CodeBlock.Builder {
    return defaultTypeMapper(codeBlockBuilder, baleenType, recursiveTypeMapper(::defaultTypeMapper))
}

fun defaultTypeMapper(codeBlockBuilder: CodeBlock.Builder, baleenType: BaleenType, typeMapper: BaleenPoetTypeMapper): CodeBlock.Builder = codeBlockBuilder.apply {
    when(baleenType) {
        is AllowsNull<*> -> {
            add("%T(", AllowsNull::class)
            typeMapper(this, baleenType.type)
            add(")")
        }
        is OccurrencesType -> {
            add("%T(", OccurrencesType::class)
            typeMapper(this, baleenType.memberType)
            add(")")
        }
        is MapType -> {
            add("%T(", MapType::class)
            typeMapper(this, baleenType.keyType)
            add(",")
            typeMapper(this, baleenType.valueType)
            add(")")
        }
        is UnionType -> {
            add("%T(", UnionType::class)
            baleenType.types.forEachIndexed { i, t ->
                if(i > 0) add(", ")
                typeMapper(this, t)
            }
        }

        is FloatType -> {
            add("%T(min = %L, max = %L)", FloatType::class,
                baleenType.min.takeIf { it > Float.NEGATIVE_INFINITY } ?: "Float.NEGATIVE_INFINITY",
                baleenType.min.takeIf { it < Float.POSITIVE_INFINITY } ?: "Float.POSITIVE_INFINITY"
            )
        }
        is DoubleType -> {
            add("%T(min = %L, max = %L)", DoubleType::class,
                baleenType.min.takeIf { it > Double.MIN_VALUE } ?: "Double.MIN_VALUE",
                baleenType.min.takeIf { it < Double.MAX_VALUE } ?: "Double.MIN_VALUE"
            )
        }
        is IntegerType -> {
            add("%T(min = \"%L\".toBigInteger(), max = \"%L\")", IntegerType::class,
                baleenType.min?.map { "\"it\".toBigInteger"},
                baleenType.min
            )
        }

        else -> add("%T()", baleenType::class)
    }
}


fun CodeBlock.Builder.addDefaultValue(baleenType: BaleenType, defaultValue: Any?): CodeBlock.Builder = this.apply {
    when (defaultValue) {
        NoDefault -> {
            // Do nothing
        }
        null -> add("null")
        else ->
            when (baleenType) {
                is IntegerType -> add("\"%L\".toBigInteger()", defaultValue)
                is NumericType -> add("\"%L\".toBigDecimal()", defaultValue)
                is OccurrencesType -> if (defaultValue is Iterable<*>) {
                    val defaultValueList = defaultValue.toList()
                    if (defaultValueList.isEmpty()) {
                        add("emptyList()")
                    } else {
                        add("listOf(")
                        defaultValueList.forEachIndexed { i, v ->
                            if (i > 0) add(", ")
                            addDefaultValue(baleenType.memberType, v)
                        }
                        add(")")
                    }
                }
                is MapType -> if (defaultValue is Map<*, *>) {
                    if (defaultValue.isEmpty()) {
                        add("emptyMap()")
                    } else {
                        add("mapOf(")
                        defaultValue.toList().forEachIndexed { i, (key, value) ->
                            if (i > 0) add(", ")
                            addDefaultValue(baleenType.keyType, key)
                            add(" to ")
                            addDefaultValue(baleenType.valueType, value)
                        }
                        add(")")
                    }
                }
                else ->
                    when (defaultValue) {
                        is String -> add("%S", defaultValue)
                        is Boolean -> add("%L", defaultValue)
                        is Int -> add("%L", defaultValue)
                        is Long -> add("%LL", defaultValue)
                        is Float -> add("%Lf", defaultValue)
                        is Double -> add("%L", defaultValue)
                        is Enum<*> -> add("%S", defaultValue.name)
                        is Instant -> add("%T.parse(%S)", Instant::class, defaultValue.toString())
                        is LocalDateTime -> add("%T.parse(%S)", LocalDateTime::class, defaultValue.toString())
                        else -> add("%T()", defaultValue::class.asClassName())
                    }
            }
    }
}