package com.example.roomiesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomiesync.auth.AuthScreen
import com.example.roomiesync.auth.AuthUiState
import com.example.roomiesync.auth.AuthViewModel
import com.example.roomiesync.home.MainScreen
import com.example.roomiesync.ui.theme.RoomieSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoomieSyncTheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (authState) {
                        is AuthUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthUiState.ShowAuthScreen -> {
                            AuthScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = authViewModel
                            )
                        }
                        is AuthUiState.Authenticated -> {
                            MainScreen(
                                modifier = Modifier.padding(innerPadding),
                                user = (authState as AuthUiState.Authenticated).user,
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                    }
                }
            }
        }
    }
}
