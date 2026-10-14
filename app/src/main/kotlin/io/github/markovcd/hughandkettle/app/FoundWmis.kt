package io.github.markovcd.hughandkettle.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.discovery_failed
import io.github.markovcd.hughandkettle.ui.discovery_found
import io.github.markovcd.hughandkettle.ui.discovery_listening
import io.github.markovcd.hughandkettle.ui.last_used
import io.github.markovcd.hughandkettle.ui.wifi_named
import io.github.markovcd.hughandkettle.ui.wifi_off
import io.github.markovcd.hughandkettle.ui.wifi_show_name
import io.github.markovcd.hughandkettle.ui.wifi_unnamed
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiBroadcast
import org.jetbrains.compose.resources.stringResource

/**
 * The WiFi the phone is on, the last box used, then every other WMI-1 heard on the network, one tappable
 * row each, listening for as long as it is shown. Tests find the rows by `last` and `found:<device id>`.
 */
@Composable
internal fun FoundWmis(
    discovery: DiscoveryState,
    last: WmiAddress?,
    onPick: (WmiAddress) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(discovery) {
        // Without the lock the WiFi radio drops broadcasts addressed to nobody in particular.
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val lock = wifi?.createMulticastLock("hughandkettle-discovery")?.apply { acquire() }
        try {
            discovery.listen()
        } finally {
            lock?.release()
        }
    }
    val colors = MaterialTheme.colorScheme
    Column(modifier.testTag("found"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val failure = discovery.failure
        val note =
            when {
                failure != null -> stringResource(Res.string.discovery_failed, failure)
                discovery.found.isEmpty() -> stringResource(Res.string.discovery_listening)
                else -> stringResource(Res.string.discovery_found)
            }
        Text(note, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        PhoneWifiLine(discovery)
        if (last != null) {
            WmiRow(stringResource(Res.string.last_used), last, onPick, Modifier.testTag("last"))
        }
        discovery.found.forEach { box ->
            val address = box.wmiAddress()
            if (address != last) WmiRow(box.deviceId, address, onPick, Modifier.testTag("found:${box.deviceId}"))
        }
    }
}

/** Which WiFi the phone is on, with a way to the location grant Android names the network only behind. */
@Composable
private fun PhoneWifiLine(discovery: DiscoveryState) {
    val wifi by discovery.wifi.collectAsState()
    val context = LocalContext.current
    val ask =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) discovery.rereadWifi()
        }
    val located =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    Row(verticalAlignment = Alignment.CenterVertically) {
        val text =
            when (val now = wifi) {
                PhoneWifi.Off -> stringResource(Res.string.wifi_off)
                PhoneWifi.Unnamed -> stringResource(Res.string.wifi_unnamed)
                is PhoneWifi.Named -> stringResource(Res.string.wifi_named, now.name)
            }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag("wifi"),
        )
        if (wifi == PhoneWifi.Unnamed && !located) {
            TextButton(
                onClick = {
                    ask.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                modifier = Modifier.testTag("name_wifi"),
            ) {
                Text(stringResource(Res.string.wifi_show_name))
            }
        }
    }
}

@Composable
private fun WmiRow(
    name: String,
    address: WmiAddress,
    onPick: (WmiAddress) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { onPick(address) }
                .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            "$address",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun WmiBroadcast.wmiAddress(): WmiAddress = WmiAddress(address.hostAddress ?: "$address", midiPort)
