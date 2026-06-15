package com.netpress.nextcaltrain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TripViewModelFactory(private val schedule: Schedule) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(schedule) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
