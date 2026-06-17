package com.vlatkogalev.data.numista

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumistaTypeDetailSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `deserializes full Numista API v3 type detail response correctly`() {
        val jsonPayload = """{
  "id": 6350,
  "url": "https://en.numista.com/6350",
  "title": "20 Euro Cents (2nd map)",
  "object_type": {
    "id": 1,
    "name": "Standard circulation coins"
  },
  "issuer": {
    "code": "grece",
    "name": "Greece"
  },
  "min_year": 2007,
  "max_year": 2026,
  "ruler": [
    {
      "id": 1254,
      "name": "Third Hellenic Republic",
      "wikidata_id": "Q17765809"
    }
  ],
  "value": {
    "text": "20 Euro Cents",
    "numeric_value": 0.2,
    "currency": {
      "id": 9008,
      "name": "Euro",
      "full_name": "Euro (2002-date)"
    }
  },
  "demonetization": {
    "is_demonetized": false
  },
  "size": 22.25,
  "thickness": 2.14,
  "shape": "Spanish flower",
  "composition": {
    "text": "Nordic gold (89% Copper, 5% Aluminium, 5% Zinc, 1% Tin)"
  },
  "technique": {
    "text": "Milled"
  },
  "obverse": {
    "engravers": [
      "Maria Antonatou",
      "Geórgios Stamatópoulos"
    ],
    "description": "A portrait of Ioannis Capodistrias...",
    "lettering": "20\\r\\n2008 ΛΕΠΤΑ\\r\\nΙ. ΚΑΠΟΔΙΣΤΡΙΑΣ\\r\\nΓΣ",
    "lettering_scripts": [
      {
        "name": "Greek"
      }
    ],
    "lettering_translation": "20\\r\\n2008 Cents\\r\\nI. Kapodistrias\\r\\nGS",
    "picture": "https://en.numista.com/catalogue/photos/grece/1818-original.jpg",
    "thumbnail": "https://en.numista.com/catalogue/photos/grece/1818-180.jpg",
    "picture_copyright": "gef"
  },
  "reverse": {
    "engravers": [
      "Luc Luycx"
    ],
    "description": "A map, next to the face value...",
    "lettering": "20\\r\\nLL\\r\\nEURO\\r\\nCENT",
    "lettering_scripts": [
      {
        "name": "Latin"
      }
    ],
    "picture": "https://en.numista.com/catalogue/photos/grece/1819-original.jpg",
    "thumbnail": "https://en.numista.com/catalogue/photos/grece/1819-180.jpg",
    "picture_copyright": "gef"
  },
  "comments": "The coin is issued in bankrolls.",
  "related_types": [
    {
      "id": 117,
      "title": "20 Euro Cents (1st map)",
      "category": "coin",
      "country": {
        "code": "grece",
        "name": "Greece"
      },
      "minYear": 2002,
      "maxYear": 2006
    }
  ],
  "tags": [
    "Map",
    "Politician"
  ],
  "references": [
    {
      "catalogue": {
        "id": 3,
        "code": "KM"
      },
      "number": "212"
    },
    {
      "catalogue": {
        "id": 24,
        "code": "Schön"
      },
      "number": "172"
    }
  ],
  "weight": 5.74,
  "orientation": "medal",
  "edge": {
    "description": "Smooth with seven indentations",
    "picture": "https://en.numista.com/catalogue/photos/grece/69d10ec6333e55.06506565-original.jpg",
    "thumbnail": "https://en.numista.com/catalogue/photos/grece/69d10ec6333e55.06506565-180.jpg",
    "picture_copyright": "Cyrillius"
  },
  "mints": [
    {
      "id": 1889,
      "name": "National Mint of the Bank of Greece"
    }
  ],
  "category": "coin",
  "type": "Standard circulation coins"
}"""

        val detail = json.decodeFromString<NumistaTypeDetail>(jsonPayload)

        assertEquals(6350, detail.id)
        assertEquals("https://en.numista.com/6350", detail.url)
        assertEquals("20 Euro Cents (2nd map)", detail.title)
        assertEquals("Greece", detail.issuer?.name)
        assertEquals(2007, detail.minYear)
        assertEquals(2026, detail.maxYear)
        assertEquals("20 Euro Cents", detail.value?.text)
        assertEquals(0.2, detail.value?.numericValue)
        assertEquals("Euro", detail.value?.currency?.name)
        assertEquals("Euro (2002-date)", detail.value?.currency?.fullName)
        assertEquals(22.25, detail.size)
        assertEquals(2.14, detail.thickness)
        assertEquals("Nordic gold (89% Copper, 5% Aluminium, 5% Zinc, 1% Tin)", detail.composition?.text)
        assertEquals(5.74, detail.weight)
        assertEquals("Smooth with seven indentations", detail.edge?.description)

        val obverse = detail.obverse!!
        assertTrue(obverse.description?.contains("Capodistrias") == true)
        assertNotNull(obverse.lettering)
        assertTrue(obverse.lettering.contains("ΛΕΠΤΑ"))
        assertTrue(obverse.lettering.contains("ΚΑΠΟΔΙΣΤΡΙΑΣ"))
        assertEquals(listOf("Maria Antonatou", "Geórgios Stamatópoulos"), obverse.engravers)
        assertEquals("https://en.numista.com/catalogue/photos/grece/1818-180.jpg", obverse.thumbnail)

        val reverse = detail.reverse!!
        assertNotNull(reverse.lettering)
        assertTrue(reverse.lettering.contains("EURO"))
        assertEquals(listOf("Luc Luycx"), reverse.engravers)
        assertEquals("https://en.numista.com/catalogue/photos/grece/1819-180.jpg", reverse.thumbnail)

        assertEquals("The coin is issued in bankrolls.", detail.comments)
    }

    @Test
    fun `deserializes minimal response with only required fields`() {
        val minimalJson = """{"id": 123}"""

        val detail = json.decodeFromString<NumistaTypeDetail>(minimalJson)

        assertEquals(123, detail.id)
        assertNull(detail.url)
        assertNull(detail.title)
        assertNull(detail.issuer)
        assertNull(detail.value)
        assertNull(detail.minYear)
        assertNull(detail.maxYear)
        assertNull(detail.composition)
        assertNull(detail.weight)
        assertNull(detail.size)
        assertNull(detail.thickness)
        assertNull(detail.edge)
        assertNull(detail.obverse)
        assertNull(detail.reverse)
        assertNull(detail.comments)
    }

    @Test
    fun `deserializes response with missing optional sections`() {
        val partialJson = """{
  "id": 456,
  "title": "Test Coin",
  "min_year": 2020,
  "value": {
    "text": "1 Dollar"
  }
}"""

        val detail = json.decodeFromString<NumistaTypeDetail>(partialJson)

        assertEquals(456, detail.id)
        assertEquals("Test Coin", detail.title)
        assertEquals(2020, detail.minYear)
        assertNull(detail.maxYear)
        assertEquals("1 Dollar", detail.value?.text)
        assertNull(detail.issuer)
        assertNull(detail.composition)
    }
}
