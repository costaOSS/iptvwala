package com.iptvwala.plainapp.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptvwala.presentation.ui.tv.home.TvNavigationRail

enum class PlainAppSection {
    REMOTE, CLIPBOARD, SOURCES, BROWSER, FILES, APPS, NOTIFICATIONS, DEVICE, VOLUME
}

@Composable
fun PlainAppPanelScreen(
    onNavigateBack: () -> Unit,
    viewModel: com.iptvwala.plainapp.viewmodel.PlainAppViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    var selectedSection by remember { mutableStateOf(PlainAppSection.REMOTE) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        PlainAppNavigationRail(
            selectedSection = selectedSection,
            onSectionSelect = { selectedSection = it },
            onBackClick = onNavigateBack
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedSection) {
                PlainAppSection.REMOTE -> RemoteControlPanel(onKeyPress = viewModel::sendKeyEvent)
                PlainAppSection.CLIPBOARD -> ClipboardPanel(
                    clipboardHistory = uiState.clipboardHistory,
                    onPaste = viewModel::onPasteFromClipboard,
                    onClear = viewModel::onClearClipboard
                )
                PlainAppSection.SOURCES -> SourceManagerPanel(
                    sources = uiState.sources,
                    onRefresh = viewModel::refreshSource,
                    onDelete = viewModel::deleteSource
                )
                PlainAppSection.BROWSER -> ChannelBrowserPanel(
                    channels = uiState.channels,
                    onChannelClick = viewModel::onPlayChannel
                )
                PlainAppSection.FILES -> FileManagerPanel(
                    files = uiState.files,
                    onPlay = viewModel::onPlayFile,
                    onDelete = viewModel::onDeleteFile
                )
                PlainAppSection.APPS -> AppLauncherPanel(
                    apps = uiState.installedApps,
                    onLaunch = viewModel::onLaunchApp,
                    onSearch = viewModel::onSearchApps
                )
                PlainAppSection.NOTIFICATIONS -> NotificationsPanel(
                    notifications = uiState.notifications,
                    onClear = viewModel::onClearNotifications,
                    onNotificationClick = viewModel::onNotificationClick
                )
                PlainAppSection.DEVICE -> DeviceInfoPanel(
                    deviceName = uiState.deviceName,
                    deviceIp = uiState.deviceIp,
                    appVersion = uiState.appVersion,
                    uptime = uiState.uptime,
                    serverRunning = uiState.serverRunning,
                    serverPort = uiState.serverPort,
                    onCopyIp = viewModel::onCopyIp
                )
                PlainAppSection.VOLUME -> VolumePanel(
                    volume = uiState.volume,
                    onVolumeChange = viewModel::onVolumeChange,
                    onWakeScreen = viewModel::onWakeScreen
                )
            }
        }
    }
}

@Composable
fun PlainAppNavigationRail(
    selectedSection: PlainAppSection,
    onSectionSelect: (PlainAppSection) -> Unit,
    onBackClick: () -> Unit
) {
    NavigationRail {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        
        HorizontalDivider()
        
        PlainAppSection.entries.forEach { section ->
            val icon: ImageVector = when (section) {
                PlainAppSection.REMOTE -> Icons.Default.Gamepad
                PlainAppSection.CLIPBOARD -> Icons.Default.ContentPaste
                PlainAppSection.SOURCES -> Icons.Default.Source
                PlainAppSection.BROWSER -> Icons.Default.Tv
                PlainAppSection.FILES -> Icons.Default.Folder
                PlainAppSection.APPS -> Icons.Default.Apps
                PlainAppSection.NOTIFICATIONS -> Icons.Default.Notifications
                PlainAppSection.DEVICE -> Icons.Default.Info
                PlainAppSection.VOLUME -> Icons.Default.VolumeUp
            }
            
            NavigationRailItem(
                selected = selectedSection == section,
                onClick = { onSectionSelect(section) },
                icon = { Icon(icon, section.name) },
                label = { Text(section.name.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.focusable()
            )
        }
    }
}

@Composable
fun RemoteControlPanel(onKeyPress: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Remote Control",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        RemoteDpad(onKeyPress = onKeyPress)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                "HOME" to Icons.Default.Home,
                "BACK" to Icons.Default.ArrowBack,
                "MENU" to Icons.Default.Menu,
                "SEARCH" to Icons.Default.Search,
                "PLAY" to Icons.Default.PlayArrow
            ).forEach { (label, icon) ->
                RemoteButton(
                    label = label,
                    icon = icon,
                    onClick = { onKeyPress(label) }
                )
            }
        }
    }
}

@Composable
fun RemoteDpad(onKeyPress: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(RoundedCornerShape(120.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            RemoteDpadButton("UP", Icons.Default.KeyboardArrowUp, Modifier.align(Alignment.TopCenter), onKeyPress)
            RemoteDpadButton("DOWN", Icons.Default.KeyboardArrowDown, Modifier.align(Alignment.BottomCenter), onKeyPress)
            RemoteDpadButton("LEFT", Icons.Default.KeyboardArrowLeft, Modifier.align(Alignment.CenterStart), onKeyPress)
            RemoteDpadButton("RIGHT", Icons.Default.KeyboardArrowRight, Modifier.align(Alignment.CenterEnd), onKeyPress)
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onKeyPress("OK") }
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    "OK",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun RemoteDpadButton(
    direction: String,
    icon: ImageVector,
    modifier: Modifier,
    onKeyPress: (String) -> Unit
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onKeyPress(direction) }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            direction,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun RemoteButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .focusable()
            .padding(12.dp)
    ) {
        Icon(
            icon,
            label,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun ClipboardPanel(
    clipboardHistory: List<String>,
    onPaste: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clipboard",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(Icons.Default.Delete, "Clear")
                Spacer(Modifier.width(8.dp))
                Text("Clear All")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (clipboardHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Clipboard is empty",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clipboardHistory.reversed()) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .clickable { onPaste(entry) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, null)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = entry,
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )
                            FilledTonalButton(onClick = { onPaste(entry) }) {
                                Text("Paste")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceManagerPanel(
    sources: List<com.iptvwala.domain.model.Source>,
    onRefresh: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Source Manager",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sources) { source ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = source.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${source.channelCount} channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(onClick = { onRefresh(source.id) }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                        IconButton(onClick = { onDelete(source.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelBrowserPanel(
    channels: List<com.iptvwala.domain.model.Channel>,
    onChannelClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Channel Browser",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(channels) { channel ->
                Card(
                    onClick = { onChannelClick(channel.id) },
                    modifier = Modifier.focusable()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (channel.logo != null) {
                                coil.compose.AsyncImage(
                                    model = channel.logo,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Tv, null)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileManagerPanel(
    files: List<com.iptvwala.domain.model.FileItem>,
    onPlay: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "File Manager",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { file ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (file.isDirectory) Icons.Default.Folder else Icons.Default.VideoFile,
                            null
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = file.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = formatFileSize(file.size),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (!file.isDirectory) {
                            IconButton(onClick = { onPlay(file.path) }) {
                                Icon(Icons.Default.PlayArrow, "Play")
                            }
                        }
                        IconButton(onClick = { onDelete(file.path) }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLauncherPanel(
    apps: List<com.iptvwala.domain.model.InstalledApp>,
    onLaunch: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it)
            },
            placeholder = { Text("Search apps...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps.filter { it.name.contains(searchQuery, ignoreCase = true) }) { app ->
                Card(
                    onClick = { onLaunch(app.packageName) },
                    modifier = Modifier
                        .focusable()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (app.iconBase64 != null) {
                            val bitmap = remember(app.iconBase64) {
                                try {
                                    val bytes = android.util.Base64.decode(app.iconBase64, android.util.Base64.NO_WRAP)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) { null }
                            }
                            bitmap?.let {
                                coil.compose.AsyncImage(
                                    model = it,
                                    contentDescription = app.name,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        } else {
                            Icon(Icons.Default.Apps, null, modifier = Modifier.size(48.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsPanel(
    notifications: List<com.iptvwala.domain.model.NotificationItem>,
    onClear: () -> Unit,
    onNotificationClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(Icons.Default.ClearAll, "Clear")
                Spacer(Modifier.width(8.dp))
                Text("Clear All")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .clickable { onNotificationClick(notification.packageName) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = notification.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = notification.appName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoPanel(
    deviceName: String,
    deviceIp: String,
    appVersion: String,
    uptime: Long,
    serverRunning: Boolean,
    serverPort: Int,
    onCopyIp: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Device Info",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("Device", deviceName)
                InfoRow("IP Address", deviceIp, onCopy = onCopyIp)
                InfoRow("App Version", appVersion)
                InfoRow("Android", android.os.Build.VERSION.RELEASE)
                InfoRow("Server", if (serverRunning) "Running on port $serverPort" else "Stopped")
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val qrData = "http://$deviceIp:$serverPort"
                val qrBitmap = remember(qrData) { generateQrCode(qrData) }
                qrBitmap?.let {
                    coil.compose.AsyncImage(
                        model = it,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(256.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, onCopy: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row {
            Text(text = value)
            if (onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun VolumePanel(
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    onWakeScreen: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Volume & Display",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Media Volume",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChange(it.toInt()) },
                    valueRange = 0f..15f,
                    steps = 14,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable()
                )
                Text(
                    text = "$volume / 15",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        
        Button(
            onClick = onWakeScreen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.WbSunny, null)
            Spacer(Modifier.width(8.dp))
            Text("Wake Screen")
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun generateQrCode(content: String): android.graphics.Bitmap? {
    return try {
        val size = 512
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val barcodeEncoder = com.journeyapps.barcodescanner.BarcodeEncoder()
        barcodeEncoder.encodeBitmap(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
    } catch (e: Exception) {
        null
    }
}
