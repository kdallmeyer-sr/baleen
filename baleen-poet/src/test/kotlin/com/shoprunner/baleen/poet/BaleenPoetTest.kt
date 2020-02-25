package com.shoprunner.baleen.poet

import com.shoprunner.baleen.poet.FileSpecAssert.Companion.assertThat
import com.shoprunner.baleen.types.BooleanType
import com.shoprunner.baleen.types.DoubleType
import com.shoprunner.baleen.types.EnumType
import com.shoprunner.baleen.types.FloatType
import com.shoprunner.baleen.types.InstantType
import com.shoprunner.baleen.types.IntType
import com.shoprunner.baleen.types.IntegerType
import com.shoprunner.baleen.types.LongType
import com.shoprunner.baleen.types.NumericType
import com.shoprunner.baleen.types.StringConstantType
import com.shoprunner.baleen.types.StringType
import com.shoprunner.baleen.types.TimestampMillisType
import java.time.Instant
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BaleenPoetTest {

    @Test
    fun `test write BooleanType`() {
        val type = BooleanType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.BooleanType
                
                val boolean: BooleanType = BooleanType()
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write DoubleType`() {
        val type = DoubleType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.DoubleType
                
                val double: DoubleType = DoubleType(min = Double.NEGATIVE_INFINITY, max = Double.POSITIVE_INFINITY)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write DoubleType with max and min`() {
        val type = DoubleType(min = 0.0, max = 10.0)
        val spec = type.toFileSpec(name = "DoubleMaxMin")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.DoubleType
                
                val DoubleMaxMin: DoubleType = DoubleType(min = 0.0, max = 10.0)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write FloatType`() {
        val type = FloatType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.FloatType
                
                val float: FloatType = FloatType(min = Float.NEGATIVE_INFINITY, max = Float.POSITIVE_INFINITY)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write FloatType with max and min`() {
        val type = FloatType(min = 0.0f, max = 10.0f)
        val spec = type.toFileSpec(name = "FloatMaxMin")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.FloatType
                
                val FloatMaxMin: FloatType = FloatType(min = 0.0f, max = 10.0f)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write IntType`() {
        val type = IntType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.IntType
                
                val int: IntType = IntType(min = Int.MIN_VALUE, max = Int.MAX_VALUE)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write IntType with max and min`() {
        val type = IntType(min = 0, max = 10)
        val spec = type.toFileSpec(name = "IntMaxMin")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.IntType
                
                val IntMaxMin: IntType = IntType(min = 0, max = 10)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write LongType`() {
        val type = LongType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.LongType
                
                val long: LongType = LongType(min = Long.MIN_VALUE, max = Long.MAX_VALUE)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write LongType with max and min`() {
        val type = LongType(min = 0, max = 10)
        val spec = type.toFileSpec(name = "LongMaxMin")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.LongType
                
                val LongMaxMin: LongType = LongType(min = 0L, max = 10L)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write IntegerType`() {
        val type = IntegerType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.IntegerType
                
                val integer: IntegerType = IntegerType(min = null, max = null)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write IntegerType with max and min`() {
        val type = IntegerType(min = 0.toBigInteger(), max = 10.toBigInteger())
        val spec = type.toFileSpec(name = "IntegerMaxMin")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.IntegerType
                
                val IntegerMaxMin: IntegerType = IntegerType(min = "0".toBigInteger(), max = "10".toBigInteger())
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write NumericType`() {
        val type = NumericType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.NumericType
                
                val number: NumericType = NumericType(min = null, max = null)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write NumericType with max and min`() {
        val type = NumericType(min = 0.toBigDecimal(), max = 10.toBigDecimal())
        val spec = type.toFileSpec(name = "NumberMaxMin")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.NumericType
                
                val NumberMaxMin: NumericType = NumericType(min = "0".toBigDecimal(), max = "10".toBigDecimal())
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write EnumType`() {
        val type = EnumType("myEnum", "A", "B", "C")
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.EnumType
                
                val enum: EnumType = EnumType("myEnum", listOf("A", "B", "C"))
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write StringConstantType`() {
        val type = StringConstantType("Hello World")
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.StringConstantType
                
                val `string constant`: StringConstantType = StringConstantType("Hello World")
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write StringType`() {
        val type = StringType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.StringType
                
                val string: StringType = StringType(min = 0, max = Int.MAX_VALUE)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write StringType with max and min`() {
        val type = StringType(min = 10, max = 20)
        val spec = type.toFileSpec(name = "StringMaxMin")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.StringType
                
                val StringMaxMin: StringType = StringType(min = 10, max = 20)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write InstantType`() {
        val type = InstantType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.InstantType
                import java.time.Instant
                
                val instant: InstantType = InstantType(before = Instant.MAX, after = Instant.MIN)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write InstantType with before and after`() {
        val type = InstantType(before = Instant.parse("2020-02-02T02:02:02Z"), after = Instant.parse("2001-01-01T01:01:01Z"))
        val spec = type.toFileSpec(name = "InstantBeforeAfter")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.InstantType
                import java.time.Instant
                
                val InstantBeforeAfter: InstantType = InstantType(before = Instant.parse("2020-02-02T02:02:02Z"), 
                                                                  after = Instant.parse("2001-01-01T01:01:01Z"))
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write TimestampMillisType`() {
        val type = TimestampMillisType()
        val spec = type.toFileSpec()

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                import com.shoprunner.baleen.types.TimestampMillisType
                
                val timestampMillis: TimestampMillisType = TimestampMillisType()
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }

    @Test
    fun `test write BaleenType with package and filename set`() {
        val type = StringType()
        val spec = type.toFileSpec("com.shoprunner.baleen.poet.test", "MyString")

        assertSoftly {
            assertThat(spec).isEqualToIgnoringWhitespace("""
                package com.shoprunner.baleen.poet.test
                
                import com.shoprunner.baleen.types.StringType
                
                val MyString: StringType = StringType(min = 0, max = Int.MAX_VALUE)
            """.trimIndent())
            assertThat(spec).canCompile()
        }
    }
}
