package com.example.smartfridgeassistant

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.RadioGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dao: FoodDao
    private val selectedTypes = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        // 检查通知权限
        checkNotificationPermission()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        dao = AppDatabase.getDatabase(this).foodDao()


        // 初始化提醒時間設置
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_reminder)
        val savedReminderTime = sharedPreferences.getInt("reminder_time", 0)
        radioGroup.check(
            when (savedReminderTime) {
                0 -> R.id.radio_week_before
                1 -> R.id.radio_day_before
                2 -> R.id.radio_same_day
                else -> R.id.radio_week_before
            }
        )

        // 設置提醒時間變更監聽器
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val reminderTime = when (checkedId) {
                R.id.radio_week_before -> 0
                R.id.radio_day_before -> 1
                R.id.radio_same_day -> 2
                else -> 0
            }
            sharedPreferences.edit().putInt("reminder_time", reminderTime).apply()
            checkAndSendNotifications(reminderTime)
        }

        // 初始化食品類型設置
        val chipGroup = findViewById<ChipGroup>(R.id.chip_group_food)
        val savedFoodTypes =
            sharedPreferences.getStringSet("food_types", setOf("all")) ?: setOf("all")
        selectedTypes.addAll(savedFoodTypes)

        // 设置已保存的选中状态
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip != null) {
                chip.isChecked = when (chip.text.toString()) {
                    "全部" -> savedFoodTypes.contains("all")
                    else -> savedFoodTypes.contains(chip.text.toString())
                }
            }
        }

        // 设置ChipGroup的监听器
        chipGroup.setOnCheckedChangeListener { group, _ ->
            selectedTypes.clear()
            var hasSelection = false

            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? Chip
                if (chip?.isChecked == true) {
                    hasSelection = true
                    val chipText = chip.text.toString().trim()
                    Log.d("SettingActivity", "Chip文本: '$chipText'")
                    if (chipText == "全部") {
                        selectedTypes.add("all")
                        Log.d("SettingActivity", "添加全部类型")
                    } else {
                        selectedTypes.add(chipText)
                        Log.d("SettingActivity", "添加类型: '$chipText'")
                    }
                }
            }

            // 如果没有选中任何选项，默认选中"全部"
            if (!hasSelection) {
                selectedTypes.add("all")
                (group.getChildAt(0) as? Chip)?.isChecked = true
                Log.d("SettingActivity", "默认选中全部")
            }

            // 保存选中的类型
            sharedPreferences.edit().putStringSet("food_types", selectedTypes).apply()
            Log.d("SettingActivity", "保存的类型: $selectedTypes")

            // 获取当前提醒时间设置并触发通知检查
            val currentReminderTime = sharedPreferences.getInt("reminder_time", 0)
            checkAndSendNotifications(currentReminderTime)
        }

        setupBottomNav(this, R.id.nav_setting)
    }

    private fun checkAndSendNotifications(reminderTime: Int) {
        lifecycleScope.launch {
            val allFoods = dao.getAll()
            val currentDate = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // 计算目标日期
            val targetDate = Calendar.getInstance()
            targetDate.add(
                Calendar.DAY_OF_MONTH, when (reminderTime) {
                    0 -> 7  // 一周后
                    1 -> 1  // 一天后
                    else -> 0  // 当天
                }
            )
            val targetDateStr = dateFormat.format(targetDate.time)

            // 添加调试信息
            Log.d("SettingActivity", "选中的类型: $selectedTypes")
            Log.d("SettingActivity", "目标日期: $targetDateStr")
            Log.d("SettingActivity", "所有食品: ${allFoods.map { "${it.name}(${it.type})" }}")

            // 检查所有食品并发送通知
            var notificationSent = false
            allFoods.forEach { food ->
                // 添加调试信息
                Log.d(
                    "SettingActivity",
                    "检查食品: ${food.name}, 类型: '${food.type}', 到期日: ${food.expiryDate}"
                )

                // 检查食品类型是否在选中范围内
                val shouldNotify = if (selectedTypes.contains("all")) {
                    Log.d("SettingActivity", "选中了全部类型")
                    true
                } else {
                    // 确保类型名称完全匹配
                    val foodType = food.type.trim()
                    val isTypeSelected = selectedTypes.any { selectedType ->
                        val matches = selectedType.trim() == foodType
                        Log.d(
                            "SettingActivity",
                            "比较类型: '$selectedType' 与 '$foodType', 匹配结果: $matches"
                        )
                        matches
                    }
                    Log.d("SettingActivity", "食品类型: '$foodType', 是否匹配: $isTypeSelected")
                    isTypeSelected
                }

                if (shouldNotify) {
                    // 确保日期格式一致
                    val foodExpiryDate = dateFormat.parse(food.expiryDate)
                    val targetDateParsed = dateFormat.parse(targetDateStr)

                    if (foodExpiryDate != null && targetDateParsed != null) {
                        val datesMatch = foodExpiryDate.time == targetDateParsed.time
                        Log.d("SettingActivity", "日期是否匹配: $datesMatch")

                        if (datesMatch) {
                            notificationSent = true
                            NotificationHelper(this@SettingActivity).showExpiryNotification(
                                food.name,
                                food.expiryDate
                            )
                            Log.d("SettingActivity", "发送通知: ${food.name}")
                        }
                    }
                }
            }

            // 如果没有找到符合条件的食品，显示提示
            if (!notificationSent) {
                runOnUiThread {
                    val message = when {
                        selectedTypes.contains("all") -> "沒有找到${
                            when (reminderTime) {
                                0 -> "一周后"
                                1 -> "一天后"
                                else -> "当天"
                            }
                        }過期的食品"

                        else -> "沒有找到${
                            when (reminderTime) {
                                0 -> "一周后"
                                1 -> "一天后"
                                else -> "当天"
                            }
                        }過期的${selectedTypes.joinToString("、")}類食品"
                    }
                    Toast.makeText(this@SettingActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通知權限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要通知權限才能接收提醒", Toast.LENGTH_SHORT).show()
            }
        }
    }
}