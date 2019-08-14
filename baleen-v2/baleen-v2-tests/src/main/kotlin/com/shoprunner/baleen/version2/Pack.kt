package com.shoprunner.baleen.version2

import com.shoprunner.baleen.DataTrace
import com.shoprunner.baleen.ValidationError
import com.shoprunner.baleen.ValidationInfo
import com.shoprunner.baleen.ValidationResult
import com.shoprunner.baleen.annotation.DataDescription
import com.shoprunner.baleen.annotation.DataTest
import java.time.Instant

@DataDescription
data class Pack(
    val name: String,
    val creationDate: Instant,
    val leadDog: Dog,
    val colorsList: List<String>? = null,
    val colorsSet: Set<String>? = null,
    val colorsArray: Array<String>? = null,
    val colorsMap: Map<Dog, String>? = null,

    val dogList: List<Dog>? = null,
    val dogArray: Array<Dog>? = null,
    val dogSet: Set<Dog>? = null,
    val dogMap: Map<String, Dog>? = null

)

@DataTest
fun assertNoDogsNamedHomer(pack: Pack, dataTrace: DataTrace): List<ValidationResult> {
    if (pack.dogList == null || pack.dogList.none { dog -> dog.name == "Homer" }) {
        return listOf(ValidationInfo(dataTrace, "No dogs named Homer in pack", pack.dogList))
    } else {
        return listOf(ValidationError(dataTrace, "No dogs named Homer in pack allowed", pack.dogList))
    }
}
