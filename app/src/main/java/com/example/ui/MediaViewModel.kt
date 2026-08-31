package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaEntry
import com.example.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaViewModel(private val repository: MediaRepository) : ViewModel() {

    val allEntries: StateFlow<List<MediaEntry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getEntryById(id: Int): StateFlow<MediaEntry?> {
        return repository.getEntryById(id).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    fun insert(entry: MediaEntry) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(entry)
    }

    fun insertAll(entries: List<MediaEntry>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertAll(entries)
    }

    fun deleteById(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteById(id)
    }
}
