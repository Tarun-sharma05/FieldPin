package com.tarun.fieldpin.ui.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarun.fieldpin.domain.model.Condition
import com.tarun.fieldpin.domain.model.LocationState
import com.tarun.fieldpin.domain.usecase.MAX_CHECK_IN_ACCURACY_M
import com.tarun.fieldpin.ui.camera.CameraCaptureScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InspectionRoute(
    siteId: String,
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: InspectionViewModel = koinViewModel { parametersOf(siteId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCamera by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) onSubmitted()
    }
    val site = state.site
    val checkInLat = state.checkInLat
    val checkInLng = state.checkInLng
    val checkInAccuracyM = state.checkInAccuracyM
    if (showCamera && site != null && checkInLat != null && checkInLng != null && checkInAccuracyM != null) {
        CameraCaptureScreen(
            siteName = site.name,
            checkInLat = checkInLat,
            checkInLng = checkInLng,
            checkInAccuracyM = checkInAccuracyM,
            locationState = state.locationState,
            onClose = { showCamera = false },
            onPhotoSaved = { path, takenAt, lat, lng, accuracy ->
                viewModel.onPhotoCaptured(path, takenAt, lat, lng, accuracy)
                showCamera = false
            }
        )
    } else {
        InspectionScreen(
            state = state,
            onBack = onBack,
            onOpenMap = onOpenMap,
            onStart = viewModel::startInspection,
            onConditionChanged = viewModel::onConditionChanged,
            onNotesChanged = viewModel::onNotesChanged,
            onAddPhoto = { showCamera = true },
            onSubmit = viewModel::submit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InspectionScreen(
    state: InspectionUiState,
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    onStart: () -> Unit,
    onConditionChanged: (Condition) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.site?.name ?: "Inspection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.site == null -> Text("This site could not be found.", color = MaterialTheme.colorScheme.error)
                state.isSubmitted -> Text("Inspection submitted.", style = MaterialTheme.typography.titleMedium)
                !state.isStarted -> {
                    Text("Check in at ${state.site.name}", style = MaterialTheme.typography.titleLarge)
                    LocationMessage(state = state, onOpenMap = onOpenMap)
                    Button(onClick = onStart, enabled = state.canStart, modifier = Modifier.fillMaxWidth()) {
                        Text("Start inspection")
                    }
                }
                else -> {
                    Text("Checked in at ${state.checkInAt}", style = MaterialTheme.typography.titleMedium)
                    Text("${state.checkInLat}, ${state.checkInLng} · ±${state.checkInAccuracyM?.toInt()} m")
                    Text("Condition", style = MaterialTheme.typography.titleMedium)
                    Condition.entries.forEach { condition ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.condition == condition,
                                onClick = { onConditionChanged(condition) }
                            )
                            Text(condition.name.lowercase().replaceFirstChar(Char::uppercase))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onAddPhoto) { Text("Add photo") }
                        Text("${state.photos.size} photo(s)")
                    }
                    state.photos.forEachIndexed { index, photo ->
                        Text("Photo ${index + 1}: ${photo.filePath.substringAfterLast('/')}")
                    }
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = onNotesChanged,
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        label = { Text("Notes") },
                        minLines = 3
                    )
                    state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = onSubmit,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSubmitting) CircularProgressIndicator()
                        else Text("Submit inspection")
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationMessage(state: InspectionUiState, onOpenMap: () -> Unit) {
    when (val location = state.locationState) {
        is LocationState.Fix -> when {
            location.accuracyM.toDouble() > MAX_CHECK_IN_ACCURACY_M -> {
                Text("GPS accuracy is ±${location.accuracyM.toInt()} m. Check-in requires ${MAX_CHECK_IN_ACCURACY_M.toInt()} m accuracy or better.")
            }
            state.isInsideSite == true -> Text("You are inside the site and can check in.")
            else -> Text("You are outside the site, ${state.distanceToSiteM?.toInt() ?: 0} m from the nearest boundary.")
        }
        LocationState.NoPermission -> {
            Text("Location permission is needed to check in. Open the Map screen to grant it.")
            Button(onClick = onOpenMap) { Text("Open map") }
        }
        LocationState.GpsOff -> Text("Location is turned off. Turn on GPS, then return to check in.")
        LocationState.Unavailable -> Text("Your location is unavailable. Wait for a GPS fix and try again.")
    }
}
