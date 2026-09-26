package com.aistudio.ecotobacco.kfzqw.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aistudio.ecotobacco.kfzqw.data.repository.TobaccoRepository

class TobaccoViewModelFactory(
    private val repository: TobaccoRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TobaccoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TobaccoViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
