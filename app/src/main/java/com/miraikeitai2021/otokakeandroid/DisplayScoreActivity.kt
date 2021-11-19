package com.miraikeitai2021.otokakeandroid

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

            val score = bestPoint * 5000 + normalPoint * 2000

            binding.scoreTextView.text = "score: $score"
            binding.bestScoreTextView.text = "best: $bestPoint"
            binding.nomalScoreTextView.text = "normal: $normalPoint"
            binding.badScoreTextView.text = "bad: $badPoint"
        }
    }
}