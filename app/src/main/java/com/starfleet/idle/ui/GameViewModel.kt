package com.starfleet.idle.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.starfleet.idle.data.BuyAmount
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

    init {
        loadGame()
        startGameLoop()
    }

    private fun loadGame() {
        val saved = repository.load()
        if (saved != null) {
            val (newState, earned) = GameEngine.calculateOfflineEarnings(saved)
            _state.value = newState
            if (earned > 1.0) {
                _offlineEarnings.value = earned
            }
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                delay(100)
                _state.value = GameEngine.tick(_state.value)
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(30_000)
                repository.save(_state.value)
            }
        }
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

    fun setBuyAmount(amount: BuyAmount) {
        _state.value = GameEngine.setBuyAmount(_state.value, amount)
    }

    fun tap() {
        _state.value = GameEngine.tap(_state.value)
    }

    fun prestige() {
        _state.value = GameEngine.prestige(_state.value)
        repository.save(_state.value)
    }

    fun hardReset() {
        repository.clear()
        _state.value = GameState()
    }

    fun dismissOfflineEarnings() {
        _offlineEarnings.value = null
    }

    fun saveGame() {
        repository.save(_state.value)
    }
}
