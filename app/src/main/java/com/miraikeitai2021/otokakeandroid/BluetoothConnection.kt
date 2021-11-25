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
const val REQUEST_ENABLE_BT = 1000
const val REQUEST_ACCESS_FINE_LOCATION = 1001

// BLE端末をスキャンする時間をさだめる。今回は10秒。
const val SCAN_PERIOD = 10000L

// BLE端末を検出してから接続するまでのインターバル．この間にデバイスの検索を止める．
const val CONNECTION_INTERVAL = 500L

// GATT定義ディスクリプタの一つ．である，CCCD(Client Characteristic Configuration Descriptor). 端末にNotiyやIndicateを制御するにはこれが使われる．
const val CCCD_UUID_STR = "00002902-0000-1000-8000-00805f9b34fb"

/* GattCallbackにおいて，接続を制御する際に使用する定数． */
const val STATE_DISCONNECTED = 0
const val STATE_CONNECTING = 1
const val STATE_CONNECTED = 2
const val CONNECTION_PERIOD = 5000L

/* BLEデバイスの値を処理するスレッドから，UIスレッドに値を渡すときのHandlerで使用する定数 */
const val SENSOR_VALUE_RECEIVE = 100
const val FOOT_ON_THE_GROUND = 101
/* 通信状態の変化を通知するHandlerで使用する定数 */
const val DEVICE_SCANNING = 102
const val DEVICE_DISCONNECTED = 103
const val DEVICE_CONNECTING = 104
const val DEVICE_CONNECTED = 105

/* BLEデバイスの値をUIスレッドで更新する際，どの値を更新するのかを指定する為のID*/
const val SENSOR_LEFT_1 = 10000
const val SENSOR_LEFT_2 = 10001
const val SENSOR_RIGHT_1 = 10002
const val SENSOR_RIGHT_2 = 10003

/* 左右それぞれの足裏デバイスの名前， */
const val DEVICE_NAME_LEFT = "Otokake_Left"
const val DEVICE_NAME_RIGHT = "Otokake_Right"

/**
 * BLEデバイスとの通信を行うスレッドからメインスレッドへデータを渡す際に使用されるHandler．
 * 通信状態の変化に関する通知にも使われる．
  */
open class BluetoothConnectionHandler(
    private val updateSensorValueTextView: (positionId: Int, sensorValue: Int) -> Unit,
    private val handleFootTouchWithTheGround: (deviceName: String) -> Unit,
    private val handleOnConnectionStatusChanged: (deviceName: String, status: Int)-> Unit
): Handler(Looper.getMainLooper()){
    override fun handleMessage(msg: Message) {
        when(msg.what){
            // 圧力の計測値をメインスレッドに渡す際は，こちらが呼ばれる．
            SENSOR_VALUE_RECEIVE -> {
                Log.d("debug", "handler called")
                updateSensorValueTextView(msg.arg1, msg.arg2)
            }
            // 足が地面と接触した際には，こちらが呼ばれる．
            FOOT_ON_THE_GROUND -> {
                Log.d("debug", "${msg.obj} on the ground")
                handleFootTouchWithTheGround(msg.obj.toString())
            }
            // デバイスが検索中のときに呼ばれる．objから対象のデバイス名を取り出してメソッドを呼ぶ．
            DEVICE_SCANNING ->{
                handleOnConnectionStatusChanged(msg.obj.toString(), DEVICE_SCANNING)
            }
            // デバイスが切断されたときに呼ばれる．objから対象のデバイス名を取り出してメソッドを呼ぶ．
            DEVICE_DISCONNECTED ->{
                handleOnConnectionStatusChanged(msg.obj.toString(), DEVICE_DISCONNECTED)
            }
            // デバイスが接続中のとき呼ばれる．objから対象のデバイス名を取り出してメソッドを呼ぶ．
            DEVICE_CONNECTING -> {
                handleOnConnectionStatusChanged(msg.obj.toString(), DEVICE_CONNECTING)
            }
            // デバイスが接続されたときに呼ばれる．objから対象のデバイス名を取り出してメソッドを呼ぶ．
            DEVICE_CONNECTED -> {
                handleOnConnectionStatusChanged(msg.obj.toString(), DEVICE_CONNECTED)
            }
        }
    }
}

class BleConnectionRunnable(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val deviceName: String,
    private val bluetoothConnectionHandler: BluetoothConnectionHandler
): Runnable{
    /* デバイスがスキャン中かどうかを管理するBoolean変数 */
    var bleDeviceScanning: Boolean = false
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private val leScanCallback = LeScanCallback(context, bluetoothLeScanner, bluetoothConnectionHandler, this)
    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()
    private val scanFilters = listOf<ScanFilter>(ScanFilter.Builder().setDeviceName(deviceName).build())
    override fun run(){
        when(bluetoothAdapter.isEnabled){
            true ->{
                // 事前に決めたスキャン時間を過ぎたらスキャンを停止する
                Handler(Looper.getMainLooper()).postDelayed({
                    // タイムアウト時間になってもまだスキャンをしていれば，スキャンを停止する．
                    if(bleDeviceScanning){
                        // メインスレッドに，接続を切断した旨を伝える
                        val connectionMessage = bluetoothConnectionHandler.obtainMessage(
                            DEVICE_DISCONNECTED,
                            deviceName
                        )
                        connectionMessage.sendToTarget()
                        Toast.makeText(context, R.string.device_not_found_message, Toast.LENGTH_LONG)
                            .show()
                        bleDeviceScanning = false
                        bluetoothLeScanner.stopScan(leScanCallback)
                    }
                }, SCAN_PERIOD)
                // メインスレッドに，デバイスを検索中である旨を伝える
                val connectionMessage = bluetoothConnectionHandler.obtainMessage(
                    DEVICE_SCANNING,
                    deviceName
                )
                connectionMessage.sendToTarget()
                bleDeviceScanning = true
                bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback)
            }
            else -> {
                // メインスレッドに，接続を中断した旨を伝える
                val connectionMessage = bluetoothConnectionHandler.obtainMessage(
                    DEVICE_DISCONNECTED,
                    deviceName
                )
                connectionMessage.sendToTarget()
                bleDeviceScanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
        }
    }

    fun disconnect(){
        leScanCallback.disconnectGatt()
    }
}

open class LeScanCallback(private val context: Context, private val bluetoothLeScanner: BluetoothLeScanner, private val bluetoothConnectionHandler: BluetoothConnectionHandler, private val bleConnectionRunnable: BleConnectionRunnable) : ScanCallback(){

    private var isAlreadyFound = false
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        Log.e("BLEDeviceScanFailed", "端末のスキャンに失敗しました")
    }

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        result?.let {
            Log.d("debug", "device ${it.device.name} found")
            // デバイスが見つかったので，デバイスの検索を止める．
            bleConnectionRunnable.bleDeviceScanning = false
            bluetoothLeScanner.stopScan(this)
            // 各接続試行につき1回しか呼ばれないようにする．
            if(!isAlreadyFound) {
                // デバイスが見つかってからデバイススキャンを停止するまでタイムラグがあるため，その時間を待ってから接続を開始する
                Handler(Looper.getMainLooper()).postDelayed({
                    // メインスレッドに，接続中である旨を伝える
                    val connectionMessage = bluetoothConnectionHandler.obtainMessage(
                        DEVICE_CONNECTING,
                        it.device.name
                    )
                    connectionMessage.sendToTarget()
                    bluetoothGatt = it.device.connectGatt(
                        context,
                        true,
                        GattCallback(context, bluetoothConnectionHandler)
                    )
                }, CONNECTION_INTERVAL)
                isAlreadyFound = true
            }
        }
    }

    // メインスレッドからデバイスを手動で切断するときに呼ばれる．
    fun disconnectGatt(){
        if(bluetoothGatt == null){
            Log.e("debug", "bluetoothGatt to disconnect is null")
        }
        Log.d("debug", "disconnect from BLE device")
        bluetoothGatt?.close()
        // デバイス名をここでは判別できない．また手動でデバイスの切断を行わないため，今回はメッセージを送らない．
    }
}

// デバイスの接続と切断を管理するコールバック関数
class GattCallback(private val context: Context, private val bluetoothConnectionHandler: BluetoothConnectionHandler) : BluetoothGattCallback() {
    private var connectionState = STATE_DISCONNECTED
    private var connectionTimedOut = false

    // BLEデバイスとの接続状況が変化すると呼ばれるメソッド．
    override fun onConnectionStateChange(
        gatt: BluetoothGatt?,
        status: Int,
        newState: Int
    ) {
        // 5秒間の間，接続を試行する．5秒経っても接続できない場合，タイムアウトしたことを示すメンバをtrueにして接続試行をやめる．
        Handler(Looper.getMainLooper()).postDelayed({
            connectionTimedOut = true
        }, CONNECTION_PERIOD)

        when (newState) {
            // 接続が確立したときに呼ばれる部分
            BluetoothProfile.STATE_CONNECTED -> {
                // メインスレッドに接続済みであることを伝える．
                val deviceName = gatt?.device?.name
                val connectionMessage = bluetoothConnectionHandler.obtainMessage(
                    DEVICE_CONNECTED,
                    deviceName
                )
                connectionMessage.sendToTarget()
                connectionState = STATE_CONNECTED
                Log.d("debug", "Connected to GATT server.")
                val isDiscoveringServices = gatt?.discoverServices()
                Log.d("debug", "Attempting to start service discovery: $isDiscoveringServices")
            }

            // デバイスとの接続が切れた際に呼ばれる部分．
            BluetoothProfile.STATE_DISCONNECTED -> {
                // メインスレッドに切断されたことを伝える．
                val connectionMessage = bluetoothConnectionHandler.obtainMessage(
                    DEVICE_DISCONNECTED,
                    gatt?.device?.name
                )
                connectionMessage.sendToTarget()
                connectionState = STATE_DISCONNECTED
                Log.d("debug", "Disconnected from GATT server.")
                gatt?.close()
                when(status){
                    // コードが133(デバイスが見つからない)場合，接続をもう一度試行する動作を5秒間繰り返す．それ以外は，接続をやめる．
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

    // 接続後，BLEで使用される「サービス」が受け取ると呼ばれる．今回使うサービスは3つ目のサービスであるため，それを抽出する．
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

    // キャラクタリスティックを一番最初に受信するとココが呼ばれる．
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

    // 1つ前に受け取ったセンサの計測値と，今うけとったセンサの計測値を格納するための配列
    private val gap1 = arrayOf(0,0)

    // デバイスから定期的にキャラクタリスティックとよばれる通信データが送られてくると呼ばれる関数．
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

                // 配列の要素をずらす．
                gap1[0] = gap1[1]
                gap1[1] = sensorValue1

                // 圧力値(低いほど高い)が3072以上，かつ，圧力の減少幅が-500以上であった場合は，足音を鳴らす信号をメインスレッドに送信する．
                if(gap1[0] in 1500..4095 && (gap1[1] - gap1[0]) <= -500 && gap1[1] in 0..1000){
                    Log.d("debug", "gap[0]: ${gap1[0]}, gap[1]: ${gap1[1]}")
                    val footOnTheGroundMsg = bluetoothConnectionHandler.obtainMessage(FOOT_ON_THE_GROUND, 0, 0, if(deviceName == DEVICE_NAME_LEFT) DEVICE_NAME_LEFT else DEVICE_NAME_RIGHT)
                    footOnTheGroundMsg.sendToTarget()
                }
                    val sensorValue1Msg = bluetoothConnectionHandler.obtainMessage(SENSOR_VALUE_RECEIVE, if(deviceName == DEVICE_NAME_LEFT) SENSOR_LEFT_1 else SENSOR_RIGHT_1, sensorValue1)
                    sensorValue1Msg.sendToTarget()
                    val sensorValue2Msg = bluetoothConnectionHandler.obtainMessage(SENSOR_VALUE_RECEIVE, if(deviceName == DEVICE_NAME_LEFT) SENSOR_LEFT_2 else SENSOR_RIGHT_2, sensorValue2)
                    sensorValue2Msg.sendToTarget()
            }
        }else{
            Log.e("debug", "characteristic value format is invalid.")
        }
    }
}