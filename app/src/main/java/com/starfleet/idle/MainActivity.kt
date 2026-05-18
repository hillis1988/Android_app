package com.starfleet.idle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.starfleet.idle.ads.AdManager
import com.starfleet.idle.billing.BillingManager
import com.starfleet.idle.leaderboard.LeaderboardManager
import com.starfleet.idle.ui.GameScreen
import com.starfleet.idle.ui.GameViewModel
import com.starfleet.idle.ui.theme.StarFleetIdleTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel
    private lateinit var billingManager: BillingManager
    private lateinit var leaderboardManager: LeaderboardManager
    private lateinit var adManager: AdManager

    private val leaderboardLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result is ignored — Play Games handles its own UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[GameViewModel::class.java]

        // Billing setup
        billingManager = BillingManager(
            context = this,
            onPurchaseVerified = { productId ->
                viewModel.grantGemsFromPurchase(productId)
            }
        )
        billingManager.connect()

        viewModel.setPurchaseLauncher { productId ->
            billingManager.launchPurchase(this, productId)
        }

        viewModel.setPriceLookup { productId ->
            val details = billingManager.productDetails.value[productId]
            details?.oneTimePurchaseOfferDetails?.formattedPrice
        }

        // Leaderboards setup
        leaderboardManager = LeaderboardManager(this)
        leaderboardManager.initialize()

        viewModel.setLeaderboardManager(leaderboardManager)
        viewModel.setLeaderboardLauncher { intent ->
            leaderboardLauncher.launch(intent)
        }

        // AdMob setup — gather consent first, then initialize
        adManager = AdManager(this)
        val consentManager = com.starfleet.idle.ads.ConsentManager(this)
        consentManager.gatherConsent {
            adManager.initialize()
        }

        viewModel.setAdShower { adType, onReward, onUnavailable ->
            adManager.showAd(this, adType, onReward, onUnavailable)
        }

        setContent {
            StarFleetIdleTheme {
                GameScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::leaderboardManager.isInitialized) {
            leaderboardManager.checkSignInOnResume()
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
