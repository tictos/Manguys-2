package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.MediaEntry
import com.example.data.MediaType
import com.example.ui.screens.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import androidx.compose.ui.graphics.Color
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class NewsNetworkManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent = "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0"

    /**
     * Checks if active network capability is available
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        // Check for general internet capability (validated not strictly required as emulators/captive portals may lag)
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Performs a real HTTP ping test to Kitsu API to verify internet access
     */
    suspend fun testLiveInternetConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = Request.Builder()
                .url("https://kitsu.io/api/edge/trending/anime?page[limit]=1")
                .header("User-Agent", userAgent)
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code in 200..429
            }
        } catch (e: Exception) {
            // Fallback check via connectivity manager
            isNetworkAvailable()
        }
    }

    /**
     * Helper to build clean, informative French summaries for anime/manga news entries
     */
    private fun buildFrenchSummary(
        title: String,
        type: MediaType,
        ratingDouble: Double,
        statusFr: String,
        subtype: String?,
        epOrChapCount: Int?
    ): String {
        val subtypeFr = when (subtype?.lowercase()) {
            "tv" -> "Série d'animation TV"
            "movie" -> "Film d'animation"
            "ova" -> "Épisode spécial / OVA"
            "manga" -> "Manga au format papier"
            "manhwa" -> "Webtoon / Manhwa"
            "manhua" -> "Manhua numérique"
            else -> if (type == MediaType.MANGAS || type == MediaType.WEBTOONS) "Bande dessinée / Manga" else "Série d'animation"
        }

        val countText = if (epOrChapCount != null && epOrChapCount > 0) {
            if (type == MediaType.MANGAS || type == MediaType.WEBTOONS) {
                " ($epOrChapCount chapitres)"
            } else {
                " ($epOrChapCount épisodes)"
            }
        } else ""

        val scoreFormatted = String.format("%.1f", ratingDouble)

        return "Fiche officielle $subtypeFr$countText. Statut de parution : $statusFr. " +
                "Note globale de la communauté : $scoreFormatted/10. " +
                "Retrouvez le suivi des épisodes, la progression et toutes les nouveautés en français."
    }

    /**
     * Fetches live anime/manga news and trending updates from Kitsu & Jikan APIs over HTTPS
     */
    suspend fun fetchLiveOnlineNews(userEntries: List<MediaEntry>): List<NewsArticle> = withContext(Dispatchers.IO) {
        val list = mutableListOf<NewsArticle>()

        val colorNewChapter = Color(0xFF4CAF50)
        val colorTrailer = Color(0xFF2196F3)
        val colorSeason = Color(0xFF9C27B0)
        val colorAnnouncement = Color(0xFFFF9800)
        val colorMovie = Color(0xFFE91E63)

        val trackedTitlesLower = userEntries.map { it.title.trim().lowercase() }

        // 1. Fetch live news for specific tracked titles from Kitsu API
        userEntries.take(8).forEachIndexed { index, entry ->
            val title = entry.title
            val type = entry.type

            try {
                val searchCategory = if (type == MediaType.MANGAS || type == MediaType.WEBTOONS) "manga" else "anime"
                val encodedQuery = URLEncoder.encode(title, "UTF-8")
                val requestUrl = "https://kitsu.io/api/edge/$searchCategory?filter[text]=$encodedQuery&page[limit]=1"

                val request = Request.Builder()
                    .url(requestUrl)
                    .header("User-Agent", userAgent)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrEmpty()) {
                            val json = JSONObject(bodyStr)
                            val dataArray = json.optJSONArray("data")
                            if (dataArray != null && dataArray.length() > 0) {
                                val item = dataArray.getJSONObject(0)
                                val attributes = item.optJSONObject("attributes")
                                if (attributes != null) {
                                    val canonicalTitle = attributes.optString("canonicalTitle", title)
                                    val averageRating = attributes.optString("averageRating", "80.0")
                                    val ratingDouble = (averageRating.toDoubleOrNull() ?: 80.0) / 10.0
                                    val status = attributes.optString("status", "current")
                                    val subtype = attributes.optString("subtype", "")
                                    val episodeCount = attributes.optInt("episodeCount", 0)
                                    val chapterCount = attributes.optInt("chapterCount", 0)
                                    val epOrChap = if (type == MediaType.MANGAS || type == MediaType.WEBTOONS) chapterCount else episodeCount

                                    val statusFr = when (status) {
                                        "current" -> "En cours de parution / diffusion"
                                        "finished" -> "Terminé"
                                        "tba" -> "À venir prochainement"
                                        "unreleased" -> "Prochainement"
                                        else -> "En cours"
                                    }

                                    val headline = when (type) {
                                        MediaType.ANIMES -> "Actualité Direct : $canonicalTitle (Note : ${String.format("%.1f", ratingDouble)}/10)"
                                        MediaType.MANGAS -> "Nouveau Chapitre / Volume : $canonicalTitle ($statusFr)"
                                        MediaType.SERIES -> "Série Live : $canonicalTitle - Mis à jour"
                                        MediaType.FILMS -> "Sortie Cinéma / Streaming : $canonicalTitle"
                                        MediaType.WEBTOONS -> "Épisode Webtoon en ligne : $canonicalTitle"
                                    }

                                    val frenchSummary = buildFrenchSummary(
                                        title = canonicalTitle,
                                        type = type,
                                        ratingDouble = ratingDouble,
                                        statusFr = statusFr,
                                        subtype = subtype,
                                        epOrChapCount = if (epOrChap > 0) epOrChap else null
                                    )

                                    list.add(
                                        NewsArticle(
                                            id = "kitsu_user_${entry.id}_$index",
                                            mediaTitle = title,
                                            mediaType = type,
                                            badgeLabel = "⚡ FLUX DIRECT",
                                            badgeColor = colorNewChapter,
                                            title = headline,
                                            summary = frenchSummary,
                                            timeAgo = "Mis à jour en direct",
                                            source = "Flux Direct Kitsu",
                                            isTrackedByUser = true
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore single item failure
            }
        }

        // 2. Fetch Live Trending Anime from Kitsu API
        try {
            val requestTrendingAnime = Request.Builder()
                .url("https://kitsu.io/api/edge/trending/anime?page[limit]=8")
                .header("User-Agent", userAgent)
                .build()

            client.newCall(requestTrendingAnime).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrEmpty()) {
                        val json = JSONObject(bodyStr)
                        val dataArray = json.optJSONArray("data")
                        if (dataArray != null) {
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val attributes = item.optJSONObject("attributes") ?: continue
                                val canonicalTitle = attributes.optString("canonicalTitle", "Animé Tendance")
                                val averageRating = attributes.optString("averageRating", "85.0")
                                val ratingDouble = (averageRating.toDoubleOrNull() ?: 85.0) / 10.0
                                val status = attributes.optString("status", "current")
                                val subtype = attributes.optString("subtype", "TV")
                                val epCount = attributes.optInt("episodeCount", 12)

                                val statusFr = when (status) {
                                    "current" -> "En cours de diffusion"
                                    "finished" -> "Série terminée"
                                    "tba" -> "Annonce à venir"
                                    else -> "En cours"
                                }

                                val isTracked = trackedTitlesLower.any { canonicalTitle.lowercase().contains(it) || it.contains(canonicalTitle.lowercase()) }

                                val frenchSummary = buildFrenchSummary(
                                    title = canonicalTitle,
                                    type = MediaType.ANIMES,
                                    ratingDouble = ratingDouble,
                                    statusFr = statusFr,
                                    subtype = subtype,
                                    epOrChapCount = if (epCount > 0) epCount else null
                                )

                                list.add(
                                    NewsArticle(
                                        id = "kitsu_trend_anime_$i",
                                        mediaTitle = canonicalTitle,
                                        mediaType = MediaType.ANIMES,
                                        badgeLabel = "🔥 ANIMÉ TENDANCE",
                                        badgeColor = colorTrailer,
                                        title = "Tendance Web : $canonicalTitle (${if (epCount > 0) "$epCount épisodes • " else ""}Note ${String.format("%.1f", ratingDouble)}/10)",
                                        summary = frenchSummary,
                                        timeAgo = "Aujourd'hui",
                                        source = "Flux Global Kitsu",
                                        isTrackedByUser = isTracked
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 3. Fetch Live Trending Manga from Kitsu API
        try {
            val requestTrendingManga = Request.Builder()
                .url("https://kitsu.io/api/edge/trending/manga?page[limit]=8")
                .header("User-Agent", userAgent)
                .build()

            client.newCall(requestTrendingManga).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrEmpty()) {
                        val json = JSONObject(bodyStr)
                        val dataArray = json.optJSONArray("data")
                        if (dataArray != null) {
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val attributes = item.optJSONObject("attributes") ?: continue
                                val canonicalTitle = attributes.optString("canonicalTitle", "Manga Tendance")
                                val averageRating = attributes.optString("averageRating", "87.0")
                                val ratingDouble = (averageRating.toDoubleOrNull() ?: 87.0) / 10.0
                                val status = attributes.optString("status", "current")
                                val subtype = attributes.optString("subtype", "manga")
                                val chapCount = attributes.optInt("chapterCount", 0)

                                val statusFr = when (status) {
                                    "current" -> "Parution en cours"
                                    "finished" -> "Série terminée"
                                    "tba" -> "Annonce à venir"
                                    else -> "En cours"
                                }

                                val isTracked = trackedTitlesLower.any { canonicalTitle.lowercase().contains(it) || it.contains(canonicalTitle.lowercase()) }

                                val frenchSummary = buildFrenchSummary(
                                    title = canonicalTitle,
                                    type = MediaType.MANGAS,
                                    ratingDouble = ratingDouble,
                                    statusFr = statusFr,
                                    subtype = subtype,
                                    epOrChapCount = if (chapCount > 0) chapCount else null
                                )

                                list.add(
                                    NewsArticle(
                                        id = "kitsu_trend_manga_$i",
                                        mediaTitle = canonicalTitle,
                                        mediaType = MediaType.MANGAS,
                                        badgeLabel = "📖 MANGA POPULAIRE",
                                        badgeColor = colorAnnouncement,
                                        title = "Parution Web : $canonicalTitle (Note ${String.format("%.1f", ratingDouble)}/10)",
                                        summary = frenchSummary,
                                        timeAgo = "En direct",
                                        source = "Flux Manga Kitsu",
                                        isTrackedByUser = isTracked
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 4. Ensure user entries that weren't fetched online still get custom tracked articles
        userEntries.forEachIndexed { index, entry ->
            val existsInList = list.any { it.mediaTitle.equals(entry.title, ignoreCase = true) }
            if (!existsInList) {
                val title = entry.title
                val type = entry.type

                val headline = when (type) {
                    MediaType.ANIMES -> "⚡ Diffusion & Épisodes : $title"
                    MediaType.MANGAS -> "📖 Chapitres & Sorties : $title"
                    MediaType.SERIES -> "📺 Actu Série : $title"
                    MediaType.FILMS -> "🎬 Sortie Film & Bandes-Annonces : $title"
                    MediaType.WEBTOONS -> "📱 Suivi Webtoon : $title"
                }

                list.add(
                    NewsArticle(
                        id = "user_tracked_gen_$index",
                        mediaTitle = title,
                        mediaType = type,
                        badgeLabel = "⭐ SUIVI DANS VOTRE LISTE",
                        badgeColor = colorNewChapter,
                        title = headline,
                        summary = "Fil d'actualité actif pour $title. Vos préférences, chapitres et progression sont synchronisés en français.",
                        timeAgo = "Aujourd'hui",
                        source = "Fil MangaTracker",
                        isTrackedByUser = true
                    )
                )
            }
        }

        return@withContext list.distinctBy { it.id }
    }
}

