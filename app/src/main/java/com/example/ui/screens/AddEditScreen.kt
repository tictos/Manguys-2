package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.MediaEntry
import com.example.data.MediaStatus
import com.example.data.MediaType
import com.example.ui.MediaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WebImageSuggestion(
    val title: String,
    val imageUrl: String,
    val type: MediaType
)

data class WebSearchResult(
    val suggestions: List<WebImageSuggestion>,
    val errorMessage: String? = null,
    val isOffline: Boolean = false
)

class SearchNetworkTracker {
    @Volatile
    var hasSuccess: Boolean = false
    @Volatile
    var lastException: Exception? = null

    fun recordSuccess() {
        hasSuccess = true
    }

    fun recordException(e: Exception) {
        lastException = e
    }

    fun isOfflineException(): Boolean {
        val ex = lastException ?: return false
        return ex is java.net.UnknownHostException ||
                ex is java.net.ConnectException ||
                (ex.cause is java.net.UnknownHostException) ||
                (ex.cause is java.net.ConnectException)
    }
}

private fun searchAniList(client: OkHttpClient, query: String, userAgent: String, tracker: SearchNetworkTracker? = null): List<WebImageSuggestion> {
    val results = mutableListOf<WebImageSuggestion>()
    try {
        val jsonQuery = "query (\$search: String) { Page(page: 1, perPage: 8) { media(search: \$search) { id title { romaji english native } coverImage { extraLarge large medium } type format } } }"
        val payload = JSONObject().apply {
            put("query", jsonQuery)
            put("variables", JSONObject().put("search", query.trim()))
        }

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .post(requestBody)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                tracker?.recordSuccess()
                val bodyStr = response.body?.string() ?: return@use
                val root = JSONObject(bodyStr)
                val mediaArray = root.optJSONObject("data")
                    ?.optJSONObject("Page")
                    ?.optJSONArray("media") ?: return@use

                for (i in 0 until mediaArray.length()) {
                    val item = mediaArray.getJSONObject(i)
                    val titleObj = item.optJSONObject("title")
                    val displayTitle = titleObj?.optString("english")?.takeIf { it.isNotBlank() }
                        ?: titleObj?.optString("romaji")?.takeIf { it.isNotBlank() }
                        ?: titleObj?.optString("native")?.takeIf { it.isNotBlank() }
                        ?: query

                    val coverObj = item.optJSONObject("coverImage") ?: continue
                    val imgUrl = coverObj.optString("large", "")
                        .ifBlank { coverObj.optString("extraLarge", "") }
                        .ifBlank { coverObj.optString("medium", "") }

                    val itemType = item.optString("type", "")
                    val mappedType = if (itemType.equals("MANGA", ignoreCase = true)) MediaType.MANGAS else MediaType.ANIMES

                    if (imgUrl.isNotBlank()) {
                        results.add(WebImageSuggestion(displayTitle, imgUrl, mappedType))
                    }
                }
            }
        }
    } catch (e: Exception) {
        tracker?.recordException(e)
    }
    return results
}

private fun searchKitsu(client: OkHttpClient, query: String, userAgent: String, mediaType: MediaType, tracker: SearchNetworkTracker? = null): List<WebImageSuggestion> {
    val results = mutableListOf<WebImageSuggestion>()
    try {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val categories = when (mediaType) {
            MediaType.MANGAS, MediaType.WEBTOONS -> listOf("manga", "anime")
            else -> listOf("anime", "manga")
        }

        for (cat in categories) {
            try {
                val url = "https://kitsu.io/api/edge/$cat?filter%5Btext%5D=$encoded&page%5Blimit%5D=8"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/vnd.api+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        tracker?.recordSuccess()
                        val bodyStr = response.body?.string() ?: return@use
                        val root = JSONObject(bodyStr)
                        val dataArray = root.optJSONArray("data") ?: return@use
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val attributes = item.optJSONObject("attributes") ?: continue
                            val canonicalTitle = attributes.optString("canonicalTitle", query)
                            val posterObj = attributes.optJSONObject("posterImage") ?: continue

                            val imgUrl = posterObj.optString("medium", "")
                                .ifBlank { posterObj.optString("large", "") }
                                .ifBlank { posterObj.optString("original", "") }

                            if (imgUrl.isNotBlank()) {
                                results.add(WebImageSuggestion(canonicalTitle, imgUrl, mediaType))
                            }
                        }
                    }
                }
                if (results.isNotEmpty()) break
            } catch (e: Exception) {
                tracker?.recordException(e)
            }
        }
    } catch (e: Exception) {
        tracker?.recordException(e)
    }
    return results
}

private fun searchTVMaze(client: OkHttpClient, query: String, userAgent: String, tracker: SearchNetworkTracker? = null): List<WebImageSuggestion> {
    val results = mutableListOf<WebImageSuggestion>()
    try {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://api.tvmaze.com/search/shows?q=$encoded"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                tracker?.recordSuccess()
                val bodyStr = response.body?.string() ?: return@use
                val array = JSONArray(bodyStr)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val show = item.optJSONObject("show") ?: continue
                    val name = show.optString("name", query)
                    val imageObj = show.optJSONObject("image") ?: continue
                    val imgUrl = imageObj.optString("medium", "")
                        .ifBlank { imageObj.optString("original", "") }

                    if (imgUrl.isNotBlank()) {
                        results.add(WebImageSuggestion(name, imgUrl, MediaType.SERIES))
                    }
                }
            }
        }
    } catch (e: Exception) {
        tracker?.recordException(e)
    }
    return results
}

private fun searchWikipedia(client: OkHttpClient, query: String, userAgent: String, mediaType: MediaType, tracker: SearchNetworkTracker? = null): List<WebImageSuggestion> {
    val results = mutableListOf<WebImageSuggestion>()
    val langs = listOf("fr", "en")
    for (lang in langs) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "https://$lang.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=5&format=json"
            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", userAgent)
                .build()

            val titles = mutableListOf<String>()
            client.newCall(searchReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    tracker?.recordSuccess()
                    val body = resp.body?.string() ?: return@use
                    val array = JSONArray(body)
                    if (array.length() > 1) {
                        val titlesArr = array.getJSONArray(1)
                        for (i in 0 until titlesArr.length()) {
                            val t = titlesArr.getString(i)
                            if (t.isNotBlank()) titles.add(t)
                        }
                    }
                }
            }

            if (!titles.contains(query.trim())) {
                titles.add(0, query.trim())
            }

            for (title in titles.take(4)) {
                try {
                    val encTitle = URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                    val summaryUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encTitle"
                    val sumReq = Request.Builder()
                        .url(summaryUrl)
                        .header("User-Agent", userAgent)
                        .build()

                    client.newCall(sumReq).execute().use { sumResp ->
                        if (sumResp.isSuccessful) {
                            tracker?.recordSuccess()
                            val body = sumResp.body?.string() ?: return@use
                            val obj = JSONObject(body)
                            val pageTitle = obj.optString("title", title)
                            val thumb = obj.optJSONObject("thumbnail")
                            val imgUrl = thumb?.optString("source", "") ?: ""
                            if (imgUrl.isNotBlank()) {
                                results.add(WebImageSuggestion(pageTitle, imgUrl, mediaType))
                            }
                        }
                    }
                } catch (e: Exception) {
                    tracker?.recordException(e)
                }
            }
            if (results.isNotEmpty()) break
        } catch (e: Exception) {
            tracker?.recordException(e)
        }
    }
    return results
}

private fun searchMangaDex(client: OkHttpClient, query: String, userAgent: String, tracker: SearchNetworkTracker? = null): List<WebImageSuggestion> {
    val results = mutableListOf<WebImageSuggestion>()
    try {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://api.mangadex.org/manga?title=$encoded&limit=6&includes[]=cover_art"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                tracker?.recordSuccess()
                val bodyStr = response.body?.string() ?: return@use
                val root = JSONObject(bodyStr)
                val dataArray = root.optJSONArray("data") ?: return@use
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val mangaId = item.optString("id")
                    val attrs = item.optJSONObject("attributes") ?: continue
                    val titleObj = attrs.optJSONObject("title")
                    val titleName = titleObj?.keys()?.asSequence()?.firstOrNull()?.let { titleObj.optString(it) }
                        ?.takeIf { it.isNotBlank() } ?: query

                    var coverFileName = ""
                    val rels = item.optJSONArray("relationships")
                    if (rels != null) {
                        for (j in 0 until rels.length()) {
                            val r = rels.getJSONObject(j)
                            if (r.optString("type") == "cover_art") {
                                coverFileName = r.optJSONObject("attributes")?.optString("fileName", "") ?: ""
                                if (coverFileName.isNotBlank()) break
                            }
                        }
                    }

                    if (coverFileName.isNotBlank()) {
                        val imgUrl = "https://uploads.mangadex.org/covers/$mangaId/$coverFileName.512.jpg"
                        results.add(WebImageSuggestion(titleName, imgUrl, MediaType.MANGAS))
                    }
                }
            }
        }
    } catch (e: Exception) {
        tracker?.recordException(e)
    }
    return results
}

private fun searchITunes(client: OkHttpClient, query: String, userAgent: String, mediaType: MediaType, tracker: SearchNetworkTracker? = null): List<WebImageSuggestion> {
    val results = mutableListOf<WebImageSuggestion>()
    try {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&limit=8"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                tracker?.recordSuccess()
                val bodyStr = response.body?.string() ?: return@use
                val root = JSONObject(bodyStr)
                val array = root.optJSONArray("results") ?: return@use
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val name = item.optString("trackName").takeIf { it.isNotBlank() }
                        ?: item.optString("collectionName").takeIf { it.isNotBlank() }
                        ?: query
                    val artwork100 = item.optString("artworkUrl100", "")
                    if (artwork100.isNotBlank()) {
                        val highResArt = artwork100.replace("100x100bb", "600x600bb")
                        results.add(WebImageSuggestion(name, highResArt, mediaType))
                    }
                }
            }
        }
    } catch (e: Exception) {
        tracker?.recordException(e)
    }
    return results
}

suspend fun fetchWebImageSuggestions(searchTitle: String, mediaType: MediaType, context: Context? = null): WebSearchResult = withContext(Dispatchers.IO) {
    val query = searchTitle.trim()
    if (query.isBlank()) return@withContext WebSearchResult(emptyList())

    val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    val userAgent = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    val tracker = SearchNetworkTracker()
    val results = mutableListOf<WebImageSuggestion>()

    // Run independent API calls in parallel via supervisorScope so failure of one does not cancel others
    supervisorScope {
        val anilistDef = async { withTimeoutOrNull(6000L) { searchAniList(client, query, userAgent, tracker) } ?: emptyList() }
        val mangadexDef = async { withTimeoutOrNull(6000L) { searchMangaDex(client, query, userAgent, tracker) } ?: emptyList() }
        val kitsuDef = async { withTimeoutOrNull(6000L) { searchKitsu(client, query, userAgent, mediaType, tracker) } ?: emptyList() }
        val itunesDef = async { withTimeoutOrNull(6000L) { searchITunes(client, query, userAgent, mediaType, tracker) } ?: emptyList() }
        val tvmazeDef = async { withTimeoutOrNull(6000L) { searchTVMaze(client, query, userAgent, tracker) } ?: emptyList() }
        val wikiDef = async { withTimeoutOrNull(6000L) { searchWikipedia(client, query, userAgent, mediaType, tracker) } ?: emptyList() }

        val anilistRes = try { anilistDef.await() } catch (e: Exception) { tracker.recordException(e); emptyList() }
        val mangadexRes = try { mangadexDef.await() } catch (e: Exception) { tracker.recordException(e); emptyList() }
        val kitsuRes = try { kitsuDef.await() } catch (e: Exception) { tracker.recordException(e); emptyList() }
        val itunesRes = try { itunesDef.await() } catch (e: Exception) { tracker.recordException(e); emptyList() }
        val tvmazeRes = try { tvmazeDef.await() } catch (e: Exception) { tracker.recordException(e); emptyList() }
        val wikiRes = try { wikiDef.await() } catch (e: Exception) { tracker.recordException(e); emptyList() }

        when (mediaType) {
            MediaType.MANGAS, MediaType.WEBTOONS -> {
                results.addAll(anilistRes)
                results.addAll(mangadexRes)
                results.addAll(kitsuRes)
                results.addAll(itunesRes)
                results.addAll(wikiRes)
                results.addAll(tvmazeRes)
            }
            MediaType.ANIMES -> {
                results.addAll(anilistRes)
                results.addAll(itunesRes)
                results.addAll(kitsuRes)
                results.addAll(wikiRes)
                results.addAll(mangadexRes)
                results.addAll(tvmazeRes)
            }
            MediaType.SERIES -> {
                results.addAll(tvmazeRes)
                results.addAll(itunesRes)
                results.addAll(wikiRes)
                results.addAll(anilistRes)
                results.addAll(kitsuRes)
            }
            MediaType.FILMS -> {
                results.addAll(itunesRes)
                results.addAll(wikiRes)
                results.addAll(tvmazeRes)
                results.addAll(anilistRes)
                results.addAll(kitsuRes)
            }
        }
    }

    val distinctResults = results.distinctBy { it.imageUrl }
    var errorMessage: String? = null
    var isOffline = false

    if (distinctResults.isEmpty()) {
        if (!tracker.hasSuccess && tracker.isOfflineException()) {
            isOffline = true
            errorMessage = "Aucune connexion internet détectée. Vérifiez votre Wi-Fi ou vos données mobiles."
        } else if (!tracker.hasSuccess && tracker.lastException is java.net.SocketTimeoutException) {
            errorMessage = "Le délai de recherche a expiré. Vérifiez votre débit internet."
        }
    }

    WebSearchResult(distinctResults, errorMessage, isOffline)
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
    var searchErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSearchOffline by remember { mutableStateOf(false) }
    var dialogSearchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    var autoSuggestions by remember { mutableStateOf<List<WebImageSuggestion>>(emptyList()) }
    var isAutoSearching by remember { mutableStateOf(false) }

    // Automatic background suggestions when title changes
    LaunchedEffect(title, selectedType) {
        val trimmed = title.trim()
        if (trimmed.length >= 2) {
            delay(500)
            isAutoSearching = true
            val res = fetchWebImageSuggestions(trimmed, selectedType)
            autoSuggestions = res.suggestions
            isAutoSearching = false
        } else {
            autoSuggestions = emptyList()
            isAutoSearching = false
        }
    }

    // Debounced search inside the dialog
    LaunchedEffect(dialogSearchQuery, showSuggestionsSheet) {
        if (showSuggestionsSheet) {
            val queryToSearch = dialogSearchQuery.trim().ifBlank { title.trim() }
            if (queryToSearch.length >= 2) {
                delay(350)
                isSearchingImages = true
                searchErrorMessage = null
                isSearchOffline = false
                val result = fetchWebImageSuggestions(queryToSearch, selectedType)
                fetchedSuggestions = result.suggestions
                searchErrorMessage = result.errorMessage
                isSearchOffline = result.isOffline
                isSearchingImages = false
            }
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
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                                .crossfade(true)
                                .build(),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                                }
                            },
                            error = {
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Image indisponible", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
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
                        dialogSearchQuery = title.trim()
                        showSuggestionsSheet = true
                    },
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

            // Automatic live poster suggestions shelf
            if (isAutoSearching || autoSuggestions.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Affiches suggérées pour « ${title.trim()} »",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isAutoSearching) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            }
                        }

                        if (autoSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(autoSuggestions) { suggestion ->
                                    val isSelected = imageUrl == suggestion.imageUrl
                                    Card(
                                        modifier = Modifier
                                            .width(95.dp)
                                            .height(135.dp)
                                            .clickable {
                                                imageUrl = suggestion.imageUrl
                                            }
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                                else Modifier
                                            ),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(suggestion.imageUrl)
                                                    .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = suggestion.title,
                                                contentScale = ContentScale.Crop,
                                                loading = {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                    }
                                                },
                                                error = {
                                                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (isSelected) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                                                ) {
                                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(2.dp))
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .background(Color.Black.copy(alpha = 0.7f))
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = suggestion.title,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
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
                }
            }

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

    // Modal Bottom Sheet for Web Image Suggestions (matching the user's requested layout)
    if (showSuggestionsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSuggestionsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with sparkle icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Images suggérées du web",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (dialogSearchQuery.isNotBlank())
                        "Sélectionnez une affiche d'illustration pour votre média :"
                    else
                        "Recherchez une affiche pour votre œuvre :",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Search field inside the bottom sheet
                OutlinedTextField(
                    value = dialogSearchQuery,
                    onValueChange = { dialogSearchQuery = it },
                    label = { Text("Titre recherché") },
                    placeholder = { Text("Ex: Naruto, Solo Leveling, One Piece...") },
                    singleLine = true,
                    trailingIcon = {
                        if (isSearchingImages) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = {
                                if (dialogSearchQuery.trim().isNotBlank()) {
                                    coroutineScope.launch {
                                        isSearchingImages = true
                                        searchErrorMessage = null
                                        isSearchOffline = false
                                        val res = fetchWebImageSuggestions(dialogSearchQuery.trim(), selectedType)
                                        fetchedSuggestions = res.suggestions
                                        searchErrorMessage = res.errorMessage
                                        isSearchOffline = res.isOffline
                                        isSearchingImages = false
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = "Rechercher")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (dialogSearchQuery.trim().isNotBlank()) {
                                coroutineScope.launch {
                                    isSearchingImages = true
                                    searchErrorMessage = null
                                    isSearchOffline = false
                                    val res = fetchWebImageSuggestions(dialogSearchQuery.trim(), selectedType)
                                    fetchedSuggestions = res.suggestions
                                    searchErrorMessage = res.errorMessage
                                    isSearchOffline = res.isOffline
                                    isSearchingImages = false
                                }
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error banner if network issue or timeout
                if (searchErrorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSearchOffline) Icons.Filled.WifiOff else Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSearchOffline) "Pas de connexion internet" else "Erreur de chargement",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = searchErrorMessage ?: "Une connexion internet est nécessaire pour suggérer des affiches.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isSearchingImages = true
                                        searchErrorMessage = null
                                        isSearchOffline = false
                                        val res = fetchWebImageSuggestions(dialogSearchQuery.trim(), selectedType)
                                        fetchedSuggestions = res.suggestions
                                        searchErrorMessage = res.errorMessage
                                        isSearchOffline = res.isOffline
                                        isSearchingImages = false
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Réessayer", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Loading State
                if (isSearchingImages) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Recherche sur internet pour « ${dialogSearchQuery.trim()} »...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (fetchedSuggestions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(fetchedSuggestions) { suggestion ->
                            val isSelected = imageUrl == suggestion.imageUrl
                            Card(
                                modifier = Modifier
                                    .width(125.dp)
                                    .height(175.dp)
                                    .clickable {
                                        imageUrl = suggestion.imageUrl
                                        if (title.isBlank()) {
                                            title = suggestion.title
                                        }
                                        showSuggestionsSheet = false
                                    }
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                        else Modifier
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(suggestion.imageUrl)
                                            .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = suggestion.title,
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            }
                                        },
                                        error = {
                                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape,
                                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(3.dp))
                                        }
                                    }
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
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (searchErrorMessage == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (dialogSearchQuery.trim().isBlank())
                                "Entrez le nom d'une œuvre ci-dessus pour chercher des affiches."
                            else
                                "Aucune image trouvée pour « ${dialogSearchQuery.trim()} ».",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dialogSearchQuery.trim().isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Vérifiez l'orthographe ou essayez le titre en anglais / japonais.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Bottom row with "Fermer" button aligned to the right (like in the screenshot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showSuggestionsSheet = false }) {
                        Text("Fermer", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
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
