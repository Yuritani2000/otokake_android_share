package com.miraikeitai2021.otokakeandroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)

class BleConnectionRunnableTest : TestCase() {

    private lateinit var leScanCallbackMock: LeScanCallback
    private lateinit var bluetoothAdapterMock: BluetoothAdapter
    private lateinit var sensorValueHandlerMock: SensorValueHandler

    @Before
    public override fun setUp() {
        super.setUp()
        leScanCallbackMock = mock<LeScanCallback>()
        bluetoothAdapterMock = mock<BluetoothAdapter>() {
            on { isEnabled } doReturn true
            on { bluetoothLeScanner } doReturn mock<BluetoothLeScanner>()
        }
        sensorValueHandlerMock = mock<SensorValueHandler>()
    }

    @Test
    fun testDisconnectSucceed(){
        // bluetoothLeScannerのモック
        val bleConnectionRunnable = BleConnectionRunnable(bluetoothAdapterMock, "TestDeviceName", leScanCallbackMock)

        bleConnectionRunnable.disconnect()

        // bleConnectionRunnable.disconnect内で，leScanCallback.disconnectが1回呼ばれた
        verify(leScanCallbackMock, times(1)).disconnectGatt()
    }

    @Test
    fun testRunFail(){
        val bluetoothDisabledAdapterMock = mock<BluetoothAdapter>(){
            on { isEnabled } doReturn false
            on { bluetoothLeScanner } doReturn mock<BluetoothLeScanner>()
        }

        val bleConnectionRunnable = BleConnectionRunnable( bluetoothDisabledAdapterMock, "TestDeviceName", leScanCallbackMock)

        bleConnectionRunnable.run()

        verify(bluetoothDisabledAdapterMock.bluetoothLeScanner, times(0)).startScan(any<List<ScanFilter>>(), any<ScanSettings>(), any<LeScanCallback>())
        verify(bluetoothDisabledAdapterMock.bluetoothLeScanner, times(1)).stopScan(any<LeScanCallback>())
    }

    @Test
    fun testRunSucceed(){
        val bleConnectionRunnable = BleConnectionRunnable( bluetoothAdapterMock, "TesDeviceName", leScanCallbackMock)

        bleConnectionRunnable.run()

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(bluetoothAdapterMock.bluetoothLeScanner, times(1)).startScan(any<List<ScanFilter>>(), any<ScanSettings>(), any<LeScanCallback>())
        verify(bluetoothAdapterMock.bluetoothLeScanner, times(1)).stopScan(any<LeScanCallback>())
    }
}