package de.hasi.hasitv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.hasi.hasitv.data.model.Channel
import de.hasi.hasitv.data.model.EpgProgram
import de.hasi.hasitv.data.model.Playlist
import de.hasi.hasitv.data.repository.IptvRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo = IptvRepository(app)

    // ─── Channels ────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup = _selectedGroup.asStateFlow()

    val allChannels: StateFlow<List<Channel>> = repo.getAllChannelsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredChannels: StateFlow<List<Channel>> =
        combine(allChannels, _selectedGroup, _searchQuery) { channels, group, query ->
            var result = channels
            result = when (group) {
                "All"          -> result
                "⭐ Favoriler" -> result.filter { it.isFavorite }
                else           -> result.filter { it.group == group }
            }
            if (query.isNotBlank()) {
                result = result.filter { it.name.contains(query, ignoreCase = true) }
            }
            result
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentChannels: StateFlow<List<Channel>> = repo.getRecentFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _groups = MutableStateFlow<List<String>>(listOf("All", "⭐ Favoriler"))
    val groups = _groups.asStateFlow()

    // ─── Playlists ───────────────────────────────────────────────
    val playlists: StateFlow<List<Playlist>> = repo.getPlaylistsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ─── Loading / Error ─────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    // ─── Player state ────────────────────────────────────────────
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _currentProgram = MutableStateFlow<EpgProgram?>(null)
    val currentProgram = _currentProgram.asStateFlow()

    private val _nextProgram = MutableStateFlow<EpgProgram?>(null)
    val nextProgram = _nextProgram.asStateFlow()

    // ─── Init ─────────────────────────────────────────────────────
    init {
        viewModelScope.launch { refreshGroups() }
        viewModelScope.launch {
            allChannels.collect { refreshGroups() }
        }
    }

    // ─── Actions ─────────────────────────────────────────────────
    fun setSearch(q: String) { _searchQuery.value = q }
    fun setGroup(g: String)  { _selectedGroup.value = g }

    fun toggleFavorite(channel: Channel) = viewModelScope.launch {
        repo.toggleFavorite(channel)
    }

    fun selectChannel(channel: Channel) = viewModelScope.launch {
        _currentChannel.value = channel
        repo.markWatched(channel)
        val epgId = channel.epgId ?: channel.id
        _currentProgram.value = repo.getCurrentProgram(epgId)
        _nextProgram.value    = repo.getNextProgram(epgId)
    }

    fun addPlaylist(playlist: Playlist) = viewModelScope.launch {
        _isLoading.value = true
        _errorMsg.value  = null
        try {
            repo.addPlaylist(playlist)
        } catch (e: Exception) {
            _errorMsg.value = e.message
        }
        _isLoading.value = false
    }

    fun deletePlaylist(playlist: Playlist) = viewModelScope.launch {
        repo.deletePlaylist(playlist)
    }

    fun refreshAll() = viewModelScope.launch {
        _isLoading.value = true
        try {
            repo.refreshAllPlaylists()
            repo.refreshEpg()
        } catch (e: Exception) {
            _errorMsg.value = e.message
        }
        _isLoading.value = false
    }

    fun refreshEpg(force: Boolean = false) = viewModelScope.launch {
        try { repo.refreshEpg(force) }
        catch (e: Exception) { _errorMsg.value = e.message }
    }

    fun clearError() { _errorMsg.value = null }

    private suspend fun refreshGroups() {
        val dbGroups = repo.getGroups().sorted()
        _groups.value = listOf("All", "⭐ Favoriler") + dbGroups
    }
}
