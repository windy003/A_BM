package com.example.batteryweb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class WebServerService : Service() {
    
    private lateinit var webServer: WebServer
    private val channelId = "WebServerChannel"
    private val notificationId = 1
    
    companion object {
        const val ACTION_START = "START_SERVER"
        const val ACTION_STOP = "STOP_SERVER"
        
        fun startService(context: Context) {
            val intent = Intent(context, WebServerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, WebServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        webServer = WebServer(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                webServer.start()
                startForeground(notificationId, createNotification())
            }
            ACTION_STOP -> {
                webServer.stop()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webServer.stop()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "电池Web服务器",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "电池状态Web服务器正在运行"
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val serverInfo = webServer.getServerInfo()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, WebServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("电池Web服务器运行中")
            .setContentText("访问地址: ${serverInfo["url"]}")
            .setSmallIcon(R.drawable.ic_battery_web)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "停止", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}