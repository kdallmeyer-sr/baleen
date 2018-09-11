# Baleen Json Schema Generator

Given a Json Schema, generate a Baleen description.

```json
{
  "id" : "com.shoprunner.data.dogs.Dog",
  "definitions" : {
    "record:com.shoprunner.data.dogs.Dog" : {
      "description" : "It's a dog. Ruff Ruff!",
      "type" : "object",
      "required" : [ "name" ],
      "additionalProperties" : false,
      "properties" : {
        "name" : {
          "description" : "The name of the dog",
          "type" : "string",
          "maxLength" : 2147483647,
          "minLength" : 0
        },
        "legs" : {
          "description" : "The number of legs",
          "default" : null,
          "oneOf" : [ {
            "type" : "null"
          }, {
            "type" : "integer",
            "maximum" : 2147483647,
            "minimum" : -2147483648
          } ]
        }
      }
    }
  },
  "$ref" : "#/definitions/record:com.shoprunner.data.dogs.Dog",
  "$schema" : "http://json-schema.org/draft-04/schema"
}
```

```kotlin
// Generate from a JSON string
BaleenGenerator.encode(jsonSchemaStr).writeTo(File("outDir"))

// Generate from a JSON file
BaleenGenerator.encode(File("Dog.schema.json")).writeTo(File("outDir"))

// Generate from a URL
BaleenGenerator.encode(URL("http://example.com/jsonschema/Dog.schema.json")).writeTo(File("outDir"))
```

Will output to file `outDir/com/shoprunner/data/dogs/Dog.kt` with the following description

```kotlin
package com.shoprunner.data.dogs

import com.shoprunner.baleen.Baleen.describe
import com.shoprunner.baleen.DataDescription
import com.shoprunner.baleen.types.AllowsNull
import com.shoprunner.baleen.types.LongType
import com.shoprunner.baleen.types.StringType

val Dog: DataDescription = Baleen.describe("Dog", "com.shoprunner.data.dogs", "It's a dog. Ruff Ruff!") {
    it.attr(
        name = "name",
        type = StringType(),
        markdownDescription = "The name of the dog",
        required = true
    )
    it.attr(
        name = "legs",
        type = AllowsNull(IntType()),
        markdownDescription = "The number of legs",
        required = false,
        default = null
    )
}
```

## Advanced

## Pattern Properties
Json schema supports regular expression matching against property names to enforce schemas. Baleen can
enforce these as additional tests by invoking the `test` method.

```json
{
  "id" : "com.shoprunner.data.dogs.Dog",
  "definitions" : {
    "record:com.shoprunner.data.dogs.Dog" : {
      "description" : "It's a dog. Ruff Ruff!",
      "type" : "object",
      "required" : [ "name" ],
      "additionalProperties" : false,
      "properties" : {
        "name" : {
          "description" : "The name of the dog",
          "type" : "string"
        }
      },
      "patternProperties" : {
        "^num_" : {
          "type" : "integer"
        }
      }
    }
  },
  "${'$'}ref" : "#/definitions/record:com.shoprunner.data.dogs.Dog",
  "${'$'}schema" : "http://json-schema.org/draft-04/schema"
}
```

This will create the follow Baleen description:

```kotlin
package com.shoprunner.data.dogs

import com.shoprunner.baleen.Baleen.describe
import com.shoprunner.baleen.DataDescription
import com.shoprunner.baleen.types.LongType
import com.shoprunner.baleen.types.StringType

/**
 * It's a dog. Ruff Ruff! */
val Dog: DataDescription = describe("Dog", "com.shoprunner.data.dogs", "It's a dog. Ruff Ruff!") {
    it.attr(
            name = "name",
            type = StringType(),
            markdownDescription = "The name of the dog",
            required = true
    )

    // Test for pattern property "^num_"
    it.test { dataTrace, data ->
        val patternType = LongType()
        sequenceOf(data.keys).flatten()
                .filter{ k -> k.matches(Regex("^num_"))}
                .flatMap{ k -> patternType.validate(dataTrace, data[k]) }
    }
}
```

## Notes

Not Supported (PR's welcome!):
* `allOf`
* `not`
* All string `format`s except for `date-time`
* When `additionalProperties` are not boolean, the `object` becomes a map even though `additionalProperties` can also be on `object`.
* `dependencies`