package com.iptvwala.presentation.ui.tv.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptvwala.domain.model.Channel
import com.iptvwala.domain.model.EpgProgram
import com.iptvwala.presentation.ui.shared.components.EmptyState
import com.iptvwala.presentation.ui.shared.components.LoadingIndicator
import com.iptvwala.presentation.viewmodel.EpgViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvEpgScreen(
    onChannelClick: (Channel) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EpgViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EPG Guide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DateSelector(
                selectedDate = selectedDate,
                onDateChange = { selectedDate = it },
                dateFormat = dateFormat
            )
            
            if (uiState.isLoading) {
                LoadingIndicator()
            } else if (uiState.channels.isEmpty()) {
                EmptyState(
                    icon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(64.dp)) },
                    title = "No EPG data",
                    subtitle = "Add sources to get program information"
                )
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    ChannelList(
                        channels = uiState.channels,
                        selectedChannel = selectedChannel,
                        onChannelSelect = { selectedChannel = it },
                        modifier = Modifier.width(200.dp)
                    )
                    
                    HorizontalDivider()
                    
                    ProgramGuide(
                        channel = selectedChannel ?: uiState.channels.firstOrNull(),
                        programs = selectedChannel?.let { uiState.programs[it.tvgId ?: it.id.toString()] } ?: emptyList(),
                        selectedDate = selectedDate,
                        onProgramClick = { program ->
                            selectedChannel?.let { onChannelClick(it) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    selectedDate: Long,
    onDateChange: (Long) -> Unit,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    val today = remember { Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) } }
    val dates = remember { 
        (0..6).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            cal.timeInMillis
        }
    }
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dates) { date ->
            val isSelected = isSameDay(date, selectedDate)
            val isToday = isSameDay(date, today.timeInMillis)
            
            FilterChip(
                selected = isSelected,
                onClick = { onDateChange(date) },
                label = {
                    Text(
                        if (isToday) "Today" else dateFormat.format(Date(date)),
                        maxLines = 1
                    )
                },
                leadingIcon = if (isToday) {
                    { Icon(Icons.Default.Today, null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.focusable()
            )
        }
    }
}

@Composable
fun ChannelList(
    channels: List<Channel>,
    selectedChannel: Channel?,
    onChannelSelect: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight()
    ) {
        items(channels) { channel ->
            ChannelListItem(
                channel = channel,
                isSelected = channel.id == selectedChannel?.id,
                onClick = { onChannelSelect(channel) }
            )
        }
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .focusable(),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
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
                    Icon(
                        Icons.Default.Tv,
                        null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProgramGuide(
    channel: Channel?,
    programs: List<EpgProgram>,
    selectedDate: Long,
    onProgramClick: (EpgProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    if (channel == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a channel")
        }
        return
    }
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (channel.groupTitle != null) {
                        Text(
                            text = channel.groupTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }
        
        if (programs.isEmpty()) {
            item {
                Text(
                    text = "No program information available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(programs) { program ->
                ProgramCard(
                    program = program,
                    timeFormat = timeFormat,
                    onClick = { onProgramClick(program) }
                )
            }
        }
    }
}

@Composable
fun ProgramCard(
    program: EpgProgram,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isCurrent = program.startTime <= now && program.endTime > now
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    text = timeFormat.format(Date(program.startTime)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = timeFormat.format(Date(program.endTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Icon(
                            Icons.Default.PlayCircle,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (program.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (program.category != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(program.category, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
