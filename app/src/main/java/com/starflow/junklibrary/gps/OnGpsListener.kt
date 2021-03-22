package com.starflow.junklibrary.gps

interface OnGpsListener {
    fun onPermissionDenied(deniedPermissions: MutableList<String>?)
    fun onGpsSettingDisabled()
    fun onFromMockProvider()
    fun onLatLong(latitude: Double, longitude: Double)
}