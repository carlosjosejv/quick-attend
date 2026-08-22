/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens.host

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.export.AttendanceExporter
import com.greenshark.quickattend.export.ColumnAlignment
import com.greenshark.quickattend.export.ExportColumn
import com.greenshark.quickattend.export.ExportDocument
import com.greenshark.quickattend.export.ExportFormat
import com.greenshark.quickattend.export.ExportRow
import com.greenshark.quickattend.ui.component.common.LoadingBox
import com.greenshark.quickattend.ui.component.export.ExportFormatSelector
import com.greenshark.quickattend.ui.component.export.ExportTablePreview
import com.greenshark.quickattend.ui.component.export.PdfPagesPreview
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * The attendance record as it will leave the app. The host picks the format,
 * looks at the actual file, and only then shares or opens it.
 */
@Composable
fun ExportPreviewScreen(
    navController: NavController,
    eventId: String,
    userId: String? = null,
    viewModel: ExportPreviewViewModel = viewModel(
        factory = ExportPreviewViewModel.factory(eventId = eventId, userId = userId.orEmpty())
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val noAppMessage = stringResource(R.string.export_no_app)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val intent = when (event) {
                is ExportPreviewEvent.Share ->
                    AttendanceExporter.shareIntent(context, event.exported)

                is ExportPreviewEvent.Open ->
                    AttendanceExporter.openIntent(context, event.exported)
            }
            if (!context.tryStart(intent)) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(noAppMessage)
            }
        }
    }

    ExportPreviewContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = { navController.popBackStack() },
        onShare = viewModel::share,
        onOpen = viewModel::open,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPreviewContent(
    state: ExportPreviewState,
    onAction: (ExportPreviewAction) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.export_title),
                            color = Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        state.document?.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                            Text(
                                text = subtitle,
                                color = Gray,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.keyboard_return),
                            contentDescription = stringResource(R.string.action_back),
                            tint = Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        bottomBar = {
            ExportActions(
                exportedName = state.exported?.name,
                exportedSize = state.exported?.sizeLabel,
                enabled = state.canShare,
                onShare = onShare,
                onOpen = onOpen
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingBox(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            state.error?.let { error ->
                ErrorLine(message = error.message(), onRetry = {
                    onAction(ExportPreviewAction.Retry)
                })
            }

            ExportFormatSelector(
                selected = state.format,
                onSelect = { onAction(ExportPreviewAction.FormatChanged(it)) },
                enabled = !state.isRendering
            )

            val document = state.document
            if (document != null) {
                Text(
                    text = stringResource(R.string.export_rows, document.rows.size),
                    color = Gray,
                    fontSize = 13.sp
                )
            }

            if (state.writeFailed) {
                ErrorLine(
                    message = stringResource(R.string.export_failed),
                    onRetry = { onAction(ExportPreviewAction.Retry) }
                )
                return@Column
            }

            if (state.isRendering) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Turquoise)
                }
                return@Column
            }

            when (state.format) {
                ExportFormat.PDF -> PdfPagesPreview(pages = state.pages)
                ExportFormat.EXCEL -> {
                    if (document != null) {
                        ExportTablePreview(document = document)
                    }
                    Text(
                        text = stringResource(R.string.export_excel_note),
                        color = Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportActions(
    exportedName: String?,
    exportedSize: String?,
    enabled: Boolean,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = White)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        if (exportedName != null) {
            Text(
                text = listOfNotNull(exportedName, exportedSize).joinToString(separator = " · "),
                color = Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            OutlinedButton(
                onClick = onOpen,
                enabled = enabled,
                shape = RoundedCornerShape(size = 14.dp),
                modifier = Modifier
                    .weight(weight = 1f)
                    .height(height = 52.dp)
            ) {
                Text(text = stringResource(R.string.action_open), color = Black)
            }
            Button(
                onClick = onShare,
                enabled = enabled,
                shape = RoundedCornerShape(size = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Turquoise,
                    contentColor = White
                ),
                modifier = Modifier
                    .weight(weight = 1f)
                    .height(height = 52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        modifier = Modifier.size(size = 18.dp)
                    )
                    Text(
                        text = stringResource(R.string.action_share),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorLine(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Ghost, shape = RoundedCornerShape(size = 12.dp))
            .padding(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Text(text = message, color = Red, fontSize = 15.sp)
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(size = 12.dp)) {
            Text(text = stringResource(R.string.action_retry), color = Black)
        }
    }
}

/** False when nothing on the phone handles the intent, which is not a crash. */
private fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (e: ActivityNotFoundException) {
    false
}

private fun DataError.message(): String = when (this) {
    DataError.NETWORK -> "No connection. Check your network and try again."
    DataError.UNAUTHORIZED -> "You are not allowed to export this record."
    DataError.NOT_FOUND -> "This event no longer exists."
    DataError.UNKNOWN -> "Something went wrong. Please try again."
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun ExportPreviewContentPreview() {
    QuickAttendTheme {
        ExportPreviewContent(
            state = ExportPreviewState(
                isLoading = false,
                format = ExportFormat.EXCEL,
                document = previewExportDocument()
            ),
            onAction = {},
            onBack = {},
            onShare = {},
            onOpen = {}
        )
    }
}

private fun previewExportDocument(): ExportDocument = ExportDocument(
    title = "Clase de Yoga",
    subtitle = "Attendance record · all attendants",
    columns = listOf(
        ExportColumn(title = "Attendant", weight = 2.4f),
        ExportColumn(title = "On time", weight = 0.9f, alignment = ColumnAlignment.CENTER),
        ExportColumn(title = "Score", weight = 0.9f, alignment = ColumnAlignment.CENTER),
        ExportColumn(title = "Signature", weight = 2.2f, alignment = ColumnAlignment.CENTER)
    ),
    rows = listOf(
        ExportRow(
            cells = listOf("Carlos Jiménez", "12", "96", ""),
            signature = AttendanceSignature.Code("QA-7F3A-91C2-88DE")
        ),
        ExportRow(
            cells = listOf("Ana Reyes", "3", "31", ""),
            signature = AttendanceSignature.None
        )
    )
)
