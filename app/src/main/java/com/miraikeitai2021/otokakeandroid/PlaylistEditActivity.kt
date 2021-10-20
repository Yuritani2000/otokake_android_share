package com.miraikeitai2021.otokakeandroid

import android.content.Intent
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

class PlaylistEditActivity : AppCompatActivity() {

    companion object {  //Mainスレッドでdatabase操作するとエラーになる
        //lateinit var db1: PlaylistDatabase
        lateinit var db2: MusicDatabase
        lateinit var db3: MiddlelistDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_edit)

        val db2 = MusicDatabase.getInstance(this)    //PlayListのDB作成
        val db2Dao = db2.MusicDao()  //Daoと接続
        val db3 = MiddlelistDatabase.getInstance(this)
        val db3Dao = db3.MiddlelistDao()

        //インテント元からプレイリスト番号を取得
        val playlistId :Int?= intent.getStringExtra("playlist_id")?.toIntOrNull()

        val musicDataList = db2Dao.getAll()

        // RecyclerViewの取得
        val recyclerView2 = findViewById<RecyclerView>(R.id.RecyclerView2)
        // LayoutManagerの設定
        recyclerView2.layoutManager = LinearLayoutManager(this)
        // CustomAdapterの生成と設定
        recyclerView2.adapter=RecyclerListAdapter(musicDataList, db2Dao, db3Dao,playlistId)

        //完了ボタンクリック時
        val finishButton = findViewById<Button>(R.id.finishButton)
        finishButton.setOnClickListener {
            val intent2MenuThanks = Intent(this@PlaylistEditActivity, PlaylistPlayActivity::class.java)
            intent2MenuThanks.putExtra("playlist_id",intent.getStringExtra("playlist_id"))  //インテント先へ再生リスト番号を渡す
            startActivity(intent2MenuThanks)
        }

        //読み込みボタンクリック時
        val inputButton = findViewById<Button>(R.id.input)
        inputButton.setOnClickListener {
            //デフォルト音楽の登録
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

                    db2Dao.insertMusic(storage_id,title,artist)

                }
            }
            val musicDataList = db2Dao.getAll()

            // RecyclerViewの取得
            val recyclerView2 = findViewById<RecyclerView>(R.id.RecyclerView2)
            // LayoutManagerの設定
            recyclerView2.layoutManager = LinearLayoutManager(this)
            // CustomAdapterの生成と設定
            recyclerView2.adapter=RecyclerListAdapter(musicDataList, db2Dao, db3Dao,playlistId)
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val musicTitle:TextView
        val checkBox:CheckBox
        init{
            musicTitle = view.findViewById(R.id.musicTitle)
            checkBox = view.findViewById(R.id.checkBox)
        }
    }

    private inner class RecyclerListAdapter(private val musicDataList: List<Music>, private val db2Dao: MusicDao, private val db3Dao: MiddlelistDao,private val playlist_id: Int?):
        RecyclerView.Adapter<PlaylistEditActivity.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistEditActivity.ViewHolder {
            //レイアウトインフレータを取得
            val inflater = LayoutInflater.from(this@PlaylistEditActivity)
            //row.xmlをインフレ―トし、1行分の画面部品とする
            val view = inflater.inflate(R.layout.playlist_edit_row, parent, false)
            //ビューホルダオブジェクトを生成
            val holder = ViewHolder(view)
            //生成したビューホルダをリターン
            return holder
        }

        override fun onBindViewHolder(holder: PlaylistEditActivity.ViewHolder, position: Int) {
            //リストデータから該当1行分のデータを取得
            val item = musicDataList[position]
            //メニュー名文字列を取得
            val musicTitle = item.title
            //ビューホルダ中のTextViewに設定
            holder.musicTitle.text = musicTitle
            //すでに登録されてる曲は最初にチェックをつける
            val defaultList = db3Dao.getPlaylist(playlist_id)   //もともと登録されてる曲一覧を取得
            if(defaultList.contains(item.backend_id)){
                holder.checkBox.isChecked = true
            }

            //チェックボックス操作時
            holder.checkBox.setOnCheckedChangeListener{_,isChecked ->
                if(isChecked){  //チェックされたら曲を再生リストに登録
                    db3Dao.insertMusic(playlist_id,item.backend_id)
                }
                else{   //チェック外れたら曲を再生リストから削除
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