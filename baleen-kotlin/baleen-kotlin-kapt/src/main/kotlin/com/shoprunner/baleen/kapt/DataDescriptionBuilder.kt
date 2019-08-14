package com.shoprunner.baleen.kapt

import com.shoprunner.baleen.Baleen
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.BooleanType
import com.shoprunner.baleen.types.DoubleType
import com.shoprunner.baleen.types.FloatType
import com.shoprunner.baleen.types.InstantType
import com.shoprunner.baleen.types.IntType
import com.shoprunner.baleen.types.LongType
import com.shoprunner.baleen.types.MapType
import com.shoprunner.baleen.types.OccurrencesType
import com.shoprunner.baleen.types.StringType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import java.io.File
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal class DataDescriptionBuilder(
    private val kaptKotlinGeneratedDir: String,
    private val elementUtils: Elements,
    private val typeUtils: Types,
    private val messager: Messager,
    private val extraTestBuilder: ExtraTestBuilder = ExtraTestBuilder(elementUtils, typeUtils, messager)
) {
    fun generateDataDescription(
        dataDescriptionElement: DataDescriptionElement,
        extraTests: List<DataTestElement>,
        allSchemas: Map<String, DataDescriptionElement>
    ) {
        val (typeElement, packageName, name) = dataDescriptionElement

        val fileName = "${name.capitalize()}Type"
        val members = (typeElement.enclosedElements ?: emptyList<Element>())
            .filter { it.kind == ElementKind.FIELD }

        FileSpec.builder(packageName, fileName)
            .addAnnotation(
                AnnotationSpec.builder(JvmName::class)
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%S", fileName)
                    .build()
            )
            .addProperty(
                PropertySpec.builder(
                    name = "${name.capitalize()}Type",
                    type = com.shoprunner.baleen.DataDescription::class
                ).addKdoc("%L", "${typeElement.annotationMirrors} $typeElement}")
                .initializer(CodeBlock.builder().apply {
                    add(
                        "%T.%L(%S, %S",
                        Baleen::class,
                        Baleen::describe.name,
                        name,
                        packageName
                    )
                    val comment = elementUtils.getDocComment(typeElement)?.trim()
                    if (comment != null) {
                        add(", markdownDescription = %S", comment)
                    }
                    beginControlFlow(")")
                    members.forEach {
                        add(generateAttributes(it as VariableElement, allSchemas))
                    }
                    extraTests.forEach {
                        add(extraTestBuilder.addExtraTest(it, typeElement))
                    }
                    endControlFlow()
                    }.build())
                .build()
            )
            .build()
            .writeTo(File(kaptKotlinGeneratedDir))
    }

    private fun generateAttributes(param: VariableElement, allSchemas: Map<String, DataDescriptionElement>): CodeBlock {
        val attrName = param.simpleName
        val attrType = param.asType().asTypeName()

        return CodeBlock.builder().apply {
            // create attribute
            add("/* ${param.annotationMirrors} $attrName: $attrType */\n")
            // add("it.%L(\n", com.shoprunner.baleen.DataDescription::attr.name)
            add("it.attr(\n")
            indent()
            // name
            add(
                "name = %S,\n",
                attrName
            )
            // type
            add("type = ")
            if (param.isNotNullField()) {
                add(getAttrType(param.asType(), allSchemas))
            } else {
                add("%T(", AllowsNull::class.java)
                add(getAttrType(param.asType(), allSchemas))
                add(")")
            }
            add(",\n")
            // markdownDescription
            val comment = elementUtils.getDocComment(param)?.trim()
            if (comment != null) {
                add("markdownDescription = %S,\n", comment)
            }
            // aliases
            val aliases = emptyArray<String>()
            if (aliases.isNotEmpty()) {
                add(
                    // "%L = %L,\n",
                    // com.shoprunner.baleen.DataDescription::attr.parameters[4].name,
                    "aliases = %L,",
                    aliases.joinToString(", ", prefix = "arrayOf(\"", postfix = "\")")
                )
            }
            // required
            add(
                // "%L = %L",
                // com.shoprunner.baleen.DataDescription::attr.parameters[5].name,
                "required = %L",
                param.isNotNullField()
            )
            // default
            // add(",\n%L = %S", com.shoprunner.baleen.DataDescription::attr.parameters[6].name, defaultValue)
            unindent()
            add(")")
        }.build()
    }

    private fun getAttrType(
        attrType: TypeMirror,
        allSchemas: Map<String, DataDescriptionElement>
    ): CodeBlock {
        val name = attrType.toString()
        return when {
            name == "java.lang.String" -> CodeBlock.of("%T()", StringType::class)
            name == "java.lang.Boolean" -> CodeBlock.of("%T()", BooleanType::class)

            // Numeric Types
            name == "java.lang.Float" -> CodeBlock.of("%T()", FloatType::class)
            name == "java.lang.Double" -> CodeBlock.of("%T()", DoubleType::class)
            name == "java.lang.Integer" -> CodeBlock.of("%T()", IntType::class)
            name == "java.lang.Long" -> CodeBlock.of("%T()", LongType::class)

            // Time Types
            name == "java.time.Instant" -> CodeBlock.of("%T()", InstantType::class)

            // Occurences Types
            attrType is ArrayType -> {
                CodeBlock.builder()
                    .add("%T(", OccurrencesType::class)
                    .add(getAttrType(attrType.componentType, allSchemas))
                    .add(")")
                    .build()
            }
            // Iterable
            attrType is DeclaredType && isIterable(typeUtils, elementUtils, attrType) -> {
                CodeBlock.builder()
                    .add("%T(", OccurrencesType::class)
                    .add(getAttrType(attrType.typeArguments.first(), allSchemas))
                    .add(")")
                    .build()
            }

            // Map
            attrType is DeclaredType && isMap(typeUtils, elementUtils, attrType) -> {
                CodeBlock.builder()
                    .add("%T(", MapType::class)
                    .add(getAttrType(attrType.typeArguments.first(), allSchemas))
                    .add(", ")
                    .add(getAttrType(attrType.typeArguments[1], allSchemas))
                    .add(")")
                    .build()
            }

            // Other Data Descriptions
            attrType is DeclaredType && allSchemas.containsKey(name) -> {
                val (_, ddPackageName, ddName) = allSchemas[name]!!
                CodeBlock.of("%M", MemberName(ddPackageName, "${ddName}Type"))
            }

            else -> {
                messager.warning { "Cannot generate type for $attrType. Defaulting to StringType" }
                CodeBlock.of("%T()", StringType::class)
            }
        }
    }
}
