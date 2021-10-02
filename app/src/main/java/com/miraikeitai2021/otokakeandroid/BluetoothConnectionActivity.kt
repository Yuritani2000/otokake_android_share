package com.miraikeitai2021.otokakeandroid

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Trace.isEnabled
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.experimental.and

// Bluetoothの有効化をリクエストするIntentに関するリクエストコード
private const val REQUEST_ENABLE_BT = 1000
private const val REQUEST_ACCESS_FINE_LOCATION = 1001

// BLE端末をスキャンする時間をさだめる。今回は10秒。
private const val SCAN_PERIOD = 10000L

// BLE端末を検出してから接続するまでのインターバル．この間にデバイスの検索を止める．
private const val CONNECTION_INTERVAL = 500L

// GATT定義ディスクリプタの一つ．である，CCCD(Client Characteristic Configuration Descriptor). 端末にNotiyやIndicateを制御するにはこれが使われる．
private const val CCCD_UUID_STR = "00002902-0000-1000-8000-00805f9b34fb"

class BluetoothConnectionActivity : AppCompatActivity() {

    /* デバイスがスキャン中かどうかを管理するBoolean変数 */
    private var bleDeviceScanning: Boolean = false

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


        val searchDeviceButton = findViewById<Button>(R.id.search_device_button)
        searchDeviceButton.setOnClickListener {
            bluetoothAdapter?.let{
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    scanDevice(it.isEnabled, it)
                }
            }
        }
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

    /* デバイスのスキャンを行う */
    private fun scanDevice(
        enable: Boolean,
        bluetoothAdapter: BluetoothAdapter
    ){
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        val leScanCallback = LeScanCallback(bluetoothLeScanner)
        val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()
        val scanFilters = listOf<ScanFilter>(ScanFilter.Builder().setDeviceName("MyESP32").build())
        when(enable){
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

    inner class LeScanCallback(private val bluetoothLeScanner: BluetoothLeScanner) : ScanCallback(){

        var hasAlreadyFound = false

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach {
                Log.d("debug", "device ${it.device}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BLEDeviceScanFailed", "端末のスキャンに失敗しました")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                Log.d("debug", "device ${it.device.name} found")
                bluetoothLeScanner.stopScan(this)
                // 第3引数のBlueotoothGattCallbackが，bluetoothGattを取得しなければ撮ることができない…
                if(!hasAlreadyFound) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val bluetoothGatt = it.device.connectGatt(
                            this@BluetoothConnectionActivity,
                            true,
                            GattCallback()
                        )
                    }, CONNECTION_INTERVAL)
                    hasAlreadyFound = true
                }
            }
        }
    }

    private var connectionState = STATE_DISCONNECTED

    // デバイスの接続と切断を管理するコールバック関数
    inner class GattCallback : BluetoothGattCallback() {
        private var connectionTimedOut = false
        override fun onConnectionStateChange(
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            val intentAction: String
            Handler(Looper.getMainLooper()).postDelayed({
                connectionTimedOut = true
            }, CONNECTION_PERIOD)

            when(status){
                133 -> {
                    if (!connectionTimedOut){
                        Log.d("debug", "connection failed, retrying...")
                        gatt?.close()
                        gatt?.device?.connectGatt(this@BluetoothConnectionActivity, true, this)
                    }else{
                        gatt?.close()
                        connectionTimedOut = false
                        Log.d("debug", "connection failed, connection timed out")
                    }
                }
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = STATE_CONNECTED
                    Log.d("debug", "Connected to GATT server.")
                    Log.d("debug", "Attempting to start service discovery: ${gatt?.discoverServices()}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = STATE_DISCONNECTED
                    Log.d("debug", "Disconnected from GATT server.")
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


//                    gattServices?.forEach { gattService ->
//                        val gattCharacteristics = gattService.characteristics
//                        gattCharacteristics?.forEach{ gattCharacteristic ->
//                            gatt.readCharacteristic(gattCharacteristic)
//                        }
//                    }
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
                    if(characteristic.value.size == 2){
                        characteristic.value.forEach {
                            Log.d("debug", "characteristic: ${it.toInt() and 0xFF}")
                            val setNotificationStatus = gatt.setCharacteristicNotification(characteristic, true)
                            characteristic.descriptors.forEach { it ->
                                Log.d("debug", "descriptor: ${it.uuid}");
                            }
                            val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                            val descriptor = characteristic.getDescriptor(uuid)
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                            Log.d("debug", "setCharacteristicNotificationStatus: $setNotificationStatus")
                        }
                        val pressure = (characteristic.value[0].toInt() and 0xFF) * 256 + (characteristic.value[1].toInt() and 0xFF)
                        Log.d("debug", "received pressure: $pressure")
                    }else{
                        Log.e("debug", "characteristic value format is invalid.")
                    }

                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            Log.d("debug", "onCharacteristicChanged called")
            if(characteristic?.value?.size == 2){
                characteristic?.value?.forEach {
                    Log.d("debug", "characteristic: ${it.toInt() and 0xFF}")
                }
                characteristic?.let{ it ->
                    val pressure = (it.value[0].toInt() and 0xFF) * 256 + (it.value[1].toInt() and 0xFF)
                    Log.d("debug", "received pressure: $pressure")
                }

            }else{
                Log.e("debug", "characteristic value format is invalid.")
            }
        }
    }

}

