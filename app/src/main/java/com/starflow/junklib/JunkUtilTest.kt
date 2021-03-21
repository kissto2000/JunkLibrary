package com.starflow.junklib

import androidx.appcompat.app.AppCompatActivity
import com.starflow.junklib.gps.GpsController
import kotlin.coroutines.coroutineContext

class JunkUtilTest: AppCompatActivity() {

    fun start() {
        val gps = GpsController.getInstance(this)
                .setGpsCallCount(5)
                .start()
    }
}