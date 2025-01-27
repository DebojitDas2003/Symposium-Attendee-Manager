package com.ddas.sam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GuestViewModel(private val repository: GuestRepository) : ViewModel() {

    private val _syncStatus = MutableStateFlow("Synced")
    val syncStatus: StateFlow<String> get() = _syncStatus

    init {
        // Start real-time synchronization
        viewModelScope.launch {
            repository.startRealtimeSync()
        }
    }

    private val _attendanceFilter = MutableStateFlow<AttendanceFilter>(AttendanceFilter.All)
    val attendanceFilter: StateFlow<AttendanceFilter> get() = _attendanceFilter

    fun updateAttendanceFilter(filter: AttendanceFilter) {
        _attendanceFilter.value = filter
    }

    enum class AttendanceFilter {
        All, Present, YetToAttend
    }

    // Flow of sorted guests
    val guests: StateFlow<List<Guest>> = repository.getAllGuests()
        .map { it.sortedBy { guest -> guest.name } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun syncGuests() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing..."
                repository.syncGuests()
                _syncStatus.value = "Synced"
            } catch (e: Exception) {
                _syncStatus.value = "Sync Failed"
            }
        }
    }

    // Real-time counts
    val attendeeCount = guests.map { it.count { guest -> guest.attending } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val giftsCount = guests.map { it.count { guest -> guest.hasGift } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val foodCouponsCount = guests.map { it.count { guest -> guest.hasFoodCoupon } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _selectedCategory = MutableStateFlow<String>("All")
    val selectedCategory: StateFlow<String> get() = _selectedCategory

    val filteredGuests = combine(guests, _selectedCategory, _attendanceFilter) { allGuests, category, attendanceFilter ->
        val categoryFiltered = if (category == "All") allGuests else allGuests.filter { it.category == category }
        when (attendanceFilter) {
            AttendanceFilter.All -> categoryFiltered
            AttendanceFilter.Present -> categoryFiltered.filter { it.attending }
            AttendanceFilter.YetToAttend -> categoryFiltered.filter { !it.attending }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val yetToAttendCount = guests.map { it.count { guest -> !guest.attending } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val categoryCounts = guests.map { list ->
        list.groupingBy { it.category }.eachCount()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // Function to add a new guest
    fun addGuest(guest: Guest) {
        viewModelScope.launch {
            if (repository.getGuestByName(guest.name) == null) {
                repository.insertGuest(guest)
            } else {
                _syncStatus.value = "Guest already exists"
            }
        }
    }

    fun updateGuest(guest: Guest) {
        viewModelScope.launch {
            repository.updateGuest(guest)
        }
    }

    fun deleteGuest(guest: Guest) {
        viewModelScope.launch {
            repository.deleteGuest(guest)
        }
    }


    // Search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> get() = _searchQuery

    val searchResults = combine(filteredGuests, _searchQuery) { filteredGuests, query ->
        if (query.isBlank()) filteredGuests
        else filteredGuests.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetFirebase() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Resetting..."
                repository.clearFirebaseData()
                repository.clearLocalDatabase()
                _syncStatus.value = "Reset Successful"
            } catch (e: Exception) {
                _syncStatus.value = "Reset Failed: ${e.message}"
            }
        }
    }











}
