package com.miraikeitai2021.otokakeandroid

import android.bluetooth.*
import android.os.Handler
import android.os.Looper
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.*

@RunWith(RobolectricTestRunner::class)

class GattCallbackTest : TestCase(){


    @Before
    public override fun setUp(){
    }

    @Test
    fun testOnConnectionStateChangeConnected(){
        // BluetoothConnectionActivityはopenでないため，mockではなく本物にする
        val context = BluetoothConnectionActivity()
        val mockSensorValueHandler = mock<SensorValueHandler>()
        val mockBluetoothGatt = mock<BluetoothGatt>()

        val gattCallback = GattCallback(context, mockSensorValueHandler)
        gattCallback.onConnectionStateChange(mockBluetoothGatt, 0, BluetoothProfile.STATE_CONNECTED)

        verify(mockBluetoothGatt, times(1)).discoverServices()
    }

    @Test
    fun testOnConnectionStateChangeDisconnectedAndTimedOut() {
        val context = BluetoothConnectionActivity()
        val mockSensorValueHandler = mock<SensorValueHandler>()
        val mockBluetoothGatt = mock<BluetoothGatt>() {
            on { device } doReturn mock<BluetoothDevice>()
        }

        val gattCallback = GattCallback(context, mockSensorValueHandler)

        gattCallback.onConnectionStateChange(mockBluetoothGatt, 133, BluetoothProfile.STATE_DISCONNECTED)

        verify(mockBluetoothGatt, times(1)).close()
        verify(mockBluetoothGatt.device, times(1)).connectGatt(context, true, gattCallback)
        verify(mockBluetoothGatt, times(0)).discoverServices()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        gattCallback.onConnectionStateChange(mockBluetoothGatt, 133, BluetoothProfile.STATE_DISCONNECTED)
        verify(mockBluetoothGatt, times(2)).close()
        verify(mockBluetoothGatt.device, times(1)).connectGatt(context, true, gattCallback)
        verify(mockBluetoothGatt, times(0)).discoverServices()
    }

    @Test
    fun testOnConnectionStateChangeHandleOtherNewStates() {
        val context = BluetoothConnectionActivity()
        val mockSensorValueHandler = mock<SensorValueHandler>()
        val mockBluetoothGatt = mock<BluetoothGatt>() {
            on { device } doReturn mock<BluetoothDevice>()
        }

        val gattCallback = GattCallback(context, mockSensorValueHandler)

        gattCallback.onConnectionStateChange(mockBluetoothGatt, 0, BluetoothProfile.STATE_CONNECTING)
        gattCallback.onConnectionStateChange(mockBluetoothGatt, 0, BluetoothProfile.STATE_DISCONNECTING)

        verify(mockBluetoothGatt, times(2)).close()
    }

    @Test
    fun testOnCharacteristicReadGattSucceed() {
        val context = BluetoothConnectionActivity()
        val mockSensorValueHandler = mock<SensorValueHandler>()
        val mockDescriptor = mock<BluetoothGattDescriptor>()
        val mockValue = byteArrayOf(0, 0, 0, 0)
        val mockCharacteristic = mock<BluetoothGattCharacteristic>(defaultAnswer = RETURNS_DEEP_STUBS) {
            on { getDescriptor(any<UUID>()) } doReturn mockDescriptor
            on { value } doReturn mockValue
        }
        val mockBluetoothGatt = mock<BluetoothGatt>() {
            on { setCharacteristicNotification(mockCharacteristic, true) } doReturn true
        }

        val gattCallback = GattCallback(context, mockSensorValueHandler)
        gattCallback.onCharacteristicRead(mockBluetoothGatt, mockCharacteristic, BluetoothGatt.GATT_SUCCESS)

        verify(mockBluetoothGatt, times(1)).setCharacteristicNotification(mockCharacteristic, true)
        verify(mockCharacteristic, times(1)).getDescriptor(any<UUID>())
        verify(mockBluetoothGatt, times(1)).writeDescriptor(mockDescriptor)
    }

    @Test
    fun testOnCharacteristicHandleOtherGattStatuses() {
        val context = BluetoothConnectionActivity()
        val mockSensorValueHandler = mock<SensorValueHandler>()
        val mockDescriptor = mock<BluetoothGattDescriptor>()
        val mockValue = byteArrayOf(0, 0, 0, 0)
        val mockCharacteristic = mock<BluetoothGattCharacteristic>(defaultAnswer = RETURNS_DEEP_STUBS) {
            on { getDescriptor(any<UUID>()) } doReturn mockDescriptor
            on { value } doReturn mockValue
        }
        val mockBluetoothGatt = mock<BluetoothGatt>() {
            on { setCharacteristicNotification(mockCharacteristic, true) } doReturn true
        }

        val gattCallback = GattCallback(context, mockSensorValueHandler)
        gattCallback.onCharacteristicRead(mockBluetoothGatt, mockCharacteristic, BluetoothGatt.GATT_FAILURE)

        verify(mockBluetoothGatt, times(0)).setCharacteristicNotification(mockCharacteristic, true)
        verify(mockCharacteristic, times(0)).getDescriptor(any<UUID>())
        verify(mockBluetoothGatt, times(0)).writeDescriptor(mockDescriptor)
    }

    @Test
    fun testOnCharacteristicInValidByteArraySize() {
        val context = BluetoothConnectionActivity()
        val mockSensorValueHandler = mock<SensorValueHandler>()
        val mockDescriptor = mock<BluetoothGattDescriptor>()
        val mockValue = byteArrayOf(0, 0, 0)
        val mockCharacteristic = mock<BluetoothGattCharacteristic>(defaultAnswer = RETURNS_DEEP_STUBS) {
            on { getDescriptor(any<UUID>()) } doReturn mockDescriptor
            on { value } doReturn mockValue
        }
        val mockBluetoothGatt = mock<BluetoothGatt>() {
            on { setCharacteristicNotification(mockCharacteristic, true) } doReturn true
        }

        val gattCallback = GattCallback(context, mockSensorValueHandler)
        gattCallback.onCharacteristicRead(mockBluetoothGatt, mockCharacteristic, BluetoothGatt.GATT_SUCCESS)

        verify(mockBluetoothGatt, times(0)).setCharacteristicNotification(mockCharacteristic, true)
        verify(mockCharacteristic, times(0)).getDescriptor(any<UUID>())
        verify(mockBluetoothGatt, times(0)).writeDescriptor(mockDescriptor)
    }
}