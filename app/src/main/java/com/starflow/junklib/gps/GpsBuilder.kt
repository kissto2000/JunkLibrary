package com.starflow.junklib.gps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission

open class GpsBuilder<T : GpsBuilder<T>>(private val context: Context) {

    private var isUseFusedLocation = true

    private var dialog: Dialog? = null
    private var listener: OnGpsListener? = null

    private var isGoogleServiceChecked = false
    private var activity: Activity? = null

    private var isPermissionDeniedDialogShowing = false

    private var gpsCallCount = 0
    private var count = 0

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    /**
     * Init
     */
    private var locationManager: LocationManager? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private var dialogMsgPermissionDenied: CharSequence? = null
    private var dialogMsgGpsSensorOff: CharSequence? = null
    private var dialogMsgGpsSensorOffPositiveBtn: CharSequence? = null
    private var dialogMsgGpsSensorOffNegativeBtn: CharSequence? = null
    private var dialogMsgGoogleServiceLower: CharSequence? = null
    private var dialogMsgGoogleServiceLowerPositiveBtn: CharSequence? = null
    private var dialogMsgGoogleServiceLowerNegativeBtn: CharSequence? = null

    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        dialogMsgPermissionDenied = "Permission Denied."

        dialogMsgGpsSensorOff = ""
        dialogMsgGpsSensorOffPositiveBtn = "Move"
        dialogMsgGpsSensorOffNegativeBtn = "Cancel"

        dialogMsgGoogleServiceLower = ""
        dialogMsgGoogleServiceLowerPositiveBtn = "Move"
        dialogMsgGoogleServiceLowerNegativeBtn = "Cancel"
    }

    /**
     * Start
     */
    fun checking() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager == null) throw IllegalArgumentException("LocationManager is Null")

        if (isUseFusedLocation) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            if (fusedLocationProviderClient == null) throw IllegalArgumentException("LocationServices is Null")
        }

        if (listener == null) throw IllegalArgumentException("setListener() is Null")

        doing()
    }

    //1. 권한 획득 하기
    private fun doing() {
        count = 0

        val ted = TedPermission.with(context)
        ted.setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        if (isPermissionDeniedDialogShowing) {
            ted.setDeniedMessage(dialogMsgPermissionDenied)
        }
        ted.setPermissionListener(object : PermissionListener {
            override fun onPermissionGranted() {
                setGpsSensorChecked()
            }
            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                listener?.onPermissionDenied(deniedPermissions)
            }
        })
        ted.check()
    }

    //2. GPS 기능 On/Off 검사 하기
    private fun setGpsSensorChecked() {
        if (locationManager == null) locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                if (dialog != null) {
                    dialog!!.cancel()
                    dialog = null
                }
            } catch (e: Exception) { e.printStackTrace() }

            try {
                dialog = AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage(dialogMsgGpsSensorOff)
                        .setPositiveButton(dialogMsgGpsSensorOffPositiveBtn) { _, _ ->
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                        .setNegativeButton(dialogMsgGpsSensorOffNegativeBtn) { _, _ ->
                            listener?.onGpsSettingDisabled()
                        }
                        .show()
            } catch (e: Exception) { e.printStackTrace() }
            return
        }

        if (dialog != null && dialog!!.isShowing) {
            dialog!!.cancel()
            dialog = null
        }

        setGoogleServiceChecked()
    }

    //3. Google Play Service 버전 검사 하기
    private fun setGoogleServiceChecked() {
        val googleApi = GoogleApiAvailability.getInstance()
        val status = googleApi.isGooglePlayServicesAvailable(context)
        if (status != ConnectionResult.SUCCESS) {
            if (isGoogleServiceChecked) {
                if (activity != null) {
                    if (googleApi.isUserResolvableError(status)) {
                        googleApi.getErrorDialog(activity!!, status, 1)?.show()
                    } else {
                        showGoogleServiceLowerDialog()
                    }
                } else {
                    showGoogleServiceLowerDialog()
                }
            } else {
                getLocation()
            }
        } else {
            getFusedLocation()
        }
    }

    private fun showGoogleServiceLowerDialog() {
        try {
            if (dialog != null) {
                dialog!!.cancel()
                dialog = null
            }
        } catch (e: Exception) { e.printStackTrace() }

        dialog = AlertDialog.Builder(context)
                .setCancelable(false)
                .setMessage(dialogMsgGoogleServiceLower)
                .setPositiveButton(dialogMsgGoogleServiceLowerPositiveBtn) { _, _ ->

                }
                .setNegativeButton(dialogMsgGoogleServiceLowerNegativeBtn) { _, _ ->
                    listener?.onGpsSettingDisabled()
                }
                .show()
    }

    //4-1. FusedLocationProviderClient
    @SuppressLint("MissingPermission")
    private fun getFusedLocation() {
        if (fusedLocationProviderClient == null) fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 1
        locationRequest.fastestInterval = 1
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult?.let {
                count++

                for (location in locationResult.locations) {
                    if (location.isFromMockProvider) {
                        listener?.onFromMockProvider()
                    } else {
                        latitude = location.latitude
                        longitude = location.longitude

                        listener?.onLatLong(latitude, longitude)
                    }
                }

                if (count == gpsCallCount) {
                    fusedLocationProviderClient?.removeLocationUpdates(this)
                }
            }
        }
    }

    //4-2. LocationManager
    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (locationManager == null) locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1f, locationListener)
        locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 1f, locationListener)
    }

    @SuppressLint("MissingPermission")
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            count++

            if (location.isFromMockProvider) {
                listener?.onFromMockProvider()
            } else {
                latitude = location.latitude
                longitude = location.longitude

                listener?.onLatLong(latitude, longitude)
            }

            if (count == gpsCallCount) {
                locationManager?.removeUpdates(this)
            }
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (isUseFusedLocation) {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        } else {
            locationManager?.removeUpdates(locationListener)
        }
    }

    /**
     *
     */

    fun setUseFusedLocation(isUsed: Boolean): T {
        isUseFusedLocation = isUsed
        return this as T
    }

    fun setListener(listener: OnGpsListener): T {
        this.listener = listener
        return this as T
    }

    fun setGoogleServiceChecked(bool: Boolean, activity: Activity): T {
        isGoogleServiceChecked = bool
        this.activity = activity
        return this as T
    }

    fun setGoogleServiceChecked(bool: Boolean): T {
        isGoogleServiceChecked = bool
        return this as T
    }

    fun setGpsCallCount(count: Int): T {
        gpsCallCount = count
        return this as T
    }

    /**
     *  Message
     */

    /*
        Dialog Msg Permission Denied
     */
    fun setDialogMsgPermissionDenied(msg: CharSequence): T {
        dialogMsgPermissionDenied = msg
        return this as T
    }

    fun setDialogMsgPermissionDenied(res: Int): T {
        dialogMsgPermissionDenied = context.getText(res)
        return this as T
    }

    /*
        Dialog Msg Gps Sensor Off
     */
    fun setDialogMsgGpsSensorOff(msg: CharSequence): T {
        dialogMsgGpsSensorOff = msg
        return this as T
    }

    fun setDialogMsgGpsSensorOff(res: Int): T {
        dialogMsgGpsSensorOff = context.getText(res)
        return this as T
    }

    fun setDialogMsgGpsSensorOffPositiveBtn(msg: CharSequence): T {
        dialogMsgGpsSensorOffPositiveBtn = msg
        return this as T
    }

    fun setDialogMsgGpsSensorOffPositiveBtn(res: Int): T {
        dialogMsgGpsSensorOffPositiveBtn = context.getText(res)
        return this as T
    }

    fun setDialogMsgGpsSensorOffNegativeBtn(msg: CharSequence): T {
        dialogMsgGpsSensorOffNegativeBtn = msg
        return this as T
    }

    fun setDialogMsgGpsSensorOffNegativeBtn(res: Int): T {
        dialogMsgGpsSensorOffNegativeBtn = context.getText(res)
        return this as T
    }

    /*
        Dialog Msg Google Service Lower
    */
    fun setDialogMsgGoogleServiceLower(msg: CharSequence): T {
        dialogMsgGoogleServiceLower = msg
        return this as T
    }

    fun setDialogMsgGoogleServiceLower(res: Int): T {
        dialogMsgGoogleServiceLower = context.getText(res)
        return this as T
    }

    fun setDialogMsgGoogleServiceLowerPositiveBtn(msg: CharSequence): T {
        dialogMsgGoogleServiceLowerPositiveBtn = msg
        return this as T
    }

    fun setDialogMsgGoogleServiceLowerPositiveBtn(res: Int): T {
        dialogMsgGoogleServiceLowerPositiveBtn = context.getText(res)
        return this as T
    }

    fun setDialogMsgGoogleServiceLowerNegativeBtn(msg: CharSequence): T {
        dialogMsgGoogleServiceLowerNegativeBtn = msg
        return this as T
    }

    fun setDialogMsgGoogleServiceLowerNegativeBtn(res: Int): T {
        dialogMsgGoogleServiceLowerNegativeBtn = context.getText(res)
        return this as T
    }

}