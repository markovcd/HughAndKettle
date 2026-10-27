package io.github.markovcd.hughandkettle.cli

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTest {
    @Test
    fun `Strings are quoted and escaped, numbers and booleans are bare`() {
        assertEquals(
            """{"a":"x\"y\\z\u0001","n":5,"b":true,"none":null,"list":[1,"two"]}""",
            Json.line("a" to "x\"y\\z\u0001", "n" to 5, "b" to true, "none" to null, "list" to listOf(1, "two")),
        )
    }

    @Test
    fun `A map is an object of its entries in order`() {
        assertEquals(
            """{"values":{"gain":100,"mid":3},"empty":{}}""",
            Json.line("values" to mapOf("gain" to 100, "mid" to 3), "empty" to emptyMap<String, Int>()),
        )
    }

    @Test
    fun `A fraction is a bare number, and one that is not a number is null`() {
        assertEquals("""{"hz":0.25,"bad":null}""", Json.line("hz" to 0.25, "bad" to Double.NaN))
    }
}
