package io.github.markovcd.hughandkettle.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OptionsTest {
    private fun parsed(vararg args: String) = Options.parse(args.toList(), setOf("seconds"))

    @Test
    fun `An optional number left out is null, given is itself, and not a number is reported`() {
        assertNull(parsed().optionalInt("seconds").getOrThrow())
        assertEquals(5, parsed("--seconds", "5").optionalInt("seconds").getOrThrow())
        assertEquals(
            "--seconds needs a whole number, got 'soon'",
            parsed("--seconds", "soon").optionalInt("seconds").exceptionOrNull()?.message,
        )
    }
}
