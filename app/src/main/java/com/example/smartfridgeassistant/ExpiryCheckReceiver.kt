package com.example.smartfridgeassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpiryCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val database = AppDatabase.getDatabase(context)
            val foodDao = database.foodDao()
            val notificationHelper = NotificationHelper(context)
            
            // 获取所有食品
            val foods = foodDao.getAll()
            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // 获取提醒天数设置
            val sharedPrefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val reminderDays = sharedPrefs.getInt("reminder_days", 1) // 默认1天
            
            foods.forEach { food ->
                try {
                    val expiryDate = dateFormat.parse(food.expiryDate)
                    if (expiryDate != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = expiryDate
                        
                        // 计算距离到期日的天数
                        val daysUntilExpiry = ((calendar.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                        
                        // 如果距离到期日正好是指定的提醒天数，发送通知
                        if (daysUntilExpiry == reminderDays) {
                            notificationHelper.showExpiryNotification(food.name, food.expiryDate)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
} 