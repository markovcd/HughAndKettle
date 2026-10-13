package io.github.markovcd.hughandkettle.app

import android.net.wifi.WifiManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiBroadcast

/**
 * The last box used, then every other WMI-1 heard on the network, one tappable row each, listening for
 * as long as it is shown. Tests find the rows by `last` and `found:<device id>`.
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
                failure != null -> stringResource(R.string.discovery_failed, failure)
                discovery.found.isEmpty() -> stringResource(R.string.discovery_listening)
                else -> stringResource(R.string.discovery_found)
            }
        Text(note, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        if (last != null) {
            WmiRow(stringResource(R.string.last_used), last, onPick, Modifier.testTag("last"))
        }
        discovery.found.forEach { box ->
            val address = box.wmiAddress()
            if (address != last) WmiRow(box.deviceId, address, onPick, Modifier.testTag("found:${box.deviceId}"))
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
