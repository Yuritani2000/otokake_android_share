package com.miraikeitai2021.otokakeandroid

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class PlaylistPlayActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_play)

        val db1 = PlaylistDatabase.getInstance(this)
        val db1Dao = db1.PlaylistDao()
        val db2 = MusicDatabase.getInstance(this)    //PlayListのDB作成
        val db2Dao = db2.MusicDao()  //Daoと接続
        val db3 = MiddleListDatabase.getInstance(this)
        val db3Dao = db3.MiddleListDao()

        val playlistId :Int = intent.getIntExtra("playlist_id",0)   //インテント元からプレイリスト番号を取得
        val musicList = db3Dao.getRegisteredMusic(playlistId)   //該当の再生リストの情報を取得
        supportActionBar?.title = db1Dao.getTitle(playlistId) //ツールバーのタイトルを変更


        //登録件数0件ならスキップ
        if(db3Dao.count(playlistId)!=0) {
            // RecyclerViewの取得
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
            // LayoutManagerの設定
            recyclerView.layoutManager = LinearLayoutManager(this)
            // CustomAdapterの生成と設定
            recyclerView.adapter = RecyclerListAdapter(musicList, db2Dao, db3Dao, playlistId)
        }

        //戻るボタンの表示
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    //ツールバーの初期化
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_options_musiclist, menu)
        return true
    }

    //ツールバータップ時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {  //戻るボタンクリック時
                finish()
            }
            R.id.edit -> {  //編集ボタンクリック時
                val playlistId :Int = intent.getIntExtra("playlist_id",0)   //インテント元からプレイリスト番号を取得
                val intent = Intent(this, PlaylistEditActivity::class.java)
                intent.putExtra("playlist_id",playlistId)
                startActivityForResult(intent, 9)
            }
        }
        return true
    }



    //このActivity起動時の処理
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != 9) { return }   //戻る以外で起動したとき(その場合はない想定だけど)

        if (resultCode == Activity.RESULT_CANCELED) {   //戻るボタンで起動したとき
            val intent = intent
            finish()
            startActivity(intent)   //このActivityを再起動して画面を更新してる
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val musicTitle: TextView = view.findViewById(R.id.musicTitle2)
        val constraintLayout: ConstraintLayout = view.findViewById(R.id.constraintLayout)
    }

    private inner class RecyclerListAdapter(private val musicList: List<MiddleList>, private val db2Dao: MusicDao, private val db3Dao: MiddleListDao,private val playlist_id: Int):
        RecyclerView.Adapter<PlaylistPlayActivity.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PlaylistPlayActivity.ViewHolder {
            //レイアウトインフレータを取得
            val inflater = LayoutInflater.from(this@PlaylistPlayActivity)
            //row.xmlをインフレ―トし、1行分の画面部品とする
            val view = inflater.inflate(R.layout.playlist_play_row, parent, false)
            //生成したビューホルダをリターン
            return ViewHolder(view)
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