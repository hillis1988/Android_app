package com.starfleet.idle.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starfleet.idle.data.BuyAmount
import com.starfleet.idle.data.EncounterData
import com.starfleet.idle.data.GameRepository
import com.starfleet.idle.data.GameState
import com.starfleet.idle.engine.GameEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state

    private val _offlineEarnings = MutableStateFlow<Double?>(null)
    val offlineEarnings: StateFlow<Double?> = _offlineEarnings

    private val _newAchievements = MutableStateFlow<List<String>>(emptyList())
    val newAchievements: StateFlow<List<String>> = _newAchievements

    private val _randomEncounter = MutableStateFlow<EncounterData?>(null)
    val randomEncounter: StateFlow<EncounterData?> = _randomEncounter

    private val _showDailyReward = MutableStateFlow(false)
    val showDailyReward: StateFlow<Boolean> = _showDailyReward

    // Callback injected from MainActivity to launch Google Play Billing
    private var purchaseLauncher: ((String) -> Unit)? = null

    fun setPurchaseLauncher(launcher: (String) -> Unit) {
        purchaseLauncher = launcher
    }

    /** Called by BillingManager when a purchase is verified by Google Play. */
    fun grantGemsFromPurchase(productId: String) {
        _state.value = GameEngine.grantGemPack(_state.value, productId)
        repository.save(_state.value)
    }

    // Leaderboard integration
    private var leaderboardManager: com.starfleet.idle.leaderboard.LeaderboardManager? = null
    private var leaderboardLauncher: ((android.content.Intent) -> Unit)? = null

    fun setLeaderboardManager(manager: com.starfleet.idle.leaderboard.LeaderboardManager) {
        leaderboardManager = manager
    }

    fun setLeaderboardLauncher(launcher: (android.content.Intent) -> Unit) {
        leaderboardLauncher = launcher
    }

    fun showLeaderboards() {
        leaderboardManager?.showAllLeaderboards { intent ->
            leaderboardLauncher?.invoke(intent)
        }
    }

    private fun submitLeaderboardScores() {
        val mgr = leaderboardManager ?: return
        val s = _state.value
        mgr.submitFleetPower(s.totalFleetPower.toLong())
        mgr.submitTotalCredits(s.totalCreditsEarned.toLong())
        mgr.submitPrestigeResets(s.totalPrestigeResets.toLong())
        mgr.submitStarCoins(s.starCoins.toLong())
        val dysons = s.sectors["void"]?.ships?.get("dyson")?.count?.toLong() ?: 0L
        mgr.submitDysonSpheres(dysons)
    }

    init {
        loadGame()
        startGameLoop()
    }

    private fun loadGame() {
        val saved = repository.load()
        if (saved != null) {
            var newState = GameEngine.checkDailyLogin(saved)
            val (afterOffline, earned) = GameEngine.calculateOfflineEarnings(newState)
            newState = afterOffline
            newState = GameEngine.checkAchievements(newState)
            _state.value = newState

            if (earned > 1.0) {
                _offlineEarnings.value = earned
            }
            if (newState.dailyRewardsClaimed < newState.dailyLoginStreak) {
                _showDailyReward.value = true
            }
        } else {
            _state.value = GameEngine.checkDailyLogin(_state.value)
            if (_state.value.dailyRewardsClaimed < _state.value.dailyLoginStreak) {
                _showDailyReward.value = true
            }
        }
    }

    private fun startGameLoop() {
        // Main game tick every 100ms
        viewModelScope.launch {
            while (true) {
                delay(100)
                _state.value = GameEngine.tick(_state.value)
            }
        }

        // Achievement check every 5 seconds
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                val before = _state.value.unlockedAchievements
                _state.value = GameEngine.checkAchievements(_state.value)
                val after = _state.value.unlockedAchievements
                val newOnes = after - before
                if (newOnes.isNotEmpty()) {
                    _newAchievements.value = newOnes.toList()
                }
            }
        }

        // Auto-save every 30 seconds
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                repository.save(_state.value)
            }
        }

        // Submit leaderboard scores every 60 seconds
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                submitLeaderboardScores()
            }
        }
    }

    fun handleEncounter(optionIndex: Int) {
        val encounter = _randomEncounter.value ?: return
        _state.value = GameEngine.handleEncounter(_state.value, encounter, optionIndex)
        _randomEncounter.value = null
    }

    fun dismissEncounter() {
        _randomEncounter.value = null
    }

    fun buyShip(tierId: String) {
        _state.value = GameEngine.buyShips(_state.value, tierId, _state.value.buyAmount)
    }

    fun buyUpgrade(tierId: String, upgradeId: String) {
        _state.value = GameEngine.buyUpgrade(_state.value, tierId, upgradeId)
    }

    fun buyShopBonus(bonusId: String) {
        _state.value = GameEngine.buyShopBonus(_state.value, bonusId)
    }

    fun buyResearch(nodeId: String) {
        _state.value = GameEngine.buyResearch(_state.value, nodeId)
    }

    fun buyPerk(perkId: String) {
        _state.value = GameEngine.buyPerk(_state.value, perkId)
    }

    fun setBuyAmount(amount: BuyAmount) {
        _state.value = GameEngine.setBuyAmount(_state.value, amount)
    }

    fun switchSector(sectorId: String) {
        _state.value = GameEngine.switchSector(_state.value, sectorId)
    }

    fun tap() {
        _state.value = GameEngine.tap(_state.value)
    }

    fun prestige() {
        _state.value = GameEngine.prestige(_state.value)
        repository.save(_state.value)
        submitLeaderboardScores()
    }

    fun activateAdBoost() {
        _state.value = GameEngine.activateAdBoost(_state.value)
    }

    fun activateSpeedBoost() {
        _state.value = GameEngine.activateSpeedBoost(_state.value)
    }

    fun doubleOfflineEarnings(earnings: Double) {
        _state.value = GameEngine.doubleOfflineEarnings(_state.value, earnings)
        _offlineEarnings.value = null
    }

    fun claimDailyReward() {
        _state.value = GameEngine.claimDailyReward(_state.value)
        _showDailyReward.value = false
    }

    fun buyGemItem(itemId: String) {
        _state.value = GameEngine.buyGemItem(_state.value, itemId)
    }

    fun purchaseGemPack(packId: String) {
        // In dev mode (DEV_MODE_FREE_GEMS = true), grant gems instantly for free
        // In production, launch Google Play Billing dialog
        if (com.starfleet.idle.data.DEV_MODE_FREE_GEMS) {
            _state.value = GameEngine.grantGemPack(_state.value, packId)
            repository.save(_state.value)
        } else {
            purchaseLauncher?.invoke(packId)
        }
    }

    fun dismissOfflineEarnings() {
        _offlineEarnings.value = null
    }

    fun dismissNewAchievements() {
        _newAchievements.value = emptyList()
    }

    fun hardReset() {
        repository.clear()
        _state.value = GameState()
    }

    fun saveGame() {
        repository.save(_state.value)
    }
}
