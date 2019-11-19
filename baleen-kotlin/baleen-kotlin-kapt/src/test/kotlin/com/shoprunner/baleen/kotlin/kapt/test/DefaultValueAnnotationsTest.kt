package com.shoprunner.baleen.kotlin.kapt.test

import com.shoprunner.baleen.kotlin.dataDescription
import com.shoprunner.baleen.kotlin.validate
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.BooleanType
import com.shoprunner.baleen.types.DoubleType
import com.shoprunner.baleen.types.FloatType
import com.shoprunner.baleen.types.InstantType
import com.shoprunner.baleen.types.IntType
import com.shoprunner.baleen.types.IntegerType
import com.shoprunner.baleen.types.LongType
import com.shoprunner.baleen.types.MapType
import com.shoprunner.baleen.types.NumericType
import com.shoprunner.baleen.types.OccurrencesType
import com.shoprunner.baleen.types.StringType
import java.time.Instant
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultValueAnnotationsTest {
    @Test
    fun `test data class with defaults produce valid data descriptions`() {
        val model = ModelWithDefaultValuesWithAnnotations(noDefault = "None")

        DataDescriptionAssert.assertThat(model.dataDescription())
            .hasName("ModelWithDefaultValuesWithAnnotations")
            .hasNamespace("com.shoprunner.baleen.kotlin.kapt.test")
            .hasAttribute("nullDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(AllowsNull(StringType()))
                    .hasDefaultValue(null)
            }
            .hasAttribute("booleanDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(BooleanType())
                    .hasDefaultValue(true)
            }
            .hasAttribute("stringDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(StringType())
                    .hasDefaultValue("default")
            }
            .hasAttribute("intDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(IntType())
                    .hasDefaultValue(100)
            }
            .hasAttribute("longDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(LongType())
                    .hasDefaultValue(100L)
            }
            .hasAttribute("bigIntDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(IntegerType())
                    .hasDefaultValue(100L.toBigInteger())
            }
            .hasAttribute("floatDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(FloatType())
                    .hasDefaultValue(1.1f)
            }
            .hasAttribute("doubleDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(DoubleType())
                    .hasDefaultValue(1.1)
            }
            .hasAttribute("bigDecimalDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(NumericType())
                    .hasDefaultValue(100.01.toBigDecimal())
            }
            .hasAttribute("instantDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(InstantType())
                    .hasDefaultValue(Instant.parse("2019-11-19T10:15:30.00Z"))
            }
            .hasAttribute("classDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(SubModelWithDefaults().dataDescription())
                    .hasDefaultValue(SubModelWithDefaults())
            }
            .hasAttribute("arrayDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(OccurrencesType(IntType()))
                    .hasDefaultValue(emptyArray<Int>())
            }
            .hasAttribute("listDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(OccurrencesType(IntType()))
                    .hasDefaultValue(emptyList<Int>())
            }
            .hasAttribute("setDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(OccurrencesType(IntType()))
                    .hasDefaultValue(emptySet<Int>())
            }
            .hasAttribute("mapDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(MapType(StringType(), IntType()))
                    .hasDefaultValue(emptyMap<String, Int>())
            }
            .hasAttribute("listOfModelDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(OccurrencesType(SubModelWithDefaults().dataDescription()))
                    .hasDefaultValue(emptyList<SubModelWithDefaults>())
            }
            .hasAttribute("noDefault") {
                AttributeDescriptionAssert.assertThat(it)
                    .hasType(AllowsNull(StringType()))
                    .hasNoDefaultValue()
            }

        Assertions.assertThat(model.validate().isValid()).isTrue()
    }
}
