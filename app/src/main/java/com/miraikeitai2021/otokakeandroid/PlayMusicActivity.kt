package com.miraikeitai2021.otokakeandroid

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.miraikeitai2021.otokakeandroid.databinding.ActivityPlayMusicBinding

class PlayMusicActivity : AppCompatActivity() {
    val checkMusicUri: CheckMusicUri = CheckMusicUri() //曲のUriを取得するクラス
    private val checkRunBpm: CheckRunBpm = CheckRunBpm() //歩調のbpmを取得するクラス
    private val checkMusicBpm: CheckMusicBpm = CheckMusicBpm() //曲のbpmを取得するクラス
    private val playMusic: PlayMusic = PlayMusic(this) //曲を再生するクラス
    private val musicId: Int = 12237 //保存したときに確認したIDの値を入れておく．

    private lateinit var binding: ActivityPlayMusicBinding

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //************************************************************
        //保存用
        //val strageMusic = StrageMusic()
//        strageMusic.StrageInMusic(this)

        //↓ID検索用
/*
        //projection: 欲しい情報を定義
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
        )
        //上のprojectionとselectionを利用した問い合わせ変数を作製
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, //外部ストレージ
            projection,
            null,
            null, // selectionArgs,
            null
        )

        lateinit var contentUri: Uri
        val text: TextView = findViewById(R.id.textView)

        cursor?.use{
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)

                contentUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                //取得したメタデータを確認
                Toast.makeText(applicationContext,
                    "id: $id, contentUir: $contentUri"
                    , Toast.LENGTH_LONG).show()
                text.setText("id: $id, contentUir: $contentUri")
                /*
                Log.d(
                    TAG, "id: $id, display_name: $displayName, content_uri: $contentUri"
                )
                 */
            }
        }

 */
        //************************************************************************************

        binding.startButton.setOnClickListener { tappedStartButton() }
        binding.stopButton.setOnClickListener { tappedStopButton() }
        binding.bluetoothButton.setOnClickListener{ tappedBluetoothButton()}

    }

    /**
    * スタートボタンとストップボタン兼用のボタンのリスナー
    * */
    private fun onClickStartAndStopButton(){
        if(playMusic.isPlaying()){
            tappedStartButton()
        }else{
            tappedStartButton()
        }
    }

    /**
     * スタートボタンがクリックされたときの処理
     */
    private fun tappedStartButton(){
        //曲をスタートする
        val text: TextView = findViewById(R.id.textView)
        val contentUri = checkMusicUri.checkUri(this, musicId, contentResolver)
        text.setText(contentUri.toString())
        playMusic.startMusic(checkMusicUri.checkUri(this, musicId, contentResolver))
        //Toast.makeText(applicationContext, "Start", Toast.LENGTH_SHORT).show()
    }

    /**
     * ストップボタンがクリックされたときの処理
     */
    private fun tappedStopButton(){
        //曲をストップする
        playMusic.stopMusic()
    }

    /**
     * Bluetoothボタンがクリックされたときの処理
     * 今はボタンで割り込んでいるが，Bluetoothの通信による割込みに変えたい
     */
    @SuppressLint("SetTextI18n")
    private fun tappedBluetoothButton(){
        //歩調のBpmによって曲の再生速度を変更する
        playMusic.chengeSpeedMusic(checkRunBpm.checkRunBpm(this, musicId),checkMusicBpm.checkMusicBpm(this, musicId))

        val text: TextView = findViewById(R.id.textView)
        text.setText("musicBpm: ${checkMusicBpm.getMusicBpms()}  " +
                "runBpm: ${checkRunBpm.getRunBpm()}  " +
                "musicSpeed: ${playMusic.getChengedMusicSpeed()}  ")
    }
}