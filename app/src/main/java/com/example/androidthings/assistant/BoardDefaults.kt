/*
 * Copyright 2016, The Android Open Source Project
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

import android.os.Build

import com.google.android.things.pio.PeripheralManagerService

class BoardDefaults {

    companion object {

        private val DEVICE_EDISON_ARDUINO = "edison_arduino"
        private val DEVICE_EDISON = "edison"
        private val DEVICE_JOULE = "joule"
        private val DEVICE_RPI3 = "rpi3"
        private val DEVICE_PICO = "imx6ul_pico"
        private val DEVICE_VVDN = "imx6ul_iopb"
        private var sBoardVariant = ""

        /**
         * Return the GPIO pin that the LED is connected on.
         * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
         * that turns on when the GPIO pin is HIGH, and off when low.
         */
        val gpioForLED: String
            get() {
                when (boardVariant) {
                    DEVICE_EDISON_ARDUINO -> return "IO13"
                    DEVICE_EDISON -> return "GP45"
                    DEVICE_JOULE -> return "J6_25"
                    DEVICE_RPI3 -> return "BCM25"
                    DEVICE_PICO -> return "GPIO4_IO21"
                    DEVICE_VVDN -> return "GPIO3_IO06"
                    else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
                }
            }

        /**
         * Return the GPIO pin that the Button is connected on.
         */
        val gpioForButton: String
            get() {
                when (boardVariant) {
                    DEVICE_EDISON_ARDUINO -> return "IO12"
                    DEVICE_EDISON -> return "GP44"
                    DEVICE_JOULE -> return "J7_71"
                    DEVICE_RPI3 -> return "BCM23"
                    DEVICE_PICO -> return "GPIO4_IO20"
                    DEVICE_VVDN -> return "GPIO3_IO01"
                    else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
                }
            }

        /**
         * Return the I2S Bus that the Voice Hat is connected to.
         */
        val i2SDeviceForVoiceHat: String
            get() {
                when (boardVariant) {
                    DEVICE_RPI3 -> return "I2S1"
                    else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
                }
            }

        /**
         * Return the GPIO pin for the Voice Hat DAC trigger.
         */
        val gpioForVoiceHatTrigger: String
            get() {
                when (boardVariant) {
                    DEVICE_RPI3 -> return "BCM16"
                    else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
                }
            }

        private // For the edison check the pin prefix
                // to always return Edison Breakout pin name when applicable.
        val boardVariant: String
            get() {
                if (!sBoardVariant.isEmpty()) {
                    return sBoardVariant
                }
                sBoardVariant = Build.DEVICE
                if (sBoardVariant == DEVICE_EDISON) {
                    val pioService = PeripheralManagerService()
                    val gpioList = pioService.gpioList
                    if (gpioList.size != 0) {
                        val pin = gpioList[0]
                        if (pin.startsWith("IO")) {
                            sBoardVariant = DEVICE_EDISON_ARDUINO
                        }
                    }
                }
                return sBoardVariant
            }
    }
}
