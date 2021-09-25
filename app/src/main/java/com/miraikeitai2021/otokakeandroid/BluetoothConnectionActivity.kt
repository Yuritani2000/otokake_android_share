package com.miraikeitai2021.otokakeandroid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
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

// Bluetoothの有効化をリクエストするIntentに関するリクエストコード
private const val REQUEST_ENABLE_BT = 1000
private const val REQUEST_ACCESS_FINE_LOCATION = 1001

// BLE端末をスキャンする時間をさだめる。今回は10秒。
private const val SCAN_PERIOD = 10000L

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
        val leScanCallback = LeScanCallback()
        when(enable){
            true ->{
                // 事前に決めたスキャン時間を過ぎたらスキャンを停止する
                Handler(Looper.getMainLooper()).postDelayed({
                    bleDeviceScanning = false
                    bluetoothLeScanner.stopScan(leScanCallback)
                }, SCAN_PERIOD)
                bleDeviceScanning = true
                bluetoothLeScanner.startScan(leScanCallback)
            }
            else -> {
                bleDeviceScanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
        }
    }

    inner class LeScanCallback : ScanCallback(){
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach {
                Log.d("debug", "device ${it.device}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BLEDeviceScanFailed", "端末のスキャンに失敗しました");
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                Log.d("debug", "device ${it.device.name} found")

            }
        }
    }
}