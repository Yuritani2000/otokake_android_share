package com.miraikeitai2021.otokakeandroid

import android.app.Service
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

private val TAG = BluetoothLeService::class.java.simpleName
const val STATE_DISCONNECTED = 0
const val STATE_CONNECTING = 1
const val STATE_CONNECTED = 2
const val ACTION_GATT_CONNECTED = "com.miraikeitai2021.otokakeandroid.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.miraikeitai2021.otokakeandroid.ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED = "com.miraikeitai2021.otokakeandroid.ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_DATA_AVAILABLE = "com.miraikeitai2021.otokakeandroid.ACTION_DATA_AVAILABLE"
const val EXTRA_DATA = "com.miraikeitai2021.otokakeandroid.EXTRA_DATA"
const val CONNECTION_PERIOD = 5000L

class BluetoothLeService(private var bluetoothGatt: BluetoothGatt?, private var context: AppCompatActivity) : Service() {

    private var connectionState = STATE_DISCONNECTED
    private var connectionTimedOut = false

    // デバイスの接続と切断を管理するコールバック関数
    val gattCallback = object : BluetoothGattCallback() {
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
                        gatt?.device?.connectGatt(context, true, this)
                    }else{
                        Log.d("debug", "connection failed, connection timed out")
                    }
                }
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = STATE_CONNECTED
//                     broadcastUpdate(intentAction)
                    Log.d("debug", "Connected to GATT server.")
                    Log.d("debug", "Attempting to start service discovery: ${bluetoothGatt?.discoverServices()}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = STATE_DISCONNECTED
                    Log.d("debug", "Disconnected from GATT server.")
//                     broadcastUpdate(intentAction)
                }
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int){
            when(status){
                BluetoothGatt.GATT_SUCCESS -> {
//                     broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
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
//                     broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                }
            }
        }
    }

    private fun broadcastUpdate(action: String){
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic){
        val intent = Intent(action)
        val data: ByteArray? = characteristic.value
        data?.let {
            Log.d("debug", "data received: $data")
            intent.putExtra(EXTRA_DATA, "$data")
        }
        sendBroadcast(intent)
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}