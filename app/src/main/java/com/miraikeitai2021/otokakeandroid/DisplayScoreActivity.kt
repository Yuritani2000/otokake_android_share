package com.miraikeitai2021.otokakeandroid

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.miraikeitai2021.otokakeandroid.databinding.ActivityDisplayScoreBinding

class DisplayScoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisplayScoreBinding



    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pointArray = intent.getIntArrayExtra("pointArray")

        pointArray?.let{
            val bestPoint = it[0]
            val normalPoint = it[1]
            val badPoint = it[2]

            val score = bestPoint * 500 + normalPoint * 200

            binding.scoreTextView.text = "score: $score"
            binding.bestScoreTextView.text = "best: $bestPoint"
            binding.nomalScoreTextView.text = "normal: $normalPoint"
            binding.badScoreTextView.text = "bad: $badPoint"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}