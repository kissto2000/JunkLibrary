package com.starflow.junklibrary.gps

import android.annotation.SuppressLint
import android.content.Context

typealias Completion = () -> Unit

open class GpsController {
    val TAG = javaClass.simpleName

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: Builder? = null

        fun getInstance(context: Context): Builder {
            if (instance == null) instance = Builder(context)
            return instance!!
        }
    }

    class Builder(context: Context) : GpsBuilder<Builder>(context) {
        fun start() {
            checking()
        }
    }
}