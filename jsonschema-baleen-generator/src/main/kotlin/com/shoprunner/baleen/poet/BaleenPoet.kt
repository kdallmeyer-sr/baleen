package com.shoprunner.baleen.poet

import com.shoprunner.baleen.AttributeDescription
import com.shoprunner.baleen.Baleen
import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.DataDescription
import com.shoprunner.baleen.NoDefault
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.CoercibleType
import com.shoprunner.baleen.types.DoubleType
import com.shoprunner.baleen.types.EnumType
import com.shoprunner.baleen.types.ErrorsAreWarnings
import com.shoprunner.baleen.types.FloatType
import com.shoprunner.baleen.types.InstantType
import com.shoprunner.baleen.types.IntType
import com.shoprunner.baleen.types.IntegerType
import com.shoprunner.baleen.types.LongCoercibleToInstant
import com.shoprunner.baleen.types.LongType
import com.shoprunner.baleen.types.MapType
import com.shoprunner.baleen.types.NumericType
import com.shoprunner.baleen.types.OccurrencesType
import com.shoprunner.baleen.types.StringCoercibleToInstant
import com.shoprunner.baleen.types.StringCoercibleToTimestamp
import com.shoprunner.baleen.types.StringConstantType
import com.shoprunner.baleen.types.StringType
import com.shoprunner.baleen.types.UnionType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun BaleenType.toFileSpec(typeMapper: BaleenPoetTypeMapper = ::defaultTypeMapper): FileSpec =
    if (this is DataDescription) {
        toFileSpec(typeMapper)
    } else {
        FileSpec.builder("", this.name())
            .addProperty(toPropertySpec(typeMapper))
            .build()
    }

fun DataDescription.toFileSpec(typeMapper: BaleenPoetTypeMapper = ::defaultTypeMapper): FileSpec =
    FileSpec.builder(this.nameSpace, this.name)
        .addProperty(this.toPropertySpec(typeMapper))
        .build()

fun BaleenType.toPropertySpec(typeMapper: BaleenPoetTypeMapper = ::defaultTypeMapper): PropertySpec =
    if (this is DataDescription) {
        this.toPropertySpec(typeMapper)
    } else {
        PropertySpec.builder(this.name(), this::class)
            .addModifiers(KModifier.PUBLIC)
            .initializer(typeMapper(CodeBlock.builder(), this).build())
            .build()
    }

fun DataDescription.toPropertySpec(typeMapper: BaleenPoetTypeMapper = ::defaultTypeMapper): PropertySpec =
    PropertySpec.builder(name, DataDescription::class)
        .addModifiers(KModifier.PUBLIC)
        .addDataDescription(this, typeMapper)
        .build()

fun PropertySpec.Builder.addDataDescription(dataDescription: DataDescription, typeMapper: BaleenPoetTypeMapper = ::defaultTypeMapper): PropertySpec.Builder =
        this.addKdoc(dataDescription.markdownDescription)
        .initializer(CodeBlock.builder()
            .beginControlFlow("%M(%S, %S, %S)",
                MemberName(Baleen::class.asClassName(), "describe"),
                dataDescription.name,
                dataDescription.nameSpace,
                dataDescription.markdownDescription)
            .addAttributeDescriptions(dataDescription.attrs, typeMapper)
            .add(CodeBlock.of("\n"))
            .endControlFlow()
            .build())

fun CodeBlock.Builder.addAttributeDescriptions(attrs: List<AttributeDescription>, typeMapper: BaleenPoetTypeMapper = ::defaultTypeMapper): CodeBlock.Builder = apply {
    attrs.forEach { this.addAttributeDescription(it, typeMapper) }
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
    add("\n)\n")
}

fun recursiveTypeMapper(typeMapper: BaleenPoetTypeMapper): BaleenPoetTypeMapper {
    return { codeBlockBuilder, baleenType -> defaultTypeMapper(codeBlockBuilder, baleenType, typeMapper) }
}

fun defaultTypeMapper(codeBlockBuilder: CodeBlock.Builder, baleenType: BaleenType): CodeBlock.Builder {
    return defaultTypeMapper(codeBlockBuilder, baleenType, recursiveTypeMapper(::defaultTypeMapper))
}

fun defaultTypeMapper(codeBlockBuilder: CodeBlock.Builder, baleenType: BaleenType, typeMapper: BaleenPoetTypeMapper): CodeBlock.Builder = codeBlockBuilder.apply {
    when (baleenType) {
        // Data Description
        // Imports the type rather than generating nested data descriptions
        is DataDescription -> {
            add("%M", MemberName(baleenType.nameSpace, baleenType.name))
        }

        // Complex Types
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
                if (i > 0) add(", ")
                typeMapper(this, t)
            }
            add(")")
        }
        is ErrorsAreWarnings<*> -> {
            add("%T(", ErrorsAreWarnings::class)
            typeMapper(this, baleenType.type)
            add(")")
        }

        // Numeric Types
        is FloatType -> {
            add("%T(min = %L, max = %L)", FloatType::class,
                when {
                    baleenType.min == Float.NEGATIVE_INFINITY -> "Float.NEGATIVE_INFINITY"
                    baleenType.min == Float.POSITIVE_INFINITY -> "Float.POSITIVE_INFINITY"
                    baleenType.min == Float.MIN_VALUE -> "Float.MIN_VALUE"
                    baleenType.min == Float.MAX_VALUE -> "Float.MAX_VALUE"
                    else -> baleenType.min
                },
                when {
                    baleenType.max == Float.NEGATIVE_INFINITY -> "Float.NEGATIVE_INFINITY"
                    baleenType.max == Float.POSITIVE_INFINITY -> "Float.POSITIVE_INFINITY"
                    baleenType.max == Float.MIN_VALUE -> "Float.MIN_VALUE"
                    baleenType.max == Float.MAX_VALUE -> "Float.MAX_VALUE"
                    else -> baleenType.max
                }
            )
        }
        is DoubleType -> {
            add("%T(min = %L, max = %L)", DoubleType::class,
                when {
                    baleenType.min == Double.NEGATIVE_INFINITY -> "Double.NEGATIVE_INFINITY"
                    baleenType.min == Double.POSITIVE_INFINITY -> "Double.POSITIVE_INFINITY"
                    baleenType.min == Double.MIN_VALUE -> "Double.MIN_VALUE"
                    baleenType.min == Double.MAX_VALUE -> "Double.MAX_VALUE"
                    else -> baleenType.min
                },
                when {
                    baleenType.max == Double.NEGATIVE_INFINITY -> "Double.NEGATIVE_INFINITY"
                    baleenType.max == Double.POSITIVE_INFINITY -> "Double.POSITIVE_INFINITY"
                    baleenType.max == Double.MIN_VALUE -> "Double.MIN_VALUE"
                    baleenType.max == Double.MAX_VALUE -> "Double.MAX_VALUE"
                    else -> baleenType.max
                }
            )
        }
        is IntegerType -> {
            add("%T(min = %L, max = %L)", IntegerType::class,
                baleenType.min?.let { "\"$it\".toBigInteger()" },
                baleenType.max?.let { "\"$it\".toBigInteger()" }
            )
        }
        is IntType -> {
            add("%T(min = %L, max = %L)", IntType::class,
                baleenType.min.takeIf { it > Int.MIN_VALUE } ?: "Int.MIN_VALUE",
                baleenType.max.takeIf { it < Int.MAX_VALUE } ?: "Int.MAX_VALUE"
            )
        }
        is LongType -> {
            add("%T(min = %L, max = %L)", LongType::class,
                baleenType.min.takeIf { it > Long.MIN_VALUE } ?: "Long.MIN_VALUE",
                baleenType.max.takeIf { it < Long.MAX_VALUE } ?: "Long.MAX_VALUE"
            )
        }
        is NumericType -> {
            add("%T(min = %L, max = %L)", NumericType::class,
                baleenType.min?.let { "\"$it\".toBigDecimal()" },
                baleenType.max?.let { "\"$it\".toBigDecimal()" }
            )
        }

        // Time types
        is InstantType -> {
            add("%T(before = ", InstantType::class)
            when {
                baleenType.before == Instant.MIN -> add("%T.MIN", Instant::class)
                baleenType.before == Instant.MAX -> add("%T.MAX", Instant::class)
                baleenType.before == Instant.EPOCH -> add("%T.EPOCH", Instant::class)
                else -> add("%T.parse(%S)", Instant::class, baleenType.before.toString())
            }
            add(", after = ")
            when {
                baleenType.after == Instant.MIN -> add("%T.MIN", Instant::class)
                baleenType.after == Instant.MAX -> add("%T.MAX", Instant::class)
                baleenType.after == Instant.EPOCH -> add("%T.EPOCH", Instant::class)
                else -> add("%T.parse(%S)", Instant::class, baleenType.after.toString())
            }
            add(")")
        }

        // String types
        is EnumType -> {
            add("%T(%S, listOf(", EnumType::class, baleenType.enumName)
            baleenType.enum.forEachIndexed { i, enum ->
                if (i > 0) add(", ")
                add("%S", enum)
            }
            add("))")
        }
        is StringConstantType -> add("%T(%S)", StringConstantType::class, baleenType.constant)
        is StringType -> {
            add("%T(min = %L, max = %L)", StringType::class,
                baleenType.min,
                baleenType.max.takeIf { it < Int.MAX_VALUE } ?: "Int.MAX_VALUE"
            )
        }

        // Coercible types
        is LongCoercibleToInstant -> {
            add("%T(", LongCoercibleToInstant::class)
            typeMapper(this, baleenType.type)
            add(", %M)", MemberName(LongCoercibleToInstant.Precision::class.asClassName(), baleenType.precision.name))
        }
        is StringCoercibleToInstant -> {
            add("%T(", StringCoercibleToInstant::class)
            typeMapper(this, baleenType.type)
            if (baleenType.dateTimeFormatter == DateTimeFormatter.ISO_INSTANT) {
                add(", %M)", MemberName(DateTimeFormatter::class.asClassName(), "ISO_INSTANT"))
            } else {
                add(
                    ", %M(%S))",
                    MemberName(DateTimeFormatter::class.asClassName(), "ofPattern"),
                    baleenType.dateTimeFormatter.toString()
                )
            }
        }
        is StringCoercibleToTimestamp -> {
            add("%T(", StringCoercibleToTimestamp::class)
            typeMapper(this, baleenType.type)
            if (baleenType.dateTimeFormatter == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                add(", %M)", MemberName(DateTimeFormatter::class.asClassName(), "ISO_LOCAL_DATE_TIME"))
            } else {
                add(
                    ", %M(%S))",
                    MemberName(DateTimeFormatter::class.asClassName(), "ofPattern"),
                    baleenType.dateTimeFormatter.toString()
                )
            }
        }
        // Most Coercible types default to XCoercibleToY(yType)
        is CoercibleType<*, *> -> {
            add("%T(", baleenType::class)
            typeMapper(this, baleenType.type)
            add(")")
        }

        // Simple implementations like BooleanType and TimestampMillisType default to the same
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
