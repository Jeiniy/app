package com.example.smartfridgeassistant

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.Food
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchMainActivity : AppCompatActivity() {

    private lateinit var adapter: SearchResultAdapter
    private lateinit var foodList: List<Food>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_activity_main)

        // 假資料（之後可以改成從資料庫取出）
        foodList = listOf(
            Food("apple", "2025/04/25"),
            Food("ice cream", "2025/04/30"),
            Food("egg", "2025/05/01"),
            Food("milk", "2025/04/27"),
            Food("chocolate cake", "2025/04/28")
        )

        // 初始化元件
        val searchInput = findViewById<EditText>(R.id.search_input)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        adapter = SearchResultAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 搜尋功能：輸入時立即篩選資料
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim().lowercase()

                val results = if (keyword.isEmpty()) {
                    listOf()  // 空字串就不顯示任何東西
                } else {
                    foodList.filter {
                        it.name.lowercase().contains(keyword)
                    }
                }

                adapter.updateData(results)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        val fabBack = findViewById<FloatingActionButton>(R.id.fab_add)

        fabBack.setOnClickListener {
            val intent = Intent(this, Main::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish()
        }
    }
}