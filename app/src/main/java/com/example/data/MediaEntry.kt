package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType(val displayName: String) {
    SERIES("Séries"),
    FILMS("Films"),
    MANGAS("Mangas"),
    ANIMES("Animés"),
    WEBTOONS("Webtoons")
}

enum class MediaStatus(val displayName: String) {
    ONGOING("En cours"),
    ON_HOLD("En pause"),
    COMPLETED("Terminé")
}

@Entity(tableName = "media_entries")
data class MediaEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: MediaType,
    val status: MediaStatus, // Statut de parution de l'œuvre (En cours, En pause, Terminé)
    val progress: String, // e.g. "Saison 1 Episode 4", "Chapitre 12", "Vu"
    val imageUrl: String? = null,
    val isFinished: Boolean = false, // Indique si l'utilisateur a fini de lire/regarder
    val timestamp: Long = System.currentTimeMillis()
)
