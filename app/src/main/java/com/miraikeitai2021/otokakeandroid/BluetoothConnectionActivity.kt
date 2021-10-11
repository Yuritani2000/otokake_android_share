package com.miraikeitai2021.otokakeandroid

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.*

// Bluetoothの有効化をリクエストするIntentに関するリクエストコード
private const val REQUEST_ENABLE_BT = 1000
private const val REQUEST_ACCESS_FINE_LOCATION = 1001

// BLE端末をスキャンする時間をさだめる。今回は10秒。
private const val SCAN_PERIOD = 10000L

// BLE端末を検出してから接続するまでのインターバル．この間にデバイスの検索を止める．
private const val CONNECTION_INTERVAL = 500L

// GATT定義ディスクリプタの一つ．である，CCCD(Client Characteristic Configuration Descriptor). 端末にNotiyやIndicateを制御するにはこれが使われる．
private const val CCCD_UUID_STR = "00002902-0000-1000-8000-00805f9b34fb"

/* GattCallbackにおいて，接続を制御する際に使用する定数． */
const val STATE_DISCONNECTED = 0
const val STATE_CONNECTING = 1
const val STATE_CONNECTED = 2
const val CONNECTION_PERIOD = 5000L

/* BLEデバイスの値を処理するスレッドから，UIスレッドに値を渡すときのHandlerで使用する定数 */
private const val SENSOR_VALUE_RECEIVE = 100
private const val FOOT_ON_THE_GROUND = 101

/* BLEデバイスの値をUIスレッドで更新する際，どの値を更新するのかを指定する為のID*/
private const val SENSOR_LEFT_1 = 10000
private const val SENSOR_LEFT_2 = 10001
private const val SENSOR_RIGHT_1 = 10002
private const val SENSOR_RIGHT_2 = 10003

/* 左右それぞれの足裏デバイスの名前， */
private const val DEVICE_NAME_LEFT = "Otokake_Left"
private const val DEVICE_NAME_RIGHT = "Otokake_Right"

open class SensorValueHandler(
    private val updateSensorValueTextView: (positionId: Int, sensorValue: Int) -> Unit,
    private val handleFootTouchWithTheGround: (deviceName: String) -> Unit
): Handler(Looper.getMainLooper()){
    override fun handleMessage(msg: Message) {
        when(msg.what){
            SENSOR_VALUE_RECEIVE -> {
                Log.d("debug", "handler called")
                updateSensorValueTextView(msg.arg1, msg.arg2)
            }
            FOOT_ON_THE_GROUND -> {
                Log.d("debug", "${msg.obj} on the ground")
                handleFootTouchWithTheGround(msg.obj.toString())
            }
        }
    }
}

class BluetoothConnectionActivity : AppCompatActivity() {

    companion object{

    }

    private var bleConnectionRunnableLeft: BleConnectionRunnable? = null
    private var bleConnectionRunnableRight: BleConnectionRunnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_connection)

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

    }

    override fun onPause() {
        super.onPause()
        bleConnectionRunnableLeft?.disconnect()
        bleConnectionRunnableRight?.disconnect()
    }

    // Bluetoothが有効であることを確認する。
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    /* Bluetooth有効化を求めた後に呼び出される関数。 */
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
        }
    }

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

    private val handleFootTouchWithTheGround: (deviceName: String) -> Unit = { deviceName ->
        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val footSoundPool = SoundPool.Builder().setAudioAttributes(audioAttributes).setMaxStreams(1).build()
        val soundId = footSoundPool.load(this, R.raw.maoo_damashii_bass_drum, 1)
        playFootSound(footSoundPool, audioAttributes, soundId)
        when(deviceName){
            DEVICE_NAME_LEFT -> {
                val notifyLeftFootOnTheGroundView =
                findViewById<View>(R.id.notify_left_foot_on_the_ground_view)
                notifyLeftFootOnTheGroundView.setBackgroundColor(Color.parseColor("#ffcccc"))
                Handler(Looper.getMainLooper()).postDelayed({
                    notifyLeftFootOnTheGroundView.setBackgroundColor(Color.parseColor("#ffffff"))
                }, 500L)
            }
            DEVICE_NAME_RIGHT -> {
                val notifyRightFootOnTheGroundView =
                    findViewById<View>(R.id.notify_right_foot_on_the_ground_view)
                notifyRightFootOnTheGroundView.setBackgroundColor(Color.parseColor("#ffcccc"))
                Handler(Looper.getMainLooper()).postDelayed({
                    notifyRightFootOnTheGroundView.setBackgroundColor(Color.parseColor("#ffffff"))
                }, 500L)
            }
            else -> {
                Log.e("debug", "Invalid device name: $deviceName")
            }
        }
    }

    fun playFootSound(footSoundPool: SoundPool, audioAttributes: AudioAttributes, soundId: Int){
        footSoundPool.setOnLoadCompleteListener { soundPool, i, i2 ->
            Log.d("debug", "sound should be played")
            val streamId = soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
            Handler(Looper.getMainLooper()).postDelayed({
                soundPool.stop(streamId)
                soundPool.release()
            }, 500L)
        }
    }

    /* デバイスのスキャンを行う */
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
}

class BleConnectionRunnable(
     private val bluetoothAdapter: BluetoothAdapter,
     deviceName: String,
     private val leScanCallback: LeScanCallback
): Runnable{
    /* デバイスがスキャン中かどうかを管理するBoolean変数 */
    private var bleDeviceScanning: Boolean = false
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()
    private val scanFilters = listOf<ScanFilter>(ScanFilter.Builder().setDeviceName(deviceName).build())
    override fun run(){
        when(bluetoothAdapter.isEnabled){
            true ->{
                // 事前に決めたスキャン時間を過ぎたらスキャンを停止する
                Handler(Looper.getMainLooper()).postDelayed({
                    bleDeviceScanning = false
                    bluetoothLeScanner.stopScan(leScanCallback)
                }, SCAN_PERIOD)
                bleDeviceScanning = true
                bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback)
            }
            else -> {
                bleDeviceScanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
        }
    }

    fun disconnect(){
        leScanCallback.disconnectGatt()
    }
}

open class LeScanCallback(private val context: BluetoothConnectionActivity, private val bluetoothLeScanner: BluetoothLeScanner, private val sensorValueHandler: SensorValueHandler) : ScanCallback(){

    private var hasAlreadyFound = false
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        Log.e("BLEDeviceScanFailed", "端末のスキャンに失敗しました")
    }

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        result?.let {
            Log.d("debug", "device ${it.device.name} found")
            bluetoothLeScanner.stopScan(this)
            // 第3引数のBlueotoothGattCallbackが，bluetoothGattを取得しなければ撮ることができない…
            if(!hasAlreadyFound) {
                Handler(Looper.getMainLooper()).postDelayed({
                    bluetoothGatt = it.device.connectGatt(
                        context,
                        true,
                        GattCallback(context, sensorValueHandler)
                    )
                }, CONNECTION_INTERVAL)
                hasAlreadyFound = true
            }
        }
    }

    fun disconnectGatt(){
        if(bluetoothGatt == null){
            Log.e("debug", "bluetoothGatt to disconnect is null")
        }
        Log.d("debug", "disconnect from BLE device")
        bluetoothGatt?.close()
    }
}

// デバイスの接続と切断を管理するコールバック関数
class GattCallback(private val context: BluetoothConnectionActivity, private val sensorValueHandler: SensorValueHandler) : BluetoothGattCallback() {
    private var connectionState = STATE_DISCONNECTED
    private var connectionTimedOut = false
    override fun onConnectionStateChange(
        gatt: BluetoothGatt?,
        status: Int,
        newState: Int
    ) {
        Handler(Looper.getMainLooper()).postDelayed({
            connectionTimedOut = true
        }, CONNECTION_PERIOD)
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                connectionState = STATE_CONNECTED
                Log.d("debug", "Connected to GATT server.")
                val isDiscoveringServices = gatt?.discoverServices()
                Log.d("debug", "Attempting to start service discovery: $isDiscoveringServices")
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                connectionState = STATE_DISCONNECTED
                Log.d("debug", "Disconnected from GATT server.")
                gatt?.close()
                when(status){
                    133 -> {
                        if (!connectionTimedOut){
                            Log.d("debug", "connection failed, retrying...")
                            gatt?.device?.connectGatt(context, true, this)
                        }else{
                            connectionTimedOut = false
                            Log.d("debug", "connection failed, connection timed out")
                        }
                    }
                }
            }
            else -> {
                gatt?.close()
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int){
        when(status){
            BluetoothGatt.GATT_SUCCESS -> {
                val gattServices = gatt.services
                gattServices[2]?.characteristics?.forEach { gattCharacteristic ->
                    gatt.readCharacteristic(gattCharacteristic)
                }
            }
            else -> {
                Log.d("debug", "onServicesDisconnected received: $status")
            }
        }
    }

    // キャラクタリスティックを受信するとココが呼ばれる？ならばまずはここでデータが撮れているかどうかを見なければ…！
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ){
        when(status){
            BluetoothGatt.GATT_SUCCESS -> {
                Log.d("debug", "characteristic read succeeded")
                if(characteristic.value.size == 4){
                    val setNotificationStatus = gatt.setCharacteristicNotification(characteristic, true)
                    val uuid = UUID.fromString(CCCD_UUID_STR)
                    val descriptor = characteristic.getDescriptor(uuid)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    Log.d("debug", "setCharacteristicNotificationStatus: $setNotificationStatus")
                }else{
                    Log.e("debug", "characteristic value format is invalid.")
                }
            }
        }
    }

    private val gap = arrayOf(0,0)
    private var notificationCount = 0

    // 前後1つずつ，3ms分の移動平均フィルタをとりあえずかける．
    private var movingAverageArray = arrayOf(0, 0, 0, 0, 0)

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
//        Log.d("debug", "onCharacteristicChanged called")
        if(characteristic?.value?.size == 4){
            val deviceName = gatt?.device?.name
            if(deviceName != DEVICE_NAME_LEFT && deviceName != DEVICE_NAME_RIGHT) return
            characteristic?.let{ it ->
                val sensorValue1 = (characteristic.value[0].toInt() and 0xFF) * 256 + (characteristic.value[1].toInt() and 0xFF)
                val sensorValue2 = (characteristic.value[2].toInt() and 0xFF) * 256 + (characteristic.value[3].toInt() and 0xFF)
                for(i in movingAverageArray.indices){
                    if(i == movingAverageArray.size-1){
                        movingAverageArray[i] = sensorValue1
                    }else{
                        movingAverageArray[i] = movingAverageArray[i + 1]
                    }
                }
                val movingAverage = movingAverageArray.sum() / movingAverageArray.size
                if(notificationCount >= 5){
                    gap[0] = gap[1]
                    gap[1] = movingAverage
                    if(gap[1] < 1024 && (gap[1] - gap[0]) / 5 <= -150 ){
                        Log.d("debug", "gap[0]: ${gap[0]}, gap[1]: ${gap[1]}")
                        val footOnTheGroundMsg = sensorValueHandler.obtainMessage(FOOT_ON_THE_GROUND, 0, 0, if(deviceName == DEVICE_NAME_LEFT) DEVICE_NAME_LEFT else DEVICE_NAME_RIGHT)
                        footOnTheGroundMsg.sendToTarget()
                    }

                    notificationCount = 0

                    val sensorValue1Msg = sensorValueHandler.obtainMessage(SENSOR_VALUE_RECEIVE, if(deviceName == DEVICE_NAME_LEFT) SENSOR_LEFT_1 else SENSOR_RIGHT_1, sensorValue1)
                    sensorValue1Msg.sendToTarget()
                    val sensorValue2Msg = sensorValueHandler.obtainMessage(SENSOR_VALUE_RECEIVE, if(deviceName == DEVICE_NAME_LEFT) SENSOR_LEFT_2 else SENSOR_RIGHT_2, sensorValue2)
                    sensorValue2Msg.sendToTarget()

                }
                notificationCount++;
            }
        }else{
            Log.e("debug", "characteristic value format is invalid.")
        }
    }
}