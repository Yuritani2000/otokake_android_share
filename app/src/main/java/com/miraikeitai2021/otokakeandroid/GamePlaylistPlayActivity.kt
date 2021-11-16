package com.miraikeitai2021.otokakeandroid

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GamePlaylistPlayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_playlist_play)

        val db4 = GameMusicDatabase.getInstance(this)
        val db4Dao = db4.GameMusicDao()

        var gameMusicDataList = db4Dao.getAll()

        var recyclerView3 = findViewById<RecyclerView>(R.id.recyclerView3)
        // LayoutManagerの設定
        recyclerView3.layoutManager = LinearLayoutManager(this)
        // CustomAdapterの生成と設定
        recyclerView3.adapter=RecyclerListAdapter(gameMusicDataList,db4Dao)

        //戻るボタンの表示
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val inputButton2 = findViewById<Button>(R.id.input2)
        inputButton2.setOnClickListener {
            // 曲のダウンロード成功後に呼ばれるコールバック関数．引数musicListResponseには，曲のデータMusicInfoが入ったListが渡ってくる．
            val handleGetMusicListSuccess = fun(musicListResponse: List<MusicInfo>) {
                musicListResponse.forEach {
                    Log.d("debug", "musicID: ${it.musicID}")
                    Log.d("debug", "musicName: ${it.musicName}")
                    Log.d("debug", "musicArtist: ${it.musicArtist}")
                    Log.d("debug", "musicURL: ${it.musicURL}")

                    // MusicInfoのプロパティは全てnullableなので，DB登録前にnullチェックを行う．
                    // 同時に，その曲がすでにDB上に存在するかを見て，二重に登録することを防ぐ．
                    if (it.musicID != null && it.musicName != null && !db4Dao.getBackendId()
                            .contains(it.musicID)
                    ) {
                        db4Dao.insertHTTPMusic(
                            it.musicID,
                            it.musicName,
                            it.musicArtist,
                            it.musicURL
                        )
                    } else {
                        Log.e("debug", "duplicated data insert")
                    }

                    Log.d("debug", "db: ${db4Dao.getAll()}")
                }

                // データベースに登録し，更新された曲一覧を取得する．
                gameMusicDataList = db4Dao.getAll()

                // RecyclerViewの取得
                recyclerView3 = findViewById(R.id.RecyclerView2)
                // LayoutManagerの設定
                recyclerView3.layoutManager = LinearLayoutManager(this)
                // CustomAdapterの生成と設定
                recyclerView3.adapter = RecyclerListAdapter(gameMusicDataList,db4Dao)

            }
        }
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val musicTitle: TextView = view.findViewById(R.id.gameMusicTitle)
        val constraintLayout: ConstraintLayout = view.findViewById(R.id.gameConstraintLayout)
    }

    private inner class RecyclerListAdapter(private val musicDataList: List<GameMusic>, private val db4Dao: GameMusicDao):
        RecyclerView.Adapter<GamePlaylistPlayActivity.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): GamePlaylistPlayActivity.ViewHolder {
            //レイアウトインフレータを取得
            val inflater = LayoutInflater.from(this@GamePlaylistPlayActivity)
            //row.xmlをインフレ―トし、1行分の画面部品とする
            val view = inflater.inflate(R.layout.game_playlist_play_row, parent, false)
            //生成したビューホルダをリターン
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: GamePlaylistPlayActivity.ViewHolder, position: Int) {
            //リストデータから該当1行分のデータを取得
            val item = musicDataList[position]
            //メニュー名文字列を取得
            val musicTitle = item.title
            //ビューホルダ中のTextViewに設定
            holder.musicTitle.text = musicTitle

            //曲タップ時の処理
            holder.constraintLayout.setOnClickListener {
                //ゲームモードのプレイ画面へインテント
            }

        }

        override fun getItemCount(): Int {
            //リストデータ中の件数をリターン
            return musicDataList.size
        }


    }

}