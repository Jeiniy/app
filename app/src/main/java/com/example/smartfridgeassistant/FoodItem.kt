package com.example.smartfridgeassistant

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_table")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val expiryDate: String,
    val note: String,
    val type: String
)

@Entity(tableName = "waste_table")
data class WasteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val date: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
)

@Entity(tableName = "eaten_table")
data class EatenItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val date: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
)
