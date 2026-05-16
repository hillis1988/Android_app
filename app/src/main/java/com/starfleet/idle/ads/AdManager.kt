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
 *  1. initialize() — sets up the SDK once
 *  2. preloadAd(type) — fetches an ad in the background
 *  3. showAd(type, onReward) — displays the ad and calls onReward() when the
 *     user finishes watching it. The reward callback only fires on completion.
 *
 * In production, swap the test IDs in res/values/admob_ids.xml for real ones.
 */
class AdManager(private val context: Context) {

    private val tag = "AdManager"

    enum class AdType { INCOME_BOOST, SPEED_BOOST }

    private val ads = mutableMapOf<AdType, RewardedAd?>()
    private val loading = mutableMapOf<AdType, Boolean>()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    fun initialize() {
        MobileAds.initialize(context) {
            Log.i(tag, "AdMob initialized")
            _isReady.value = true
            // Preload both ads so they're ready when the user taps
            preloadAd(AdType.INCOME_BOOST)
            preloadAd(AdType.SPEED_BOOST)
        }
    }

    private fun adUnitId(type: AdType): String = when (type) {
        AdType.INCOME_BOOST -> context.getString(R.string.admob_rewarded_income_boost)
        AdType.SPEED_BOOST -> context.getString(R.string.admob_rewarded_speed_boost)
    }

    private fun preloadAd(type: AdType) {
        if (loading[type] == true) return
        if (ads[type] != null) return

        loading[type] = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context, adUnitId(type), adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.i(tag, "Ad loaded: $type")
                    ads[type] = ad
                    loading[type] = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(tag, "Ad failed to load ($type): ${error.message}")
                    ads[type] = null
                    loading[type] = false
                }
            }
        )
    }

    /**
     * Shows the ad. The [onReward] callback fires only if the user watches the
     * full ad — not on close or skip.
     */
    fun showAd(activity: Activity, type: AdType, onReward: () -> Unit) {
        val ad = ads[type]
        if (ad == null) {
            Log.w(tag, "Ad not ready ($type), preloading and retrying")
            preloadAd(type)
            // Fail silently — UI should show a "not ready" state if needed
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(tag, "Ad dismissed")
                ads[type] = null
                preloadAd(type) // Preload the next ad
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(tag, "Ad failed to show ($type): ${error.message}")
                ads[type] = null
                preloadAd(type)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(tag, "Ad shown")
            }
        }

        ad.show(activity, OnUserEarnedRewardListener {
            Log.i(tag, "User earned reward from $type ad")
            onReward()
        })
    }

    fun isAdReady(type: AdType): Boolean = ads[type] != null
}
