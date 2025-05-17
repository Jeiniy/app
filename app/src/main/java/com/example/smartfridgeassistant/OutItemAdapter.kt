package com.example.smartfridgeassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class OutItem(
    val name: String,
    val state: String,
    val date: String,
    val category: String,
    val type: String,
    val note: String
)

class OutItemAdapter(
    private val outList: MutableList<OutItem>,
    private val onBackClick: (OutItem) -> Unit
) : RecyclerView.Adapter<OutItemAdapter.OutViewHolder>() {

    inner class OutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOut: TextView = itemView.findViewById(R.id.tvOut)
        val tvOutState: TextView = itemView.findViewById(R.id.tvOutState)
        val tvOutCategory: TextView = itemView.findViewById(R.id.tvOutCategory)
        val tvOutType: TextView = itemView.findViewById(R.id.tvOutType)
        val tvOutNote: TextView = itemView.findViewById(R.id.tvOutNote)
        val btnBack: ImageButton = itemView.findViewById(R.id.btnBack)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_out, parent, false)
        return OutViewHolder(view)
    }

    override fun onBindViewHolder(holder: OutViewHolder, position: Int) {
        val item = outList[position]
        holder.tvOut.text = item.name
        holder.tvOutState.text = item.state
//        holder.tvOutCategory.text = item.category
//        holder.tvOutType.text = item.type
//        holder.tvOutNote.text = item.note
        holder.btnBack.setOnClickListener {
            onBackClick(item)
        }
    }

    override fun getItemCount(): Int = outList.size

    fun updateData(newList: List<OutItem>) {
        outList.clear()
        outList.addAll(newList)
        notifyDataSetChanged()
    }
} 