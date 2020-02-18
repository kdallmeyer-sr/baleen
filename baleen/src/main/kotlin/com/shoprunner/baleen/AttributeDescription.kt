package com.shoprunner.baleen

class AttributeDescription(
    val dataDescription: DataDescription,
    val name: String,
    val type: BaleenType,
    val markdownDescription: String,
    val aliases: Array<String>,
    val required: Boolean,
    val default: Any?
) {
    /**
     * Add a test for the attribute with all other attributes of the Data map.
     *
     * @param validator Function that takes a DataTrace and the Data map and returns a Sequence<ValidationResult>.
     * The Data map has all attributes for the map.
     * @return this AttributeDescription
     */
    fun test(validator: Validator) {
        // TODO change context
        dataDescription.test(validator)
    }

    /**
     * Allow for attribute level testing with the attribute's context.
     *
     * @param validator Function that takes a DataValue and returns a Sequence<ValidationResult>. DataValue has the
     * attribute level context and the value.
     * @return this AttributeDescription
     */
    fun test(validator: (DataValue) -> Sequence<ValidationResult>): AttributeDescription {
        dataDescription.test { dataTrace, data ->
            val dataValue = data.attributeDataValue(name, dataTrace)
            validator(dataValue)
        }
        return this
    }

    fun describe(block: (AttributeDescription) -> Unit) {
        block(this)
    }
}
