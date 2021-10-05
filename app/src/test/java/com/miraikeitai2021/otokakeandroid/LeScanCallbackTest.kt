package com.miraikeitai2021.otokakeandroid

import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.common.base.Joiner.on
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.kotlin.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.logging.Handler

@RunWith(RobolectricTestRunner::class)

class LeScanCallbackTest : TestCase() {

    private val SENSOR_VALUE_RECEIVE = 100
    private open inner class SensorValueHandler(val bluetoothConnectionActivity: BluetoothConnectionActivity): android.os.Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            when(msg.what){
                SENSOR_VALUE_RECEIVE -> {
                }
            }
        }
    }

    public override fun setUp() {
        super.setUp()

    }

    public override fun tearDown() {}

    @Test
    fun testOnScanResultSucceed(){
        val bluetoothLeScannerMock = mock<BluetoothLeScanner>{
            on { stopScan(any<ScanCallback>()) }.then {  }
        }

        // onScanCallbackにてconnectGattのスタブメソッドの返り値として用意されたbluetoothGatt
        val bluetoothGattStub = mock<BluetoothGatt> {}
        // テスト対象になるleScanCallbackに渡すためのresultMock(ScanResult?)のMock
        val resultMock = mock<ScanResult>(defaultAnswer = RETURNS_DEEP_STUBS) {
            on { device.connectGatt( any<Activity>(), eq(true), any<GattCallback>() ) } doReturn bluetoothGattStub
        }
        val context = BluetoothConnectionActivity()
        val mockHandler = mock<SensorValueHandler> {}
        // テスト対象のLeScanCallbackのオブジェクト．これは本物
        val leScanCallbackTarget = LeScanCallback(context, bluetoothLeScannerMock, mockHandler)

        leScanCallbackTarget.onScanResult(0, resultMock)
        // メンバとして宣言したhasAlreadyFoundが稼働しているのを確かめる目的で呼ぶ．
        // 正常に動作していればstopScanは2回呼び出されるが
        // connectGattはif文で弾かれて2回目は呼ばれないはずなので，
        // connectGattが呼ばれた回数が1回かどうかで検証できる
        leScanCallbackTarget.onScanResult(0, resultMock)

        verify(bluetoothLeScannerMock, times(2)).stopScan(leScanCallbackTarget)
        // Handler.postDelayedが使用される場合のテストは，以下の文をつける．
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(resultMock.device, times(1)).connectGatt(any<Activity>(), eq(true), any<GattCallback>())
    }
}