package com.miraikeitai2021.otokakeandroid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.miraikeitai2021.otokakeandroid.databinding.ActivityPlayMusicBinding

private const val REQUEST_READ_EXTERNAL_STORAGE = 1001

class PlayMusicActivity : AppCompatActivity() {
    val checkMusicUri: CheckMusicUri = CheckMusicUri() //曲のUriを取得するクラス
    private val checkRunBpm: CheckRunBpm = CheckRunBpm() //歩調のbpmを取得するクラス
    private val checkMusicBpm: CheckMusicBpm = CheckMusicBpm() //曲のbpmを取得するクラス
    private val playMusic: PlayMusic = PlayMusic(this) //曲を再生するクラス
    //private val musicId: Int = 12248 //保存したときに確認したIDの値を入れておく．
    private var previousDeviceName = "" // 前回地面に足が接したときのデバイス名．重複防止に使う．
    private val playMusicContinue: PlayMusicContinue = PlayMusicContinue() //曲を連続再生するクラス
    private val PERMISSION_WRITE_EX_STR = 1 //外部ストレージへのパーミッション許可に使用する．


    private var nowSetFootsteps = "和太鼓" //現在設定している足音
    private var footSoundMap:MutableMap<String, Any> = mutableMapOf<String, Any>() //足音とそのIDの組のMap

    private lateinit var binding: ActivityPlayMusicBinding

    // 左足デバイスと通信してデータを受け取るスレッド
    private var bleConnectionRunnableLeft: BleConnectionRunnable? = null
    // 右足デバイスと通信してデータを受け取るスレッド
    private var bleConnectionRunnableRight: BleConnectionRunnable? = null

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

        binding.startButton.setOnClickListener { tappedStartButton(storageIdList) }
        binding.stopButton.setOnClickListener { tappedStopButton() }
        binding.bluetoothButton.setOnClickListener{ tappedBluetoothButton()}


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

        val searchLeftDeviceButton = findViewById<Button>(R.id.search_device_button_left)
        searchLeftDeviceButton.setOnClickListener {
            bluetoothAdapter?.let{
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    bleConnectionRunnableLeft = startBleConnection(it, DEVICE_NAME_LEFT)
                }
            }
        }

        val searchRightDeviceButton = findViewById<Button>(R.id.search_device_button_right)
        searchRightDeviceButton.setOnClickListener {
            bluetoothAdapter?.let{
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    bleConnectionRunnableRight = startBleConnection(it, DEVICE_NAME_RIGHT)
                }
            }
        }

        val disconnectLeftDeviceButton = findViewById<Button>(R.id.disconnect_device_button_left)
        disconnectLeftDeviceButton.setOnClickListener {
            bleConnectionRunnableLeft?.disconnect()
        }

        val disconnectRightDeviceButton = findViewById<Button>(R.id.disconnect_device_button_right)
        disconnectRightDeviceButton.setOnClickListener {
            bleConnectionRunnableLeft?.disconnect()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * スタートボタンがクリックされたときの処理
     */
    private fun tappedStartButton(storageIdList: Array<Long>){
        // APIバージョンが29以上(許可が必要ない)か，ストレージへのアクセス許可が取れている場合のみ音楽を再生
        if(Build.VERSION.SDK_INT >= 29 || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            playMusicContinue.orderMusic(storageIdList, this, playMusic)
        }
//        //曲をスタートする
//        val text: TextView = findViewById(R.id.textView)
//        val contentUri = checkMusicUri.checkUri(musicId, contentResolver)
//        text.setText(contentUri.toString())
//        playMusic.startMusic(contentUri)
        //Toast.makeText(applicationContext, "Start", Toast.LENGTH_SHORT).show()
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
     * Bluetoothボタンがクリックされたときの処理
     * 今はボタンで割り込んでいるが，Bluetoothの通信による割込みに変えたい
     */
    @SuppressLint("SetTextI18n")
    private fun tappedBluetoothButton(){
        // APIバージョンが29以上(許可が必要ない)か，ストレージへのアクセス許可が取れている場合のみ動作する
        if(Build.VERSION.SDK_INT >= 29 || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
          if(playMusic.getMediaPlayer() != null){ //変更箇所 音楽再生前に，bluetoothボタンを押すときの誤動作を避ける
              //ストレージIDを取得
              val musicId = playMusicContinue.getStorageId()

              //歩調のBpmによって曲の再生速度を変更する
              playMusic.changeSpeedMusic(checkRunBpm.checkRunBpm(this, musicId.toInt()),checkMusicBpm.checkMusicBpm(this, musicId.toInt()))

              val text: TextView = findViewById(R.id.textView)
              text.setText("musicBpm: ${checkMusicBpm.getMusicBpms()}  " +
                      "runBpm: ${checkRunBpm.getRunBpm()}  " +
                      "musicSpeed: ${playMusic.getChangedMusicSpeed()}  ")
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
     * SensorValueHandlerから圧力の値を受け取った時，圧力の計測値を示すTextViewを更新するラムダ式．
     * ラムダ式である理由は，SensorValueHandlerのメンバとして渡すため．
     */
    private val updateSensorValueTextView: (Int, Int) -> Unit = { positionId, sensorValue ->
        when(positionId){
            SENSOR_LEFT_1 ->{
                val sensorValueLeft1TextView = findViewById<TextView>(R.id.sensor_value_left_1_text_view)
                sensorValueLeft1TextView.text = sensorValue.toString()
            }
            SENSOR_LEFT_2 ->{
                val sensorValueLeft2TextView = findViewById<TextView>(R.id.sensor_value_left_2_text_view)
                sensorValueLeft2TextView.text = sensorValue.toString()
            }
            SENSOR_RIGHT_1 ->{
                val sensorValueRight1TextView = findViewById<TextView>(R.id.sensor_value_right_1_text_view)
                sensorValueRight1TextView.text = sensorValue.toString()
            }
            SENSOR_RIGHT_2 ->{
                val sensorValueRight2TextView = findViewById<TextView>(R.id.sensor_value_right_2_text_view)
                sensorValueRight2TextView.text = sensorValue.toString()
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
     * デバイスのスキャンを行う．
     */
    private fun startBleConnection(
        bluetoothAdapter: BluetoothAdapter,
        deviceName: String
    ): BleConnectionRunnable {
        val sensorValueHandler = SensorValueHandler(updateSensorValueTextView, handleFootTouchWithTheGround)
        val leScanCallback = LeScanCallback(this, bluetoothAdapter.bluetoothLeScanner, sensorValueHandler)
        val bleConnectionRunnable = BleConnectionRunnable(bluetoothAdapter, deviceName, leScanCallback)
        val bluetoothConnectionThread = Thread(bleConnectionRunnable)
        bluetoothConnectionThread.start()
        return bleConnectionRunnable
    }

    /**
     * Activityが終わるたびに，Bluetoothの接続を切っておく必要がある．
     */
    override fun onPause() {
        super.onPause()
        bleConnectionRunnableLeft?.disconnect()
        bleConnectionRunnableRight?.disconnect()
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
     * メニューバーを実現するためのメソッド
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options_play_music, menu)
        return true
    }


    /**
     * メニューバーを押した時に呼ばれるメソッド
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var returnVal = true

        val footstepsText = findViewById<TextView>(R.id.nowFootsteps)

        when(item.itemId) {
            android.R.id.home -> {
                finish()
            }

            R.id.boyon -> {
                nowSetFootsteps = "ボヨン"
                footstepsText.text = nowSetFootsteps
            }

            R.id.japanese_drum ->{
                nowSetFootsteps = "和太鼓"
                footstepsText.text = nowSetFootsteps
            }

            else -> {
                returnVal = super.onOptionsItemSelected(item)
            }
        }

        return returnVal
    }
}