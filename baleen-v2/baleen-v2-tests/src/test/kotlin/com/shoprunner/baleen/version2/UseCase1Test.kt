package com.shoprunner.baleen.version2

import com.shoprunner.baleen.Baleen.describeAs
import com.shoprunner.baleen.ValidationAssert.Companion.assertThat
import com.shoprunner.baleen.kotlin.validate
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.IntType
import com.shoprunner.baleen.types.OccurrencesType
import com.shoprunner.baleen.types.StringType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.avro.io.EncoderFactory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Given we have the data, we define the schema manually and validate the data
 */
class UseCase1Test {

    @Nested
    inner class `Start with the data and and manually define the schema` {

        @Nested
        inner class `then with existing map data` {

            val data = listOf(
                mapOf("name" to "Fido", "numLegs" to 4),
                mapOf("name" to "Dug", "numLegs" to 4)
            )

            val schema = "Dog".describeAs {
                attr("name", StringType())
                attr("numLegs", AllowsNull(IntType()))
            }

            @Test
            fun `the data can be validated`() {
                assertThat(data.validate(schema)).isValid()
            }
        }

        @Nested
        inner class `then with existing csv data` {

            val data = """
                "Fido",4
                "Dug",4
            """.trimIndent()

            val schema = "Dog".describeAs {
                attr("name", StringType())
                attr("numLegs", AllowsNull(IntType()))
            }

            @Test
            fun `the data can be validated`() {
                assertThat(data.byteInputStream().validate(schema, CsvDataHandler())).isValid()
            }
        }

        @Nested
        inner class `then with existing xml data` {

            val data = """
                <pack>
                    <dog>
                        <name>Fido</name>
                        <numLegs>4</numLegs>
                    </dog>
                    <dog>
                        <name>Dug</name>
                        <numLegs>4</numLegs>
                    </dog>
                </pack>
            """.trimIndent()

            val schema = "dog".describeAs {
                attr("name", StringType())
                attr("numLegs", AllowsNull(IntType()))
            }

            val root = "pack".describeAs {
                attr("dog", OccurrencesType(schema))
            }

            @Test
            fun `the data can be validated`() {
                assertThat(data.byteInputStream().validate(root, XmlDataHandler)).isValid()
            }
        }

        @Nested
        inner class `then with existing json data` {

            @Nested
            inner class `as a single json object` {
                val data = """
                {
                    "name": "Fido",
                    "numLegs": 4
                }
            """.trimIndent()

                val schema = "Dog".describeAs {
                    attr("name", StringType())
                    attr("numLegs", AllowsNull(IntType()))
                }

                @Test
                fun `the data can be validated`() {
                    assertThat(data.byteInputStream().validate(schema, JsonDataHandler)).isValid()
                }
            }

            @Nested
            inner class `as one json object per line` {
                val data = """
                    { "name": "Fido", "numLegs": 4 }
                    { "name": "Dug", "numLegs": 4 }
                """.trimIndent()

                val schema = "Dog".describeAs {
                    attr("name", StringType())
                    attr("numLegs", AllowsNull(IntType()))
                }

                @Test
                fun `the data can be validated`() {
                    assertThat(data.byteInputStream().validate(schema, JsonPerLineDataHandler)).isValid()
                }
            }

            @Nested
            inner class `as a json array` {
                val data = """
                    [
                        { "name": "Fido", "numLegs": 4 },
                        { "name": "Dug", "numLegs": 4 }
                    ]
                """.trimIndent()

                val schema = "Dog".describeAs {
                    attr("name", StringType())
                    attr("numLegs", AllowsNull(IntType()))
                }

                @Test
                fun `the data can be validated`() {
                    assertThat(data.byteInputStream().validate(schema, JsonArrayDataHandler)).isValid()
                }
            }
        }

        @Nested
        inner class `then with existing avro data` {
            val parser = Schema.Parser()
            val avroSchemaStr = """
            {
               "type": "record",
               "name": "Dog",
               "fields": [
                    { "name": "name", "type": "string" },
                    { "name": "numLegs", "type": ["null", "int"], "default": null }
               ]
            }
            """.trimIndent()
            val avroSchema = parser.parse(avroSchemaStr)

            @Nested
            inner class `from a GenericRecord` {

                val data = GenericRecordBuilder(avroSchema)
                    .set("name", "Fido")
                    .set("numLegs", 4)
                    .build()

                val schema = "Dog".describeAs {
                    attr("name", StringType())
                    attr("numLegs", AllowsNull(IntType()))
                }

                @Test
                fun `the data can be validated`() {
                    assertThat(data.validate(schema)).isValid()
                }
            }

            @Nested
            inner class `from an Avro InputStream` {
                val data = with(ByteArrayOutputStream()) {
                    val record = GenericRecordBuilder(avroSchema)
                        .set("name", "Fido")
                        .set("numLegs", 4)
                        .build()

                    val writer = GenericDatumWriter<GenericRecord>(avroSchema)
                    val encoder = EncoderFactory().directBinaryEncoder(this, null)
                    writer.write(record, encoder)
                    encoder.flush()
                    ByteArrayInputStream(this.toByteArray())
                }

                val schema = "Dog".describeAs {
                    attr("name", StringType())
                    attr("numLegs", AllowsNull(IntType()))
                }

                @Test
                fun `the data can be validated`() {
                    assertThat(data.validate(schema, AvroDataHandler)).isValid()
                }
            }
        }

        @Nested
        inner class `then with existing kotlin data object` {

            val data = Dog("Fido", 4)

            val dataList = listOf(
                data,
                Dog("Dug", 4)
            )

            val schema = "Dog".describeAs {
                attr("name", StringType())
                attr("numLegs", AllowsNull(IntType()))
            }

            @Test
            fun `the data can be validated`() {
                assertThat(data.validate()).isValid()
            }

            @Test
            fun `the data list can be validated`() {
                assertThat(dataList.validate()).isValid()
            }
        }
    }
}
