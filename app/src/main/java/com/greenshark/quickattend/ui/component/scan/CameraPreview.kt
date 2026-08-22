/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.scan

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.greenshark.quickattend.R
import com.greenshark.quickattend.qr.QrImageAnalyzer
import com.greenshark.quickattend.ui.theme.Black
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Live back camera with a QR reader wired into it.
 *
 * [onQrCode] fires once per scan rather than once per frame: the analyzer latches
 * on the first read, and only turning [isScanning] back on unlatches it. That is
 * what lets the caller show a result over a paused viewfinder and then carry on,
 * without a code left in shot recording the same attendance again.
 */
@Composable
fun CameraPreview(
    onQrCode: (String) -> Unit,
    modifier: Modifier = Modifier,
    isScanning: Boolean = true,
    isTorchOn: Boolean = false,
    onTorchAvailability: (Boolean) -> Unit = {}
) {
    // Previews and layout inspectors have no camera to bind, so they get the
    // dark plate the viewfinder would otherwise fill.
    if (LocalInspectionMode.current) {
        CameraPlaceholder(modifier = modifier)
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnQrCode by rememberUpdatedState(onQrCode)
    val latestOnTorchAvailability by rememberUpdatedState(onTorchAvailability)

    val previewView = remember(context) { context.newPreviewView() }
    val analyzer = remember { QrImageAnalyzer { code -> latestOnQrCode(code) } }
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(executor) {
        onDispose { executor.shutdown() }
    }

    LaunchedEffect(lifecycleOwner, previewView) {
        val cameraProvider = context.awaitCameraProvider()
        provider = cameraProvider
        camera = cameraProvider.bindScanner(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            analyzer = analyzer,
            executor = executor
        )
        latestOnTorchAvailability(camera?.cameraInfo?.hasFlashUnit() == true)
    }

    DisposableEffect(provider) {
        onDispose {
            // The use cases are bound to the activity, not to this composable,
            // so leaving the screen would otherwise leave the camera running
            // until the activity itself stopped.
            provider?.unbindAll()
            camera = null
        }
    }

    LaunchedEffect(isScanning) {
        // Unlatching on the way back in, not on the way out, so a code that is
        // still in shot when a result closes does not immediately re-fire.
        if (isScanning) analyzer.reset()
    }

    LaunchedEffect(camera, isTorchOn) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(isTorchOn)
    }

    val description = stringResource(R.string.scan_camera_description)
    AndroidView(
        factory = { previewView },
        modifier = modifier.semantics { contentDescription = description }
    )
}

/** What a design tool draws where the viewfinder goes. */
@Composable
private fun CameraPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(color = Black)
    )
}

private fun Context.newPreviewView(): PreviewView = PreviewView(this).apply {
    scaleType = PreviewView.ScaleType.FILL_CENTER
    // Compatible mode draws through a TextureView, which composes correctly
    // under the rounded viewfinder cut-out a SurfaceView would punch through.
    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
}

/**
 * Binds the two use cases a scanner needs and hands back the camera, so the
 * caller can drive the torch. Rebinding from scratch each time avoids leaving a
 * stale use case attached after a configuration change.
 */
private fun ProcessCameraProvider.bindScanner(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    analyzer: ImageAnalysis.Analyzer,
    executor: ExecutorService
): Camera? {
    val preview = Preview.Builder().build().apply {
        surfaceProvider = previewView.surfaceProvider
    }

    val analysis = ImageAnalysis.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build()
        )
        // Decoding is slower than the stream, so old frames are dropped rather
        // than queued; a scanner only ever cares about what the lens sees now.
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply { setAnalyzer(executor, analyzer) }

    return try {
        unbindAll()
        bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    } catch (e: IllegalArgumentException) {
        // No back camera on this device, or the use cases cannot be combined.
        null
    } catch (e: IllegalStateException) {
        null
    }
}

/** The provider comes back on a callback; this waits for it without blocking. */
private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider {
    val future = ProcessCameraProvider.getInstance(this)
    if (future.isDone) return future.get()

    return suspendCancellableCoroutine { continuation ->
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
        continuation.invokeOnCancellation { future.cancel(false) }
    }
}
