package com.miraikeitai2021.otokakeandroid

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Example2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example2)

        val storageId: Long = intent.getLongExtra("storageId",0)

        lateinit var mediaPlayer: MediaPlayer

        //1曲目の再生だけ
        mediaPlayer = MediaPlayer.create(this, CheckMusicUri().checkUri(storageId.toInt(), this.contentResolver))  //再生の準備
        mediaPlayer.isLooping = false   //ループ再生OFF
        mediaPlayer.start() //再生開始
    }
}