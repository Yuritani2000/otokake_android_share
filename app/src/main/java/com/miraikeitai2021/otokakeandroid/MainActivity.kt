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
        Log.v("TAG","test insert ${db2Dao.getAll().toString()}")

         */

        //コメントアウト部分は動作確認で使ったもの.元に戻せば動くはず

    }
}