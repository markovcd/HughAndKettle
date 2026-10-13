package io.github.markovcd.hughandkettle.app

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.MidiStreamParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * The USB MIDI cables Android's MIDI service sees. A cable's first input port is where the app's
 * messages go to the amp, and its first output port, when it has one, is where the amp is heard.
 */
class MidiCableJack(
    context: Context,
) : CableJack {
    private val manager: MidiManager? =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            context.getSystemService(MidiManager::class.java)
        } else {
            null
        }
    private val main = Handler(Looper.getMainLooper())
    private val plugged = ConcurrentHashMap<Int, MidiDeviceInfo>()
    private val lost = ConcurrentHashMap<Int, (String) -> Unit>()
    private val found = MutableStateFlow(emptyList<Cable>())

    override val cables: StateFlow<List<Cable>> = found.asStateFlow()

    init {
        manager?.let { manager ->
            val callback =
                object : MidiManager.DeviceCallback() {
                    override fun onDeviceAdded(device: MidiDeviceInfo) = add(device)

                    override fun onDeviceRemoved(device: MidiDeviceInfo) = remove(device)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM).forEach(::add)
                manager.registerDeviceCallback(MidiManager.TRANSPORT_MIDI_BYTE_STREAM, { main.post(it) }, callback)
            } else {
                legacyDevices(manager).forEach(::add)
                legacyRegister(manager, callback)
            }
        }
    }

    override fun open(
        cable: Cable,
        onMessage: (MidiMessage) -> Unit,
        onLost: (reason: String) -> Unit,
        onOpened: (Result<CableLink>) -> Unit,
    ) {
        val info = plugged[cable.id]
        if (manager == null || info == null) {
            onOpened(Result.failure(IllegalStateException("not plugged in")))
            return
        }
        manager.openDevice(info, { device ->
            val input = device?.openInputPort(0)
            if (device == null || input == null) {
                device?.close()
                onOpened(Result.failure(IllegalStateException("Android would not open it; another app may hold it")))
                return@openDevice
            }
            val output = if (info.outputPortCount > 0) device.openOutputPort(0) else null
            val parser = MidiStreamParser()
            output?.connect(
                object : MidiReceiver() {
                    override fun onSend(
                        msg: ByteArray,
                        offset: Int,
                        count: Int,
                        timestamp: Long,
                    ) = synchronized(parser) { parser.feed(msg.copyOfRange(offset, offset + count)) }.forEach(onMessage)
                },
            )
            lost[cable.id] = onLost
            val link =
                CableLink(
                    cable,
                    write = { input.send(it, 0, it.size) },
                    release = {
                        lost.remove(cable.id)
                        output?.close()
                        input.close()
                        device.close()
                    },
                )
            onOpened(Result.success(link))
        }, main)
    }

    private fun add(info: MidiDeviceInfo) {
        // Only a USB device the app can send to is a cable; virtual ports from other apps are not.
        if (info.type != MidiDeviceInfo.TYPE_USB || info.inputPortCount == 0) return
        plugged[info.id] = info
        found.value = plugged.values.map { it.cable() }.sortedBy { it.id }
    }

    private fun remove(info: MidiDeviceInfo) {
        plugged.remove(info.id) ?: return
        found.value = plugged.values.map { it.cable() }.sortedBy { it.id }
        lost.remove(info.id)?.invoke("unplugged")
    }

    private fun MidiDeviceInfo.cable(): Cable {
        val name =
            properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "USB MIDI $id"
        return Cable(id, name)
    }

    // The transport-filtered calls above need API 33; before it, these are the only ones there are.
    @Suppress("DEPRECATION")
    private fun legacyDevices(manager: MidiManager): List<MidiDeviceInfo> = manager.devices.toList()

    @Suppress("DEPRECATION")
    private fun legacyRegister(
        manager: MidiManager,
        callback: MidiManager.DeviceCallback,
    ) = manager.registerDeviceCallback(callback, main)
}
