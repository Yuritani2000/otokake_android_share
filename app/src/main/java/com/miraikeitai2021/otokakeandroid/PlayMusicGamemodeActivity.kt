package com.miraikeitai2021.otokakeandroid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.miraikeitai2021.otokakeandroid.databinding.ActivityPlayMusicGamemodeBinding
import kotlinx.coroutines.*
import java.lang.Exception
import kotlin.math.abs

/**
 * 将来的にここ変えないといけない．リクエストコードは一緒のファイルにまとめておかないと重複に気付かない．
 */
private const val REQUEST_DISPLAY_SCORE_ACTIVITY = 1002

class PlayMusicGamemodeActivity : AppCompatActivity() {
    val checkMusicUri: CheckMusicUri = CheckMusicUri() //曲のUriを取得するクラス
    private val checkRunBpm: CheckRunBpm = CheckRunBpm() //歩調のbpmを取得するクラス
    private val checkMusicBpm: CheckMusicBpm = CheckMusicBpm() //曲のbpmを取得するクラス
    private var previousDeviceName = "" // 前回地面に足が接したときのデバイス名．重複防止に使う．
    private val PERMISSION_WRITE_EX_STR = 1 //外部ストレージへのパーミッション許可に使用
    private var storageId: Long = -1 //ストレージIdの初期値

    companion object {
        private var pointArray: IntArray = intArrayOf(0, 0, 0) //それぞれ良,可,不可の回数
    }

    private var nowSetFootsteps = "和太鼓" //現在設定している足音
    private var footSoundMap:MutableMap<String, Any> = mutableMapOf<String, Any>() //足音とそのIDの組のMap

    private lateinit var binding: ActivityPlayMusicGamemodeBinding

    private val playMusicGamemode: PlayMusicGamemode = PlayMusicGamemode(this, this) //曲を再生するクラス

    // 左足デバイスと通信してデータを受け取るスレッド
    private var bleConnectionRunnableLeft: BleConnectionRunnable? = null
    // 右足デバイスと通信してデータを受け取るスレッド
    private var bleConnectionRunnableRight: BleConnectionRunnable? = null

    // 曲目データベースのインスタンス
    private val musicDatabase = MusicDatabase.getInstance(this)
    val musicDao = musicDatabase.MusicDao()

    // 曲がまだ再生されていないかどうかを確かめる．
    private var isMusicNotPlayed = true

    // 左足デバイスが接続されているかを示す
    private var isLeftDeviceConnected = false

    // 右足デバイスが接続されているかを示す
    private var isRightDeviceConnected = false

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayMusicGamemodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // アクションバーを非表示にする
        supportActionBar?.hide()

        //ストレージIDをインテント元から取得
        storageId = intent.getLongExtra("storageId",0)
        //ストレージIDが取得できなかった場合，activityを終了する．
        if(storageId < 0){
            Toast.makeText(this,"音楽ファイルを読み込むことができませんでした。", Toast.LENGTH_LONG).show()
            finish()
        }

        onCreateFootstepsMap()

        //************************************************************
        //保存用
//        val strageMusic = StrageMusic()
//        strageMusic.StrageInMusic(this)

        //↓ID検索用

//        //projection: 欲しい情報を定義
//        val projection = arrayOf(
//            MediaStore.Audio.Media._ID,
//        )
//        //上のprojectionとselectionを利用した問い合わせ変数を作製
//        val cursor = contentResolver.query(
//            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, //外部ストレージ
//            projection,
//            null,
//            null, // selectionArgs,
//            null
//        )
//
//        lateinit var contentUri: Uri
//        val text: TextView = findViewById(R.id.textView)
//
//        cursor?.use{
//            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
//
//            while (cursor.moveToNext()) {
//                val id = cursor.getLong(idColumn)
//
//                contentUri = Uri.withAppendedPath(
//                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                    id.toString()
//                )
//
//                //取得したメタデータを確認
//                Toast.makeText(applicationContext,
//                    "id: $id, contentUir: $contentUri"
//                    , Toast.LENGTH_LONG).show()
//                text.setText("id: $id, contentUir: $contentUri")
//                /*
//                Log.d(
//                    TAG, "id: $id, display_name: $displayName, content_uri: $contentUri"
//                )
//                 */
//            }
//        }

        //************************************************************************************
        binding.backButton.setOnClickListener { tappedBackButton() }
        binding.changeFootstepButton.setOnClickListener { view -> tappedChangeFootStepButton(view) }
        // テスト用．本番環境では無効にする必要がある．
        binding.bluetoothButton.setOnClickListener { tappedBluetoothButton() }

        // この記法が文法的にわからない。抽象メソッドの実装をしている？復習が必要
        fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)

        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE){
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // 位置情報の使用許可リクエスト
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_ACCESS_FINE_LOCATION)
        }

        // 外部ストレージへの使用許可リクエスト
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_WRITE_EX_STR)
        }

        // 左側の足裏デバイスの接続ボタンのリスナ登録
        binding.connectLeftDeviceImageButton.setOnClickListener{
            bluetoothAdapter?.let{
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    bleConnectionRunnableLeft = startBleConnection(it, DEVICE_NAME_LEFT)
                }
            }
        }

        // 左側の足裏デバイスの接続ボタンのリスナ登録
        binding.connectRightDeviceImageButton.setOnClickListener{
            bluetoothAdapter?.let{
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    bleConnectionRunnableRight = startBleConnection(it, DEVICE_NAME_RIGHT)
                }
            }
        }

        // シークバーの初期化．1000段階で表示．
        // ゲームモードではユーザーがシークバーを操作できない．
        binding.musicSeekBar.max = 1000
        binding.musicSeekBar.progress = 0

        // バックエンドから曲の情報を取得．曲名，アーティスト名，カバー画像．
        val musicInfo = musicDao.getMusicFromStorageId(storageId)
        musicInfo?.let {
            val firstMusicTitle = musicInfo.title
            val firstMusicArtist = musicInfo.artist
            // TextViewにセット
            binding.musicTitleTextView.text = firstMusicTitle
            binding.musicArtistTextView.text = firstMusicArtist
        }

        // 曲のアルバム画像を取得
        val albumPictureByteArray = getAlbumPictureFromMetadata(storageId)
        albumPictureByteArray?.let{ albumPictureByteArray ->
            val albumPictureBitmap = BitmapFactory.decodeByteArray(albumPictureByteArray, 0, albumPictureByteArray.size)
            albumPictureBitmap?.let { albumPictureBitmap ->
                binding.musicAlbumImageView.setImageBitmap(albumPictureBitmap)
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 100msに1回曲の再生状態を確認してUIを更新するCoroutines
        watchPlayerStatusCoroutine()
    }

    /**
     * スタートボタンがクリックされたときの処理
     */
    private fun tappedStartButton(){
        //曲をスタートする
        val contentUri = checkMusicUri.checkUri(storageId.toInt(), contentResolver)
        playMusicGamemode.startMusic(contentUri)
    }

    /**
     * ストップボタンがクリックされたときの処理
     */
    private fun tappedStopButton(){
        //曲をストップする
        playMusicGamemode.stopMusic()
    }

    /**
     * 前の画面に戻るボタンがタップされたときの処理
     */
    private fun tappedBackButton(){
        tappedStopButton()
        finish()
    }

    /**
     * 足音の変更ボタンがタップされたときの処理
     */
    private fun tappedChangeFootStepButton(v: View){
        val footStepChangePopup = PopupMenu(this, v)
        val inflater: MenuInflater = footStepChangePopup.menuInflater
        inflater.inflate(R.menu.menu_options_play_music, footStepChangePopup.menu)
        footStepChangePopup.setOnMenuItemClickListener { item ->
            when(item.itemId){
                R.id.boyon -> {
                    nowSetFootsteps = "ボヨン"
                    true
                }

                R.id.japanese_drum ->{
                    nowSetFootsteps = "和太鼓"
                    true
                }
                else -> false
            }
        }
        footStepChangePopup.show()
    }

    /**
     * 曲のメタデータから画像を持ってくる処理
     */
    private fun getAlbumPictureFromMetadata(storageId: Long): ByteArray?{
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(this, checkMusicUri.checkUri(storageId.toInt(), this.contentResolver))
        var albumPictureByteArray: ByteArray? = null
        if(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) != null) {
            try {
                albumPictureByteArray = mmr.embeddedPicture
            }catch(e: Exception){
                Log.e("debug", "failed to get album picture: $e")
            }
        }
        return albumPictureByteArray
    }

    /**
     * Bluetoothボタンがクリックされたときの処理
     * 今はボタンで割り込んでいるが，Bluetoothの通信による割込みに変えたい
     */
    @SuppressLint("SetTextI18n")
    private fun tappedBluetoothButton(){
        //歩調のBpmによってスコアを算出する．
        val runBpm = checkRunBpm.checkRunBpm(this, storageId.toInt())

        //曲のBpmを取得する
        val musicBpm = checkMusicBpm.checkMusicBpm(this, storageId.toInt())

        //歩調のBpmと曲のBpmの差を算出
        val gapPoint = abs(musicBpm - runBpm)

        if(gapPoint <= 25){
            pointArray[0] += 1
        }else if(gapPoint <= 45){
            pointArray[1] += 1
        }else{
            pointArray[2] += 1
        }
        Log.d("debug", "pointArray[1]: ${pointArray[0]}")
        Log.d("debug", "pointArray[2]: ${pointArray[1]}")
        Log.d("debug", "pointArray[3]: ${pointArray[2]}")
    }

    /**
     * Bluetoothが有効であることを確認する。ためのメソッド．BluetoothAdapterを拡張している？
     */
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    /**
     * 本体のBluetoothの有効化をユーザーに求めた後に呼び出される．
     * また，点数表示画面から処理が戻ってきた際にも呼ばれ，その際にはそのままこのActivityを閉じて曲選択画面に戻る
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_ENABLE_BT -> {
                when(resultCode) {
                    RESULT_OK -> {
                        Toast.makeText(this, R.string.bluetooth_enabled, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, R.string.bluetooth_not_enabled_warning, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            REQUEST_DISPLAY_SCORE_ACTIVITY -> {
                // 点数表示た面から戻ってきた際は，そのままActivityを閉じる．
                finish()
            }
        }
    }

    /**
     * 位置情報の有効化をユーザーに求めた後に呼び出される．
     * 外部ストレージへのアクセス許可をユーザに求めた後に呼び出される.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_ACCESS_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty()) && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.access_fine_location_denied_warning, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            PERMISSION_WRITE_EX_STR -> {
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"外部ストレージへのアクセスを許可しない場合，この機能を使用できません。", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    /**
     * SensorValueHandlerが足と地面の接触を検知したときに呼び出されるメソッド，
     * こちらも，SensorValueHandlerに渡すためにラムダ式にする．
     */
    private val handleFootTouchWithTheGround: (deviceName: String) -> Unit = { deviceName ->
        // 走るときは左右交互に足が出るため，一つ前に同じ足の接触を検知した際は，動作を行わない
        if(deviceName != previousDeviceName) {
            // 現在検知された足デバイス名を，前に検知された足デバイス名とする．これで，次に同じデバイスが接触を検知したとしてもキャンセルする．
            previousDeviceName = deviceName
            // 足が地面についたことを知らせ，曲の速度を変える．
            tappedBluetoothButton()

            // 以下は，足音を鳴らすために必要なオブジェクト群．
            // 再生するサウンドの種類を指定する
            val audioAttributes =
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                ).build()
            // 足音を再生する際にはSoundPoolというクラスを用いる．Builderを用いてオブジェクトを取得．
            val footSoundPool =
                SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(1).build()
            // 再生する足音をres/rawフォルダからもってきて，そのIDを取得．
//            val soundId = footSoundPool.load(this, R.raw.maoo_damashii_bass_drum, 1)
//            // playSoundメソッドは，上記3つのオブジェクトを
            val sound = footSoundMap[nowSetFootsteps] as Int
            val soundId = footSoundPool.load(this, sound, 1)
            playFootSound(footSoundPool, soundId)
            // 足と地面の接触を検知するデバイスが，左足の物か右足の物かにより，処理を変えうる．
            when (deviceName) {
                // 左足デバイスだった場合，左足の接触を検知したことを示すViewを変える．
                DEVICE_NAME_LEFT -> {
                    val notifyLeftFootOnTheGroundView =
                        findViewById<View>(R.id.notify_left_foot_on_the_ground_view)
                    notifyLeftFootOnTheGroundView.setBackgroundColor(Color.parseColor("#ffcccc"))
                    Handler(Looper.getMainLooper()).postDelayed({
                        notifyLeftFootOnTheGroundView.setBackgroundColor(Color.parseColor("#00000000"))
                    }, 500L)
                }
                // 右足デバイスだった場合，右足の接触を検知したことを示すViewを変える．
                DEVICE_NAME_RIGHT -> {
                    val notifyRightFootOnTheGroundView =
                        findViewById<View>(R.id.notify_right_foot_on_the_ground_view)
                    notifyRightFootOnTheGroundView.setBackgroundColor(Color.parseColor("#ffcccc"))
                    Handler(Looper.getMainLooper()).postDelayed({
                        notifyRightFootOnTheGroundView.setBackgroundColor(Color.parseColor("#00000000"))
                    }, 500L)
                }
                // どちらでもなかった場合は，エラーログを表示する．
                else -> {
                    Log.e("debug", "Invalid device name: $deviceName")
                }
            }
        }
    }

    /**
     * 足と地面が接したときに足音の再生を行う．
     */
    private fun playFootSound(footSoundPool: SoundPool, soundId: Int){
        footSoundPool.setOnLoadCompleteListener { soundPool, i, i2 ->
            Log.d("debug", "sound should be played")
            val streamId = soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
            Handler(Looper.getMainLooper()).postDelayed({
                soundPool.stop(streamId)
                soundPool.release()
            }, 500L)
        }
    }

    /**
     * デバイスの接続状態が変化したときに呼ばれるメソッド
     * */
    private val handleOnConnectionStatusChanged = fun(deviceName: String, status: Int){
        // 表示するテキストとその色が入る変数
        var text = ""
        var textColor = 0
        // 接続状態が「切断」か，「接続中」か，「接続済み」かを見分け，入れるテキストと色を指定
        when(status){
            DEVICE_SCANNING -> {
                text = getString(R.string.scanning)
                textColor = Color.BLACK
            }
            DEVICE_DISCONNECTED -> {
                text = getString(R.string.disconnected)
                textColor = Color.BLACK
                when(deviceName){
                    DEVICE_NAME_LEFT -> {
                        isLeftDeviceConnected = false
                    }
                    DEVICE_NAME_RIGHT -> {
                        isRightDeviceConnected = false
                    }
                }
            }
            DEVICE_CONNECTING -> {
                text = getString(R.string.connecting)
                textColor = Color.BLACK
            }
            DEVICE_CONNECTED -> {
                text = getString(R.string.connected)
                textColor = Color.parseColor("#66cdaa")
                when(deviceName){
                    DEVICE_NAME_LEFT -> {
                        isLeftDeviceConnected = true
                    }
                    DEVICE_NAME_RIGHT -> {
                        isRightDeviceConnected = true
                    }
                }
            }
        }
        // 対象のデバイスが左右のどちらかを指定
        when(deviceName){
            DEVICE_NAME_LEFT -> {
                binding.deviceConnectionStatusLeft.text = text
                binding.deviceConnectionStatusLeft.setTextColor(textColor)
            }
            DEVICE_NAME_RIGHT -> {
                binding.deviceConnectionStatusRight.text = text
                binding.deviceConnectionStatusRight.setTextColor(textColor)
            }
        }
    }

    /**
     * デバイスのスキャンを行う．
     */
    private fun startBleConnection(
        bluetoothAdapter: BluetoothAdapter,
        deviceName: String
    ): BleConnectionRunnable {
        val bluetoothConnectionHandler = BluetoothConnectionHandler(handleFootTouchWithTheGround, handleOnConnectionStatusChanged)
        val bleConnectionRunnable = BleConnectionRunnable(this, bluetoothAdapter, deviceName, bluetoothConnectionHandler)
        val bluetoothConnectionThread = Thread(bleConnectionRunnable)
        bluetoothConnectionThread.start()
        return bleConnectionRunnable
    }

    /**
     * Activityが終わるたびに，Bluetoothの接続を切っておく必要がある．
     */
    override fun onDestroy() {
        super.onDestroy()
        bleConnectionRunnableLeft?.disconnect()
        bleConnectionRunnableRight?.disconnect()
        playMusicGamemode.stopMusic()
    }

    /**
     * 足音のファイルをロードして得られるIDと足音の名前を対応付けるMapを作成する
     */
    private fun onCreateFootstepsMap(){
        this.footSoundMap = mutableMapOf<String, Any>(
            "ボヨン" to R.raw.test_boyon,
            "和太鼓" to R.raw.test_japanese_drum
        )
    }

    fun displayScore(){
        Log.d("debug", "displayScore")
        val intent = Intent(this@PlayMusicGamemodeActivity,DisplayScoreActivity::class.java)
        intent.putExtra("pointArray", pointArray)
        intent.putExtra("storageId", storageId)
        startActivityForResult(intent, REQUEST_DISPLAY_SCORE_ACTIVITY)
    }

    fun resetScore(){
        pointArray[0] = 0
        pointArray[1] = 0
        pointArray[2] = 0
        Log.d("debug", "pointArray[1]: ${pointArray[0]}")
        Log.d("debug", "pointArray[2]: ${pointArray[1]}")
        Log.d("debug", "pointArray[3]: ${pointArray[2]}")
    }


    /**
     * Coroutinesという，軽量な非同期処理を行う機構．100ms毎にループして動作するように設定．
     * 曲の曲の進捗に関する処理を行う．
     */
    private fun watchPlayerStatusCoroutine(){
        GlobalScope.launch {
            while(true){
                // 今回はこの中でUIの更新を行いたい．しかしCoroutinesのデフォルトはUIスレッドでないらしいので，UIスレッドでの処理とする．
                withContext(Dispatchers.Main){

                    // 曲の現在の進捗を取得(ミリ秒)
                    val musicProgress = playMusicGamemode.getProgress()
                    // 曲の長さを取得(ミリ秒)
                    val musicDuration = playMusicGamemode.getDuration()

                    if(musicProgress != -1){
                        val minute = musicProgress / 1000 / 60
                        val second = musicProgress / 1000 % 60
                        binding.musicTimeProgressMinuteTextView.text = minute.toString()
                        binding.musicTimeProgressSecondTextView.text = "%02d".format(second)
                    }

                    // 曲の残りの長さを見てUIを更新する．
                    if(musicProgress != -1 && musicDuration != -1){
                        val remainingTimeMilliSeconds = musicDuration - musicProgress
                        if(remainingTimeMilliSeconds >= 0){
                            val remainingMinute = remainingTimeMilliSeconds / 1000 / 60
                            val remainingSecond = remainingTimeMilliSeconds / 1000 % 60
                            binding.musicTimeRemainMinuteTextView.text = remainingMinute.toString()
                            binding.musicTimeRemainSecondTextView.text = "%02d".format(remainingSecond)
                        }
                    }


                    // 再生中は曲の長さに応じてシークバーを操作する．
                    if(playMusicGamemode.isPlaying()) {
                        binding.musicSeekBar.progress =
                            (1000 * (musicProgress.toDouble() / musicDuration.toDouble())).toInt()
                    }

                    // 両方のデバイスの接続が出来ていて，かつ曲がまだ再生されていない状況で，自動で曲の再生を始める．
                    if(isLeftDeviceConnected && isRightDeviceConnected && isMusicNotPlayed){
                        tappedStartButton()
                        isMusicNotPlayed = false
                    }
                    delay(100)
                }
            }
        }
    }
}