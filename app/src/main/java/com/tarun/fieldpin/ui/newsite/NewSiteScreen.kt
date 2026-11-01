package com.tarun.fieldpin.ui.newsite

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarun.fieldpin.BuildConfig
import com.tarun.fieldpin.domain.model.LatLng
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

private const val EDITOR_SOURCE = "new-site-boundary"
private const val EDITOR_FILL = "new-site-boundary-fill"
private const val EDITOR_LINE = "new-site-boundary-line"
private const val EDITOR_CORNERS = "new-site-boundary-corners"
private const val EDITOR_FALLBACK_STYLE = """{"version":8,"sources":{},"layers":[{"id":"background","type":"background","paint":{"background-color":"#F2F4F7"}}]}"""

@Composable
fun NewSiteRoute(
    onBack: () -> Unit,
    onSiteSaved: () -> Unit,
    viewModel: NewSiteViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSiteSaved()
    }
    NewSiteScreen(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onCategoryChanged = viewModel::onCategoryChanged,
        onMapTap = viewModel::onMapTap,
        onUndo = viewModel::undoLastCorner,
        onSave = viewModel::saveSite,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSiteScreen(
    state: NewSiteUiState,
    onNameChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onMapTap: (LatLng) -> Unit,
    onUndo: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New site") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            BoundaryEditorMap(
                boundary = state.boundary,
                onMapTap = onMapTap,
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.weight(1f),
                    label = { Text("Site name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.category,
                    onValueChange = onCategoryChanged,
                    modifier = Modifier.weight(1f),
                    label = { Text("Category") },
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricValue("Area", "${"%.2f".format(state.areaSqm)} m²")
                MetricValue("Acres", "${"%.4f".format(state.areaAcres)}")
                MetricValue("Perimeter", "${"%.2f".format(state.perimeterM)} m")
            }
            Spacer(Modifier.height(8.dp))
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onUndo, enabled = state.boundary.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Text("Undo corner")
                }
                Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.weight(1f)) {
                    if (state.isSaving) CircularProgressIndicator()
                    else Text("Save site")
                }
            }
            if (state.boundary.size < 3) {
                Text("Tap the map to add at least 3 corners.", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MetricValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BoundaryEditorMap(boundary: List<LatLng>, onMapTap: (LatLng) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = remember(context) {
        MapLibre.getInstance(context, null, WellKnownTileServer.MapLibre)
        MapView(context)
    }
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val currentBoundary by rememberUpdatedState(boundary)
    var map by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember(mapView) { mutableStateOf(false) }

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

    AndroidView(
        factory = { _: Context ->
            mapView.apply {
                getMapAsync { readyMap ->
                    map = readyMap
                    fun finishStyle(style: Style) {
                        if (styleReady) return
                        installEditorLayers(style, currentBoundary)
                        styleReady = true
                        readyMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(26.9124, 75.7873), 13.0)
                        )
                        readyMap.addOnMapClickListener { point ->
                            currentOnMapTap(LatLng(point.latitude, point.longitude))
                            true
                        }
                    }
                    val failedListener = MapView.OnDidFailLoadingMapListener { _ ->
                        if (!styleReady) {
                            readyMap.setStyle(Style.Builder().fromJson(EDITOR_FALLBACK_STYLE), ::finishStyle)
                        }
                    }
                    mapView.addOnDidFailLoadingMapListener(failedListener)
                    readyMap.setStyle(Style.Builder().fromUri(BuildConfig.MAP_STYLE_URL), ::finishStyle)
                }
            }
        },
        modifier = modifier
    )

    LaunchedEffect(map, styleReady, boundary) {
        if (!styleReady) return@LaunchedEffect
        val source = map?.style?.getSource(EDITOR_SOURCE) as? GeoJsonSource ?: return@LaunchedEffect
        source.setGeoJson(editorFeatures(boundary))
    }
}

private fun installEditorLayers(style: Style, boundary: List<LatLng>) {
    style.addSource(GeoJsonSource(EDITOR_SOURCE, editorFeatures(boundary)))
    val boundaryFilter = Expression.eq(Expression.get("kind"), Expression.literal("boundary"))
    style.addLayer(FillLayer(EDITOR_FILL, EDITOR_SOURCE)
        .withFilter(boundaryFilter)
        .withProperties(fillColor(AndroidColor.rgb(46, 125, 50)), fillOpacity(0.25f)))
    style.addLayer(LineLayer(EDITOR_LINE, EDITOR_SOURCE)
        .withFilter(boundaryFilter)
        .withProperties(lineColor(AndroidColor.rgb(46, 125, 50)), lineWidth(3f)))
    style.addLayer(CircleLayer(EDITOR_CORNERS, EDITOR_SOURCE)
        .withFilter(Expression.eq(Expression.get("kind"), Expression.literal("corner")))
        .withProperties(circleColor(AndroidColor.rgb(21, 101, 192)), circleRadius(6f)))
}

private fun editorFeatures(boundary: List<LatLng>): FeatureCollection {
    val features = mutableListOf<Feature>()
    boundary.forEachIndexed { index, point ->
        features += Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply {
            addStringProperty("kind", "corner")
            addNumberProperty("index", index + 1)
        }
    }
    when {
        boundary.size >= 3 -> {
            val ring = boundary.map { Point.fromLngLat(it.longitude, it.latitude) }.toMutableList()
            if (ring.first() != ring.last()) ring += ring.first()
            features += Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply { addStringProperty("kind", "boundary") }
        }
        boundary.size == 2 -> {
            features += Feature.fromGeometry(LineString.fromLngLats(boundary.map { Point.fromLngLat(it.longitude, it.latitude) }))
                .apply { addStringProperty("kind", "boundary") }
        }
    }
    return FeatureCollection.fromFeatures(features)
}
