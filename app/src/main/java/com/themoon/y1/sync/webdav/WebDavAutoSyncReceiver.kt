package com.themoon.y1.sync.webdav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager

/** Starts a WebDAV sync when Auto Sync is on, Wi-Fi is up and the device is charging. */
class WebDavAutoSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val provider = WebDavSyncProvider.getInstance(app)
        if (!provider.autoSync) return
        val net = (app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo
        val wifi = net != null && net.isConnected && net.type == ConnectivityManager.TYPE_WIFI
        // Sticky broadcast read with a null receiver is allowed from a manifest receiver.
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val charging = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        val sync = WebDavSync.getInstance(app)
        val now = System.currentTimeMillis()
        if (!AutoSyncGate.shouldRun(true, provider.isConfigured(), wifi, charging, sync.isRunning, now, maxOf(provider.lastSync, provider.lastAutoAttempt))) return
        provider.lastAutoAttempt = now
        sync.start(object : WebDavSync.Listener {
            override fun onProgress(text: String, done: Long, total: Long) {}
            override fun onDone(changed: Boolean) {
                if (changed) app.sendBroadcast(Intent(WebDavSync.ACTION_SYNCED).setPackage(app.packageName))
            }
            override fun onError(e: Throwable) {}
        })
    }
}
