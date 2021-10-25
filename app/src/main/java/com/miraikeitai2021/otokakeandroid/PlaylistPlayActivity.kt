package com.miraikeitai2021.otokakeandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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

        val playlistId :Int = intent.getIntExtra("playlist_id",0)   //インテント元からプレイリスト番号を取得
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
            val intent = Intent(this@PlaylistPlayActivity, PlaylistActivity::class.java)
            startActivity(intent)
        }

        //編集ボタンクリック時
        val editButtun = findViewById<Button>(R.id.editButton)
        editButtun.setOnClickListener{
            val intent = Intent(this@PlaylistPlayActivity, PlaylistEditActivity::class.java)
            intent.putExtra("playlist_id",intent.getIntExtra("playlist_id",0))
            startActivity(intent)
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val musicTitle = view.findViewById<TextView>(R.id.musicTitle2)
        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.constraintLayout)
    }

    private inner class RecyclerListAdapter(private val musicList: List<Middlelist>, private val db2Dao: MusicDao, private val db3Dao: MiddlelistDao,private val playlist_id: Int):
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
            holder.constraintLayout.setOnClickListener {
                val data: Array<Int> = db3Dao.tap(playlist_id,item.backend_id)   //タップした曲以降のバックエンドIDを取得
                var storageIdList: Array<Long> = arrayOf()  //ストレージIDを格納する配列
                for (i in data.indices){    //バックエンドIDの配列からストレージIDの配列を取得
                    storageIdList += db2Dao.getStorageId(data[i])
                }

                //インテント処理
                val intent = Intent(this@PlaylistPlayActivity, ExampleActivity::class.java)
                intent.putExtra("storageIdList",storageIdList)
                startActivity(intent)
            }

        }
        override fun getItemCount(): Int {
            //リストデータ中の件数をリターン
            return musicList.size
        }
    }
}