package com.iptvwala.presentation.ui.tv.channel

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.iptvwala.domain.model.ChannelCategory
import com.iptvwala.presentation.ui.shared.components.*
import com.iptvwala.presentation.viewmodel.ChannelsViewModel

enum class FilterChip {
    ALL, LIVE, VOD, SERIES, FAVORITES
}

enum class SortMode {
    ALPHABETICAL, SOURCE_ORDER, RECENTLY_WATCHED, GROUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvChannelBrowserScreen(
    onChannelClick: (Channel) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    var selectedFilter by remember { mutableStateOf(FilterChip.ALL) }
    var selectedSort by remember { mutableStateOf(SortMode.ALPHABETICAL) }
    var showFilters by remember { mutableStateOf(false) }
    
    val filteredChannels = remember(uiState.channels, selectedFilter, uiState.searchQuery, selectedSort) {
        var result = when (selectedFilter) {
            FilterChip.ALL -> uiState.channels
            FilterChip.LIVE -> uiState.channels.filter { it.category == ChannelCategory.LIVE }
            FilterChip.VOD -> uiState.channels.filter { it.category == ChannelCategory.VOD }
            FilterChip.SERIES -> uiState.channels.filter { it.category == ChannelCategory.SERIES }
            FilterChip.FAVORITES -> uiState.favorites
        }
        
        if (uiState.searchQuery.isNotBlank()) {
            result = result.filter {
                it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                it.groupTitle?.contains(uiState.searchQuery, ignoreCase = true) == true
            }
        }
        
        result = when (selectedSort) {
            SortMode.ALPHABETICAL -> result.sortedBy { it.name }
            SortMode.SOURCE_ORDER -> result.sortedBy { it.channelNumber }
            SortMode.RECENTLY_WATCHED -> result.sortedByDescending { it.lastWatched ?: 0 }
            SortMode.GROUP -> result.sortedWith(compareBy({ it.groupTitle }, { it.name }))
        }
        
        result
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channels") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, "Filters")
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
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (showFilters) {
                FilterSection(
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    selectedSort = selectedSort,
                    onSortChange = { selectedSort = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            if (uiState.isLoading) {
                LoadingIndicator()
            } else if (filteredChannels.isEmpty()) {
                EmptyState(
                    icon = { Icon(Icons.Default.Tv, null, modifier = Modifier.size(64.dp)) },
                    title = "No channels found",
                    subtitle = if (uiState.searchQuery.isNotBlank()) "Try a different search term" else "Add a source to get started"
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { channel ->
                        TvChannelGridItem(
                            channel = channel,
                            onClick = { onChannelClick(channel) },
                            onFavoriteClick = { viewModel.toggleFavorite(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusable(),
        placeholder = { Text("Search channels...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true
    )
}

@Composable
fun FilterSection(
    selectedFilter: FilterChip,
    onFilterChange: (FilterChip) -> Unit,
    selectedSort: SortMode,
    onSortChange: (SortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.focusable()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = "Sort: ${selectedSort.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().focusable()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    SortMode.values().forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(sort.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onSortChange(sort)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun TvChannelGridItem(
    channel: Channel,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.focusable()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp),
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
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            
            if (channel.groupTitle != null) {
                Text(
                    text = channel.groupTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(16.dp),
                    tint = if (channel.isFavorite) androidx.compose.ui.graphics.Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
