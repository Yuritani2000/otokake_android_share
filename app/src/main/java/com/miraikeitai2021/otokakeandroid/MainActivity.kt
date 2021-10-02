package com.miraikeitai2021.otokakeandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import java.sql.Types.NULL

class MainActivity : AppCompatActivity() {

    /*
    companion object {  //Mainスレッドでdatabase操作するとエラーになる
        lateinit var db1: PlaylistDatabase
        lateinit var db2: MusicDatabase
        lateinit var db3: MiddlelistDatabase
    }

     */




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        db2 = MusicDatabase.getInstance(this)   //MusicのDBでも同じ操作してる
        val db2Dao = db2.MusicDao()
        var newlist2 = Music(0,123456789,"あさ","","www")
        db2Dao.insert(newlist2)
        newlist2 = Music(0,123456,"ひる","","www")
        db2Dao.insert(newlist2)
        newlist2 = Music(0,123,"よる","","www")
        db2Dao.insert(newlist2)
        Log.v("TAG","test insert ${db2Dao.getAll().toString()}")

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



        //コメントアウト部分は動作確認で使ったもの.元に戻せば動くはず

    }
}