package dev.crc32.talktome

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.lang.Exception


class PebbleForgetter : Service() {
    private val binder = LocalBinder()
    val adapter = BluetoothAdapter.getDefaultAdapter()

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (intent?.action == BluetoothDevice.ACTION_ACL_DISCONNECTED && device != null) {
                if (device.bondState == BluetoothDevice.BOND_BONDED && device.type == BluetoothDevice.DEVICE_TYPE_LE && device.name.startsWith(
                        "Pebble "
                    ) && !device.name.startsWith("Pebble LE")
                ) {
                    Log.i("Service", "Pebble 2 disconnected, will un-pair")
                    try {
                        device::class.java.getMethod("removeBond").invoke(device)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): PebbleForgetter = this@PebbleForgetter
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("Service", "Bind")
        return binder
    }

    override fun onCreate() {
        Log.i("Service", "Service created")
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(receiver, filter)
        Log.i("Service", "Receiver registered")
        updateRunning()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "Service",
                    "Service",
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        val notif = NotificationCompat.Builder(this, "Service")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("TalkToMe running")
            .setContentText("Keeping your Pebble 2 talking to you in the background")
            .setPriority(NotificationCompat.PRIORITY_MIN).build()
        startForeground(1, notif)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Service", "Start")
        val notif = NotificationCompat.Builder(this, "Service")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("TalkToMe running")
            .setContentText("Keeping your Pebble 2 talking to you in the background")
            .setPriority(NotificationCompat.PRIORITY_MIN).build()
        startForeground(1, notif)
        return START_STICKY
    }

    fun updateRunning() {
        if (!getSharedPreferences("talktomePrefs", MODE_PRIVATE).getBoolean(
                "enableService",
                false
            )
        ) {
            Log.i("Service", "No longer meant to run, stopping...")
            stopSelf()
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
