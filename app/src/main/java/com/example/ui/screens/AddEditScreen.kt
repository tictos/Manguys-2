package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WebImageSuggestion(
    val title: String,
    val imageUrl: String,
    val type: MediaType
)

val PRESET_IMAGE_SUGGESTIONS = listOf(
    WebImageSuggestion("One Piece", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop", MediaType.MANGAS),
    WebImageSuggestion("Jujutsu Kaisen", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop", MediaType.MANGAS),
    WebImageSuggestion("Demon Slayer", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop", MediaType.ANIMES),
    WebImageSuggestion("Solo Leveling", "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&auto=format&fit=crop", MediaType.WEBTOONS),
    WebImageSuggestion("Attack on Titan", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop", MediaType.ANIMES),
    WebImageSuggestion("Naruto", "https://images.unsplash.com/photo-1613376023733-0a73315d9b06?w=600&auto=format&fit=crop", MediaType.MANGAS),
    WebImageSuggestion("Cyberpunk", "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=600&auto=format&fit=crop", MediaType.SERIES),
    WebImageSuggestion("Cinema Epic", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop", MediaType.FILMS)
)

suspend fun fetchWebImageSuggestions(searchTitle: String, mediaType: MediaType): List<WebImageSuggestion> = withContext(Dispatchers.IO) {
    val results = mutableListOf<WebImageSuggestion>()
    if (searchTitle.isBlank()) return@withContext results

    val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val userAgent = "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0"

    val categoriesToSearch = when (mediaType) {
        MediaType.MANGAS, MediaType.WEBTOONS -> listOf("manga", "anime")
        else -> listOf("anime", "manga")
    }

    for (category in categoriesToSearch) {
        try {
            val encodedQuery = URLEncoder.encode(searchTitle.trim(), "UTF-8")
            val url = "https://kitsu.io/api/edge/$category?filter[text]=$encodedQuery&page[limit]=8"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrEmpty()) {
                        val json = JSONObject(bodyStr)
                        val dataArray = json.optJSONArray("data")
                        if (dataArray != null) {
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val attributes = item.optJSONObject("attributes") ?: continue
                                val canonicalTitle = attributes.optString("canonicalTitle", searchTitle)
                                val posterObj = attributes.optJSONObject("posterImage") ?: continue

                                val imageUrl = posterObj.optString("medium", "")
                                    .ifBlank { posterObj.optString("large", "") }
                                    .ifBlank { posterObj.optString("original", "") }

                                if (imageUrl.isNotBlank()) {
                                    results.add(
                                        WebImageSuggestion(
                                            title = canonicalTitle,
                                            imageUrl = imageUrl,
                                            type = mediaType
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore single network call errors
        }

        if (results.isNotEmpty()) break
    }

    return@withContext results.distinctBy { it.imageUrl }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: MediaViewModel,
    entryId: Int,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = entryId != -1

    val entryToEdit by if (isEditMode) {
        viewModel.getEntryById(entryId).collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(null) }
    }

    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MediaType.MANGAS) }
    var selectedStatus by remember { mutableStateOf(MediaStatus.ONGOING) }
    var isUserFinished by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    var seasonInput by remember { mutableStateOf("") }
    var episodeInput by remember { mutableStateOf("") }
    var readingUnit by remember { mutableStateOf("Chapitre") }
    var unitNumberInput by remember { mutableStateOf("") }

    var expandedType by remember { mutableStateOf(false) }
    var expandedStatus by remember { mutableStateOf(false) }
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }
    var showSuggestionsSheet by remember { mutableStateOf(false) }
    var isSearchingImages by remember { mutableStateOf(false) }
    var fetchedSuggestions by remember { mutableStateOf<List<WebImageSuggestion>>(emptyList()) }

    val isTitleEntered = title.trim().isNotBlank()

    LaunchedEffect(showSuggestionsSheet, title, selectedType) {
        if (showSuggestionsSheet && title.trim().isNotBlank()) {
            isSearchingImages = true
            fetchedSuggestions = fetchWebImageSuggestions(title, selectedType)
            isSearchingImages = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            imageUrl = it.toString()
        }
    }

    LaunchedEffect(entryToEdit) {
        entryToEdit?.let {
            title = it.title
            selectedType = it.type
            selectedStatus = it.status
            isUserFinished = it.isFinished
            imageUrl = it.imageUrl

            when (it.type) {
                MediaType.SERIES, MediaType.ANIMES -> {
                    val (s, e) = parseSeriesProgress(it.progress)
                    seasonInput = s
                    episodeInput = e
                }
                MediaType.MANGAS, MediaType.WEBTOONS -> {
                    val (unit, num) = parseReadingProgress(it.progress)
                    readingUnit = unit
                    unitNumberInput = num
                }
                MediaType.FILMS -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Modifier l'entrée" else "Nouvelle entrée") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview Header Area (Cover Image OR Colored Background with Title)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Clear photo button overlay
                        IconButton(
                            onClick = { imageUrl = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "Supprimer l'image", tint = Color.White)
                        }
                    } else {
                        // Colored Background gradient with Media Name centered
                        val gradientColors = remember(title, selectedType) {
                            getPlaceholderGradient(title, selectedType)
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
                                modifier = Modifier.padding(24.dp)
                            ) {
                                val typeIcon = when (selectedType) {
                                    MediaType.MANGAS, MediaType.WEBTOONS -> Icons.Outlined.AutoStories
                                    MediaType.ANIMES -> Icons.Outlined.PlayCircle
                                    MediaType.SERIES -> Icons.Outlined.Tv
                                    MediaType.FILMS -> Icons.Outlined.Movie
                                }

                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(40.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = title.ifBlank { "Nom du média" },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "Fond coloré par défaut",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Image Picker Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galerie", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (isTitleEntered) {
                            showSuggestionsSheet = true
                        }
                    },
                    enabled = isTitleEntered,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Suggestions", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showUrlInputDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("URL Web", fontSize = 12.sp)
                }
            }

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre du média") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Dropdown for Media Type
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = !expandedType }
            ) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    MediaType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                selectedType = type
                                expandedType = false
                            }
                        )
                    }
                }
            }

            // Status Section
            if (selectedType == MediaType.FILMS) {
                // For Films: Simple "À voir" or "Vu" choice
                Text(
                    text = "Statut du film",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !isUserFinished,
                        onClick = {
                            isUserFinished = false
                            selectedStatus = MediaStatus.COMPLETED
                        },
                        label = { Text("À voir") },
                        leadingIcon = if (!isUserFinished) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = isUserFinished,
                        onClick = {
                            isUserFinished = true
                            selectedStatus = MediaStatus.COMPLETED
                        },
                        label = { Text("Vu (Terminé)") },
                        leadingIcon = if (isUserFinished) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Dropdown for Media Publication Status
                ExposedDropdownMenuBox(
                    expanded = expandedStatus,
                    onExpandedChange = { expandedStatus = !expandedStatus }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Statut de publication de l'œuvre") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedStatus,
                        onDismissRequest = { expandedStatus = false }
                    ) {
                        MediaStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.displayName) },
                                onClick = {
                                    selectedStatus = status
                                    expandedStatus = false
                                }
                            )
                        }
                    }
                }

                // Card for User Completion Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedType == MediaType.MANGAS || selectedType == MediaType.WEBTOONS) "Terminé de lire (par vous)" else "Terminé de regarder (par vous)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Activer si vous avez terminé la lecture/visionnage, quel que soit le statut de parution de l'œuvre.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isUserFinished,
                            onCheckedChange = { isUserFinished = it }
                        )
                    }
                }
            }

            // Category-dependent Progression Fields
            when (selectedType) {
                MediaType.SERIES, MediaType.ANIMES -> {
                    Text(
                        text = "Progression (Saison & Épisode)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = seasonInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) seasonInput = input
                            },
                            label = { Text("Saison") },
                            placeholder = { Text("Ex: 1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = episodeInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) episodeInput = input
                            },
                            label = { Text("Épisode") },
                            placeholder = { Text("Ex: 12") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                MediaType.MANGAS, MediaType.WEBTOONS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Unité de progression",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = readingUnit == "Chapitre",
                                onClick = { readingUnit = "Chapitre" },
                                label = { Text("Chapitre") },
                                leadingIcon = if (readingUnit == "Chapitre") {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(8.dp)
                            )

                            FilterChip(
                                selected = readingUnit == "Tome",
                                onClick = { readingUnit = "Tome" },
                                label = { Text("Tome") },
                                leadingIcon = if (readingUnit == "Tome") {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = unitNumberInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) unitNumberInput = input
                            },
                            label = { Text("Numéro de $readingUnit") },
                            placeholder = { Text("Ex: 42") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                MediaType.FILMS -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Les films sont traités comme une unité unique (À voir ou Vu).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val computedProgress = when (selectedType) {
                            MediaType.SERIES, MediaType.ANIMES -> {
                                val s = seasonInput.filter { it.isDigit() }
                                val e = episodeInput.filter { it.isDigit() }
                                when {
                                    s.isNotBlank() && e.isNotBlank() -> "Saison $s - Épisode $e"
                                    s.isNotBlank() -> "Saison $s"
                                    e.isNotBlank() -> "Épisode $e"
                                    else -> "Non spécifié"
                                }
                            }
                            MediaType.MANGAS, MediaType.WEBTOONS -> {
                                val num = unitNumberInput.filter { it.isDigit() }
                                if (num.isNotBlank()) "$readingUnit $num" else "Non spécifié"
                            }
                            MediaType.FILMS -> if (isUserFinished) "Vu" else "À voir"
                        }

                        val finalStatus = if (selectedType == MediaType.FILMS) MediaStatus.COMPLETED else selectedStatus

                        val newEntry = MediaEntry(
                            id = if (isEditMode) entryId else 0,
                            title = title,
                            type = selectedType,
                            status = finalStatus,
                            progress = computedProgress,
                            imageUrl = imageUrl,
                            isFinished = isUserFinished
                        )
                        coroutineScope.launch {
                            viewModel.insert(newEntry)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Enregistrer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal Dialog to enter Image URL
    if (showUrlInputDialog) {
        AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = { Text("Entrer l'URL d'une image web") },
            text = {
                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    placeholder = { Text("https://example.com/poster.jpg") },
                    label = { Text("Lien direct vers l'image") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customUrlInput.isNotBlank()) {
                            imageUrl = customUrlInput
                            customUrlInput = ""
                        }
                        showUrlInputDialog = false
                    }
                ) {
                    Text("Appliquer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlInputDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Bottom Sheet / Dialog for Web Image Suggestions
    if (showSuggestionsSheet) {
        AlertDialog(
            onDismissRequest = { showSuggestionsSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Affiches pour « ${title.trim()} »")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSearchingImages) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Recherche d'images sur internet pour « ${title.trim()} »...",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val suggestionsToDisplay = if (fetchedSuggestions.isNotEmpty()) {
                            fetchedSuggestions
                        } else {
                            PRESET_IMAGE_SUGGESTIONS
                        }

                        Text(
                            text = if (fetchedSuggestions.isNotEmpty()) {
                                "Sélectionnez une affiche officielle trouvée sur internet :"
                            } else {
                                "Aucune image spécifique trouvée. Choisissez parmi les suggestions :"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(suggestionsToDisplay) { suggestion ->
                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(150.dp)
                                        .clickable {
                                            imageUrl = suggestion.imageUrl
                                            showSuggestionsSheet = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = suggestion.imageUrl,
                                            contentDescription = suggestion.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .background(Color.Black.copy(alpha = 0.75f))
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = suggestion.title,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuggestionsSheet = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

fun parseSeriesProgress(progress: String): Pair<String, String> {
    val seasonRegex = Regex("""Saison\s*(\d+)""", RegexOption.IGNORE_CASE)
    val epRegex = Regex("""Épisode\s*(\d+)|Ep\.\s*(\d+)|Episode\s*(\d+)""", RegexOption.IGNORE_CASE)

    val season = seasonRegex.find(progress)?.groupValues?.get(1) ?: ""
    val epMatch = epRegex.find(progress)
    val episode = epMatch?.groupValues?.filter { it.isNotBlank() }?.lastOrNull() ?: ""

    if (season.isEmpty() && episode.isEmpty() && progress.isNotBlank()) {
        val nums = Regex("""\d+""").findAll(progress).map { it.value }.toList()
        val s = nums.getOrNull(0) ?: ""
        val e = nums.getOrNull(1) ?: ""
        return Pair(s, e)
    }

    return Pair(season, episode)
}

fun parseReadingProgress(progress: String): Pair<String, String> {
    val isTome = progress.contains("Tome", ignoreCase = true)
    val unit = if (isTome) "Tome" else "Chapitre"
    val digits = Regex("""\d+""").find(progress)?.value ?: ""
    return Pair(unit, digits)
}

fun getPlaceholderGradient(title: String, type: MediaType): List<Color> {
    val hash = kotlin.math.abs((title + type.name).hashCode())
    val palettes = listOf(
        listOf(Color(0xFF512DA8), Color(0xFF673AB7)), // Purple
        listOf(Color(0xFF00796B), Color(0xFF009688)), // Teal
        listOf(Color(0xFFC2185B), Color(0xFFE91E63)), // Pink
        listOf(Color(0xFF1976D2), Color(0xFF2196F3)), // Blue
        listOf(Color(0xFFD84315), Color(0xFFFF5722)), // Deep Orange
        listOf(Color(0xFF283593), Color(0xFF3F51B5)), // Indigo
        listOf(Color(0xFF388E3C), Color(0xFF4CAF50)), // Green
        listOf(Color(0xFF4527A0), Color(0xFF7E57C2))  // Deep Purple
    )
    return palettes[hash % palettes.size]
}
