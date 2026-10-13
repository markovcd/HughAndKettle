package io.github.markovcd.hughandkettle.cli

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetCommandTest {
    private val folder: Path = Files.createTempDirectory("hk-preset-names")
    private val dir = folder.resolve("names").toString()

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() = folder.deleteRecursively()

    private class Run(
        val code: Int,
        val out: String,
        val err: String,
    )

    private fun run(vararg args: String): Run {
        val out = StringWriter()
        val err = StringWriter()
        val code = Cli.run(listOf("preset", *args, "--dir", dir), PrintWriter(out, true), PrintWriter(err, true))
        return Run(code, out.toString().trim(), err.toString())
    }

    @Test
    fun `Named presets list in preset order for their amp alone`() {
        assertEquals(
            """{"amp":"grandmeister_deluxe_40","preset":12,"name":"Lead solo"}""",
            run("name", "grandmeister_deluxe_40", "12", "Lead solo").out,
        )
        run("name", "grandmeister_deluxe_40", "3", "Rhythm")
        run("name", "black_spirit_200", "1", "Other amp")
        assertEquals(
            """
            {"preset":3,"name":"Rhythm"}
            {"preset":12,"name":"Lead solo"}
            """.trimIndent(),
            run("list", "grandmeister_deluxe_40").out,
        )
    }

    @Test
    fun `An unnamed preset leaves the list`() {
        run("name", "grandmeister_deluxe_40", "12", "Lead solo")
        assertEquals(ExitCodes.OK, run("unname", "grandmeister_deluxe_40", "12").code)
        assertEquals("", run("list", "grandmeister_deluxe_40").out)
    }

    @Test
    fun `A preset the amp does not have, a blank name or an unknown amp is a bad argument`() {
        val runs =
            listOf(
                run("name", "grandmeister_deluxe_40", "129", "Lost"),
                run("name", "grandmeister_deluxe_40", "x", "Lost"),
                run("name", "grandmeister_deluxe_40", "1", " "),
                run("list", "no_such_amp"),
            )
        assertEquals(listOf(ExitCodes.BAD_ARGUMENTS), runs.map { it.code }.distinct(), runs.joinToString { it.err })
        assertTrue("1..128" in runs[0].err, runs[0].err)
    }
}
