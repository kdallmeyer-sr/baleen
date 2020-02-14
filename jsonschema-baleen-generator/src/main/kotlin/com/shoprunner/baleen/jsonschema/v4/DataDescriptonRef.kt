package com.shoprunner.baleen.jsonschema.v4

import com.shoprunner.baleen.Baleen
import com.shoprunner.baleen.DataDescription

fun DataDescriptonRef(name: String, nameSpace: String = ""): DataDescription = Baleen.describe(name, nameSpace)
