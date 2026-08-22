package com.greenshark.quickattend.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.ApprovalState
import com.greenshark.quickattend.ui.component.scan.AttendanceConfirmationCard
import com.greenshark.quickattend.ui.component.scan.CameraPermissionPanel
import com.greenshark.quickattend.ui.component.scan.CameraPreview
import com.greenshark.quickattend.ui.component.scan.ScanErrorCard
import com.greenshark.quickattend.ui.component.scan.ScannerViewfinder
import com.greenshark.quickattend.ui.component.scan.previewRecordedAttendance
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * Created by Carlos Jiménez on 22-Aug-24.
 */

/**
 * Recording attendance by pointing the camera at an event's QR.
 *
 * The screen owns nothing but the camera and the two permission dialogs. What a
 * scan means — which session it lands on, whether the venue check passes, and
 * whether the host still has to approve the person — is decided in
 * [ScanViewModel], so the same rules hold however a scan gets triggered.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: ScanViewModel = viewModel(factory = ScanViewModel.factory(LocalContext.current))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    // Android reports no rationale both before the first ask and after a hard
    // denial, so the only way to tell them apart is to remember having asked.
    var cameraAsked by rememberSaveable { mutableStateOf(false) }
    // Coarse alone is enough for a venue sized radius, and Android hands it over
    // when the user picks "approximate" on the dialog.
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        onPermissionsResult = { results ->
            viewModel.onAction(ScanAction.LocationPermissionResult(results.values.any { it }))
        }
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ScanEvent.RequestLocationPermission ->
                    locationPermissions.launchMultiplePermissionRequest()

                is ScanEvent.AttendanceRecorded -> haptics.performHapticFeedback(
                    if (event.recorded.approval == ApprovalState.ENROLLED) {
                        HapticFeedbackType.Confirm
                    } else {
                        HapticFeedbackType.ContextClick
                    }
                )
            }
        }
    }

    ScanContent(
        state = state,
        hasCameraPermission = cameraPermission.status.isGranted,
        isCameraPermanentlyDenied = cameraAsked &&
                !cameraPermission.status.isGranted &&
                !cameraPermission.status.shouldShowRationale,
        onAction = viewModel::onAction,
        onBack = { navController.popBackStack() },
        onGrantCamera = {
            cameraAsked = true
            cameraPermission.launchPermissionRequest()
        },
        onOpenSettings = context::openAppSettings
    )
}

@Composable
fun ScanContent(
    state: ScanState,
    hasCameraPermission: Boolean,
    onAction: (ScanAction) -> Unit,
    onBack: () -> Unit,
    onGrantCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isCameraPermanentlyDenied: Boolean = false
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (hasCameraPermission) Black else White
    ) { innerPadding ->
        if (!hasCameraPermission) {
            CameraPermissionPanel(
                onGrant = onGrantCamera,
                onOpenSettings = onOpenSettings,
                isPermanentlyDenied = isCameraPermanentlyDenied,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(
                onQrCode = { payload -> onAction(ScanAction.QrScanned(payload)) },
                isScanning = state.isCameraActive,
                isTorchOn = state.isTorchOn,
                modifier = Modifier.fillMaxSize()
            )
            ScannerViewfinder(
                isScanning = state.isCameraActive,
                accent = if (state.isCameraActive) Turquoise else White
            )

            ScanTopBar(
                isTorchOn = state.isTorchOn,
                onBack = onBack,
                onToggleTorch = { onAction(ScanAction.TorchToggled) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                ScanFooter(state = state, onAction = onAction, onOpenSettings = onOpenSettings)
            }
        }
    }
}

/** Whatever the scan is up to right now: a hint, a spinner, or a result. */
@Composable
private fun ScanFooter(
    state: ScanState,
    onAction: (ScanAction) -> Unit,
    onOpenSettings: () -> Unit
) {
    // The card slides up from under the viewfinder rather than appearing on top
    // of it, which reads as an answer to the scan instead of an interruption.
    AnimatedVisibility(
        visible = state.stage == ScanStage.RESULT,
        enter = slideInVertically { height -> height } + fadeIn(),
        exit = slideOutVertically { height -> height } + fadeOut()
    ) {
        val recorded = state.recorded
        val error = state.error
        when {
            recorded != null -> AttendanceConfirmationCard(
                recorded = recorded,
                onDone = { onAction(ScanAction.Dismissed) },
                onScanAnother = { onAction(ScanAction.Dismissed) }
            )

            error != null -> ScanErrorCard(
                error = error,
                onDismiss = { onAction(ScanAction.Dismissed) },
                onOpenSettings = onOpenSettings
            )
        }
    }

    AnimatedVisibility(
        visible = state.stage != ScanStage.RESULT,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ScanStatusStrip(state = state)
    }
}

/** The dark pill under the viewfinder that narrates the slow steps. */
@Composable
private fun ScanStatusStrip(state: ScanState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(size = 18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp, alignment = Alignment.CenterHorizontally)
    ) {
        if (state.isWorking) {
            CircularProgressIndicator(
                color = Turquoise,
                strokeWidth = 2.dp,
                modifier = Modifier.size(size = 20.dp)
            )
        }
        Text(
            text = stringResource(
                when (state.stage) {
                    ScanStage.RESOLVING -> R.string.scan_resolving
                    ScanStage.LOCATING -> R.string.scan_locating
                    else -> R.string.scan_hint
                }
            ),
            color = White,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScanTopBar(
    isTorchOn: Boolean,
    onBack: () -> Unit,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScrimIconButton(
            icon = R.drawable.keyboard_return,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack
        )
        Text(
            text = stringResource(R.string.scan_title),
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(weight = 1f)
        )
        ScrimIconButton(
            icon = if (isTorchOn) R.drawable.flash_on else R.drawable.flash_off,
            contentDescription = stringResource(
                if (isTorchOn) R.string.scan_torch_off else R.string.scan_torch_on
            ),
            onClick = onToggleTorch,
            tint = if (isTorchOn) Turquoise else White
        )
    }
}

/** An icon that stays readable over whatever the camera happens to be showing. */
@Composable
private fun ScrimIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = White
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.background(color = Black.copy(alpha = 0.4f), shape = CircleShape)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

/** The app's own page in Settings, which is the only way back from a hard denial. */
private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

@Preview(showBackground = true, heightDp = 760)
@Composable
fun ScanContentScanningPreview() {
    QuickAttendTheme {
        ScanContent(
            state = ScanState(),
            hasCameraPermission = true,
            onAction = {},
            onBack = {},
            onGrantCamera = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
fun ScanContentLocatingPreview() {
    QuickAttendTheme {
        ScanContent(
            state = ScanState(stage = ScanStage.LOCATING),
            hasCameraPermission = true,
            onAction = {},
            onBack = {},
            onGrantCamera = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
fun ScanContentRecordedPreview() {
    QuickAttendTheme {
        ScanContent(
            state = ScanState(
                stage = ScanStage.RESULT,
                recorded = previewRecordedAttendance(distanceMeters = 22.0)
            ),
            hasCameraPermission = true,
            onAction = {},
            onBack = {},
            onGrantCamera = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
fun ScanContentPendingPreview() {
    QuickAttendTheme {
        ScanContent(
            state = ScanState(
                stage = ScanStage.RESULT,
                recorded = previewRecordedAttendance(
                    approval = ApprovalState.PENDING_APPROVAL,
                    minutesLate = 12
                )
            ),
            hasCameraPermission = true,
            onAction = {},
            onBack = {},
            onGrantCamera = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
fun ScanContentErrorPreview() {
    QuickAttendTheme {
        ScanContent(
            state = ScanState(
                stage = ScanStage.RESULT,
                error = ScanError(
                    reason = ScanErrorReason.TOO_FAR,
                    eventTitle = "Clase de Yoga",
                    distanceMeters = 412.0,
                    allowedMeters = 75
                )
            ),
            hasCameraPermission = true,
            onAction = {},
            onBack = {},
            onGrantCamera = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
fun ScanContentNoPermissionPreview() {
    QuickAttendTheme {
        ScanContent(
            state = ScanState(),
            hasCameraPermission = false,
            onAction = {},
            onBack = {},
            onGrantCamera = {},
            onOpenSettings = {}
        )
    }
}
