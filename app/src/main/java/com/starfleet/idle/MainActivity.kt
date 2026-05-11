package com.starfleet.idle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.starfleet.idle.ui.GameScreen
import com.starfleet.idle.ui.GameViewModel
import com.starfleet.idle.ui.theme.StarFleetIdleTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[GameViewModel::class.java]

        setContent {
            StarFleetIdleTheme {
                GameScreen(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveGame()
    }
}
