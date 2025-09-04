package com.example.batteryweb

import android.content.Context
import android.util.Log
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.NetworkInterface

class WebServer(
    private val context: Context,
    private val port: Int = 8080
) {
    private var server: NettyApplicationEngine? = null
    private val batteryMonitor = BatteryMonitor(context)
    private var serverJob: Job? = null
    
    fun start() {
        try {
            serverJob = CoroutineScope(Dispatchers.IO).launch {
                server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        gson {
                            setPrettyPrinting()
                        }
                    }
                    
                    routing {
                        get("/") {
                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <title>Battery Status Server</title>
                                    <meta charset="utf-8">
                                    <meta name="viewport" content="width=device-width, initial-scale=1">
                                    <style>
                                        body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
                                        .container { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                                        .status { font-size: 18px; margin: 10px 0; }
                                        .charging { color: #4CAF50; }
                                        .not-charging { color: #f44336; }
                                        .refresh-btn { background: #2196F3; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; margin: 20px 0; }
                                        .refresh-btn:hover { background: #0b7dda; }
                                    </style>
                                </head>
                                <body>
                                    <div class="container">
                                        <h1>🔋 Battery Status Monitor</h1>
                                        <div id="status"></div>
                                        <button class="refresh-btn" onclick="refreshStatus()">刷新状态</button>
                                        <p>API端点: <code>/api/battery</code></p>
                                    </div>
                                    <script>
                                        function refreshStatus() {
                                            fetch('/api/battery')
                                                .then(response => response.json())
                                                .then(data => {
                                                    const chargingClass = data.isCharging ? 'charging' : 'not-charging';
                                                    const chargingText = data.isCharging ? '🔌 充电中' : '🔋 未充电';
                                                    document.getElementById('status').innerHTML = `
                                                        <div class="status">电量: <strong>${'$'}{data.batteryLevel}%</strong></div>
                                                        <div class="status ${'$'}{chargingClass}">${'$'}{chargingText}</div>
                                                        <div class="status">温度: ${'$'}{(data.temperature/10).toFixed(1)}°C</div>
                                                        <div class="status">电压: ${'$'}{data.voltage}mV</div>
                                                        <div class="status">更新时间: ${'$'}{new Date(data.timestamp).toLocaleString()}</div>
                                                    `;
                                                });
                                        }
                                        refreshStatus();
                                        setInterval(refreshStatus, 5000);
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            call.respondText(html, ContentType.Text.Html)
                        }
                        
                        get("/api/battery") {
                            val batteryData = batteryMonitor.getBatteryStatusJson()
                            call.respond(batteryData)
                        }
                        
                        get("/api/status") {
                            call.respond(mapOf(
                                "server" to "running",
                                "port" to port,
                                "ip" to getLocalIpAddress()
                            ))
                        }
                    }
                }.start(wait = false)
                
                Log.d("WebServer", "服务器启动成功，端口: $port")
                Log.d("WebServer", "本机IP: ${getLocalIpAddress()}")
            }
        } catch (e: Exception) {
            Log.e("WebServer", "启动服务器失败", e)
        }
    }
    
    fun stop() {
        serverJob?.cancel()
        server?.stop(1000, 5000)
        batteryMonitor.unregister()
        Log.d("WebServer", "服务器已停止")
    }
    
    fun getServerInfo(): Map<String, String> {
        return mapOf(
            "ip" to getLocalIpAddress(),
            "port" to port.toString(),
            "url" to "http://${getLocalIpAddress()}:$port"
        )
    }
    
    private fun getLocalIpAddress(): String {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in networkInterfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is InetAddress && address.hostAddress?.indexOf(':') == -1) {
                        return address.hostAddress ?: "未知"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WebServer", "获取IP地址失败", e)
        }
        return "未知"
    }
}