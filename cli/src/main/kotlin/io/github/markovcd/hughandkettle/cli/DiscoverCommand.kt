package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import io.github.markovcd.hughandkettle.wmi.WmiDiscovery
import java.io.PrintWriter
import kotlin.time.Duration.Companion.seconds

/** Listens for WMI-1 broadcasts and prints each device as it is heard. */
public object DiscoverCommand : Command {
    override val name: String = "discover"
    override val usage: String = "hk discover [--seconds N] [--port N] [--first]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val seconds = args.int("seconds", WmiDiscovery.DefaultDuration.inWholeSeconds.toInt())
        val port = args.int("port", WmiDefaults.BROADCAST_PORT)
        val failure = listOf(seconds, port).firstOrNull { it.isFailure }
        if (failure != null) {
            err.println(failure.exceptionOrNull()?.message)
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val stopAtFirst = args.flag("first")
        val found =
            WmiDiscovery(port.getOrThrow())
                .listen(seconds.getOrThrow().seconds) { device ->
                    out.println(
                        Json.line(
                            "host" to device.address.hostAddress,
                            "port" to device.midiPort,
                            "device" to device.deviceId,
                            "firmware" to device.firmware,
                            "access_point" to device.accessPointMac,
                            "rssi" to device.rssi,
                        ),
                    )
                    !stopAtFirst
                }.getOrElse {
                    err.println("cannot listen on UDP ${port.getOrThrow()}: ${it.message}")
                    return ExitCodes.NO_LINK
                }
        return if (found.isEmpty()) ExitCodes.NOTHING_FOUND else ExitCodes.OK
    }
}
