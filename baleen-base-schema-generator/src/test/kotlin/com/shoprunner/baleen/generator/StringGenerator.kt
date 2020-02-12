package com.shoprunner.baleen.generator

import com.shoprunner.baleen.BaleenType
import com.shoprunner.baleen.DataDescription
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.CoercibleType
import com.shoprunner.baleen.types.MapType
import com.shoprunner.baleen.types.OccurrencesType
import com.shoprunner.baleen.types.UnionType

internal object StringGenerator : BaseGenerator<BaleenType, String, StringOptions> {
    override fun defaultTypeMapper(
        typeMapper: TypeMapper<BaleenType, String, StringOptions>,
        source: BaleenType,
        options: StringOptions
    ): String =
        when (source) {
            is AllowsNull<*> -> "AllowsNull(${typeMapper(source.type, options)})"
            is CoercibleType<*, *> -> {
                val subType = source.toSubType(options.coercibleHandlerOption)
                val mapped = typeMapper(subType, options)
                when (options.coercibleHandlerOption) {
                    CoercibleHandlerOption.FROM -> "CoercibleFrom($mapped)"
                    CoercibleHandlerOption.TO -> "CoercibleTo($mapped)"
                }
            }
            is OccurrencesType -> "Occurrences(${typeMapper(source.memberType, options)})"
            is MapType -> {
                val key = typeMapper(source.valueType, options)
                val value = typeMapper(source.valueType, options)
                "Map($key, $value)"
            }
            is UnionType -> {
                source.types
                    .map { typeMapper(it, options) }
                    .joinToString(", ", "Union(", ")")
            }
            is DataDescription -> {
                val name =
                    if (source.nameSpace.isNotBlank()) "${source.nameSpace}.${source.name()}"
                    else source.name
                val attrs = source.attrs
                    .map { "${it.name}=${typeMapper(it.type, options)}" }
                    .joinToString(", ")
                "$name($attrs)"
            }
            else -> source.name()
        }
}
