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
}
