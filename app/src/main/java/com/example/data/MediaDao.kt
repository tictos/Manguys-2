package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE id = :id")
    fun getEntryById(id: Int): Flow<MediaEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MediaEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<MediaEntry>)

    @Query("DELETE FROM media_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Int)
}
