package com.example.smartfridgeassistant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.Food
import kotlinx.coroutines.launch


class SearchActivity : AppCompatActivity() {

    private lateinit var adapter: SearchResultAdapter
    private var foodList: List<FoodItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBars.top,   // ✅ 增加上方邊距，避開狀態列
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // 初始化元件
        val searchInput = findViewById<EditText>(R.id.search_input)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        adapter = SearchResultAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            foodList = db.foodDao().getAllFoods()
            adapter.updateData(foodList)  // 預設載入全部資料
        }

        // 搜尋功能：輸入時立即篩選資料
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim().lowercase()

                val results = if (keyword.isEmpty()) {
                    foodList.sortedBy { it.name.lowercase() }  // 🔠 預設排序
                } else {
                    foodList
                        .filter { it.name.lowercase().contains(keyword) ||
                                  it.expiryDate.lowercase().contains(keyword)} //搜尋日期
                        .sortedBy { it.name.lowercase() }  // 🔠 加上排序
                }

                adapter.updateData(results)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        setupBottomNav(this, R.id.nav_search)
    }
}