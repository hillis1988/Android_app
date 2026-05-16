package com.starfleet.idle.leaderboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.starfleet.idle.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages Google Play Games Services v2 integration for leaderboards.
 *
 * Follows the official migration guide:
 * https://developer.android.com/games/pgs/android/migrate-to-v2
 *
 * Sign-in flow:
 *  1. PlayGamesSdk.initialize() in onCreate
 *  2. signInSilently() checks if already authenticated
 *  3. If not, calls gamesSignInClient.signIn() once
 *  4. On resume, re-checks auth state
 */
class LeaderboardManager(private val activity: Activity) {

    private val tag = "LeaderboardManager"

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    private val _playerName = MutableStateFlow<String?>(null)
    val playerName: StateFlow<String?> = _playerName

    fun initialize() {
        PlayGamesSdk.initialize(activity)
        signInSilently()
    }

    /** Re-check auth state when the activity resumes (per v2 docs). */
    fun checkSignInOnResume() {
        signInSilently()
    }

    private fun signInSilently() {
        val signInClient = PlayGames.getGamesSignInClient(activity)
        signInClient.isAuthenticated.addOnCompleteListener { task ->
            val isAuthenticated = task.isSuccessful && task.result?.isAuthenticated == true
            _isSignedIn.value = isAuthenticated
            if (isAuthenticated) {
                fetchPlayerName()
                Log.i(tag, "Already authenticated with Play Games")
            } else {
                Log.i(tag, "Not authenticated yet — user must tap to sign in")
            }
        }
    }

    /** Manual sign-in. Called when the user explicitly taps a sign-in button. */
    fun signIn(onResult: (Boolean) -> Unit = {}) {
        val signInClient = PlayGames.getGamesSignInClient(activity)
        signInClient.signIn().addOnCompleteListener { task ->
            val success = task.isSuccessful && task.result?.isAuthenticated == true
            _isSignedIn.value = success
            if (success) {
                fetchPlayerName()
                Log.i(tag, "Sign-in successful")
            } else {
                Log.w(tag, "Sign-in failed: ${task.exception?.message}")
            }
            onResult(success)
        }
    }

    private fun fetchPlayerName() {
        PlayGames.getPlayersClient(activity).currentPlayer
            .addOnSuccessListener { player ->
                _playerName.value = player.displayName
                Log.i(tag, "Signed in as ${player.displayName}")
            }
            .addOnFailureListener { e ->
                Log.w(tag, "Could not fetch player name: ${e.message}")
            }
    }

    // --- Score submission ---

    fun submitFleetPower(value: Long) = submitScore(R.string.leaderboard_fleet_power, value)
    fun submitTotalCredits(value: Long) = submitScore(R.string.leaderboard_total_credits, value)
    fun submitPrestigeResets(value: Long) = submitScore(R.string.leaderboard_prestige_resets, value)
    fun submitStarCoins(value: Long) = submitScore(R.string.leaderboard_star_coins, value)
    fun submitDysonSpheres(value: Long) = submitScore(R.string.leaderboard_dyson_spheres, value)

    private fun submitScore(leaderboardIdRes: Int, value: Long) {
        if (!_isSignedIn.value) return
        val leaderboardId = activity.getString(leaderboardIdRes)
        if (leaderboardId.startsWith("REPLACE_WITH")) return // not configured yet
        if (value <= 0) return // skip empty submissions

        PlayGames.getLeaderboardsClient(activity)
            .submitScore(leaderboardId, value)
        Log.d(tag, "Submitted score $value to $leaderboardId")
    }

    // --- UI launch ---

    /** Opens the native Play Games leaderboards browser. */
    fun showAllLeaderboards(onLaunched: (Intent) -> Unit) {
        if (!_isSignedIn.value) {
            // Try sign-in, then show leaderboards if successful
            signIn { success ->
                if (success) showAllLeaderboards(onLaunched)
            }
            return
        }
        PlayGames.getLeaderboardsClient(activity).allLeaderboardsIntent
            .addOnSuccessListener { intent -> onLaunched(intent) }
            .addOnFailureListener { e -> Log.e(tag, "Failed to open leaderboards: ${e.message}") }
    }

    /** Opens a specific leaderboard. */
    fun showLeaderboard(leaderboardIdRes: Int, onLaunched: (Intent) -> Unit) {
        if (!_isSignedIn.value) {
            signIn { success ->
                if (success) showLeaderboard(leaderboardIdRes, onLaunched)
            }
            return
        }
        val leaderboardId = activity.getString(leaderboardIdRes)
        if (leaderboardId.startsWith("REPLACE_WITH")) return

        PlayGames.getLeaderboardsClient(activity).getLeaderboardIntent(leaderboardId)
            .addOnSuccessListener { intent -> onLaunched(intent) }
            .addOnFailureListener { e -> Log.e(tag, "Failed to open leaderboard: ${e.message}") }
    }
}
