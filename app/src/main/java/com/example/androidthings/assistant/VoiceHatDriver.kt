/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.assistant

import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.util.Log

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.I2sDevice
import com.google.android.things.pio.PeripheralManagerService
import com.google.android.things.userdriver.AudioInputDriver
import com.google.android.things.userdriver.AudioOutputDriver
import com.google.android.things.userdriver.UserDriverManager

import java.io.IOException
import java.nio.ByteBuffer

class VoiceHatDriver @Throws(IOException::class)
constructor(i2sBus: String, triggerGpioPin: String, audioFormat: AudioFormat) : AutoCloseable {
    private var mDevice: I2sDevice? = null
    private var mTriggerGpio: Gpio? = null
    private var mAudioFormat: AudioFormat? = null
    private var mAudioInputDriver: AudioInputUserDriver? = null
    private var mAudioOutputDriver: AudioOutputUserDriver? = null

    init {
        val pioService = PeripheralManagerService()
        try {
            mDevice = pioService.openI2sDevice(i2sBus, audioFormat)
            mTriggerGpio = pioService.openGpio(triggerGpioPin)
            mTriggerGpio!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            mAudioFormat = audioFormat
        } catch (e: IOException) {
            try {
                close()
            } catch (ignored: IOException) {
            } catch (ignored: RuntimeException) {
            }

            throw e
        }

    }

    @Throws(IOException::class)
    override fun close() {
        unregisterAudioInputDriver()
        unregisterAudioOutputDriver()
        if (mDevice != null) {
            try {
                mDevice!!.close()
            } finally {
                mDevice = null
            }
        }
        if (mTriggerGpio != null) {
            try {
                mTriggerGpio!!.close()
            } finally {
                mTriggerGpio = null
            }
        }
    }

    fun registerAudioInputDriver() {
        Log.d(TAG, "registering audio input driver")
        mAudioInputDriver = AudioInputUserDriver()
        UserDriverManager.getManager().registerAudioInputDriver(
                mAudioInputDriver, mAudioFormat, AudioDeviceInfo.TYPE_BUILTIN_MIC, BUFFER_SIZE
        )
    }

    fun registerAudioOutputDriver() {
        Log.d(TAG, "registering audio output driver")
        mAudioOutputDriver = AudioOutputUserDriver()
        UserDriverManager.getManager().registerAudioOutputDriver(
                mAudioOutputDriver, mAudioFormat, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, BUFFER_SIZE
        )
    }

    fun unregisterAudioInputDriver() {
        if (mAudioInputDriver != null) {
            UserDriverManager.getManager().unregisterAudioInputDriver(mAudioInputDriver)
            mAudioInputDriver = null
        }
    }

    fun unregisterAudioOutputDriver() {
        if (mAudioOutputDriver != null) {
            UserDriverManager.getManager().unregisterAudioOutputDriver(mAudioOutputDriver)
            mAudioOutputDriver = null
        }
    }

    private inner class AudioInputUserDriver : AudioInputDriver() {


        override fun onStandbyChanged(b: Boolean) {
            Log.d(TAG, "audio input driver standby changed:" + b)
        }

        override fun read(byteBuffer: ByteBuffer, i: Int): Int {
            try {
                return mDevice!!.read(byteBuffer, i)
            } catch (e: IOException) {
                Log.e(TAG, "error during read operation:", e)
                return -1
            }

        }
    }

    private inner class AudioOutputUserDriver : AudioOutputDriver() {

        override fun onStandbyChanged(inStandby: Boolean) {
            Log.d(TAG, "audio output driver standby changed:" + inStandby)
            try {
                if (!inStandby) {
                    Log.d(TAG, "turning voice hat DAC on")
                    val buf = ByteArray(FLUSH_SIZE)
                    mDevice!!.write(buf, 0, buf.size)
                    mTriggerGpio!!.value = true
                } else {
                    Log.d(TAG, "turning voice hat DAC off")
                    mTriggerGpio!!.value = false
                }
            } catch (e: IOException) {
                Log.e(TAG, "error during standby trigger:", e)
            }

        }

        override fun write(byteBuffer: ByteBuffer, i: Int): Int {
            try {
                return mDevice!!.write(byteBuffer, i)
            } catch (e: IOException) {
                Log.e(TAG, "error during write operation:", e)
                return -1
            }

        }
    }

    companion object {
        private val TAG = "VoiceHatDriver"
        // buffer of 0.05 sec of sample data at 48khz / 16bit.
        private val BUFFER_SIZE = 96000 / 20
        // buffer of 0.5 sec of sample data at 48khz / 16bit.
        private val FLUSH_SIZE = 48000
    }
}
