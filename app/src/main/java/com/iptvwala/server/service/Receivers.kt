package com.iptvwala.server.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "settings")

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val enabled = context.dataStore.data.map { prefs ->
                    prefs[booleanPreferencesKey("server_enabled")] ?: false
                }.first()
                
                if (enabled) {
                    val port = context.dataStore.data.map { prefs ->
                        prefs[intPreferencesKey("server_port")] ?: 8080
                    }.first()
                    
                    val serviceIntent = Intent(context, PlainAppServerService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            }
        }
    }
}

class PipReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.app.action.ENTER_PICTURE_IN_PICTURE") {
        }
    }
}
