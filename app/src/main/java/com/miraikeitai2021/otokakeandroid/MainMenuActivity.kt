package com.miraikeitai2021.otokakeandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val trainingbutton = findViewById<Button>(R.id.trainingbutton)
        val gamebutton = findViewById<Button>(R.id.gamebutton)

        trainingbutton.setOnClickListener {
            val intent = Intent(this@MainMenuActivity, PlaylistActivity::class.java)
            startActivity(intent)
        }

        gamebutton.setOnClickListener {
            val intent = Intent(this@MainMenuActivity, GamePlaylistPlayActivity::class.java)
            startActivity(intent)
        }
    }
}