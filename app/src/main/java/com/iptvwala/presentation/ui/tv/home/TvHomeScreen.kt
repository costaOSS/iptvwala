package com.iptvwala.presentation.ui.tv.home

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptvwala.domain.model.Channel
import com.iptvwala.presentation.ui.shared.components.*
import com.iptvwala.presentation.viewmodel.HomeViewModel

@Composable
fun TvHomeScreen(
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToEpg: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlainApp: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    
    Row(modifier = Modifier.fillMaxSize()) {
        TvNavigationRail(
            onHomeClick = {},
            onChannelsClick = onNavigateToChannels,
            onFavoritesClick = onNavigateToFavorites,
            onEpgClick = onNavigateToEpg,
            onPlainAppClick = onNavigateToPlainApp,
            onSettingsClick = onNavigateToSettings,
            selectedItem = "home"
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                TvSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (uiState.recentlyWatched.isNotEmpty()) {
                item {
                    TvCategoryRow(
                        title = "Recently Watched",
                        channels = uiState.recentlyWatched,
                        onChannelClick = onChannelClick,
                        onChannelLongClick = onChannelLongClick
                    )
                }
            }
            
            if (uiState.featuredChannels.isNotEmpty()) {
                item {
                    TvFeaturedCarousel(
                        channels = uiState.featuredChannels,
                        onChannelClick = onChannelClick
                    )
                }
            }
            
            uiState.groupedChannels.forEach { (group, channels) ->
                item {
                    TvCategoryRow(
                        title = group.ifEmpty { "Uncategorized" },
                        channels = channels,
                        onChannelClick = onChannelClick,
                        onChannelLongClick = onChannelLongClick
                    )
                }
            }
            
            if (uiState.isLoading) {
                item {
                    LoadingIndicator(message = "Loading channels...")
                }
            }
            
            if (uiState.error != null) {
                item {
                    ErrorMessage(
                        message = uiState.error!!,
                        onRetry = viewModel::refresh
                    )
                }
            }
        }
    }
}

@Composable
fun TvNavigationRail(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onChannelsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onEpgClick: () -> Unit,
    onPlainAppClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        NavigationRailItem(
            selected = selectedItem == "home",
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") },
            modifier = Modifier.focusable()
        )
        
        NavigationRailItem(
            selected = selectedItem == "channels",
            onClick = onChannelsClick,
            icon = { Icon(Icons.Default.Tv, "Channels") },
            label = { Text("Channels") },
            modifier = Modifier.focusable()
        )
        
        NavigationRailItem(
            selected = selectedItem == "favorites",
            onClick = onFavoritesClick,
            icon = { Icon(Icons.Default.Favorite, "Favorites") },
            label = { Text("Favorites") },
            modifier = Modifier.focusable()
        )
        
        NavigationRailItem(
            selected = selectedItem == "epg",
            onClick = onEpgClick,
            icon = { Icon(Icons.Default.Schedule, "EPG") },
            label = { Text("EPG") },
            modifier = Modifier.focusable()
        )
        
        NavigationRailItem(
            selected = selectedItem == "plainapp",
            onClick = onPlainAppClick,
            icon = { Icon(Icons.Default.Dashboard, "PlainApp") },
            label = { Text("PlainApp") },
            modifier = Modifier.focusable()
        )
        
        NavigationRailItem(
            selected = selectedItem == "settings",
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, "Settings") },
            label = { Text("Settings") },
            modifier = Modifier.focusable()
        )
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun TvSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusable(),
        placeholder = { Text("Search channels...") },
        leadingIcon = { Icon(Icons.Default.Search, "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true
    )
}

@Composable
fun TvCategoryRow(
    title: String,
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channels) { channel ->
                TvChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    onLongClick = { onChannelLongClick(channel) }
                )
            }
        }
    }
}

@Composable
fun TvChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .focusable(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo != null) {
                    coil.compose.AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }
    }
}

@Composable
fun TvFeaturedCarousel(
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    Column(modifier = modifier) {
        Text(
            text = "Featured",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            channels.take(3).forEachIndexed { index, channel ->
                Card(
                    onClick = { 
                        selectedIndex = index
                        onChannelClick(channel)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(16f / 9f)
                        .focusable(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (index == selectedIndex)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (channel.logo != null) {
                            coil.compose.AsyncImage(
                                model = channel.logo,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
