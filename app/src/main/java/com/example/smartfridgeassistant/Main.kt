package com.example.smartfridgeassistant

import android.webkit.WebView
import android.webkit.WebViewClient
import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*


class Main : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    private lateinit var dao: FoodDao
    private lateinit var wasteDao: WasteDao
    private lateinit var eatenDao: EatenDao
    private val itemList = mutableListOf<FoodItem>()
    private lateinit var adapter: FoodAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main)

        val lightbulb: ImageButton = findViewById(R.id.lightbulb)
        lightbulb.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_help, null)

            val webView = dialogView.findViewById<WebView>(R.id.youtube_webview)
            webView.settings.javaScriptEnabled = true
            webView.loadData(
                "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/j7TpJtYL9CM\" frameborder=\"0\" allowfullscreen></iframe>",
                "text/html",
                "utf-8"
            )

            AlertDialog.Builder(this)
                .setTitle("💡操作說明")
                .setView(dialogView)
                .setPositiveButton("關閉", null)
                .show()
        }


        // 检查并请求通知权限
        checkNotificationPermission()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }

        db = AppDatabase.getDatabase(this)

        val scanButton: ImageButton = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            IntentIntegrator(this).apply {
                setOrientationLocked(true)
                setPrompt("請將 QRCode 對準掃描框")
                setBeepEnabled(true)
                initiateScan()
            }
        }
        scanButton.setOnClickListener {
            IntentIntegrator(this).initiateScan()  // ✅ 啟動掃描器
        }

        val database = AppDatabase.getDatabase(this)
        dao = database.foodDao()
        wasteDao = database.wasteDao()
        eatenDao = database.eatenDao()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_setting -> {
                    startActivity(Intent(this, SettingActivity::class.java))
                    true
                }
                R.id.nav_analyze -> {
                    startActivity(Intent(this, AnalyzeActivity::class.java))
                    true
                }
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    true
                }
                else -> false
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = FoodAdapter(
            itemList = itemList,
            onItemClick = { foodItem -> showEditDialog(foodItem) },
            onDeleteItem = { foodItem -> lifecycleScope.launch { dao.delete(foodItem); refreshItemList() } },
            onTrashItem = { foodItem ->
                lifecycleScope.launch {
                    wasteDao.insert(
                        WasteItem(
                            name = foodItem.name,
                            category = foodItem.category,
                            note = foodItem.note,
                            type = foodItem.type,
                            date = foodItem.expiryDate
                        )
                    )
                    dao.delete(foodItem)
                    refreshItemList()
                }
            },
            onEatItem = { foodItem ->
                lifecycleScope.launch {
                    eatenDao.insert(EatenItem(
                        name = foodItem.name,
                        category = foodItem.category,
                        note = foodItem.note,
                        type = foodItem.type,
                        date = foodItem.expiryDate
                        )
                    )
                    dao.delete(foodItem)
                    refreshItemList()
                }
            }

        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        val sortSpinner = findViewById<Spinner>(R.id.spinner)
        val sortOptions = arrayOf("預設", "A~Z", "Z~A", "到期日近到遠", "到期日遠到近")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = spinnerAdapter

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    1 -> itemList.sortBy { it.name.lowercase() }
                    2 -> itemList.sortByDescending { it.name.lowercase() }
                    3 -> itemList.sortBy { it.expiryDate }
                    4 -> itemList.sortByDescending { it.expiryDate }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.add_item, null)
            val dialog = AlertDialog.Builder(this).setView(dialogView).create()

            val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
            val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinner_category)
            val dateText = dialogView.findViewById<TextView>(R.id.tv_expiry_date)
            val noteEditText = dialogView.findViewById<EditText>(R.id.et_note)
            val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group)
            val saveButton = dialogView.findViewById<Button>(R.id.btn_save)

            val categoryOptions = arrayOf("冷藏", "冷凍", "常溫")
            categorySpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categoryOptions
            )

            var selectedType = ""
            chipGroup.setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                selectedType = chip?.text?.toString() ?: ""
            }

            dateText.setOnClickListener {
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("選擇到期日")
                    .build()

                datePicker.show(supportFragmentManager, "DATE_PICKER")

                datePicker.addOnPositiveButtonClickListener { selection ->
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = sdf.format(Date(selection))
                    dateText.text = date
                }
            }

            saveButton.setOnClickListener {
                val name = nameEditText.text.toString()
                val category = categorySpinner.selectedItem.toString()
                val date = dateText.text.toString()
                val note = noteEditText.text.toString()
                val type = selectedType.trim()
                val datePattern = Regex("\\d{4}-\\d{1,2}-\\d{1,2}")
                if (name.isNotBlank() && type.isNotBlank() && date.matches(datePattern)) {
                    val newItem = FoodItem(name = name, category = category, expiryDate = date, note = note, type = type)
                    lifecycleScope.launch {
                        dao.insert(newItem)
                        refreshItemList()
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "請填寫完整資訊（名稱、種類與到期日）", Toast.LENGTH_SHORT).show()
                }

        }
            dialog.show()
        }

        // 初始載入列表
        refreshItemList()

    }

    private fun showEditDialog(item: FoodItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_item, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val etName = dialogView.findViewById<EditText>(R.id.et_name)
        val etNote = dialogView.findViewById<EditText>(R.id.et_note)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_expiry_date)
        val btnDone = dialogView.findViewById<Button>(R.id.btn_done)

        etName.setText(item.name)
        etNote.setText(item.note)
        tvDate.text = item.expiryDate

        val categoryOptions = arrayOf("冷藏", "冷凍", "常溫")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions)
        spinnerCategory.adapter = categoryAdapter
        spinnerCategory.setSelection(categoryOptions.indexOf(item.category))

        val typeOptions = arrayOf("肉類", "海鮮類","蔬菜類","乳品類","水果類", "飲料類", "點心類","熟食","其他")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeOptions)
        spinnerType.adapter = typeAdapter
        spinnerType.setSelection(typeOptions.indexOf(item.type))

        tvDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("選擇到期日")
                .build()

            datePicker.show(this@Main.supportFragmentManager, "EDIT_DATE_PICKER")

            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.format(Date(selection))
                tvDate.text = date
            }
        }

        btnDone.setOnClickListener {
            val newItem = item.copy(
                name = etName.text.toString(),
                note = etNote.text.toString(),
                category = spinnerCategory.selectedItem.toString(),
                type = spinnerType.selectedItem.toString(),
                expiryDate = tvDate.text.toString()
            )
            lifecycleScope.launch {
                dao.update(newItem)
                refreshItemList()
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun refreshItemList() {
        lifecycleScope.launch {
            val data = dao.getAll()
            itemList.clear()
            itemList.addAll(data)
            adapter.notifyDataSetChanged()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            try {
                val json = JSONObject(result.contents)

                val name = json.getString("name")
                val category = json.optString("category", "")
                val expiryDate = json.getString("expiryDate")
                val note = json.optString("note", "")
                val type = json.optString("type", "")

                val foodItem = FoodItem(
                    name = name,
                    category = category,
                    expiryDate = expiryDate,
                    note = note,
                    type = type
                )

                val db = AppDatabase.getDatabase(this)

                lifecycleScope.launch {
                    db.foodDao().insert(foodItem)
                    Toast.makeText(this@Main, "✅ 已新增：$name", Toast.LENGTH_SHORT).show()
                    refreshItemList()  // ← 如果你要同步畫面
                }

            } catch (e: Exception) {
                Toast.makeText(this, "❌ QR 格式錯誤", Toast.LENGTH_SHORT).show()
                Log.e("QR_ERROR", e.toString())
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}

