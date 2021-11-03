package com.miraikeitai2021.otokakeandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlaylistEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_edit)

        val db1 = PlaylistDatabase.getInstance(this)
        val db1Dao = db1.PlaylistDao()
        val db2 = MusicDatabase.getInstance(this)    //PlayListのDB作成
        val db2Dao = db2.MusicDao()  //Daoと接続
        val db3 = MiddleListDatabase.getInstance(this)
        val db3Dao = db3.MiddleListDao()


        val playlistId :Int = intent.getIntExtra("playlist_id",0)   //インテント元からプレイリスト番号を取得
        supportActionBar?.title = db1Dao.getTitle(playlistId) //ツールバーのタイトルを変更

        var musicDataList = db2Dao.getAll() //データベースの全曲取得

        val defaultList = db3Dao.getPlaylist(playlistId)   //もともと登録されてる曲一覧を取得

        // RecyclerViewの取得
        var recyclerView2 = findViewById<RecyclerView>(R.id.RecyclerView2)
        // LayoutManagerの設定
        recyclerView2.layoutManager = LinearLayoutManager(this)
        // CustomAdapterの生成と設定
        recyclerView2.adapter=RecyclerListAdapter(musicDataList, defaultList, db3Dao, playlistId)

        //戻るボタンの表示
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //読み込みボタンクリック時(将来的に削除予定)
        val inputButton = findViewById<Button>(R.id.input)
        inputButton.setOnClickListener {
            val callback = fun(){

                musicDataList = db2Dao.getAll()

                // RecyclerViewの取得
                recyclerView2 = findViewById(R.id.RecyclerView2)
                // LayoutManagerの設定
                recyclerView2.layoutManager = LinearLayoutManager(this)
                // CustomAdapterの生成と設定
                recyclerView2.adapter=RecyclerListAdapter(musicDataList, defaultList, db3Dao, playlistId)

                Log.d("debug", "コールバック呼ばれました");
            }

            // 曲の一覧を取得するHTTPリクエスト
            val musicRepository = MusicRepository()
            val musicViewModel = MusicViewModel(musicRepository,db2Dao)
            musicViewModel.getMusicList(callback)

        }
    }

    //戻るボタンクリック時
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { //戻るボタンクリック時
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val musicTitle: TextView = view.findViewById(R.id.musicTitle)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val constraintLayout: ConstraintLayout = view.findViewById(R.id.editContstraintLayout)
    }

    private inner class RecyclerListAdapter(private val musicDataList: List<Music>, val defaultList: List<Int>, private val db3Dao: MiddleListDao,private val playlist_id: Int):
        RecyclerView.Adapter<PlaylistEditActivity.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PlaylistEditActivity.ViewHolder {
            //レイアウトインフレータを取得
            val inflater = LayoutInflater.from(this@PlaylistEditActivity)
            //row.xmlをインフレ―トし、1行分の画面部品とする
            val view = inflater.inflate(R.layout.playlist_edit_row, parent, false)
            //生成したビューホルダをリターン
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: PlaylistEditActivity.ViewHolder, position: Int) {
            //リストデータから該当1行分のデータを取得
            val item = musicDataList[position]
            //メニュー名文字列を取得
            val musicTitle = item.title
            //ビューホルダ中のTextViewに設定
            holder.musicTitle.text = musicTitle
            //すでに登録されてる曲は最初にチェックをつける
            holder.checkBox.isChecked = defaultList.contains(item.backend_id)

            //曲タップ時の処理
            holder.constraintLayout.setOnClickListener {
                if(!holder.checkBox.isChecked){  //チェック入ってない(登録)時
                    db3Dao.insertMusic(playlist_id,item.backend_id)
                    holder.checkBox.isChecked = true
                }
                else{   //チェック入ってる(削除)時
                    db3Dao.deleteMusic(playlist_id,item.backend_id)
                    holder.checkBox.isChecked = false
                }
            }

            //チェックボックスタップ時の処理
            holder.checkBox.setOnClickListener {
                if(holder.checkBox.isChecked){  //チェック入ってない(登録)時
                    db3Dao.insertMusic(playlist_id,item.backend_id)
                }
                else{   //チェック入ってる(削除)時
                    db3Dao.deleteMusic(playlist_id,item.backend_id)
                }
            }
        }

        override fun getItemCount(): Int {
            //リストデータ中の件数をリターン
            return musicDataList.size
        }

    }
}