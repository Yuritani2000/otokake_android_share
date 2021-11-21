package com.miraikeitai2021.otokakeandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater

import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream

class GamePlaylistPlayActivity : AppCompatActivity() {

    private val PERMISSION_WRITE_EX_STR = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_playlist_play)

        val db2 = MusicDatabase.getInstance(this)
        val db2Dao = db2.MusicDao()

        var musicDataList = db2Dao.getAll()

        var recyclerView3 = findViewById<RecyclerView>(R.id.recyclerView3)
        // LayoutManagerの設定
        recyclerView3.layoutManager = LinearLayoutManager(this)
        // CustomAdapterの生成と設定
        recyclerView3.adapter = RecyclerListAdapter(musicDataList, db2Dao)

        //戻るボタンの表示
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //パーミッション許可をとる
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_WRITE_EX_STR
            )
        }


        //読み込みボタンクリック時(将来的に削除予定)
        val inputButton = findViewById<Button>(R.id.input2)
        inputButton.setOnClickListener {

            // 曲のダウンロード成功後に呼ばれるコールバック関数．引数musicListResponseには，曲のデータMusicInfoが入ったListが渡ってくる．
            val handleGetMusicListSuccess = fun(musicListResponse: List<MusicInfo>) {
                musicListResponse.forEach {
                    Log.d("debug", "musicID: ${it.musicID}")
                    Log.d("debug", "musicName: ${it.musicName}")
                    Log.d("debug", "musicArtist: ${it.musicArtist}")
                    Log.d("debug", "musicURL: ${it.musicURL}")

                    // MusicInfoのプロパティは全てnullableなので，DB登録前にnullチェックを行う．
                    // 同時に，その曲がすでにDB上に存在するかを見て，二重に登録することを防ぐ．
                    if (it.musicID != null && it.musicName != null && !db2Dao.getBackendId()
                            .contains(it.musicID)
                    ) {
                        db2Dao.insertHTTPMusic(
                            it.musicID,
                            it.musicName,
                            it.musicArtist,
                            it.musicURL
                        )
                    } else {
                        Log.e("debug", "duplicated data insert")
                    }
                    Log.d("debug", "db: ${db2Dao.getAll()}")
                }

                // データベースに登録し，更新された曲一覧を取得する．
                musicDataList = db2Dao.getAll()

                // RecyclerViewの取得
                recyclerView3 = findViewById(R.id.recyclerView3)
                // LayoutManagerの設定
                recyclerView3.layoutManager = LinearLayoutManager(this)
                // CustomAdapterの生成と設定
                recyclerView3.adapter = RecyclerListAdapter(musicDataList, db2Dao)

                Log.d("debug", "コールバック呼ばれました")
            }

            // 曲一覧の取得に失敗したときに呼ばれるコールバック．エラー内容をダイアログに表示する．
            val handleGetMusicListFailed = fun(exception: Exception) {
                Log.e("debug", "failed to get music list from backend.")
                AlertDialog.Builder(this@GamePlaylistPlayActivity)
                    .setTitle(R.string.get_music_list_failed_title)
                    .setMessage("${getString(R.string.get_music_list_failed_message)}\n エラー詳細: ${exception.toString()}")
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }

            // HTTP通信を行っているスレッドから情報を受け取るためのHandlerを呼び出す．
            val getMusicListHandler = GetMusicListHandler(
                handleGetMusicListFailed,
                handleGetMusicListSuccess
            )

            // 曲の一覧を取得するHTTPリクエスト
            val musicHttpRequests = MusicHttpRequests()
            musicHttpRequests.requestGetMusicList(getMusicListHandler)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { //戻るボタンクリック時
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val musicTitle: TextView = view.findViewById(R.id.gameMusicTitle)
        val constraintLayout: ConstraintLayout = view.findViewById(R.id.gameConstraintLayout)
    }

    private inner class RecyclerListAdapter(
        private val musicDataList: List<Music>,
        private val db2Dao: MusicDao
    ) :
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
            // 未ダウンロードの曲をダウンロードする際に表示するダイアログ．
            var musicDownloadDialog: MusicDownloadDialog? = null

            // 曲をダウンロードするHTTPリクエストに使用するオブジェクト．
            var musicHttpRequests: MusicHttpRequests? = null

            //リストデータから該当1行分のデータを取得
            val item = musicDataList[position]
            //メニュー名文字列を取得
            val musicTitle = item.title
            //ビューホルダ中のTextViewに設定
            holder.musicTitle.text = musicTitle

            // 曲のダウンロードが終了した後に呼ばれるコールバック関数．音楽ファイルをストレージに保存する．
            val handleMusicDownloadSuccess = fun(inputStream: InputStream) {
                Log.d("debug", "callback called: $inputStream")
                val storageMusic = StorageMusic()
                // APIレベルが29以上である（ファイル書き出し許可が必要ない）か，ファイル書き出し許可が取れている場合のみ，曲のストレージへの保存処理を行う．
                if (Build.VERSION.SDK_INT >= 29 || ContextCompat.checkSelfPermission(
                        this@GamePlaylistPlayActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("debug", "save music in storage")
                    storageMusic.storageInMusic(
                        this@GamePlaylistPlayActivity,
                        inputStream,
                        item.backend_id,
                        "otokake_${item.backend_id}.mp3"
                    )

                    // 保存した曲のストレージIDを保存する．
                    val storageId = storageMusic.checkStorageId(this@GamePlaylistPlayActivity)
                    Log.d("debug", "new music storage id: $storageId")
                    if (storageId != -1L) {   // 保存した曲のストレージIDが返って来れば，それをDBへ登録する．
                        val db2 = MusicDatabase.getInstance(this@GamePlaylistPlayActivity)
                        val db2Dao = db2.MusicDao()
                        //ストレージIDをデータベースへ登録
                        db2Dao.updateStorageId(item.backend_id, storageId)
                    } else {
                        Log.e("debug", "could not get ")
                    }
                } else {
                    Log.e("debug", "external storage write is not allowed")
                }
            }

            // 曲のダウンロードの進捗が変わるたびに呼ばれるコールバック
            // ダウンロード進捗が前回の更新と違う場合にのみダイアログの表示内容を更新する．
            var previousPercentage = 0
            val handleMusicDownloadProgressUpdated = fun(progressPercentage: Int) {
                if (previousPercentage != progressPercentage) {
                    Log.d("debug", "ダウンロード中... $progressPercentage%")
                    musicDownloadDialog?.onProgressUpdated(progressPercentage)
                    previousPercentage = progressPercentage
                }
            }

            // 曲のダウンロードが失敗したときに呼ばれるコールバック．エラー内容をダイアログに表示する．
            val handleMusicDownloadFailed = fun(exception: Exception) {
                AlertDialog.Builder(this@GamePlaylistPlayActivity)
                    .setTitle(R.string.download_music_failed_title)
                    .setMessage("${getString(R.string.download_music_failed_message)}\n エラー詳細: $exception")
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }

            // ダイアログのキャンセルボタンが押されたときに呼び出される関数．
            val onClickMusicDownloadDialogCancelButton = fun() {
                musicHttpRequests?.cancelDownloadingMusic()
            }

            //曲タップ時の処理
            holder.constraintLayout.setOnClickListener {
                // 曲がまだダウンロードされていない(item.storage_idがnull)の時は，タップされた曲をAmazon S3からダウンロードする．
                if (db2Dao.tap(item.backend_id) == null) {
                    // 曲をダウンロードする際のダイアログを表示
                    musicDownloadDialog =
                        MusicDownloadDialog(onClickMusicDownloadDialogCancelButton)
                    musicDownloadDialog?.show(supportFragmentManager, "music_downloading_dialog")
                    Log.d("debug", "start downloading")
                    musicHttpRequests = MusicHttpRequests()
                    musicHttpRequests?.let { musicHttpRequests ->
                        item.url?.let {
                            // HTTP通信を行うスレッドから情報をメインスレッドに取得すると，以下のコンストラクタに渡したメソッドが呼び出される．
                            val musicDownloadHandler = MusicDownloadHandler(
                                handleMusicDownloadProgressUpdated,
                                handleMusicDownloadFailed,
                                handleMusicDownloadSuccess
                            )
                            // 実際にHTTPリクエストを送信して曲をダウンロードする．
                            musicHttpRequests.requestDownloadMusic(it, musicDownloadHandler)

                        }
                    }
                }
                else{
                    Log.d("debug","${db2Dao.getAll()}")
                    //インテント処理
                    val intent = Intent(this@GamePlaylistPlayActivity, Example2Activity::class.java)
                    intent.putExtra("storageId",db2Dao.tap(item.backend_id))
                    startActivity(intent)
                }

            }

        }

        override fun getItemCount(): Int {
            //リストデータ中の件数をリターン
            return musicDataList.size
        }

    }


    /**
     * 外部ストレージを使用してよいかについて，ユーザの操作を受け取る
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size <= 0) {
            return
        }
        when (requestCode) {
            //許可されなかった場合はアプリを止める
            PERMISSION_WRITE_EX_STR -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "アプリを起動できません", Toast.LENGTH_LONG).show()
                finish()
            }
            else -> return
        }
    }
}