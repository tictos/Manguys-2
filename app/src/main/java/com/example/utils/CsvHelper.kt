package com.example.utils

import com.example.data.MediaEntry
import com.example.data.MediaStatus
import com.example.data.MediaType

object CsvHelper {

    fun exportToCsv(entries: List<MediaEntry>): String {
        val sb = StringBuilder()
        sb.append("Titre,Type,Statut,Progression,ImageURL,TermineParUtilisateur\n")
        for (entry in entries) {
            val titleEsc = escapeCsvField(entry.title)
            val typeEsc = escapeCsvField(entry.type.name)
            val statusEsc = escapeCsvField(entry.status.name)
            val progressEsc = escapeCsvField(entry.progress)
            val imgEsc = escapeCsvField(entry.imageUrl ?: "")
            val isFinishedEsc = escapeCsvField(entry.isFinished.toString())
            sb.append("$titleEsc,$typeEsc,$statusEsc,$progressEsc,$imgEsc,$isFinishedEsc\n")
        }
        return sb.toString()
    }

    private fun escapeCsvField(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }

    fun parseCsv(csvText: String): List<MediaEntry> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val parsedEntries = mutableListOf<MediaEntry>()

        for ((index, line) in lines.withIndex()) {
            val tokens = parseCsvLine(line)
            if (tokens.isEmpty()) continue

            // Skip header if line 0 looks like header ("titre", "title", etc.)
            if (index == 0 && (tokens[0].equals("titre", ignoreCase = true) || tokens[0].equals("title", ignoreCase = true))) {
                continue
            }

            if (tokens.size >= 2) {
                val title = tokens[0].trim()
                if (title.isEmpty()) continue

                val typeStr = tokens.getOrNull(1)?.trim() ?: ""
                val statusStr = tokens.getOrNull(2)?.trim() ?: ""
                val progressStr = tokens.getOrNull(3)?.trim() ?: ""
                val imgUrlStr = tokens.getOrNull(4)?.trim().let { if (it.isNullOrBlank()) null else it }
                val isFinishedStr = tokens.getOrNull(5)?.trim() ?: ""

                val mediaType = parseMediaType(typeStr)
                val mediaStatus = parseMediaStatus(statusStr)
                val isFinished = if (isFinishedStr.isNotBlank()) {
                    isFinishedStr.lowercase() == "true" || isFinishedStr == "1" || isFinishedStr.contains("oui", ignoreCase = true)
                } else {
                    mediaStatus == MediaStatus.COMPLETED
                }

                parsedEntries.add(
                    MediaEntry(
                        title = title,
                        type = mediaType,
                        status = mediaStatus,
                        progress = if (progressStr.isEmpty()) "Non démarré" else progressStr,
                        imageUrl = imgUrlStr,
                        isFinished = isFinished
                    )
                )
            }
        }

        return parsedEntries
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (insideQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++ // Skip escaped quote
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                c == ',' && !insideQuotes -> {
                    tokens.add(sb.toString())
                    sb.clear()
                }
                c == ';' && !insideQuotes -> {
                    // Support semicolon separator (common in French Excel CSV exports)
                    tokens.add(sb.toString())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }

    private fun parseMediaType(raw: String): MediaType {
        val clean = raw.trim().uppercase()
        return when {
            clean.contains("SERIE") || clean.contains("SÉRIE") -> MediaType.SERIES
            clean.contains("FILM") || clean.contains("MOVIE") -> MediaType.FILMS
            clean.contains("MANGA") -> MediaType.MANGAS
            clean.contains("ANIME") || clean.contains("ANIMÉ") -> MediaType.ANIMES
            clean.contains("WEBTOON") -> MediaType.WEBTOONS
            else -> {
                MediaType.entries.find { it.name.equals(clean, ignoreCase = true) || it.displayName.equals(raw, ignoreCase = true) }
                    ?: MediaType.MANGAS
            }
        }
    }

    private fun parseMediaStatus(raw: String): MediaStatus {
        val clean = raw.trim().uppercase()
        return when {
            clean.contains("COURS") || clean.contains("ONGOING") || clean.contains("IN_PROGRESS") -> MediaStatus.ONGOING
            clean.contains("PAUSE") || clean.contains("ON_HOLD") || clean.contains("HOLD") -> MediaStatus.ON_HOLD
            clean.contains("TERMIN") || clean.contains("FINI") || clean.contains("COMPLETED") || clean.contains("DONE") -> MediaStatus.COMPLETED
            else -> {
                MediaStatus.entries.find { it.name.equals(clean, ignoreCase = true) || it.displayName.equals(raw, ignoreCase = true) }
                    ?: MediaStatus.ONGOING
            }
        }
    }
}
