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
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.miraikeitai2021.otokakeandroid.databinding.ActivityPlayMusicGamemodeBinding
import kotlin.math.abs

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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayMusicGamemodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.startButton.setOnClickListener { tappedStartButton() }
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

        // 外部ストレージへの使用許可リクエスト
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_WRITE_EX_STR)
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
    private fun tappedStartButton(){
        //曲をスタートする
        val text: TextView = findViewById(R.id.textView)
        val contentUri = checkMusicUri.checkUri(storageId.toInt(), contentResolver)
        text.setText(contentUri.toString())
        playMusicGamemode.startMusic(contentUri)
        //Toast.makeText(applicationContext, "Start", Toast.LENGTH_SHORT).show()
    }

    /**
     * ストップボタンがクリックされたときの処理
     */
    private fun tappedStopButton(){
        //曲をストップする
        playMusicGamemode.stopMusic()
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



//        //歩調のBpmによって曲の再生速度を変更する
//        playMusicGamemode.changeSpeedMusic(checkRunBpm.checkRunBpm(this, musicId),checkMusicBpm.checkMusicBpm(this, musicId))
//
//        val text: TextView = findViewById(R.id.textView)
//        text.setText("musicBpm: ${checkMusicBpm.getMusicBpms()}  " +
//                "runBpm: ${checkRunBpm.getRunBpm()}  " +
//                "musicSpeed: ${playMusicGamemode.getChangedMusicSpeed()}  ")
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
            PERMISSION_WRITE_EX_STR -> {
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"外部ストレージへのアクセスを許可しない場合，この機能を使用できません。", Toast.LENGTH_LONG).show()
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
        //変更箇所=========================================
        val playMusicActivity = PlayMusicActivity()
        val leScanCallback = LeScanCallback(playMusicActivity, bluetoothAdapter.bluetoothLeScanner, sensorValueHandler)
        //==================================================
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

    fun displayScore(){
        Log.d("debug", "displayScore")
        val intent = Intent(this@PlayMusicGamemodeActivity,DisplayScoreActivity::class.java)
        intent.putExtra("pointArray", pointArray)
        startActivity(intent)
    }

    fun resetScore(){
        pointArray[0] = 0
        pointArray[1] = 0
        pointArray[2] = 0
        Log.d("debug", "pointArray[1]: ${pointArray[0]}")
        Log.d("debug", "pointArray[2]: ${pointArray[1]}")
        Log.d("debug", "pointArray[3]: ${pointArray[2]}")
    }
}