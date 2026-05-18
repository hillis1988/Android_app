package com.starfleet.idle.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.starfleet.idle.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages Google AdMob rewarded video ads.
 *
 * Flow:
 *  1. initialize() — sets up the SDK once, then preloads both ad types
 *  2. showAd(type, onReward, onUnavailable) — displays the ad and calls
 *     onReward() when the user finishes watching it
 *  3. If no ad is preloaded yet, attempts to load one on-demand and shows
 *     it as soon as it's ready (with a timeout)
 */
class AdManager(private val context: Context) {

    private val tag = "AdManager"

    enum class AdType { INCOME_BOOST, SPEED_BOOST, DOUBLE_BOOST }

    private val ads = mutableMapOf<AdType, RewardedAd?>()
    private val loading = mutableMapOf<AdType, Boolean>()
    private var initialized = false
    // Pending show requests - if user taps before ad is ready, we show as soon as loaded
    private val pendingShow = mutableMapOf<AdType, PendingShow?>()

    private data class PendingShow(
        val activity: Activity,
        val onReward: () -> Unit,
        val onUnavailable: () -> Unit,
        val requestedAtMs: Long
    )

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    fun initialize() {
        if (initialized) return
        initialized = true
        try {
            MobileAds.initialize(context) {
                Log.i(tag, "AdMob initialized")
                _isReady.value = true
                preloadAd(AdType.INCOME_BOOST)
                preloadAd(AdType.SPEED_BOOST)
                preloadAd(AdType.DOUBLE_BOOST)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AdMob: ${e.message}")
            initialized = false
        }
    }

    private fun adUnitId(type: AdType): String = when (type) {
        AdType.INCOME_BOOST -> context.getString(R.string.admob_rewarded_income_boost)
        AdType.SPEED_BOOST -> context.getString(R.string.admob_rewarded_speed_boost)
        AdType.DOUBLE_BOOST -> context.getString(R.string.admob_rewarded_double_boost)
    }

    private fun preloadAd(type: AdType) {
        if (loading[type] == true) {
            Log.d(tag, "Already loading $type, skipping duplicate request")
            return
        }
        if (ads[type] != null) {
            Log.d(tag, "$type already loaded, skipping")
            return
        }

        loading[type] = true
        Log.d(tag, "Loading ad: $type")

        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context, adUnitId(type), adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.i(tag, "✅ Ad loaded: $type")
                        ads[type] = ad
                        loading[type] = false

                        // If user is waiting, show it now
                        val pending = pendingShow[type]
                        if (pending != null) {
                            pendingShow[type] = null
                            // Don't show if user has been waiting too long (>15s)
                            if (System.currentTimeMillis() - pending.requestedAtMs < 15_000) {
                                showAdNow(pending.activity, type, pending.onReward, pending.onUnavailable)
                            } else {
                                Log.w(tag, "Pending request expired for $type")
                            }
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(tag, "❌ Ad failed to load ($type): code=${error.code} msg=${error.message}")
                        ads[type] = null
                        loading[type] = false

                        // If user is waiting, tell them
                        val pending = pendingShow[type]
                        if (pending != null) {
                            pendingShow[type] = null
                            pending.onUnavailable()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception loading ad ($type): ${e.message}")
            loading[type] = false
        }
    }

    /**
     * Shows a rewarded ad. Calls [onReward] only on successful completion.
     * Calls [onUnavailable] if no ad can be served (network error, no fill, etc.).
     *
     * If the ad isn't loaded yet, this will trigger a load and show it as soon
     * as it's ready (within 15 seconds), or call onUnavailable on timeout/failure.
     */
    fun showAd(
        activity: Activity,
        type: AdType,
        onReward: () -> Unit,
        onUnavailable: () -> Unit = {}
    ) {
        // If AdMob hasn't initialized yet, try now
        if (!initialized) {
            Log.w(tag, "AdMob not initialized — initializing now")
            initialize()
        }

        val ad = ads[type]
        if (ad != null) {
            showAdNow(activity, type, onReward, onUnavailable)
            return
        }

        // No ad ready — queue the request and start loading
        Log.w(tag, "Ad not ready ($type) — queuing show request and loading")
        pendingShow[type] = PendingShow(activity, onReward, onUnavailable, System.currentTimeMillis())
        preloadAd(type)
    }

    private fun showAdNow(
        activity: Activity,
        type: AdType,
        onReward: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        val ad = ads[type]
        if (ad == null) {
            Log.e(tag, "showAdNow called but no ad available for $type")
            onUnavailable()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(tag, "Ad dismissed: $type")
                ads[type] = null
                preloadAd(type)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(tag, "Ad failed to show ($type): ${error.message}")
                ads[type] = null
                preloadAd(type)
                onUnavailable()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(tag, "Ad shown: $type")
            }
        }

        try {
            ad.show(activity, OnUserEarnedRewardListener {
                Log.i(tag, "🎁 User earned reward from $type ad")
                onReward()
            })
        } catch (e: Exception) {
            Log.e(tag, "Exception showing ad ($type): ${e.message}")
            onUnavailable()
        }
    }

    fun isAdReady(type: AdType): Boolean = ads[type] != null
}
