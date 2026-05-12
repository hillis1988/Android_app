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

    init {
        // loadGame()
        // startGameLoop()
    }

    private fun loadGame() {
        // Disabled for debugging
    }

    private fun startGameLoop() {
        // Disabled for debugging
    }

    fun handleEncounter(optionIndex: Int) {}
    fun dismissEncounter() {}
    fun buyShip(tierId: String) {}
    fun buyUpgrade(tierId: String, upgradeId: String) {}
    fun buyShopBonus(bonusId: String) {}
    fun buyResearch(nodeId: String) {}
    fun buyPerk(perkId: String) {}
    fun setBuyAmount(amount: BuyAmount) {}
    fun switchSector(sectorId: String) {}
    fun tap() {}
    fun prestige() {}
    fun activateAdBoost() {}
    fun activateSpeedBoost() {}
    fun doubleOfflineEarnings(earnings: Double) {}
    fun claimDailyReward() {}
    fun buyGemItem(itemId: String) {}
    fun purchaseGemPack(packId: String) {}
    fun dismissOfflineEarnings() {}
    fun dismissNewAchievements() {}
    fun hardReset() {}
    fun saveGame() {}
}
