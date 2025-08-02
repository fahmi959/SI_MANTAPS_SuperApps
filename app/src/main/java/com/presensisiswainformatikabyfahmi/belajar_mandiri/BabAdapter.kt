package com.presensisiswainformatikabyfahmi.belajar_mandiri

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.presensisiswainformatikabyfahmi.R

class BabAdapter(private val items: List<Bab>) : RecyclerView.Adapter<BabAdapter.MateriViewHolder>() {

    class MateriViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.babTitle)
        val penjelasan = view.findViewById<TextView>(R.id.babDesc)
        val kelas = view.findViewById<TextView>(R.id.babKelas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MateriViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_belajar_mandiri, parent, false)
        return MateriViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: MateriViewHolder, position: Int) {
        val item = items[position]
        holder.kelas.text = item.kelas
        holder.title.text = item.bab
        holder.penjelasan.text = item.penjelasan

        if (item.link.isNullOrBlank()) {
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
            holder.itemView.alpha = 0.6f
            holder.itemView.findViewById<TextView>(R.id.statusText).apply {
                visibility = View.VISIBLE
                text = "Coming Soon"
            }
        } else {
            holder.itemView.alpha = 1f
            holder.itemView.isClickable = true
            holder.itemView.findViewById<TextView>(R.id.statusText).visibility = View.GONE

            val videoId = extractYoutubeId(item.link)

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, VideoPlayerActivity::class.java)
                intent.putExtra("VIDEO_ID", videoId)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    private fun extractYoutubeId(url: String): String {
        val regex = Regex("(?:youtube\\.com.*(?:\\?|&)v=|youtu\\.be/)([a-zA-Z0-9_-]+)")
        val match = regex.find(url)
        return match?.groupValues?.get(1) ?: ""
    }
}
