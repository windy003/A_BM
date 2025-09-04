package com.example.batteryweb

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var openBrowserButton: Button
    private lateinit var batteryMonitor: BatteryMonitor
    private var isServiceRunning = false
    private var serverUrl = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initBatteryMonitor()
        setupClickListeners()
        requestBatteryOptimization()
        updateUI()
        startBatteryStatusUpdates()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        openBrowserButton = findViewById(R.id.openBrowserButton)
    }
    
    private fun initBatteryMonitor() {
        batteryMonitor = BatteryMonitor(this)
    }
    
    private fun setupClickListeners() {
        startButton.setOnClickListener {
            startWebServer()
        }
        
        stopButton.setOnClickListener {
            stopWebServer()
        }
        
        openBrowserButton.setOnClickListener {
            if (serverUrl.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                startActivity(intent)
            }
        }
    }
    
    private fun startWebServer() {
        WebServerService.startService(this)
        isServiceRunning = true
        
        val webServer = WebServer(this)
        val serverInfo = webServer.getServerInfo()
        serverUrl = serverInfo["url"] ?: ""
        
        updateUI()
        Toast.makeText(this, "Web服务器已启动", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopWebServer() {
        WebServerService.stopService(this)
        isServiceRunning = false
        serverUrl = ""
        updateUI()
        Toast.makeText(this, "Web服务器已停止", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateUI() {
        startButton.isEnabled = !isServiceRunning
        stopButton.isEnabled = isServiceRunning
        openBrowserButton.isEnabled = isServiceRunning && serverUrl.isNotEmpty()
        
        if (isServiceRunning && serverUrl.isNotEmpty()) {
            statusText.text = "服务器运行中\n访问地址: $serverUrl\n\n局域网内的其他设备可以通过此地址访问电池信息"
        } else {
            statusText.text = "服务器未启动\n\n点击启动按钮开始Web服务器"
        }
    }
    
    private fun startBatteryStatusUpdates() {
        lifecycleScope.launch {
            while (true) {
                val batteryData = batteryMonitor.getBatteryStatusJson()
                val batteryInfo = "\n\n当前电池状态:\n" +
                        "电量: ${batteryData["batteryLevel"]}%\n" +
                        "充电状态: ${if (batteryData["isCharging"] == true) "充电中" else "未充电"}\n" +
                        "温度: ${(batteryData["temperature"] as Int) / 10.0}°C"
                
                val currentText = if (isServiceRunning && serverUrl.isNotEmpty()) {
                    "服务器运行中\n访问地址: $serverUrl\n\n局域网内的其他设备可以通过此地址访问电池信息$batteryInfo"
                } else {
                    "服务器未启动\n\n点击启动按钮开始Web服务器$batteryInfo"
                }
                
                statusText.text = currentText
                delay(2000)
            }
        }
    }
    
    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // 忽略异常，某些设备可能不支持
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        batteryMonitor.unregister()
    }
}