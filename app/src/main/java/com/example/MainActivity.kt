package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.auth.GoogleAuthManager
import com.example.data.local.AppDatabase
import com.example.data.repository.UangKuRepository
import com.example.ui.UangKuApp
import com.example.ui.viewmodel.UangKuViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: UangKuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = UangKuRepository(database.appDao())
        val googleAuthManager = GoogleAuthManager(applicationContext)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UangKuViewModel(repository, googleAuthManager) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[UangKuViewModel::class.java]

        setContent {
            UangKuApp(viewModel = viewModel)
        }
    }
}
