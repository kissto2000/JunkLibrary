package com.starflow.junklibrary.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.starflow.junklibrary.gps.GpsController

class SampleActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GpsController.getInstance(this)
            .start()
    }
}