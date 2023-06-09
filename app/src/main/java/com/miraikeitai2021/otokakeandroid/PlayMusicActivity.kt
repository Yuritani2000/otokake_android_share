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
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.miraikeitai2021.otokakeandroid.databinding.ActivityPlayMusicBinding
import kotlinx.coroutines.*
import java.lang.Exception

// ストレージの読込Permissionをリクエストするときのリクエストコード
private const val REQUEST_READ_EXTERNAL_STORAGE = 1001



class PlayMusicActivity : AppCompatActivity() {
    val checkMusicUri: CheckMusicUri = CheckMusicUri() //曲のUriを取得するクラス
    private val checkRunBpm: CheckRunBpm = CheckRunBpm() //歩調のbpmを取得するクラス
    private val checkMusicBpm: CheckMusicBpm = CheckMusicBpm() //曲のbpmを取得するクラス
    private val playMusic: PlayMusic = PlayMusic(this) //曲を再生するクラス
    private var previousDeviceName = "" // 前回地面に足が接したときのデバイス名．重複防止に使う．
    private val playMusicContinue: PlayMusicContinue = PlayMusicContinue() //曲を連続再生するクラス

    private var nowSetFootsteps = "和太鼓" //現在設定している足音
    private var footSoundMap:MutableMap<String, Any> = mutableMapOf<String, Any>() //足音とそのIDの組のMap

    private lateinit var binding: ActivityPlayMusicBinding

    // 左足デバイスと通信してデータを受け取るスレッド
    private var bleConnectionRunnableLeft: BleConnectionRunnable? = null
    // 右足デバイスと通信してデータを受け取るスレッド
    private var bleConnectionRunnableRight: BleConnectionRunnable? = null

    // 曲が再生中か一時停止中かを示すフィールド
    private var isPlayingMusic = false
    // 曲の再生がActivityを起動してから最初に行われるどうか判定する
    private var isFirstTimeToPlay = true

    // 曲目データベースのインスタンス
    private val musicDatabase = MusicDatabase.getInstance(this)
    private val musicDao = musicDatabase.MusicDao()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onCreateFootstepsMap()

        val storageIdList: Array<Long> =
            intent.getSerializableExtra("storageIdList") as Array<Long> //インテント元から配列を取得
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

        binding.musicPlayAndPauseImageButton.setOnClickListener { tappedPlayAndPauseButton(storageIdList) }
        binding.musicRewindImageButton.setOnClickListener { tappedRewindButton() }
        binding.musicSkipImageButton.setOnClickListener { tappedSkipButton() }
        binding.backButton.setOnClickListener { tappedBackButton() }
        binding.changeFootstepButton.setOnClickListener { view -> tappedChangeFootStepButton(view) }
        // テスト用．本番環境では無効にする必要がある．
        binding.bluetoothButton.setOnClickListener { tappedBluetoothButton() }

        // アクションバーの非表示
        supportActionBar?.hide()

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

        // ストレージへのアクセスリクエスト(APIレベル28(Android 9)以下を対象)
        if(Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_EXTERNAL_STORAGE)
        }

        // 左側の足裏デバイスの接続ボタンのリスナ登録
        binding.connectLeftDeviceImageButton.setOnClickListener{
            bluetoothAdapter?.let{
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    bleConnectionRunnableRight = startBleConnection(it, DEVICE_NAME_LEFT)
                }
            }
        }

        // 左側の足裏デバイスの接続ボタンのリスナ登録
        binding.connectRightDeviceImageButton.setOnClickListener{
            bluetoothAdapter?.let{
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    bleConnectionRunnableLeft = startBleConnection(it, DEVICE_NAME_RIGHT)
                }
            }
        }

        // シークバーの初期化．1000段階で表示．
        binding.musicSeekBar.max = 1000
        binding.musicSeekBar.progress = 0

        // シークバーが操作されたときのリスナを登録
        binding.musicSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    onSeekBarProgressChanged(progress)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 100msに1回曲の再生状態を確認してUIを更新するCoroutines
        watchPlayerStatusCoroutine(storageIdList)
    }

    /**
     * スタートボタンと一時停止を兼用したボタンがタップされたときの処理
     */
    private fun tappedPlayAndPauseButton(storageIdList: Array<Long>){
        // APIバージョンが29以上(許可が必要ない)か，ストレージへのアクセス許可が取れている場合のみ音楽を再生
        if(Build.VERSION.SDK_INT >= 29 || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // 曲が停止中かつ，Activityを起動後に初めて再生する場合
            if (isFirstTimeToPlay && !playMusic.isPlaying()) {
                playMusicContinue.orderMusic(storageIdList, this, playMusic)
                isFirstTimeToPlay = false
            } else if (playMusic.isPlaying()) {// 曲が再生中の場合
                // 再生を一時停止する
                playMusic.pauseMusic()
            }else{// 曲が再生中でない場合
                // 曲の再生を再開する
                playMusic.resumeMusic()
            }
        }
    }

    /**
     * 巻き戻しボタンがタップされたときの処理
     */
    private fun tappedRewindButton(){
        // スキップ処理を行う前の再生状態を保存しておく
        val isPlayingBefore = playMusic.isPlaying()
        // 曲を一度も再生したことがない状態で作動しないようにする
        if(playMusic.getMediaPlayer() != null){
            // 始めから2秒以内のところで巻き戻しボタンを押した場合，前の曲を再生．
            if(playMusic.getProgress() <= 2000){
                playMusicContinue.playPreviousTrack(this, playMusic)
            }else{// それ以外は，再生位置を0に戻す
                playMusic.seekTo(0)
            }
        }
        // 処理前に一時停止中であったのなら，再び一時停止する
        if(!isPlayingBefore){
            Handler(Looper.getMainLooper()).postDelayed({
                playMusic.pauseMusic()
            }, 10)
        }
    }

    /**
     * 早送りボタンがタップされたときの処理
     */
    private fun tappedSkipButton(){
        // スキップ処理を行う前の再生状態を保存しておく
        val isPlayingBefore = playMusic.isPlaying()
        // 曲を一度も再生したことがない状態で作動しないようにする
        if(playMusic.getMediaPlayer() != null){
            // 再生中にスキップボタンがタップされたのであれば，次の曲を再生する．
            playMusicContinue.callBackPlayMusic(this, playMusic)
        }

        // 処理前に一時停止中であったのなら，再び一時停止する．
        if(!isPlayingBefore){
            Handler(Looper.getMainLooper()).postDelayed({
                playMusic.pauseMusic()
            }, 10)
        }
    }

    /**
     * ストップボタンがクリックされたときの処理
     */
    private fun tappedStopButton(){
        //曲をストップする
        // APIバージョンが29以上(許可が必要ない)か，ストレージへのアクセス許可が取れている場合のみ音楽を停止
        if(Build.VERSION.SDK_INT >= 29 || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            playMusic.stopMusic()
        }
    }

    /**
     * シークバーが操作されたときの処理
     */
    private fun onSeekBarProgressChanged(progress: Int){
        // SeekBar上のprogressは千分率なので，割合に戻す．
        val progressRate = progress.toDouble() / 1000
        val nextProgress = (playMusic.getDuration() * progressRate).toInt()
        if(playMusic.getDuration() != -1){
            playMusic.seekTo(nextProgress)
        }
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
        // APIバージョンが29以上(許可が必要ない)か，ストレージへのアクセス許可が取れている場合のみ動作する
        if(Build.VERSION.SDK_INT >= 29 || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
          if(playMusic.getMediaPlayer() != null && playMusic.isPlaying()){ //変更箇所 音楽再生前に，bluetoothボタンを押すときの誤動作を避ける．また，一時停止中に曲の速度を変更して曲の再生が再び始まってしまうのを避ける．
              //ストレージIDを取得
              val musicId = playMusicContinue.getStorageId()

              //歩調のBpmによって曲の再生速度を変更する
              val runBpm = checkRunBpm.checkRunBpm(this, musicId.toInt())
              playMusic.changeSpeedMusic(runBpm,checkMusicBpm.checkMusicBpm(this, musicId.toInt()))

              // BPMのTextViewをRunBPMで更新
              binding.runBpmValueTextView.text = "%.1f".format(runBpm)
          }
        }
    }

    /**
     * Bluetoothが有効であることを確認する。ためのメソッド．BluetoothAdapterを拡張している？
      */
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    /**
     * 本体のBluetoothの有効化をユーザーに求めた後に呼び出される．
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
            REQUEST_READ_EXTERNAL_STORAGE -> {
                if((grantResults.isNotEmpty()) && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, R.string.read_external_storage_denied_warning, Toast.LENGTH_SHORT).show()
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
            // playSoundメソッドは，上記3つのオブジェクトを
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
            }
            DEVICE_CONNECTING -> {
                text = getString(R.string.connecting)
                textColor = Color.BLACK
            }
            DEVICE_CONNECTED -> {
                text = getString(R.string.connected)
                textColor = Color.parseColor("#66cdaa")
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
     * Activityが破棄されるときに呼び出されるメソッド．曲の再生を止めると同時に，Bluetoothの接続を切断する．
     */
    override fun onDestroy() {
        super.onDestroy()
        bleConnectionRunnableLeft?.disconnect()
        bleConnectionRunnableRight?.disconnect()
        playMusic.stopMusic()
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
        // この関数を呼び出すボタンの上に，ポップアップメニューとして足跡の一覧を表示する．
        footStepChangePopup.show()
    }

    /**
     * Coroutinesという，軽量な非同期処理を行う機構．100ms毎にループして動作するように設定．
     * 曲の再生，停止，一時停止，曲の進捗等に関する処理を行う．
     */
    private fun watchPlayerStatusCoroutine(storageIdList: Array<Long>){
        // 不必要な時にUIの更新を行わないように，前の状態と比較する為の変数．
        var previousPlayState = playMusic.isPlaying()
        // 不必要なクエリやUIの更新を行わないように，前の，再生していた曲の状態を見るための変数．
        var previousMusicOrder = -1
        GlobalScope.launch {
            while(true){
                // 今回はこの中でUIの更新を行いたい．しかしCoroutinesのデフォルトはUIスレッドでないらしいので，UIスレッドでの処理とする．
                withContext(Dispatchers.Main){

                    // 曲の現在の進捗を取得(ミリ秒)
                    val musicProgress = playMusic.getProgress()
                    // 曲の長さを取得(ミリ秒)
                    val musicDuration = playMusic.getDuration()

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

                    // 曲が再生中かどうかを見て再生/一時停止ボタンのUIを変更する

                    if(previousPlayState != playMusic.isPlaying()){
                        binding.musicPlayAndPauseImageButton.background = if(playMusic.isPlaying()) getDrawable(R.drawable.button_play_pause) else getDrawable(R.drawable.button_play_play)
                    }
                    previousPlayState = playMusic.isPlaying()

                    // 再生中は曲の長さに応じてシークバーを操作する．
                    if(playMusic.isPlaying()) {
                        binding.musicSeekBar.progress =
                            (1000 * (musicProgress.toDouble() / musicDuration.toDouble())).toInt()
                    }

                    if(previousMusicOrder != playMusicContinue.getOrder()){
                        // 現在再生中の曲の情報を取得
                        // ここで例外（配列範囲外）が起こっている．原因を突き止め対処する
                        Log.d("debug", "current order: ${playMusicContinue.getOrder()}")
                        Log.d("debug", "storageIdList size: ${storageIdList.size}")
                        if(playMusicContinue.getOrder() in 0..storageIdList.lastIndex){
                            val firstMusicInfo = musicDao.getMusicFromStorageId(storageIdList[playMusicContinue.getOrder()])
                            firstMusicInfo?.let {
                                val firstMusicTitle = firstMusicInfo.title
                                val firstMusicArtist = firstMusicInfo.artist
                                // TextViewにセット
                                binding.musicTitleTextView.text = firstMusicTitle
                                binding.musicArtistTextView.text = firstMusicArtist
                            }

                            // 曲のアルバム画像を取得
                            val albumPictureByteArray = getAlbumPictureFromMetadata(storageIdList[playMusicContinue.getOrder()])
                            albumPictureByteArray?.let{ albumPictureByteArray ->
                                val albumPictureBitmap = BitmapFactory.decodeByteArray(albumPictureByteArray, 0, albumPictureByteArray.size)
                                albumPictureBitmap?.let { albumPictureBitmap ->
                                    binding.musicAlbumImageView.setImageBitmap(albumPictureBitmap)
                                }
                            }
                        }else{
                            Log.e("debug", "array out of bound")
                        }
                    }
                    previousMusicOrder = playMusicContinue.getOrder()
                    delay(100)
                }
            }
        }
    }
}