package com.galaxyjoy.cpuinfo.feat.app

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.galaxyjoy.cpuinfo.domain.model.ExtendedApplicationData
import com.galaxyjoy.cpuinfo.ui.component.CpuSnackbar
import com.galaxyjoy.cpuinfo.ui.component.CpuSwitchBox
import com.galaxyjoy.cpuinfo.ui.component.DraggableBox
import com.galaxyjoy.cpuinfo.ui.component.SurfaceTopAppBar
import com.galaxyjoy.cpuinfo.ui.theme.CpuInfoTheme
import com.galaxyjoy.cpuinfo.ui.theme.rowActionIconSize
import com.galaxyjoy.cpuinfo.ui.theme.spacingSmall
import com.galaxyjoy.cpuinfo.ui.theme.spacingXSmall
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import com.galaxyjoy.cpuinfo.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ApplicationsScreen(
    uiState: VMNewApplications.UiState,
    onAppClicked: (packageName: String) -> Unit,
    onRefreshApplications: () -> Unit,
    onSnackbarDismissed: () -> Unit,
    onCardExpanded: (id: String) -> Unit,
    onCardCollapsed: (id: String) -> Unit,
    onAppUninstallClicked: (id: String) -> Unit,
    onAppSettingsClicked: (id: String) -> Unit,
    onNativeLibsClicked: (nativeLibraryDir: String) -> Unit,
    onSystemAppsSwitched: (enabled: Boolean) -> Unit,
    onOpenPlayStore: (packageName: String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        scope.launch {
            if (uiState.snackbarMessage != -1) {
                val result = snackbarHostState.showSnackbar(
                    context.getString(uiState.snackbarMessage)
                )
                if (result == SnackbarResult.Dismissed) {
                    onSnackbarDismissed()
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopBar(
                withSystemApps = uiState.withSystemApps,
                onSystemAppsSwitched = onSystemAppsSwitched
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                CpuSnackbar(data)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPaddingModifier ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = uiState.isLoading,
            onRefresh = { onRefreshApplications() },
        )
        Box(
            modifier = Modifier
                .pullRefresh(pullRefreshState)
                .padding(innerPaddingModifier),
        ) {
            ApplicationsList(
                appList = uiState.applications,
                revealedCardId = uiState.revealedCardId,
                onAppClicked = onAppClicked,
                onCardExpanded = onCardExpanded,
                onCardCollapsed = onCardCollapsed,
                onAppUninstallClicked = onAppUninstallClicked,
                onAppSettingsClicked = onAppSettingsClicked,
                onNativeLibsClicked = onNativeLibsClicked,
                onOpenPlayStore = onOpenPlayStore,
            )
            PullRefreshIndicator(
                refreshing = uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun TopBar(
    withSystemApps: Boolean,
    onSystemAppsSwitched: (enabled: Boolean) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    SurfaceTopAppBar(
        title = stringResource(id = R.string.applications),
        actions = {
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        CpuSwitchBox(
                            text = stringResource(id = R.string.apps_show_system_apps),
                            isChecked = withSystemApps,
                            onCheckedChange = { onSystemAppsSwitched(!withSystemApps) }
                        )
                    },
                    onClick = { onSystemAppsSwitched(!withSystemApps) },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.apps_sort_order),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = { },
                )
            }
        }
    )
}

@Composable
private fun ApplicationsList(
    appList: List<ExtendedApplicationData>,
    @Suppress("UNUSED_PARAMETER") revealedCardId: String?,
    onAppClicked: (packageName: String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onCardExpanded: (id: String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onCardCollapsed: (id: String) -> Unit,
    onAppUninstallClicked: (id: String) -> Unit,
    onAppSettingsClicked: (id: String) -> Unit,
    onNativeLibsClicked: (nativeLibraryDir: String) -> Unit,
    onOpenPlayStore: (packageName: String) -> Unit = {},
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        itemsIndexed(
            items = appList,
            key = { _, item -> item.packageName }
        ) { index, item ->
            var showBottomSheetForItem by remember { mutableStateOf(false) }

            ApplicationItem(
                appData = item,
                onAppClicked = onAppClicked,
                onLongPress = {
                    timber.log.Timber.d("App long press: ${item.packageName}")
                    showBottomSheetForItem = true
                },
                onNativeLibsClicked = onNativeLibsClicked,
                showBottomSheet = showBottomSheetForItem,
                onBottomSheetDismiss = { showBottomSheetForItem = false },
                onAppUninstallClicked = onAppUninstallClicked,
                onAppSettingsClicked = onAppSettingsClicked,
                onOpenPlayStore = onOpenPlayStore,
            )

            if (index < appList.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ApplicationItem(
    appData: ExtendedApplicationData,
    onAppClicked: (packageName: String) -> Unit,
    onLongPress: () -> Unit,
    onNativeLibsClicked: (nativeLibraryDir: String) -> Unit,
    showBottomSheet: Boolean,
    onBottomSheetDismiss: () -> Unit,
    onAppUninstallClicked: (packageName: String) -> Unit,
    onAppSettingsClicked: (packageName: String) -> Unit,
    onOpenPlayStore: (packageName: String) -> Unit,
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
            .combinedClickable(
                onClick = {
                    timber.log.Timber.d("App clicked: ${appData.packageName}")
                    onAppClicked(appData.packageName)
                },
                onLongClick = {
                    timber.log.Timber.d("App long clicked: ${appData.packageName}")
                    onLongPress()
                }
            )
            .padding(spacingSmall),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(appData.appIconUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.size(50.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = spacingXSmall)
        ) {
            Text(
                text = appData.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = appData.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (appData.hasNativeLibs) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(spacingXSmall))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.ic_c_plus_plus)
                    .build(),
                contentDescription = stringResource(id = R.string.native_libs),
                modifier = Modifier
                    .requiredSize(40.dp)
                    .clickable { appData.nativeLibraryDir?.let { onNativeLibsClicked(it) } },
            )
        }
    }

    // Bottom Sheet Dialog
    if (showBottomSheet) {
        AppActionsBottomSheet(
            appName = appData.name,
            packageName = appData.packageName,
            onDismiss = onBottomSheetDismiss,
            onSettings = {
                onBottomSheetDismiss()
                onAppSettingsClicked(appData.packageName)
            },
            onUninstall = {
                onBottomSheetDismiss()
                onAppUninstallClicked(appData.packageName)
            },
            onOpenPlayStore = {
                onBottomSheetDismiss()
                onOpenPlayStore(appData.packageName)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppActionsBottomSheet(
    appName: String,
    packageName: String,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onUninstall: () -> Unit,
    onOpenPlayStore: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Settings option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSettings)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Uninstall option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUninstall)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Uninstall",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Uninstall",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Play Store option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlayStore)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "View in Play Store",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "View in Play Store",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun LoadingDialog(message: String) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { /* Block dismiss */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ApplicationInfoPreview() {
    CpuInfoTheme {
        ApplicationsScreen(
            uiState = VMNewApplications.UiState(
                applications = persistentListOf(previewAppData1, previewAppData2)
            ),
            onAppClicked = {},
            onRefreshApplications = {},
            onSnackbarDismissed = {},
            onCardExpanded = {},
            onCardCollapsed = {},
            onAppSettingsClicked = {},
            onAppUninstallClicked = {},
            onNativeLibsClicked = {},
            onSystemAppsSwitched = {},
        )
    }
}

private val previewAppData1 = ExtendedApplicationData(
    name = "Cpu Info",
    packageName = "com.galaxyjoy.cpuinfo",
    sourceDir = "/testDir",
    nativeLibraryDir = null,
    hasNativeLibs = false,
    appIconUri = Uri.parse("https://avatars.githubusercontent.com/u/6407041?s=32&v=4")
)

private val previewAppData2 = ExtendedApplicationData(
    name = "Cpu Info1",
    packageName = "com.galaxyjoy.cpuinfo1",
    sourceDir = "/testDir",
    nativeLibraryDir = null,
    hasNativeLibs = false,
    appIconUri = Uri.parse("https://avatars.githubusercontent.com/u/6407041?s=32&v=4")
)
