package com.miraikeitai2021.otokakeandroid

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import com.miraikeitai2021.otokakeandroid.databinding.ActivityDisplayScoreBinding

class DisplayScoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisplayScoreBinding



    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //インテント元から値受け取り
        val pointArray = intent.getIntArrayExtra("pointArray")
        val storageId = intent.getLongExtra("storageId", -1)

        //曲目データベースを使用
        val musicDatabase = MusicDatabase.getInstance(this)
        val musicDao = musicDatabase.MusicDao()  //Daoと接続

        //アクションバー非表示
        val actionBar: androidx.appcompat.app.ActionBar? = supportActionBar
        actionBar?.hide()

        //ストレージIdが取得できなかった時はActivityを終了する．
        if(storageId.toInt() == -1){
            Toast.makeText(this, R.string.game_score_get_storage_id_string, Toast.LENGTH_SHORT).show()
            finish()
        }

        pointArray?.let{
            val bestPoint = it[0]
            val normalPoint = it[1]
            val badPoint = it[2]

            val score = bestPoint * 500 + normalPoint * 200

            binding.scoreTextView.text = "$score"
            binding.perfectScoreTextView.text = "$bestPoint"
            binding.goodScoreTextView.text = "$normalPoint"
            binding.missScoreTextView.text = "$badPoint"
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //メタデータからジャケット画像を設定
        val storageMusic = StorageMusic()
        binding.imageView2.setImageBitmap(storageMusic.getImage(storageId,this))

        //曲目テーブルからストレージIDを基に曲データを取得
        val musicInfo = musicDao.getMusicFromStorageId(storageId)
        musicInfo?.let{
            //曲名を設定
            binding.textView2.text = musicInfo.title

            //アーティストを設定
            binding.textView5.text = musicInfo.artist
        }

        //OKボタンを押したときの処理
        binding.scoreBackButtonView.setOnClickListener{
            finish()
        }
    }

    //アクションバーの戻るボタンを押したときの処理(無効中)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}