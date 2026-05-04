package com.jb.shoplist

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.jb.shoplist.ui.navigation.ShopListNavHost
import com.jb.shoplist.ui.theme.ShopListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShopListTheme {
                TodoApp()
            }
        }
    }
}

@Composable
fun TodoApp() {
    val viewModel: TodoViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionAnnulable by viewModel.actionAnnulable.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.evenements.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(actionAnnulable) {
        actionAnnulable?.let { action ->
            val result = snackbarHostState.showSnackbar(
                message = action.message,
                actionLabel = "Annuler",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.annulerDerniereAction()
                SnackbarResult.Dismissed -> viewModel.consommerAction()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        // On pourrait passer innerPadding à ShopListNavHost si besoin de gérer les insets
        ShopListNavHost(
            navController = navController,
            uiState = uiState,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState
        )
    }
}
