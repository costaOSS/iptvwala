package com.iptvwala.presentation.ui.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptvwala.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlainApp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "Sources") {
                    SettingsItem(
                        icon = Icons.Default.Source,
                        title = "Manage Sources",
                        subtitle = "${uiState.sourceCount} sources",
                        onClick = { /* Navigate to sources */ }
                    )
                }
            }
            
            item {
                SettingsSection(title = "EPG") {
                    SettingsItem(
                        icon = Icons.Default.Schedule,
                        title = "EPG Settings",
                        subtitle = "Last refresh: ${uiState.lastEpgRefresh}",
                        onClick = { /* Navigate to EPG settings */ }
                    )
                    SettingsItem(
                        icon = Icons.Default.Refresh,
                        title = "Refresh EPG Now",
                        subtitle = "Refresh all program data",
                        onClick = { viewModel.refreshEpg() }
                    )
                }
            }
            
            item {
                SettingsSection(title = "PlainApp Server") {
                    SettingsSwitch(
                        icon = Icons.Default.Server,
                        title = "Server Enabled",
                        checked = uiState.serverEnabled,
                        onCheckedChange = { viewModel.setServerEnabled(it) }
                    )
                    if (uiState.serverEnabled) {
                        SettingsItem(
                            icon = Icons.Default.Language,
                            title = "Server URL",
                            subtitle = "http://${uiState.serverIp}:${uiState.serverPort}",
                            onClick = { }
                        )
                        SettingsItem(
                            icon = Icons.Default.QrCode,
                            title = "Show QR Code",
                            subtitle = "Scan to access from browser",
                            onClick = { /* Show QR dialog */ }
                        )
                    }
                    SettingsItem(
                        icon = Icons.Default.Dashboard,
                        title = "PlainApp Panel",
                        subtitle = "Open in-app control panel",
                        onClick = onNavigateToPlainApp
                    )
                }
            }
            
            item {
                SettingsSection(title = "Player") {
                    var qualityExpanded by remember { mutableStateOf(false) }
                    SettingsDropdown(
                        icon = Icons.Default.HighQuality,
                        title = "Video Quality",
                        value = uiState.videoQuality,
                        expanded = qualityExpanded,
                        onExpandedChange = { qualityExpanded = it },
                        options = listOf("Auto", "1080p", "720p", "480p"),
                        onOptionSelected = { viewModel.setVideoQuality(it) }
                    )
                    
                    var bufferExpanded by remember { mutableStateOf(false) }
                    SettingsDropdown(
                        icon = Icons.Default.BufferStorage,
                        title = "Buffer Size",
                        value = uiState.bufferSize,
                        expanded = bufferExpanded,
                        onExpandedChange = { bufferExpanded = it },
                        options = listOf("Low", "Medium", "High"),
                        onOptionSelected = { viewModel.setBufferSize(it) }
                    )
                    
                    SettingsSwitch(
                        icon = Icons.Default.BackgroundPlayback,
                        title = "Background Playback",
                        checked = uiState.backgroundPlayback,
                        onCheckedChange = { viewModel.setBackgroundPlayback(it) }
                    )
                }
            }
            
            item {
                SettingsSection(title = "Appearance") {
                    var themeExpanded by remember { mutableStateOf(false) }
                    SettingsDropdown(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        value = uiState.theme,
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = it },
                        options = listOf("System", "Light", "Dark"),
                        onOptionSelected = { viewModel.setTheme(it) }
                    )
                    
                    var layoutExpanded by remember { mutableStateOf(false) }
                    SettingsDropdown(
                        icon = Icons.Default.ViewModule,
                        title = "Channel Layout",
                        value = uiState.channelLayout,
                        expanded = layoutExpanded,
                        onExpandedChange = { layoutExpanded = it },
                        options = listOf("Grid", "List"),
                        onOptionSelected = { viewModel.setChannelLayout(it) }
                    )
                    
                    SettingsSwitch(
                        icon = Icons.Default.Numbers,
                        title = "Show Channel Numbers",
                        checked = uiState.showNumbers,
                        onCheckedChange = { viewModel.setShowNumbers(it) }
                    )
                }
            }
            
            item {
                SettingsSection(title = "Cache & Storage") {
                    SettingsItem(
                        icon = Icons.Default.CleaningServices,
                        title = "Clear EPG Cache",
                        subtitle = "Free up space",
                        onClick = { viewModel.clearEpgCache() }
                    )
                    SettingsItem(
                        icon = Icons.Default.Image,
                        title = "Clear Image Cache",
                        subtitle = "Free up space",
                        onClick = { viewModel.clearImageCache() }
                    )
                    SettingsItem(
                        icon = Icons.Default.History,
                        title = "Clear Watch History",
                        subtitle = "Remove all history",
                        onClick = { viewModel.clearHistory() }
                    )
                }
            }
            
            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = uiState.appVersion,
                        onClick = { }
                    )
                    SettingsItem(
                        icon = Icons.Default.Description,
                        title = "Open Source Licenses",
                        subtitle = "View third-party licenses",
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .clickable { onExpandedChange(true) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}
