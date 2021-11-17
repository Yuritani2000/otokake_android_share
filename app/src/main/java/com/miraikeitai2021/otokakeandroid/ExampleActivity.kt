package com.miraikeitai2021.otokakeandroid

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore

import android.view.View
import android.widget.Button
import android.widget.Toast

class ExampleActivity : AppCompatActivity() {
    lateinit var mediaPlayer: MediaPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_music)


        val storageIdList: Array<Long> =
            intent.getSerializableExtra("storageIdList") as Array<Long> //インテント元から配列を取得
        /*
        for (i in storageIdList.indices){   //受け取り内容の確認
            Log.v("TAG","要素${i}:${storageIdList[i]}")
        }
        */

        // 音楽再生ボタン
        val buttonStart: Button = findViewById(R.id.start)
        // 再生ボタンイベント
        buttonStart.setOnClickListener { v: View? ->
            //playMusic()
            for (i in storageIdList.indices) {
                mediaPlayer =
                    MediaPlayer.create(this, checkUri(this, storageIdList[i]))  //再生の準備
                mediaPlayer.isLooping = false   //ループ再生OFF
                mediaPlayer.start() //再生開始
                mediaPlayer.setOnCompletionListener {stopMusic()}
            }
        }
        // 音楽停止ボタン
        val buttonStop: Button = findViewById(R.id.stop)
        // 停止ボタンイベント
        buttonStop.setOnClickListener { v: View? ->

            /*
            if (mediaPlayer != null) {
                // 音楽停止
                // 再生終了
                mediaPlayer.stop()
                // リセット
                mediaPlayer.reset()
                // リソースの解放
                mediaPlayer.release()
            }

                 */

            stopMusic()
        }
    }

    private fun checkUri(context: Context, musicId: Long): Uri {
        //projection: 欲しい情報を定義
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )


        //selection: filterでAudioFileのみのIDを得るように定義
        val selection = MediaStore.Audio.Media._ID + "=" + musicId.toString()

        //上のprojectionとselectionを利用した問い合わせ変数を作製
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI, //外部ストレージ
            projection,
            selection,  // selection,
            null, // selectionArgs,
            null
        )

        lateinit var contentUri: Uri

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                contentUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                    id.toString()
                )
                Toast.makeText(
                    context,
                    "id: $id, displayName: $displayName, contentUri: $contentUri", Toast.LENGTH_LONG
                ).show()
            }
        }
        return contentUri

    }
/*
    //曲の再生
    private fun playMusic(){
        //曲を順番に再生
        for (i in storageIdList.indices) {
            mediaPlayer =
                MediaPlayer.create(this, checkUri(this, storageIdList[i]))  //再生の準備
            mediaPlayer.isLooping = false   //ループ再生OFF
            mediaPlayer.start() //再生開始
            mediaPlayer.setOnCompletionListener {  }
        }
    }

 */

    //曲の停止
    private fun stopMusic() {
        //歩調のbpm情報をリセット
        //Toast.makeText(myContext, "${checkRunBpm.getRunBpm()}", Toast.LENGTH_LONG).show()
        mediaPlayer.stop()
        mediaPlayer.reset()
        mediaPlayer.release()
    }

}