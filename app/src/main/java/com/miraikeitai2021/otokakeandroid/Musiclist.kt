package com.miraikeitai2021.otokakeandroid

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

class Musiclist : AppCompatActivity() {

    companion object {  //Mainスレッドでdatabase操作するとエラーになる
        //lateinit var db1: PlaylistDatabase
        lateinit var db2: MusicDatabase
        //lateinit var db3: MiddlelistDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_musiclist)

        db2 = MusicDatabase.getInstance(this)   //MusicのDBでも同じ操作してる
        val db2Dao = db2.MusicDao()
        Log.v("TAG","test insert ${db2Dao.getAll().toString()}")

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )


        val cursor = this.contentResolver.query(
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
            projection,
            null, null, null
        )

        cursor?.use {
            val id_index = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val title_index = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val arttist_index = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)

            while (cursor.moveToNext()) {
                var storage_id = cursor.getLong(id_index)
                var title = cursor.getString(title_index)
                var artist = cursor.getString(arttist_index)

                Log.v("TAG","test ${storage_id},${title},${artist}")

                db2Dao.insertMusic(storage_id,title,artist)

            }
            Log.v("TAG","test insert ${db2Dao.getAll().toString()}")
        }

        val list = db2Dao.getMusic()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,list)
        val listView: ListView = findViewById(R.id.listview)
        listView.adapter = adapter

        lateinit var mediaPlayer: MediaPlayer
        
        listView.setOnItemClickListener { parent, view, position, id -> //リストクリック時の処理
            mediaPlayer = MediaPlayer.create(this, checkUri(this, db2Dao.getId(list[position])))    //再生の準備
            mediaPlayer.isLooping = false
            mediaPlayer.start() //再生開始
            playNext(db2Dao,mediaPlayer,this,position+1,list)
        }
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

fun playNext(db2Dao: MusicDao,mediaPlayer: MediaPlayer,context: Context,i: Int,list: List<String>){
    if(i < list.lastIndex){
        mediaPlayer.setOnCompletionListener {   //再生終了したときの処理
            mediaPlayer.release()   //mediaPlayerの停止
            lateinit var mediaPlayer: MediaPlayer
            mediaPlayer = MediaPlayer.create(context, checkUri(context, db2Dao.getId(list[i]))) //次の曲の再生準備
            mediaPlayer.isLooping = false
            mediaPlayer.start() //再生開始
            playNext(db2Dao, mediaPlayer, context, i+1, list)   //再帰で次の曲の再生へ
        }
    }
}