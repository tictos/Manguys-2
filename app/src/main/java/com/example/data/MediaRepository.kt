package com.example.data

import kotlinx.coroutines.flow.Flow

class MediaRepository(private val mediaDao: MediaDao) {
    val allEntries: Flow<List<MediaEntry>> = mediaDao.getAllEntries()

    fun getEntryById(id: Int): Flow<MediaEntry?> {
        return mediaDao.getEntryById(id)
    }

    suspend fun insert(entry: MediaEntry) {
        mediaDao.insertEntry(entry)
    }

    suspend fun insertAll(entries: List<MediaEntry>) {
        mediaDao.insertAll(entries)
    }

    suspend fun deleteById(id: Int) {
        mediaDao.deleteEntryById(id)
    }
}
