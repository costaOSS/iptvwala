package com.iptvwala.plainapp.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.iptvwala.server.service.KeyEventInjector

class MyAccessibilityService : AccessibilityService() {
    
    private var keyEventInjector: KeyEventInjector? = null
    
    override fun onCreate() {
        super.onCreate()
        keyEventInjector = KeyEventInjector(this)
    }
    
    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }
    
    override fun onInterrupt() {
    }
    
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            
            if (isDpadKey(keyCode)) {
                return true
            }
        }
        
        return false
    }
    
    private fun isDpadKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MUTE,
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> true
            else -> false
        }
    }
    
    fun performDpadKey(keyCode: Int) {
        try {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                KeyEvent.KEYCODE_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
                KeyEvent.KEYCODE_MENU -> performGlobalAction(GLOBAL_ACTION_RECENTS) // Best approximation for MENU
                else -> {
                    keyEventInjector?.injectKeyEvent(keyCode)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
    
    companion object {
        var instance: MyAccessibilityService? = null
            private set
    }
    
    init {
        instance = this
    }
}

class MyNotificationListenerService : android.service.notification.NotificationListenerService() {
    
    private val notifications = mutableListOf<com.iptvwala.domain.model.NotificationItem>()
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        sbn ?: return
        
        val notification = com.iptvwala.domain.model.NotificationItem(
            id = sbn.key,
            packageName = sbn.packageName,
            appName = sbn.packageName.substringAfterLast("."),
            title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: "",
            text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: "",
            timestamp = sbn.postTime
        )
        
        notifications.add(0, notification)
        if (notifications.size > 50) {
            notifications.removeAt(notifications.size - 1)
        }
        
        com.iptvwala.server.service.ServerState::class.java.getDeclaredField("notifications")
            .apply { isAccessible = true }
            .get(com.iptvwala.server.service.ServerState::class.java)
    }
    
    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) {
        sbn ?: return
        notifications.removeAll { it.id == sbn.key }
    }
    
    fun getNotifications(): List<com.iptvwala.domain.model.NotificationItem> {
        return notifications.toList()
    }
    
    fun clearAll() {
        cancelAllNotifications()
        notifications.clear()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
    
    companion object {
        var instance: MyNotificationListenerService? = null
            private set
    }
}
