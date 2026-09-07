package com.example

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D1B2A)
                ) {
                    GameApp(viewModel = viewModel)
                }
            }
        }
    }

    private fun hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }
}

@Composable
fun GameApp(viewModel: GameViewModel) {
    val screen by viewModel.currentScreen.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1B2A))) {
        when (screen) {
            AppScreen.MAIN_MENU -> {
                MainMenuScreen(
                    viewModel = viewModel,
                    onStartDrive = { viewModel.navigateTo(AppScreen.DRIVING) },
                    onOpenGarage = { viewModel.navigateTo(AppScreen.GARAGE) },
                    onOpenCharacters = { viewModel.navigateTo(AppScreen.CHARACTER_SELECT) },
                    onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                    onOpenMusic = { viewModel.navigateTo(AppScreen.MY_MUSIC) }
                )
            }
            AppScreen.DRIVING -> {
                DrivingScreen(
                    viewModel = viewModel,
                    onOpenMenu = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
            }
            AppScreen.GARAGE -> {
                GarageScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
            }
            AppScreen.CHARACTER_SELECT -> {
                CharacterSelectScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
            }
            AppScreen.MY_MUSIC -> {
                MyMusicScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                )
            }
        }
    }
}
