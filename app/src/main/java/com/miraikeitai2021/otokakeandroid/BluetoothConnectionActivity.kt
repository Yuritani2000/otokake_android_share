package com.miraikeitai2021.otokakeandroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

// Bluetoothの有効化をリクエストする際のリクエストコード
private const val REQUEST_ENABLE_BT = 1000

class BluetoothConnectionActivity : AppCompatActivity() {
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
    }

    // Bluetoothが有効であることを確認する。
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

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
}