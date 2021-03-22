package com.starflow.junklibrary

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.starflow.junklibrary.gps.GpsController

class JunkUtilTest: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gps = GpsController.getInstance(this)
                .setGpsCallCount(5)
                .start()
    }
}