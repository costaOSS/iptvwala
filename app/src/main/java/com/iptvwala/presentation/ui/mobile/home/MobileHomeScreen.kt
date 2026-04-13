package com.iptvwala.presentation.ui.mobile.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileHomeScreen(
    onChannelClick: (Channel) -> Unit,
    onNavigateToChannels: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IPTVwala") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            )
        },
        bottomBar = {
            MobileBottomNav(
                selectedItem = "home",
                onHomeClick = {},
                onChannelsClick = onNavigateToChannels,
                onFavoritesClick = onNavigateToFavorites,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.searchQuery.isNotBlank()) {
                item {
                    SearchResults(
                        channels = uiState.filteredChannels,
                        onChannelClick = onChannelClick,
                        onFavoriteClick = { viewModel.setEvent(com.iptvwala.presentation.viewmodel.HomeEvent.ChannelFavoriteClicked(it)) }
                    )
                }
            } else {
                if (uiState.recentlyWatched.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Recently Watched")
                    }
                    
                    items(uiState.recentlyWatched.take(5)) { channel ->
                        MobileChannelItem(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onFavoriteClick = { viewModel.setEvent(com.iptvwala.presentation.viewmodel.HomeEvent.ChannelFavoriteClicked(channel)) }
                        )
                    }
                }
                
                if (uiState.favorites.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Favorites")
                    }
                    
                    items(uiState.favorites.take(5)) { channel ->
                        MobileChannelItem(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onFavoriteClick = { viewModel.setEvent(com.iptvwala.presentation.viewmodel.HomeEvent.ChannelFavoriteClicked(channel)) }
                        )
                    }
                }
                
                uiState.groupedChannels.forEach { (group, channels) ->
                    item {
                        SectionHeader(title = group.ifEmpty { "Uncategorized" })
                    }
                    
                    items(channels.take(10)) { channel ->
                        MobileChannelItem(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onFavoriteClick = { viewModel.setEvent(com.iptvwala.presentation.viewmodel.HomeEvent.ChannelFavoriteClicked(channel)) }
                        )
                    }
                }
            }
            
            if (uiState.isLoading) {
                item {
                    LoadingIndicator()
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
fun MobileBottomNav(
    selectedItem: String,
    onHomeClick: () -> Unit,
    onChannelsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedItem == "home",
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") }
        )
        
        NavigationBarItem(
            selected = selectedItem == "channels",
            onClick = onChannelsClick,
            icon = { Icon(Icons.Default.Tv, "Channels") },
            label = { Text("Channels") }
        )
        
        NavigationBarItem(
            selected = selectedItem == "favorites",
            onClick = onFavoritesClick,
            icon = { Icon(Icons.Default.Favorite, "Favorites") },
            label = { Text("Favorites") }
        )
        
        NavigationBarItem(
            selected = selectedItem == "settings",
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, "Settings") },
            label = { Text("Settings") }
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun MobileChannelItem(
    channel: Channel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp),
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
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall
                )
                if (channel.groupTitle != null) {
                    Text(
                        text = channel.groupTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) androidx.compose.ui.graphics.Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchResults(
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onFavoriteClick: (Channel) -> Unit
) {
    if (channels.isEmpty()) {
        EmptyState(
            icon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(64.dp)) },
            title = "No results found"
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            channels.forEach { channel ->
                MobileChannelItem(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    onFavoriteClick = { onFavoriteClick(channel) }
                )
            }
        }
    }
}
