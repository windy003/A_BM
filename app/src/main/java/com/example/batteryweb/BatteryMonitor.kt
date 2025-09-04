package com.example.batteryweb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
    val temperature: Int,
    val voltage: Int
)

class BatteryMonitor(private val context: Context) {
    
    private val _batteryStatus = MutableStateFlow(BatteryStatus(0, false, 0, 0))
    val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus
    
    private var batteryReceiver: BroadcastReceiver? = null
    
    init {
        registerBatteryReceiver()
        updateBatteryStatus()
    }
    
    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateBatteryStatus()
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        
        context.registerReceiver(batteryReceiver, filter)
    }
    
    private fun updateBatteryStatus() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        batteryIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = (level * 100 / scale.toFloat()).toInt()
            
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL
            
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            
            _batteryStatus.value = BatteryStatus(
                level = batteryPct,
                isCharging = isCharging,
                temperature = temperature,
                voltage = voltage
            )
        }
    }
    
    fun getBatteryStatusJson(): Map<String, Any> {
        val status = _batteryStatus.value
        return mapOf(
            "batteryLevel" to status.level,
            "isCharging" to status.isCharging,
            "temperature" to status.temperature,
            "voltage" to status.voltage,
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    fun unregister() {
        batteryReceiver?.let { receiver ->
            context.unregisterReceiver(receiver)
        }
        batteryReceiver = null
    }
}