package com.vlatkogalev.data.numista

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class NumistaHtmlTest {

    @Test
    fun `converts Numista historical context HTML to plain text with preserved line breaks`() {
        val input = """The standard weight of this coin was 516 grains.<br />
On the obverse, from 1908 through 1911 there were 46 stars and from 1912 through 1933 48 stars.<br /><br />
These coins circulated in British Honduras (Belize) until 1933.<br /><br />
<span style="font-weight:bold;">The 1933 Double Eagle:</span><br />
445,500 specimens minted:<br />
- 445,487 melted by executive order 6102 of President Franklin D. Roosevelt (intent was to melt all but two)<br />
- 12 held by US Government (10 of which were improperly removed from government holdings by Israel Switt and then recovered from his descendants, the Langbords)<br />
- 1 once owned by King Farouk of Egypt, now in private hands (Sold for US$7.59 million in 2002, and again on June 8, 2021 for US$18.8 million)<br /><br />
1909<br />
<a href="https://en.numista.com/catalogue/images/62e4f2abb9d08.jpg"><img src="https://en.numista.com/catalogue/images/miniatures/62e4f2abb9d08.jpg" alt="" width="200" height="150" /></a><br />
1911+1912<br />
<a href="https://en.numista.com/catalogue/images/62e4f2cd54314.jpg"><img src="https://en.numista.com/catalogue/images/miniatures/62e4f2cd54314.jpg" alt="" width="200" height="150" /></a>"""

        val expected = """The standard weight of this coin was 516 grains.
On the obverse, from 1908 through 1911 there were 46 stars and from 1912 through 1933 48 stars.

These coins circulated in British Honduras (Belize) until 1933.

The 1933 Double Eagle:
445,500 specimens minted:
- 445,487 melted by executive order 6102 of President Franklin D. Roosevelt (intent was to melt all but two)
- 12 held by US Government (10 of which were improperly removed from government holdings by Israel Switt and then recovered from his descendants, the Langbords)
- 1 once owned by King Farouk of Egypt, now in private hands (Sold for US${'$'}7.59 million in 2002, and again on June 8, 2021 for US${'$'}18.8 million)"""

        val result = htmlToPlainText(input)

        assertEquals(expected, result)
    }

    @Test
    fun `strips all markup images and image captions`() {
        val input = """The standard weight of this coin was 516 grains.<br /><br />
1909<br />
<a href="https://en.numista.com/x.jpg"><img src="https://en.numista.com/x.jpg" alt="" /></a>"""

        val result = htmlToPlainText(input)!!

        assertFalse(result.contains("<"), "should not contain HTML tags")
        assertFalse(result.contains(">"), "should not contain HTML tags")
        assertFalse(result.contains("\r"), "should not contain carriage returns")
        assertFalse(result.contains("\uE000"), "should not leak newline marker")
        assertFalse(result.contains("\uE001"), "should not leak image marker")
        assertFalse(result.contains("1909"), "should drop image caption line")
        assertFalse(result.contains("numista.com"), "should drop image/link URLs")
        assertEquals("The standard weight of this coin was 516 grains.", result)
    }

    @Test
    fun `decodes html entities`() {
        assertEquals("Tom & Jerry < > test", htmlToPlainText("Tom &amp; Jerry &lt; &gt; test"))
    }

    @Test
    fun `leaves plain text essentially unchanged`() {
        assertEquals("Just plain description text.", htmlToPlainText("Just plain description text."))
    }

    @Test
    fun `returns null for null or blank input`() {
        assertNull(htmlToPlainText(null))
        assertNull(htmlToPlainText(""))
        assertNull(htmlToPlainText("   "))
        assertNull(htmlToPlainText("<br /><br />"))
    }
}
