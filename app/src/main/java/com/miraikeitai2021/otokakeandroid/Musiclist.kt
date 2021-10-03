package com.miraikeitai2021.otokakeandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView

class Musiclist : AppCompatActivity() {

    companion object {  //Mainスレッドでdatabase操作するとエラーになる
        //lateinit var db1: PlaylistDatabase
        lateinit var db2: MusicDatabase
        //lateinit var db3: MiddlelistDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_musiclist)

        /*
        db1 = PlaylistDatabase.getInstance(this)    //PlayListのDB作成
        val db1Dao = db1.PlaylistDao()  //Daoと接続
        var newlist1 = Playlist(0,"J-POP")  //適当なデータを用意
        db1Dao.insert(newlist1) //上のデータをDBに追加
        newlist1 = Playlist(0,"ボカロ")    //2つ目のデータ(idを0にすると勝手に番号付けしてくれる)
        db1Dao.insert(newlist1)
        newlist1 = Playlist(0,"アニソン")
        db1Dao.insert(newlist1)
        Log.v("TAG","test insert ${db1Dao.getAll().toString()}")    //ログに要素を全て出力
        */

        db2 = MusicDatabase.getInstance(this)   //MusicのDBでも同じ操作してる
        val db2Dao = db2.MusicDao()
        Log.v("TAG","test insert ${db2Dao.getAll().toString()}")

        /*
        //色々試運転した残り
        db3 = MiddlelistDatabase.getInstance(this)
        val db3Dao = db3.MiddlelistDao()
        var newlist3 = Middlelist(0,1,1)
        db3Dao.insert(newlist3)
        newlist3 = Middlelist(0,1,2)
        db3Dao.insert(newlist3)
        newlist3 = Middlelist(0,2,1)
        db3Dao.insert(newlist3)
        db3Dao.insertMusic(1,3)
        Log.v("TAG","test insert ${db3Dao.getAll().toString()}")
        Log.v("TAG","test insert ${db3Dao.getPlaylist(1).toString()}")
        db3Dao.deletePlaylist(1)
        Log.v("TAG","test insert ${db3Dao.getAll().toString()}")

         */

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

    }


}