package com.starfleet.idle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.starfleet.idle.billing.BillingManager
import com.starfleet.idle.ui.GameScreen
import com.starfleet.idle.ui.GameViewModel
import com.starfleet.idle.ui.theme.StarFleetIdleTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[GameViewModel::class.java]

        // Wire billing: when a purchase is verified, grant gems via ViewModel
        billingManager = BillingManager(
            context = this,
            onPurchaseVerified = { productId ->
                viewModel.grantGemsFromPurchase(productId)
            }
        )
        billingManager.connect()

        // Inject billing launch into the ViewModel
        viewModel.setPurchaseLauncher { productId ->
            billingManager.launchPurchase(this, productId)
        }

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

    override fun onDestroy() {
        super.onDestroy()
        billingManager.disconnect()
    }
}
