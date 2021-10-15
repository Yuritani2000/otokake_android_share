package com.miraikeitai2021.otokakeandroid

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.MutableStateFlow

class PlaylistPlayActivity : AppCompatActivity() {

    companion object {  //Mainスレッドでdatabase操作するとエラーになる
        lateinit var db2: MusicDatabase
        lateinit var db3: MiddlelistDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_play)

        val db2 = MusicDatabase.getInstance(this)    //PlayListのDB作成
        val db2Dao = db2.MusicDao()  //Daoと接続
        val db3 = MiddlelistDatabase.getInstance(this)
        val db3Dao = db3.MiddlelistDao()

        val playlistId :Int?= intent.getStringExtra("playlist_id")?.toIntOrNull()   //インテント元からプレイリスト番号を取得
        val musicList = db3Dao.getResisteredMusic(playlistId)   //該当の再生リストの情報を取得

        //登録件数0件ならスキップ
        if(db3Dao.count(playlistId)!=0) {
            // RecyclerViewの取得
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
            // LayoutManagerの設定
            recyclerView.layoutManager = LinearLayoutManager(this)
            // CustomAdapterの生成と設定
            recyclerView.adapter = RecyclerListAdapter(musicList, db2Dao, db3Dao, playlistId)
        }

        //戻るボタンクリック時
        val backButton = findViewById<Button>(R.id.backBotton)
        backButton.setOnClickListener {
            val intent2MenuThanks = Intent(this@PlaylistPlayActivity, PlaylistActivity::class.java)
            startActivity(intent2MenuThanks)
        }

        //編集ボタンクリック時
        val editButtun = findViewById<Button>(R.id.editButton)
        editButtun.setOnClickListener{
            val intent2MenuThanks = Intent(this@PlaylistPlayActivity, PlaylistEditActivity::class.java)
            intent2MenuThanks.putExtra("playlist_id",intent.getStringExtra("playlist_id"))
            startActivity(intent2MenuThanks)
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val musicTitle: TextView
        init{
            musicTitle = view.findViewById(R.id.musicTitle2)
        }
    }

    private inner class RecyclerListAdapter(private val musicList: List<Middlelist>, private val db2Dao: MusicDao, private val db3Dao: MiddlelistDao,private val playlist_id: Int?):
        RecyclerView.Adapter<PlaylistPlayActivity.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistPlayActivity.ViewHolder {
            //レイアウトインフレータを取得
            val inflater = LayoutInflater.from(this@PlaylistPlayActivity)
            //row.xmlをインフレ―トし、1行分の画面部品とする
            val view = inflater.inflate(R.layout.playlist_play_row, parent, false)
            //ビューホルダオブジェクトを生成
            val holder = ViewHolder(view)
            //生成したビューホルダをリターン
            return holder
        }

        override fun onBindViewHolder(holder: PlaylistPlayActivity.ViewHolder, position: Int) {
            //リストデータから該当1行分のデータを取得
            val item = db2Dao.getMusic(musicList[position].middle_backend_id)
            //メニュー名文字列を取得
            val musicTitle = item.title
            //ビューホルダ中のTextViewに設定
            holder.musicTitle.text = musicTitle

            //曲クリック時の処理
            holder.itemView.setOnClickListener {
                Toast.makeText(applicationContext, "実装中だよ！", Toast.LENGTH_LONG).show()
            }

        }
        override fun getItemCount(): Int {
            //リストデータ中の件数をリターン
            return musicList.size
        }
    }
}