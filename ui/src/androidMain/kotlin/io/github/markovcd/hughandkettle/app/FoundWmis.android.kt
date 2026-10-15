package io.github.markovcd.hughandkettle.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.wifi_named
import io.github.markovcd.hughandkettle.ui.wifi_off
import io.github.markovcd.hughandkettle.ui.wifi_show_name
import io.github.markovcd.hughandkettle.ui.wifi_unnamed
import org.jetbrains.compose.resources.stringResource

/** WiFi's multicast lock: without it the radio drops broadcasts addressed to nobody in particular. */
@Composable
internal actual fun rememberBroadcastHold(): () -> AutoCloseable {
    val context = LocalContext.current
    return {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val lock = wifi?.createMulticastLock("hughandkettle-discovery")?.apply { acquire() }
        AutoCloseable { lock?.release() }
    }
}

/** Which WiFi the phone is on, with a way to the location grant Android names the network only behind. */
@Composable
internal actual fun WifiLine(discovery: DiscoveryState) {
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
