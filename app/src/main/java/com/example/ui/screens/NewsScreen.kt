package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MediaEntry
import com.example.data.MediaType
import com.example.network.NewsNetworkManager
import com.example.ui.MediaViewModel
import kotlinx.coroutines.launch

data class NewsArticle(
    val id: String,
    val mediaTitle: String,
    val mediaType: MediaType,
    val badgeLabel: String,
    val badgeColor: Color,
    val title: String,
    val summary: String,
    val timeAgo: String,
    val source: String,
    val isTrackedByUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: MediaViewModel,
    onAddMediaClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val networkManager = remember { NewsNetworkManager(context) }

    val userEntries by viewModel.allEntries.collectAsStateWithLifecycle()

    var selectedFilterTab by remember(userEntries) {
        mutableIntStateOf(if (userEntries.isEmpty()) 1 else 0) // Default to "Toutes les actus" if list is empty so page shows news immediately
    }
    var selectedMediaTypeFilter by remember { mutableStateOf<MediaType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var bookmarkedNewsIds by remember { mutableStateOf(setOf<String>()) }
    var expandedArticleId by remember { mutableStateOf<String?>(null) }

    // Online Connection State
    var isConnected by remember { mutableStateOf(true) }
    var isLoadingNews by remember { mutableStateOf(false) }
    var liveArticlesList by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
    var hasAttemptedInitialFetch by remember { mutableStateOf(false) }

    // Function to check connection and load news
    fun refreshOnlineNews() {
        scope.launch {
            isLoadingNews = true
            val hasInternet = networkManager.testLiveInternetConnection()
            isConnected = hasInternet

            if (hasInternet) {
                val fetched = networkManager.fetchLiveOnlineNews(userEntries)
                liveArticlesList = fetched
            }
            isLoadingNews = false
            hasAttemptedInitialFetch = true
        }
    }

    // Trigger initial online news fetch when entering or when userEntries change
    LaunchedEffect(userEntries) {
        refreshOnlineNews()
    }

    // Determine current news to display
    val currentNewsToDisplay = remember(isConnected, liveArticlesList, userEntries) {
        if (liveArticlesList.isNotEmpty()) {
            liveArticlesList
        } else {
            // Fallback cached news generated from local list
            generateFallbackNews(userEntries)
        }
    }

    // Filtered news
    val filteredNews = remember(currentNewsToDisplay, selectedFilterTab, selectedMediaTypeFilter, searchQuery, userEntries) {
        currentNewsToDisplay.filter { news ->
            val matchesTab = when (selectedFilterTab) {
                0 -> if (userEntries.isEmpty()) true else news.isTrackedByUser
                else -> true
            }
            val matchesType = selectedMediaTypeFilter == null || news.mediaType == selectedMediaTypeFilter
            val matchesSearch = searchQuery.isBlank() ||
                    news.title.contains(searchQuery, ignoreCase = true) ||
                    news.mediaTitle.contains(searchQuery, ignoreCase = true) ||
                    news.summary.contains(searchQuery, ignoreCase = true)

            matchesTab && matchesType && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Newspaper,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Actualités Internet",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Network Status Badge
                        Surface(
                            shape = CircleShape,
                            color = if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
                                )
                                Text(
                                    text = if (isConnected) "En Ligne" else "Hors-Ligne",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Connection Status Banner
            if (!isConnected) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WifiOff,
                            contentDescription = "Connexion requise",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connexion Internet requise pour les Actus",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "La section Actualités utilise Internet pour télécharger en direct. Vos médias, catégories et stats restent 100% accessibles hors-ligne.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                lineHeight = 15.sp
                            )
                        }
                        IconButton(
                            onClick = { refreshOnlineNews() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                        ) {
                            if (isLoadingNews) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Réessayer",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Online info banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Connecté aux serveurs web d'actualités",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1B5E20)
                            )
                        }

                        IconButton(
                            onClick = { refreshOnlineNews() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (isLoadingNews) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2E7D32)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Actualiser",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Filter Tabs ("Pour vous" vs "Toutes les actus")
            PrimaryTabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedFilterTab == 0,
                    onClick = { selectedFilterTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Stars, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Pour vous (${currentNewsToDisplay.count { it.isTrackedByUser }})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedFilterTab == 1,
                    onClick = { selectedFilterTab = 1 },
                    text = { Text("Toutes les actus", fontWeight = FontWeight.Bold) }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Rechercher une actualité ou un titre...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Effacer")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedMediaTypeFilter == null,
                        onClick = { selectedMediaTypeFilter = null },
                        label = { Text("Tous les genres") },
                        shape = CircleShape
                    )
                }
                items(MediaType.entries.toTypedArray()) { type ->
                    FilterChip(
                        selected = selectedMediaTypeFilter == type,
                        onClick = {
                            selectedMediaTypeFilter = if (selectedMediaTypeFilter == type) null else type
                        },
                        label = { Text(type.displayName) },
                        shape = CircleShape
                    )
                }
            }

            // Loading Indicator state
            if (isLoadingNews && liveArticlesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Connexion et chargement des actualités en direct...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredNews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (!isConnected) Icons.Filled.WifiOff else Icons.Filled.Feed,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (!isConnected) {
                                "Connexion Internet requise"
                            } else if (selectedFilterTab == 0 && userEntries.isEmpty()) {
                                "Aucun média dans votre liste !"
                            } else {
                                "Aucune actualité trouvée"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (!isConnected) {
                                "Activez le Wi-Fi ou les données mobiles pour consulter le fil d'actualités en direct."
                            } else if (selectedFilterTab == 0 && userEntries.isEmpty()) {
                                "Ajoutez vos animés, mangas, séries ou films préférés pour suivre leurs actualités en direct."
                            } else {
                                "Essayez de modifier votre recherche ou vos filtres."
                            },
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        if (!isConnected) {
                            Button(
                                onClick = { refreshOnlineNews() },
                                modifier = Modifier.padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Réessayer la connexion")
                            }
                        } else if (selectedFilterTab == 0 && userEntries.isEmpty()) {
                            Button(
                                onClick = onAddMediaClick,
                                modifier = Modifier.padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ajouter mon premier média")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Feature banner if in "Pour vous" mode
                    if (selectedFilterTab == 0 && userEntries.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Fil personnalisé activé",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Les actualités ci-dessous sont sélectionnées en ligne selon vos ${userEntries.size} médias enregistrés.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(filteredNews, key = { it.id }) { article ->
                        val isBookmarked = bookmarkedNewsIds.contains(article.id)
                        val isExpanded = expandedArticleId == article.id

                        NewsArticleCard(
                            article = article,
                            isBookmarked = isBookmarked,
                            isExpanded = isExpanded,
                            onBookmarkToggle = {
                                bookmarkedNewsIds = if (isBookmarked) {
                                    bookmarkedNewsIds - article.id
                                } else {
                                    bookmarkedNewsIds + article.id
                                }
                            },
                            onExpandToggle = {
                                expandedArticleId = if (isExpanded) null else article.id
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsArticleCard(
    article: NewsArticle,
    isBookmarked: Boolean,
    isExpanded: Boolean,
    onBookmarkToggle: () -> Unit,
    onExpandToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onExpandToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Badges & Tracked Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge
                    Text(
                        text = article.badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = article.badgeColor,
                        modifier = Modifier
                            .background(article.badgeColor.copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )

                    // Media Type
                    Text(
                        text = article.mediaType.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Is Tracked Tag
                if (article.isTrackedByUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Suivi",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Media Title & Publication time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.mediaTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${article.source} • ${article.timeAgo}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Article Headline Title
            Text(
                text = article.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            // Article Summary
            Text(
                text = article.summary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Expanded extra content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    Text(
                        text = "Restez informé en ligne des prochaines sorties et événements concernant ${article.mediaTitle}. N'oubliez pas d'ajuster votre progression dans l'application !",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Bottom action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onExpandToggle,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Réduire" else "Lire la suite",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onBookmarkToggle) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Sauvegarder",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun generateFallbackNews(userEntries: List<MediaEntry>): List<NewsArticle> {
    val list = mutableListOf<NewsArticle>()

    val colorNewChapter = Color(0xFF4CAF50)
    val colorTrailer = Color(0xFF2196F3)
    val colorSeason = Color(0xFF9C27B0)
    val colorAnnouncement = Color(0xFFFF9800)

    val trackedTitlesLower = userEntries.map { it.title.trim().lowercase() }

    userEntries.forEachIndexed { index, entry ->
        val title = entry.title
        val type = entry.type

        list.add(
            NewsArticle(
                id = "fallback_user_$index",
                mediaTitle = title,
                mediaType = type,
                badgeLabel = "📦 EN CACHE",
                badgeColor = colorNewChapter,
                title = "Actualité sauvegardée pour $title",
                summary = "Cet article a été téléchargé lors de votre dernière connexion internet. Connectez-vous à internet pour actualiser.",
                timeAgo = "Dernier accès",
                source = "Cache local",
                isTrackedByUser = true
            )
        )
    }

    if (list.isEmpty()) {
        list.add(
            NewsArticle(
                id = "fallback_trend_1",
                mediaTitle = "One Piece",
                mediaType = MediaType.MANGAS,
                badgeLabel = "📦 EN CACHE",
                badgeColor = colorAnnouncement,
                title = "One Piece : Mises à jour des chapitres récents",
                summary = "Article pré-enregistré. Connectez-vous à Internet pour obtenir les dernières dépêches en direct.",
                timeAgo = "Hors-Ligne",
                source = "Cache local",
                isTrackedByUser = trackedTitlesLower.contains("one piece")
            )
        )
    }

    return list
}
