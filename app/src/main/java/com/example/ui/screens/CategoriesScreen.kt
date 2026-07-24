package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MediaEntry
import com.example.data.MediaType
import com.example.ui.MediaViewModel
import com.example.ui.theme.SophisticatedDarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: MediaViewModel,
    onCategorySelect: (MediaType) -> Unit
) {
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()

    val watchableCategories = listOf(MediaType.ANIMES, MediaType.SERIES, MediaType.FILMS)
    val readableCategories = listOf(MediaType.MANGAS, MediaType.WEBTOONS)

    val watchableTotalCount = entries.count { it.type in watchableCategories }
    val readableTotalCount = entries.count { it.type in readableCategories }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catégories", fontWeight = FontWeight.Medium, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Explorez vos œuvres par type de média et format",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header 1: REGARDÉ
                item(span = { GridItemSpan(2) }) {
                    CategoryGroupHeader(
                        title = "REGARDÉ",
                        subtitle = "Animes, Séries & Films",
                        count = watchableTotalCount,
                        icon = Icons.Outlined.PlayCircle
                    )
                }

                items(watchableCategories) { mediaType ->
                    CategoryCard(
                        mediaType = mediaType,
                        entries = entries,
                        onCategorySelect = onCategorySelect
                    )
                }

                // Header 2: LU
                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryGroupHeader(
                        title = "LU",
                        subtitle = "Mangas & Webtoons",
                        count = readableTotalCount,
                        icon = Icons.Outlined.AutoStories
                    )
                }

                items(readableCategories) { mediaType ->
                    CategoryCard(
                        mediaType = mediaType,
                        entries = entries,
                        onCategorySelect = onCategorySelect
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGroupHeader(
    title: String,
    subtitle: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = "$count titre${if (count > 1) "s" else ""}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun CategoryCard(
    mediaType: MediaType,
    entries: List<MediaEntry>,
    onCategorySelect: (MediaType) -> Unit
) {
    val count = entries.count { it.type == mediaType }
    val icon = when (mediaType) {
        MediaType.MANGAS, MediaType.WEBTOONS -> Icons.Outlined.AutoStories
        MediaType.ANIMES -> Icons.Outlined.PlayCircle
        MediaType.SERIES -> Icons.Outlined.Tv
        MediaType.FILMS -> Icons.Outlined.Movie
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .clickable { onCategorySelect(mediaType) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = mediaType.displayName,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column {
                Text(
                    text = mediaType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$count titre${if (count > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
