package com.miraikeitai2021.otokakeandroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)

class BleConnectionRunnableTest : TestCase() {

    @Before
    public override fun setUp() {
        super.setUp()
    }

    @After
    public override fun tearDown() {}

    @Test
    fun testDisconnectSucceed(){
        // bluetoothLeScannerのモック
        val leScanCallbackMock = mock<LeScanCallback>()
        val context = BluetoothConnectionActivity()

        val bluetoothAdapterMock = mock<BluetoothAdapter>()
        val sensorValueHandlerMock = mock<SensorValueHandler>()

        val bleConnectionRunnable = BleConnectionRunnable(context, bluetoothAdapterMock, sensorValueHandlerMock, "",leScanCallbackMock)

        bleConnectionRunnable.disconnect()

        // bleConnectionRunnable.disconnect内で，leScanCallback.disconnectが1回呼ばれた
        verify(leScanCallbackMock, times(1)).disconnectGatt()
    }

    @Test
    fun testDisconnectFailed(){

    }

}