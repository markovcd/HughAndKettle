package io.github.markovcd.hughandkettle.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.wmi.LinkState
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the process, and so the link, alive while the screen is off: a foreground service that runs
 * for as long as the link is not idle, with a notification that says where it stands and can end it.
 * The link itself lives in [LinkHolder]; ADR 0007 says why it is held this way.
 */
class LinkService : Service() {
    private val scope = MainScope()
    private var watching: Job? = null
    private val holder: LinkHolder get() = (application as HughAndKettleApplication).link

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_DISCONNECT) holder.stop()
        // A started foreground service must show its notification at once, whatever happens next.
        show(holder.state.value)
        if (watching == null) {
            watching =
                scope.launch {
                    holder.state.collect { state ->
                        if (state == LinkState.Idle) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        } else {
                            show(state)
                        }
                    }
                }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun show(state: LinkState) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.link_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val disconnect =
            PendingIntent.getService(
                this,
                0,
                Intent(this, LinkService::class.java).setAction(ACTION_DISCONNECT),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            Notification
                .Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(LinkWords.of(state, resources))
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(
                    Notification.Action
                        .Builder(null, getString(R.string.disconnect), disconnect)
                        .build(),
                ).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION, notification)
        }
    }

    companion object {
        private const val CHANNEL = "link"
        private const val NOTIFICATION = 1
        private const val ACTION_DISCONNECT = "io.github.markovcd.hughandkettle.DISCONNECT"

        /** Starts the service; it stops itself when the link goes idle. */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, LinkService::class.java))
        }
    }
}
