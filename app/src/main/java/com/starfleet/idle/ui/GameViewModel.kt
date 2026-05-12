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
        _state.value = GameEngine.purchaseGemPack(_state.value, packId)
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
