package com.shoprunner.baleen

object Transforms {
    /**
     * Renames one attribute to a new name. If the new attribute exists already then it is overwritten. If the old
     * attribute does not exist than the new attribute will not exist either.
     */
    fun rename(new: String, old: String) = fun(data: Data): Data = object : Data {
        override fun containsKey(key: String): Boolean {
            if (key == old) return false
            if (key == new) return data.containsKey(old)

            return data.containsKey(key)
        }

        override fun get(key: String): Any? {
            if (key == old) return null
            if (key == new) return data.get(old)

            return data[key]
        }

        override val keys: Set<String> = data.keys.minus(old).plus(new)
    }

    fun coerceToDouble(name: String) = fun(data: Data): Data = object : Data {
        override fun containsKey(key: String): Boolean = data.containsKey(key)

        override fun get(key: String): Any? {
            if (key == name) {
                val anyVal = data[name]
                return if (anyVal is String) {
                    anyVal.toDoubleOrNull()
                } else {
                    null
                }
            }

            return data[key]
        }

        override val keys: Set<String> = data.keys
    }

    fun mapValue(name: String, old: Any, new: Any?) = fun(data: Data): Data = object : Data {
        override fun containsKey(key: String): Boolean = data.containsKey(key)

        override fun get(key: String): Any? {
            if ((key == name) && (data[name] == old)) {
                return new
            }

            return data[key]
        }

        override val keys: Set<String> = data.keys
    }
}