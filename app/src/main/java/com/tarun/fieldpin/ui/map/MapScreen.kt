package com.tarun.fieldpin.ui.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarun.fieldpin.BuildConfig
import com.tarun.fieldpin.domain.model.LatLng
import com.tarun.fieldpin.domain.model.LocationState
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.usecase.MAX_CHECK_IN_ACCURACY_M
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

private const val SOURCE_ID = "fieldpin-sites"
private const val FILL_LAYER_ID = "fieldpin-site-fills"
private const val SELECTED_OUTLINE_LAYER_ID = "fieldpin-selected-outline"
private const val USER_LOCATION_LAYER_ID = "fieldpin-user-location"
private const val OFFLINE_FALLBACK_STYLE = """{"version":8,"sources":{},"layers":[{"id":"background","type":"background","paint":{"background-color":"#F2F4F7"}}]}"""

@Composable
fun MapRoute(onStartInspection: (String) -> Unit, viewModel: MapViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(
        state = state,
        onSiteTap = viewModel::onSiteTap,
        onStartInspection = onStartInspection,
        onLocationTrackingChanged = viewModel::setLocationTrackingEnabled
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapScreen(
    state: MapUiState,
    onSiteTap: (String) -> Unit,
    onStartInspection: (String) -> Unit,
    onLocationTrackingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasRequestedPermission = true
        val granted = context.hasLocationPermission()
        permissionDenied = !granted
        permanentlyDenied = !granted && activity?.let {
            !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) &&
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
        } == true
        onLocationTrackingChanged(granted)
    }

    LaunchedEffect(Unit) {
        if (context.hasLocationPermission()) {
            permissionDenied = false
            onLocationTrackingChanged(true)
        } else if (hasRequestedPermission) {
            permissionDenied = true
        } else {
            showPermissionRationale = true
        }
    }

    DisposableEffect(lifecycleOwner, hasRequestedPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasRequestedPermission) {
                val granted = context.hasLocationPermission()
                permissionDenied = !granted
                permanentlyDenied = !granted && activity?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
                } == true
                onLocationTrackingChanged(granted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        SitesMap(
            sites = state.sites,
            userLocation = state.userLocation,
            selectedSiteId = state.selectedSite?.id,
            onSiteTap = onSiteTap
        )
        if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
        else if (state.sites.isEmpty()) Text("No sites to show", Modifier.align(Alignment.Center))
        val locationMessage = when (state.locationState) {
            is LocationState.Fix -> null
            LocationState.NoPermission -> if (permissionDenied) "Location permission was denied." else null
            LocationState.GpsOff -> "Location is turned off. Turn on GPS to see your position."
            LocationState.Unavailable -> "Your location is currently unavailable. Check GPS and try again."
        }
        if (locationMessage != null) {
            LocationStatusBanner(
                message = locationMessage,
                actionLabel = when {
                    state.locationState == LocationState.GpsOff -> "Location settings"
                    permissionDenied && permanentlyDenied -> "App settings"
                    permissionDenied -> "Try again"
                    else -> null
                },
                onAction = {
                    when {
                        state.locationState == LocationState.GpsOff -> context.openLocationSettings()
                        permissionDenied && permanentlyDenied -> context.openAppSettings()
                        permissionDenied -> showPermissionRationale = true
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)
            )
        }
        state.selectedSite?.let { site ->
            ModalBottomSheet(onDismissRequest = { onSiteTap("") }) {
                Surface(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
                    Column {
                        Text(site.name, style = MaterialTheme.typography.titleLarge)
                        Text("${site.areaSqm.toInt()} m²", style = MaterialTheme.typography.bodyLarge)
                        when (state.locationState) {
                            is LocationState.Fix -> {
                                if (state.isUserInsideSelectedSite == true) Text("You are inside")
                                else Text("${state.distanceToSelectedSiteM?.toInt() ?: 0} m to the site boundary")
                                if (state.checkInAccuracyM?.let { it > MAX_CHECK_IN_ACCURACY_M } == true) {
                                    Text("GPS accuracy is too low for check-in. Wait for a fix of ${MAX_CHECK_IN_ACCURACY_M.toInt()} m or better.")
                                }
                            }
                            LocationState.NoPermission -> Text("Allow location access to see your distance and check in.")
                            LocationState.GpsOff -> Text("GPS is off. Turn it on to see your distance.")
                            LocationState.Unavailable -> Text("Location is unavailable. Check GPS and try again.")
                        }
                        Button(onClick = { onStartInspection(site.id) }, enabled = state.canCheckIn) {
                            Text("Check in")
                        }
                    }
                }
            }
        }

        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = { Text("Allow location access") },
                text = { Text("FieldPin uses your location to show where you are and how far you are from a site while the map is open.") },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionRationale = false
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }) { Text("Continue") }
                },
                dismissButton = { TextButton(onClick = { showPermissionRationale = false }) { Text("Not now") } }
            )
        }
    }
}

@Composable
private fun LocationStatusBanner(message: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Draws site polygons; an empty id from the tap callback means the map background was tapped. */
@Composable
fun SitesMap(
    sites: List<Site>,
    userLocation: LatLng?,
    selectedSiteId: String?,
    onSiteTap: (siteId: String) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember(context) {
        MapLibre.getInstance(context, null, WellKnownTileServer.MapLibre)
        MapView(context)
    }
    val currentOnSiteTap by rememberUpdatedState(onSiteTap)
    var map by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember(mapView) { mutableStateOf(false) }
    var didFrameSites by remember(mapView) { mutableStateOf(false) }
    var recenterRequest by remember(mapView) { mutableStateOf(0) }

    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { _: Context ->
                mapView.apply {
                    getMapAsync { readyMap ->
                        map = readyMap
                        val failedListener = MapView.OnDidFailLoadingMapListener { _ ->
                            if (!styleReady) {
                                readyMap.setStyle(Style.Builder().fromJson(OFFLINE_FALLBACK_STYLE)) { style ->
                                    installSiteLayers(style, readyMap, sites, userLocation, selectedSiteId, currentOnSiteTap)
                                    styleReady = true
                                }
                            }
                        }
                        mapView.addOnDidFailLoadingMapListener(failedListener)
                        readyMap.setStyle(Style.Builder().fromUri(BuildConfig.MAP_STYLE_URL)) { style ->
                            installSiteLayers(style, readyMap, sites, userLocation, selectedSiteId, currentOnSiteTap)
                            styleReady = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = { recenterRequest += 1 },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            enabled = userLocation != null
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My location")
        }
    }

    LaunchedEffect(map, styleReady, sites, userLocation) {
        if (!styleReady) return@LaunchedEffect
        val readyMap = map ?: return@LaunchedEffect
        (readyMap.style?.getSource(SOURCE_ID) as? GeoJsonSource)
            ?.setGeoJson(FeatureCollection.fromFeatures(mapFeatures(sites, userLocation)))
        if (!didFrameSites) {
            moveCameraToSites(readyMap, sites)
            didFrameSites = sites.isNotEmpty()
        }
    }
    LaunchedEffect(map, styleReady, selectedSiteId) {
        if (!styleReady) return@LaunchedEffect
        val selectedLayer = map?.style?.getLayer(SELECTED_OUTLINE_LAYER_ID) as? LineLayer
        selectedLayer?.setFilter(Expression.eq(Expression.get("siteId"), Expression.literal(selectedSiteId.orEmpty())))
    }
    LaunchedEffect(map, recenterRequest) {
        if (recenterRequest > 0 && userLocation != null) {
            val point = org.maplibre.android.geometry.LatLng(userLocation.latitude, userLocation.longitude)
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 16.0))
        }
    }
}

private fun siteFeature(site: Site): Feature? {
    if (site.boundary.size < 3) return null
    val ring = site.boundary.map { Point.fromLngLat(it.longitude, it.latitude) }.toMutableList()
    if (ring.first() != ring.last()) ring += ring.first()
    return Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
        addStringProperty("siteId", site.id)
        addStringProperty("status", site.status.name)
    }
}

private fun mapFeatures(sites: List<Site>, userLocation: LatLng?): List<Feature> = buildList {
    sites.mapNotNullTo(this, ::siteFeature)
    userLocation?.let { location ->
        add(Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude)).apply {
            addStringProperty("featureType", "userLocation")
        })
    }
}

private fun installSiteLayers(
    style: Style,
    map: MapLibreMap,
    sites: List<Site>,
    userLocation: LatLng?,
    selectedSiteId: String?,
    onSiteTap: (String) -> Unit
) {
    if (style.getSource(SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(mapFeatures(sites, userLocation))))
        val statusColor = Expression.match(
            Expression.get("status"),
            Expression.color(0xFF2E7D32.toInt()),
            Expression.stop("NEEDS_REPAIR", Expression.color(0xFFFFB300.toInt())),
            Expression.stop("CLOSED", Expression.color(0xFFD32F2F.toInt()))
        )
        style.addLayer(FillLayer(FILL_LAYER_ID, SOURCE_ID).withProperties(fillColor(statusColor), fillOpacity(0.38f)))
        style.addLayer(LineLayer(SELECTED_OUTLINE_LAYER_ID, SOURCE_ID)
            .withFilter(Expression.eq(Expression.get("siteId"), Expression.literal(selectedSiteId.orEmpty())))
            .withProperties(lineColor(AndroidColor.WHITE), lineWidth(4f)))
        style.addLayer(CircleLayer(USER_LOCATION_LAYER_ID, SOURCE_ID)
            .withFilter(Expression.eq(Expression.get("featureType"), Expression.literal("userLocation")))
            .withProperties(circleColor(0xFF1976D2.toInt()), circleRadius(7f), circleStrokeColor(AndroidColor.WHITE), circleStrokeWidth(2f)))
    }
    map.addOnMapClickListener { point ->
        val screenPoint = map.projection.toScreenLocation(point)
        val hit = map.queryRenderedFeatures(screenPoint, FILL_LAYER_ID).firstOrNull()
        onSiteTap(hit?.getStringProperty("siteId") ?: "")
        hit != null
    }
}

private fun moveCameraToSites(map: MapLibreMap, sites: List<Site>) {
    val points = sites.flatMap { it.boundary }
        .map { org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) }
    if (points.isEmpty()) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(26.9124, 75.7873), 11.0))
        return
    }
    val bounds = LatLngBounds.Builder().includes(points).build()
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    })
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
