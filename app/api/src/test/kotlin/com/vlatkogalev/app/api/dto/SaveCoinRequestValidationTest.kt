package com.vlatkogalev.app.api.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SaveCoinRequestValidationTest {
    @Test
    fun blankObverseKey_returnsError() {
        val request = validRequest().copy(obverseKey = " ")

        assertEquals("obverseKey is required", request.validate())
    }

    @Test
    fun blankReverseKey_returnsError() {
        val request = validRequest().copy(reverseKey = " ")

        assertEquals("reverseKey is required", request.validate())
    }

    @Test
    fun blankCatalogueName_returnsError() {
        val request = validRequest().copy(
            catalogueNumbers = listOf(CatalogueNumberDto(catalogueName = " ", number = "1", confidence = "HIGH")),
        )

        assertEquals("catalogueName must not be blank", request.validate())
    }

    @Test
    fun validRequest_returnsNull() {
        assertNull(validRequest().validate())
    }

    private fun validRequest(): SaveCoinRequest =
        SaveCoinRequest(
            obverseKey = "obverse.jpg",
            reverseKey = "reverse.jpg",
            recognitionResult = RecognitionResultDto(
                overallConfidence = "HIGH",
                rawJson = "{\"coin\":\"ok\"}",
            ),
            catalogueNumbers = listOf(CatalogueNumberDto(catalogueName = "Krause", number = "KM#1", confidence = "HIGH")),
            notes = "test",
        )
}
