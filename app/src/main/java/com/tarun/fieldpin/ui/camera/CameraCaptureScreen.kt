package com.tarun.fieldpin.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tarun.fieldpin.data.photo.PhotoStamp
import com.tarun.fieldpin.data.photo.PhotoStamper
import com.tarun.fieldpin.domain.model.LocationState
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    siteName: String,
    checkInLat: Double,
    checkInLng: Double,
    checkInAccuracyM: Double,
    locationState: LocationState,
    onClose: () -> Unit,
    onPhotoSaved: (String, Long, Double, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val providerFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraHasFlash by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var frontCamera by rememberSaveable { mutableStateOf(false) }
    var flashEnabled by rememberSaveable { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val stamper = remember { PhotoStamper() }
    val boundProvider = remember { AtomicReference<ProcessCameraProvider?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRequested = true
        permissionGranted = granted
        permanentlyDenied = !granted && activity?.let {
            !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } == true
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            if (permissionRequested) errorMessage = "Camera permission was denied."
            else showRationale = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && permissionRequested) {
                permissionGranted = context.hasCameraPermission()
                if (permissionGranted) errorMessage = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(lifecycleOwner, previewView, permissionGranted, frontCamera) {
        val view = previewView
        if (view == null || !permissionGranted) return@DisposableEffect onDispose { }
        val listener = Runnable {
            try {
                val provider = providerFuture.get()
                boundProvider.set(provider)
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val capture = ImageCapture.Builder().setTargetRotation(view.display.rotation).build()
                val selector = if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                imageCapture = capture
                cameraHasFlash = camera.cameraInfo.hasFlashUnit()
                errorMessage = null
            } catch (error: Exception) {
                imageCapture = null
                errorMessage = "No camera is available: ${error.message ?: "camera could not start"}"
            }
        }
        providerFuture.addListener(listener, mainExecutor)
        onDispose {
            boundProvider.getAndSet(null)?.unbindAll()
            imageCapture = null
        }
    }

    val fix = locationState as? LocationState.Fix
    val stampLat = fix?.lat ?: checkInLat
    val stampLng = fix?.lng ?: checkInLng
    val stampAccuracy = fix?.accuracyM?.toDouble() ?: checkInAccuracyM

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Site photo") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to inspection")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).also { previewView = it } },
                    modifier = Modifier.fillMaxSize()
                )
                if (!permissionGranted) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage ?: "Camera permission is needed to take a stamped site photo.")
                        Button(onClick = {
                            if (permanentlyDenied) context.openAppSettings()
                            else showRationale = true
                        }) { Text(if (permanentlyDenied) "App settings" else "Allow camera") }
                    }
                } else if (errorMessage != null) {
                    Text(errorMessage.orEmpty(), Modifier.align(Alignment.Center).padding(16.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { frontCamera = !frontCamera; flashEnabled = false }, enabled = !isCapturing) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip camera")
                }
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    enabled = cameraHasFlash && !frontCamera && !isCapturing
                ) {
                    Icon(if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Toggle flash")
                }
                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton
                        isCapturing = true
                        val takenAt = System.currentTimeMillis()
                        val directory = File(context.filesDir, "photos").apply { mkdirs() }
                        val file = File(directory, "${UUID.randomUUID()}.jpg")
                        capture.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                        val output = ImageCapture.OutputFileOptions.Builder(file).build()
                        capture.takePicture(output, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                try {
                                    stamper.stamp(file, PhotoStamp(siteName, stampLat, stampLng, stampAccuracy, takenAt))
                                    onPhotoSaved(file.absolutePath, takenAt, stampLat, stampLng, stampAccuracy)
                                } catch (error: Exception) {
                                    errorMessage = error.message ?: "Could not stamp the captured photo."
                                } finally {
                                    isCapturing = false
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                errorMessage = exception.message ?: "Could not capture the photo."
                                isCapturing = false
                            }
                        })
                    },
                    enabled = permissionGranted && imageCapture != null && !isCapturing
                ) {
                    if (isCapturing) CircularProgressIndicator()
                    else Icon(Icons.Default.CameraAlt, contentDescription = "Capture photo")
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Allow camera access") },
            text = { Text("FieldPin uses the camera to capture evidence photos for this inspection. The saved photo includes the site and location stamp.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showRationale = false; onClose() }) { Text("Not now") } }
        )
    }
}

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    })
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
