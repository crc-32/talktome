package dev.crc32.talktome

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit


class MainActivity : AppCompatActivity() {
    private var serviceBinder: PebbleForgetter? = null
    private var isBound = false

    var myConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            serviceBinder = (binder as PebbleForgetter.LocalBinder).getService()
            isBound = true
            serviceBinder?.updateRunning()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            serviceBinder = null
            isBound = false
        }
    }

    private fun bind() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, PebbleForgetter::class.java))
        } else {
            startService(Intent(this, PebbleForgetter::class.java))
        }
        bindService(Intent(this, PebbleForgetter::class.java), myConnection, BIND_AUTO_CREATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("talktomePrefs", MODE_PRIVATE)
        val switch = findViewById<Switch>(R.id.switch1)
        switch.isChecked = prefs.getBoolean("enableService", false)

        switch.setOnCheckedChangeListener { compoundButton, checked ->
            prefs.edit {
                putBoolean("enableService", checked)
                commit()
            }
            if (!isBound && checked) bind()
            serviceBinder?.updateRunning()
        }
    }

    override fun onResume() {
        super.onResume()
        bind()
    }

    override fun onPause() {
        super.onPause()
        if (isBound && serviceBinder != null) {
            unbindService(myConnection)
            serviceBinder = null
        }
    }
}