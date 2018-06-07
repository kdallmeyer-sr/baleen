package com.shoprunner.baleen

/** Describes the path through the data. */
typealias DataTrace = List<String>

fun dataTrace(vararg trace: String): DataTrace = trace.toList()