package com.miraikeitai2021.otokakeandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream
import java.security.Permission

const val HANDLE_MUSIC_DOWNLOAD_PROGRESS = 200

class PlaylistEditActivity : AppCompatActivity() {

    private val PERMISSION_WRITE_EX_STR = 1

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

        //パーミッション許可をとる
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_WRITE_EX_STR
            )
        }

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
            // 未ダウンロードの曲をダウンロードする際に表示するダイアログ．
            var musicDownloadDialog: MusicDownloadDialog? = null

            // 曲をダウンロードするHTTPリクエストに使用するオブジェクト．
            var musicRepository: MusicRepository? = null

            //リストデータから該当1行分のデータを取得
            val item = musicDataList[position]
            //メニュー名文字列を取得
            val musicTitle = item.title
            //ビューホルダ中のTextViewに設定
            holder.musicTitle.text = musicTitle
            //すでに登録されてる曲は最初にチェックをつける
            holder.checkBox.isChecked = defaultList.contains(item.backend_id)

            // 曲のダウンロードが終了した後に呼ばれるコールバック関数．音楽ファイルをストレージに保存する．
            val downloadMusicSuccessCallback = fun(inputStream: InputStream){
                Log.d("debug", "callback called: $inputStream")
                val storageMusic = StorageMusic()
                if(Build.VERSION.SDK_INT >= 29 || ContextCompat.checkSelfPermission(this@PlaylistEditActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Log.d("debug", "save music in storage");
                    storageMusic.storageInMusic(
                        this@PlaylistEditActivity,
                        inputStream,
                        item.backend_id,
                        "otokake_${item.backend_id}.mp3"
                    )

                    val storageId = storageMusic.checkStorageId(this@PlaylistEditActivity)
                    Log.d("debug", "new music storage id: $storageId");
                    if(storageId != -1L){
                        val db2 = MusicDatabase.getInstance(this@PlaylistEditActivity)
                        val db2Dao = db2.MusicDao()
                        //ストレージIDをデータベースへ登録
                        db2Dao.updateStorageId(item.backend_id,storageId)
                    }else{
                        Log.e("debug", "could not get ");
                    }
                }else{
                    Log.e("debug", "external storage write is not allowed")
                }
            }

            //
            val updateMusicDownloadProgressBar = fun(progressPercentage: Int){
                musicDownloadDialog?.onProgressUpdated(progressPercentage)
            }

            var previousPercentage = 0
            // 曲のダウンロードの進捗が変わるたびに呼ばれるコールバック
            val onDownloadMusicProgressUpdated = fun(progressPercentage: Int){
                if(previousPercentage != progressPercentage) {
                    Log.d("debug", "ダウンロード中... $progressPercentage%")
                    MusicDownloadProgressHandler(updateMusicDownloadProgressBar).obtainMessage(
                        HANDLE_MUSIC_DOWNLOAD_PROGRESS, progressPercentage, -1).sendToTarget()
                    previousPercentage = progressPercentage
                }
            }

            // ダイアログのキャンセルボタンが押されたときに呼び出される関数．DBへの登録解除と，チェックボックスをはずす動作をする．
            val onClickMusicDownloadDialogCancelButton = fun(){
                // DBの中間テーブルから削除
                db3Dao.deleteMusic(playlist_id,item.backend_id)
                holder.checkBox.isChecked = false
                musicRepository?.cancelDownloadingMusic()
            }

            val onClickCheckBox = fun(){
                if(!holder.checkBox.isChecked){  //チェック入ってない(登録)時
                    // DBの中間テーブルへ登録
                    db3Dao.insertMusic(playlist_id,item.backend_id)
                    holder.checkBox.isChecked = true
                    Log.d("debug", "element tapped in PlaylistEditActivity")

                    // 曲がまだダウンロードされていない(item.storage_idがnull)の時は，タップされた曲をAmazon S3からダウンロードする．
                    if(item.storage_id == null){
                        // 曲をダウンロードする際のダイアログを表示
                        musicDownloadDialog = MusicDownloadDialog(onClickMusicDownloadDialogCancelButton)
                        musicDownloadDialog?.show(supportFragmentManager, "music_downloading_dialog")
                        Log.d("debug", "start downloading")
                        val db2 = MusicDatabase.getInstance(this@PlaylistEditActivity)    //PlayListのDB作成
                        val db2Dao = db2.MusicDao()  //Daoと接続
                        musicRepository = MusicRepository()
                        musicRepository?.let{
                            val musicViewModel = MusicViewModel(it, db2Dao)
                            item.url?.let{
                                musicViewModel.downloadMusic(it, downloadMusicSuccessCallback, onDownloadMusicProgressUpdated)
                            }
                        }
                    }
                }
                else{   //チェック入ってる(削除)時
                    // DBの中間テーブルから削除
                    db3Dao.deleteMusic(playlist_id,item.backend_id)
                    holder.checkBox.isChecked = false
                }
            }

            //曲タップ時の処理
            holder.constraintLayout.setOnClickListener {
                onClickCheckBox()
            }

            //チェックボックスタップ時の処理
            holder.checkBox.setOnClickListener {
                onClickCheckBox()
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
        if(grantResults.size <= 0){return}
        when(requestCode){
            //許可されなかった場合はアプリを止める
            PERMISSION_WRITE_EX_STR -> if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"アプリを起動できません", Toast.LENGTH_LONG).show()
                finish()
            }
            else -> return
        }

    }
}

/**
 * 曲のダウンロードの進捗を報告するメソッドは別スレッドで呼ばれる．別スレッド上でUIを更新することは許されていないため，
 * 一旦ダウンロード進捗を報告するメソッドが呼ばれたスレッドからUI更新可能なメインスレッドに値を渡してやる必要がある．
 * その処理を担うのがこのHandler.
  */
open class MusicDownloadProgressHandler(
    private val updateMusicDownloadProgressBar: (progressPercentage: Int) -> Unit
): Handler(Looper.getMainLooper()){
    override fun handleMessage(msg: Message){
        when(msg.what){
            HANDLE_MUSIC_DOWNLOAD_PROGRESS -> {
                updateMusicDownloadProgressBar(msg.arg1)
            }
        }
    }
}