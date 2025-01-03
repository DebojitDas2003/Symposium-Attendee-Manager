package com.ddas.sam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GuestViewModelFactory(private val repository: GuestRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GuestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GuestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}