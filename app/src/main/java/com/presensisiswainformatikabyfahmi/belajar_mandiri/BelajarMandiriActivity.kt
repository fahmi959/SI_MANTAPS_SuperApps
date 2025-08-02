package com.presensisiswainformatikabyfahmi.belajar_mandiri

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.presensisiswainformatikabyfahmi.R
import java.io.InputStreamReader

class BelajarMandiriActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.belajar_mandiri)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val inputStream = assets.open("belajar-mandiri-informatika.json")
        val reader = InputStreamReader(inputStream)
        val data = Gson().fromJson(reader, BelajarMandiriData::class.java)

        val allItems = mutableListOf<Bab>()

        data.kelas10.forEach {
            allItems.add(Bab("Kelas 10", it.bab, it.penjelasan, it.link))
        }
        data.kelas11.forEach {
            allItems.add(Bab("Kelas 11", it.bab, it.penjelasan, it.link))
        }
        data.kelas12.forEach {
            allItems.add(Bab("Kelas 12", it.bab, it.penjelasan, it.link))
        }

        val adapter = BabAdapter(allItems)
        recyclerView.adapter = adapter
    }
}