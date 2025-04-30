package com.example.smartfridgeassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class AnalyzeActivity : AppCompatActivity() {
    private lateinit var wasteDao: WasteDao
    private lateinit var eatenDao: EatenDao
    private val outList = mutableListOf<OutItem>()
    private lateinit var outAdapter: OutItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analyze)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化 Room DAO
        val database = AppDatabase.getDatabase(this)
        wasteDao = database.wasteDao()
        eatenDao = database.eatenDao()

        // 初始化 RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        outAdapter = OutItemAdapter(outList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = outAdapter

        // 加载厨余和完食数据
        lifecycleScope.launch {
            try {
                outList.clear()
                // 添加厨余记录
                wasteDao.getAll().forEach { waste ->
                    outList.add(OutItem(waste.name, "廚餘", waste.date))
                }
                // 添加完食记录
                eatenDao.getAll().forEach { eaten ->
                    outList.add(OutItem(eaten.name, "完食", eaten.date))
                }
                // 按日期排序
                outList.sortByDescending { it.date }
                outAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val fabBack = findViewById<FloatingActionButton>(R.id.fab_add)
        fabBack.setOnClickListener {
            val intent = Intent(this, Main::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish()
        }
    }
}