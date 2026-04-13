package com.iptvwala.presentation.ui.tv.player

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.iptvwala.domain.model.Channel
import com.iptvwala.domain.model.EpgProgram
import kotlinx.coroutines.delay

@Composable
fun TvPlayerScreen(
    channel: Channel,
    currentProgram: EpgProgram?,
    nextProgram: EpgProgram?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    position: Long,
    duration: Long,
    playbackSpeed: Float,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPipClick: () -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    showControls = !showControls
                }
        )
        
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerOverlay(
                channel = channel,
                currentProgram = currentProgram,
                nextProgram = nextProgram,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                position = position,
                duration = duration,
                playbackSpeed = playbackSpeed,
                showSpeedMenu = showSpeedMenu,
                onBackClick = onBackClick,
                onPlayPause = onPlayPause,
                onSeek = onSeek,
                onSpeedChange = { showSpeedMenu = true },
                onSpeedSelected = {
                    onSpeedChange(it)
                    showSpeedMenu = false
                },
                onPipClick = onPipClick,
                onPreviousChannel = onPreviousChannel,
                onNextChannel = onNextChannel
            )
        }
        
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PlayerOverlay(
    channel: Channel,
    currentProgram: EpgProgram?,
    nextProgram: EpgProgram?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    position: Long,
    duration: Long,
    playbackSpeed: Float,
    showSpeedMenu: Boolean,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onPipClick: () -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit
) {
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        TopBar(
            channel = channel,
            currentProgram = currentProgram,
            nextProgram = nextProgram,
            timeFormat = timeFormat,
            onBackClick = onBackClick,
            onPipClick = onPipClick
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        CenterControls(
            isPlaying = isPlaying,
            onPlayPause = onPlayPause,
            onPreviousChannel = onPreviousChannel,
            onNextChannel = onNextChannel
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        BottomControls(
            position = position,
            duration = duration,
            playbackSpeed = playbackSpeed,
            showSpeedMenu = showSpeedMenu,
            timeFormat = timeFormat,
            onSeek = onSeek,
            onSpeedChange = onSpeedChange,
            onSpeedSelected = onSpeedSelected
        )
    }
}

@Composable
fun TopBar(
    channel: Channel,
    currentProgram: EpgProgram?,
    nextProgram: EpgProgram?,
    timeFormat: java.text.SimpleDateFormat,
    onBackClick: () -> Unit,
    onPipClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.focusable()
        ) {
            Icon(
                Icons.Default.ArrowBack,
                "Back",
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.2f)),
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
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            if (currentProgram != null) {
                Text(
                    text = "${timeFormat.format(java.util.Date(currentProgram.startTime))} - ${currentProgram.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            if (nextProgram != null) {
                Text(
                    text = "Up next: ${nextProgram.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
        
        IconButton(
            onClick = onPipClick,
            modifier = Modifier.focusable()
        ) {
            Icon(
                Icons.Default.PictureInPicture,
                "Picture in Picture",
                tint = Color.White
            )
        }
    }
}

@Composable
fun CenterControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousChannel,
            modifier = Modifier.size(64.dp).focusable()
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                "Previous",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(32.dp))
        
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(80.dp).focusable()
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(32.dp))
        
        IconButton(
            onClick = onNextChannel,
            modifier = Modifier.size(64.dp).focusable()
        ) {
            Icon(
                Icons.Default.SkipNext,
                "Next",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun BottomControls(
    position: Long,
    duration: Long,
    playbackSpeed: Float,
    showSpeedMenu: Boolean,
    timeFormat: java.text.SimpleDateFormat,
    onSeek: (Long) -> Unit,
    onSpeedChange: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        
        LaunchedEffect(position) {
            if (duration > 0) {
                sliderPosition = position.toFloat() / duration.toFloat()
            }
        }
        
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { 
                onSeek((sliderPosition * duration).toLong()) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusable(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatDuration(position)} / ${formatDuration(duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            
            Box {
                TextButton(
                    onClick = onSpeedChange,
                    modifier = Modifier.focusable()
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = Color.White
                    )
                }
                
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { onSpeedSelected(playbackSpeed) }
                ) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        DropdownMenuItem(
                            text = { Text("${speed}x") },
                            onClick = { onSpeedSelected(speed) },
                            leadingIcon = {
                                if (speed == playbackSpeed) {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%d:%02d", minutes, seconds % 60)
    }
}
