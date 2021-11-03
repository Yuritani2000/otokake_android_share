package com.miraikeitai2021.otokakeandroid

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

class ExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        val storageIdList: Array<Long> = intent.getSerializableExtra("storageIdList") as Array<Long> //インテント元から配列を取得


        for (i in storageIdList.indices){   //受け取り内容の確認
            Log.v("TAG","要素${i}:${storageIdList[i]}")
        }


        lateinit var mediaPlayer: MediaPlayer

        //1曲目の再生だけ
        mediaPlayer = MediaPlayer.create(this, checkUri(this,storageIdList[0]))  //再生の準備
        mediaPlayer.isLooping = false   //ループ再生OFF
        mediaPlayer.start() //再生開始
    }
}

fun checkUri(context: Context, musicId: Long): Uri {

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

    cursor?.use{
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val displayName = cursor.getString(displayNameColumn)
            contentUri = Uri.withAppendedPath(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                id.toString()
            )
            Toast.makeText(context,
                "id: $id, displayName: $displayName, contentUir: $contentUri", Toast.LENGTH_LONG).show()
        }
    }
    return contentUri
}