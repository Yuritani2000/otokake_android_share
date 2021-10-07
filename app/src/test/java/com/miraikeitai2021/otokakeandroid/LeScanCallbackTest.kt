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
import org.junit.Before
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

// Android固有のクラスをテストで使うには，この機ワードを入れる
@RunWith(RobolectricTestRunner::class)

class LeScanCallbackTest : TestCase() {

    private val SENSOR_VALUE_RECEIVE = 100

    private lateinit var bluetoothLeScannerMock: BluetoothLeScanner
    private lateinit var bluetoothGattMock: BluetoothGatt
    private lateinit var resultMock: ScanResult
    private lateinit var context: BluetoothConnectionActivity
    private lateinit var mockHandler: SensorValueHandler

    @Before
    public override fun setUp() {
        super.setUp()
        // onScanResult内でstopScanを呼び出すためのMock
        bluetoothLeScannerMock = mock<BluetoothLeScanner>{
            on { stopScan(any<ScanCallback>()) }.then {  }
        }

        // onScanCallbackにてconnectGattのスタブメソッドの返り値として用意されたbluetoothGatt
        bluetoothGattMock = mock<BluetoothGatt> {
            on { close() }.then {  }
        }
        // テスト対象になるleScanCallbackに渡すためのresultMock(ScanResult?)のMock
        resultMock = mock<ScanResult>(defaultAnswer = RETURNS_DEEP_STUBS) {
            on { device.connectGatt( any<BluetoothConnectionActivity>(), eq(true), any<GattCallback>() ) } doReturn bluetoothGattMock
        }
        context = BluetoothConnectionActivity()
        mockHandler = mock<SensorValueHandler>{}
    }

    @Test
    fun testOnScanResultSucceed(){
        // テスト対象のLeScanCallbackのオブジェクト．これは本物
        val leScanCallback = LeScanCallback(context, bluetoothLeScannerMock, mockHandler)

        leScanCallback.onScanResult(0, resultMock)
        // メンバとして宣言したhasAlreadyFoundが稼働しているのを確かめる目的で呼ぶ．
        // 正常に動作していればstopScanは2回呼び出されるが
        // connectGattはif文で弾かれて2回目は呼ばれないはずなので，
        // connectGattが呼ばれた回数が1回かどうかで検証できる
        leScanCallback.onScanResult(0, resultMock)

        verify(bluetoothLeScannerMock, times(2)).stopScan(leScanCallback)
        // Handler.postDelayedが使用される場合のテストは，以下の文をつける．
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(resultMock.device, times(1)).connectGatt(any<Activity>(), eq(true), any<GattCallback>())
    }

    @Test
    fun testOnScanResultFailed(){
        // テスト対象のLeScanCallbackのオブジェクト．これは本物
        val leScanCallback = LeScanCallback(context, bluetoothLeScannerMock, mockHandler)

        // 今回は，resultにnullが渡ってきた想定の動作も行う．
        // resultにnullが渡ってきても，nullチェックが働いてヌルポが出ず，なにも実行されないことを確認する．
        leScanCallback.onScanResult(0, null)
        leScanCallback.onScanResult(0, null)

        //なにも実行されないことを確認するので，メソッドの呼び出し回数が零であることを検証する．
        verify(bluetoothLeScannerMock, times(0)).stopScan(leScanCallback)
        // Handler.postDelayedが使用される場合のテストは，以下の文をつける．
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(resultMock.device, times(0)).connectGatt(any<Activity>(), eq(true), any<GattCallback>())
    }

    @Test
    fun testDisconnectGattSucceed(){
        val leScanCallback = LeScanCallback(context, bluetoothLeScannerMock, mockHandler)
        leScanCallback.onScanResult(0, resultMock)

        // bluetoothGattが取得できるのは500ミリ秒後なので，bluetoothGattが取得できるタイミングを待ってから，
        // 切断処理を行う．
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        leScanCallback.disconnectGatt()
        verify(bluetoothGattMock, times(1)).close()
    }

    @Test
    fun testDisconnectGattFailed(){
        val leScanCallback = LeScanCallback(context, bluetoothLeScannerMock, mockHandler)
        val failedResultMock = mock<ScanResult> (defaultAnswer = RETURNS_DEEP_STUBS) {
            on {device.connectGatt( any<BluetoothConnectionActivity>(), eq(true), any<GattCallback>() )} doReturn null
        }

        leScanCallback.onScanResult(0, failedResultMock)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        leScanCallback.disconnectGatt()
        verify(bluetoothGattMock, times(0)).close()
    }
}