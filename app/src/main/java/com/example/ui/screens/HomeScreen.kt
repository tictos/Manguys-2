package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.MediaEntry
import com.example.data.MediaStatus
import com.example.data.MediaType
import com.example.ui.MediaViewModel
import com.example.ui.theme.SophisticatedDarkSurfaceVariant
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusOnHold
import com.example.ui.theme.StatusOngoing
import com.example.ui.theme.getStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MediaViewModel,
    initialCategory: MediaType? = null,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()

    val defaultGroup = when (initialCategory) {
        MediaType.MANGAS, MediaType.WEBTOONS -> 1
        else -> 0
    }
    var selectedGroupFilter by remember { mutableIntStateOf(defaultGroup) } // 0: Regardé, 1: Lu
    var selectedTypeFilter by remember { mutableStateOf<MediaType?>(initialCategory) }
    var selectedStatusFilter by remember { mutableStateOf<MediaStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<MediaEntry?>(null) }

    val filteredEntries = entries.filter { entry ->
        val isWatchable = entry.type == MediaType.ANIMES || entry.type == MediaType.SERIES || entry.type == MediaType.FILMS
        val isReadable = entry.type == MediaType.MANGAS || entry.type == MediaType.WEBTOONS

        val matchesGroup = when (selectedGroupFilter) {
            0 -> isWatchable
            1 -> isReadable
            else -> true
        }
        val matchesType = selectedTypeFilter == null || entry.type == selectedTypeFilter
        val matchesStatus = selectedStatusFilter == null || entry.status == selectedStatusFilter
        val matchesSearch = searchQuery.isBlank() || 
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.progress.contains(searchQuery, ignoreCase = true)
        matchesGroup && matchesType && matchesStatus && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher un manga, animé...", fontSize = 14.sp) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Fermer")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        )
                    } else {
                        Text("Manguys", fontWeight = FontWeight.Medium, fontSize = 24.sp)
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Recherche", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter", modifier = Modifier.size(30.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Group Tab Selector (Regardé | Lu)
            PrimaryTabRow(
                selectedTabIndex = selectedGroupFilter,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedGroupFilter == 0,
                    onClick = {
                        selectedGroupFilter = 0
                        if (selectedTypeFilter != null && selectedTypeFilter != MediaType.ANIMES && selectedTypeFilter != MediaType.SERIES && selectedTypeFilter != MediaType.FILMS) {
                            selectedTypeFilter = null
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Regardé", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedGroupFilter == 1,
                    onClick = {
                        selectedGroupFilter = 1
                        if (selectedTypeFilter != null && selectedTypeFilter != MediaType.MANGAS && selectedTypeFilter != MediaType.WEBTOONS) {
                            selectedTypeFilter = null
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Lu", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category filter chips filtered by active group
            val availableCategories = when (selectedGroupFilter) {
                0 -> listOf(MediaType.ANIMES, MediaType.SERIES, MediaType.FILMS)
                1 -> listOf(MediaType.MANGAS, MediaType.WEBTOONS)
                else -> MediaType.entries
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("Tout") },
                        leadingIcon = if (selectedTypeFilter == null) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SophisticatedDarkSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onBackground,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                items(availableCategories) { type ->
                    val isSelected = selectedTypeFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = if (isSelected) null else type },
                        label = { Text(type.displayName) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SophisticatedDarkSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onBackground,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Status filter chips (En cours / En pause / Terminé)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("Tous statuts", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = CircleShape
                    )
                }
                item {
                    val ongoingColors = MediaStatus.ONGOING.getStatusColors()
                    FilterChip(
                        selected = selectedStatusFilter == MediaStatus.ONGOING,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == MediaStatus.ONGOING) null else MediaStatus.ONGOING
                        },
                        label = { Text("En cours", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ongoingColors.dotColor.copy(alpha = 0.2f),
                            selectedLabelColor = ongoingColors.textColor,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = CircleShape
                    )
                }
                item {
                    val onHoldColors = MediaStatus.ON_HOLD.getStatusColors()
                    FilterChip(
                        selected = selectedStatusFilter == MediaStatus.ON_HOLD,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == MediaStatus.ON_HOLD) null else MediaStatus.ON_HOLD
                        },
                        label = { Text("En pause", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = onHoldColors.dotColor.copy(alpha = 0.2f),
                            selectedLabelColor = onHoldColors.textColor,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = CircleShape
                    )
                }
                item {
                    val completedColors = MediaStatus.COMPLETED.getStatusColors()
                    FilterChip(
                        selected = selectedStatusFilter == MediaStatus.COMPLETED,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == MediaStatus.COMPLETED) null else MediaStatus.COMPLETED
                        },
                        label = { Text("Terminé", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = completedColors.dotColor.copy(alpha = 0.2f),
                            selectedLabelColor = completedColors.textColor,
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = CircleShape
                    )
                }
            }

            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (entries.isEmpty()) "Aucune entrée. Ajoutez votre premier média !" else "Aucun résultat trouvé pour ce filtre.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val watchableList = filteredEntries.filter {
                    it.type == MediaType.ANIMES || it.type == MediaType.SERIES || it.type == MediaType.FILMS
                }
                val readableList = filteredEntries.filter {
                    it.type == MediaType.MANGAS || it.type == MediaType.WEBTOONS
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section 1: Regardé (Animes, Séries, Films)
                    if (watchableList.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "REGARDÉ",
                                subtitle = "Animes, Séries & Films",
                                count = watchableList.size,
                                icon = Icons.Outlined.PlayCircle
                            )
                        }

                        items(watchableList, key = { it.id }) { entry ->
                            MediaEntryCard(
                                entry = entry,
                                onClick = { onEditClick(entry.id) },
                                onDeleteClick = { entryToDelete = entry },
                                onUpdateEntry = { updatedEntry -> viewModel.insert(updatedEntry) }
                            )
                        }
                    }

                    // Section 2: Lu (Mangas, Webtoons)
                    if (readableList.isNotEmpty()) {
                        item {
                            if (watchableList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            SectionHeader(
                                title = "LU",
                                subtitle = "Mangas & Webtoons",
                                count = readableList.size,
                                icon = Icons.Outlined.AutoStories
                            )
                        }

                        items(readableList, key = { it.id }) { entry ->
                            MediaEntryCard(
                                entry = entry,
                                onClick = { onEditClick(entry.id) },
                                onDeleteClick = { entryToDelete = entry },
                                onUpdateEntry = { updatedEntry -> viewModel.insert(updatedEntry) }
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text("Mode Hors-Ligne Activé", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (entryToDelete != null) {
        val target = entryToDelete!!
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = {
                Text("Supprimer l'œuvre", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Voulez-vous vraiment supprimer « ${target.title} » de votre liste ? Cette action est irréversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteById(target.id)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { entryToDelete = null }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun MediaEntryCard(
    entry: MediaEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateEntry: ((MediaEntry) -> Unit)? = null
) {
    val isWatchable = entry.type == MediaType.ANIMES ||
            entry.type == MediaType.SERIES ||
            entry.type == MediaType.FILMS

    if (isWatchable) {
        WatchableMediaCard(
            entry = entry,
            onClick = onClick,
            onDeleteClick = onDeleteClick,
            onUpdateEntry = onUpdateEntry
        )
    } else {
        ReadableMediaCard(
            entry = entry,
            onClick = onClick,
            onDeleteClick = onDeleteClick,
            onUpdateEntry = onUpdateEntry
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchableMediaCard(
    entry: MediaEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateEntry: ((MediaEntry) -> Unit)? = null
) {
    val isFilm = entry.type == MediaType.FILMS

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onDeleteClick() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Cover Banner for watchable media (Animes, Series, Films)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!entry.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val gradientColors = remember(entry.title, entry.type) {
                        getPlaceholderGradient(entry.title, entry.type)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            val icon = when (entry.type) {
                                MediaType.ANIMES -> Icons.Outlined.PlayCircle
                                MediaType.SERIES -> Icons.Outlined.Tv
                                MediaType.FILMS -> Icons.Outlined.Movie
                                else -> Icons.Outlined.Movie
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Completion Badge on top corner
                if (entry.isFinished) {
                    Surface(
                        color = StatusCompleted,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                            Text(
                                text = if (isFilm) "Vu" else "Terminé",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Information Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = entry.type.displayName.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (!isFilm) {
                    Text(
                        text = "Progression : ${entry.progress}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isFilm) {
                        // Work Publication Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val statusColors = entry.status.getStatusColors()
                            val statusText = when (entry.status) {
                                MediaStatus.COMPLETED -> "Œuvre : Terminé"
                                MediaStatus.ON_HOLD -> "Œuvre : En pause"
                                MediaStatus.ONGOING -> "Œuvre : En cours"
                            }

                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColors.dotColor))
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColors.textColor
                            )
                        }
                    } else {
                        // Film Status Text
                        Text(
                            text = if (entry.isFinished) "Film visionné" else "Film à regarder",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // User Completion Toggle Button / Chip
                    if (onUpdateEntry != null) {
                        Surface(
                            onClick = {
                                val nextFinished = !entry.isFinished
                                val nextProgress = if (isFilm) (if (nextFinished) "Vu" else "À voir") else entry.progress
                                onUpdateEntry(entry.copy(isFinished = nextFinished, progress = nextProgress))
                            },
                            shape = CircleShape,
                            color = if (entry.isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (entry.isFinished) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            border = if (!entry.isFinished) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (entry.isFinished) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    text = if (isFilm) (if (entry.isFinished) "Vu" else "Marquer Vu")
                                    else (if (entry.isFinished) "Terminé" else "Marquer Terminé"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadableMediaCard(
    entry: MediaEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateEntry: ((MediaEntry) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onDeleteClick() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover Image OR Colored Background Box
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!entry.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val gradientColors = remember(entry.title, entry.type) {
                        getPlaceholderGradient(entry.title, entry.type)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = entry.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (entry.isFinished) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Surface(
                            color = StatusCompleted,
                            shape = CircleShape,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Lu", tint = Color.White, modifier = Modifier.size(12.dp).padding(1.dp))
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = entry.type.displayName.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "Progression : ${entry.progress}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusColors = entry.status.getStatusColors()
                    val statusText = when (entry.status) {
                        MediaStatus.COMPLETED -> "Œuvre : Terminé"
                        MediaStatus.ON_HOLD -> "Œuvre : En pause"
                        MediaStatus.ONGOING -> "Œuvre : En cours"
                    }

                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColors.dotColor))
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColors.textColor
                    )
                }

                // User Completion Toggle Chip
                if (onUpdateEntry != null) {
                    Surface(
                        onClick = {
                            onUpdateEntry(entry.copy(isFinished = !entry.isFinished))
                        },
                        shape = CircleShape,
                        color = if (entry.isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (entry.isFinished) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        border = if (!entry.isFinished) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            if (entry.isFinished) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(10.dp))
                            }
                            Text(
                                text = if (entry.isFinished) "Tout lu" else "Marquer Lu",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Far Right Stepper (+ / -) aligned vertically
            if (onUpdateEntry != null) {
                val (readingUnit, numberStr) = parseReadingProgress(entry.progress)
                val currentNum = numberStr.toIntOrNull() ?: 0

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val newProgress = "$readingUnit ${currentNum + 1}"
                                onUpdateEntry(entry.copy(progress = newProgress))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Augmenter",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "$currentNum",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = {
                                if (currentNum > 0) {
                                    val newProgress = "$readingUnit ${currentNum - 1}"
                                    onUpdateEntry(entry.copy(progress = newProgress))
                                }
                            },
                            enabled = currentNum > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "Diminuer",
                                tint = if (currentNum > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
