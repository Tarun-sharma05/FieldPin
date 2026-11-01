package com.tarun.fieldpin.data.location

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.tarun.fieldpin.domain.model.LocationState
import com.tarun.fieldpin.domain.repository.LocationProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FusedLocationProvider(context: Context) : LocationProvider {
    private val appContext = context.applicationContext
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun locationUpdates(): Flow<LocationState> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val fix = result.lastLocation
                if (fix == null) trySend(LocationState.Unavailable)
                else trySend(LocationState.Fix(fix.latitude, fix.longitude, fix.accuracy))
            }
        }

        fun hasLocationPermission(): Boolean =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        fun refreshUpdates() {
            if (!hasLocationPermission()) {
                trySend(LocationState.NoPermission)
                fusedClient.removeLocationUpdates(callback)
                return
            }
            val enabled = try {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (_: RuntimeException) {
                false
            }
            if (!enabled) {
                trySend(LocationState.GpsOff)
                fusedClient.removeLocationUpdates(callback)
                return
            }
            try {
                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener { trySend(LocationState.Unavailable) }
            } catch (_: SecurityException) {
                trySend(LocationState.NoPermission)
            }
        }

        val providersReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refreshUpdates()
        }
        ContextCompat.registerReceiver(
            appContext,
            providersReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        refreshUpdates()

        awaitClose {
            appContext.unregisterReceiver(providersReceiver)
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
