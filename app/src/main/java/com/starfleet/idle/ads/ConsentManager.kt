package com.starfleet.idle.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Handles GDPR/UMP consent for AdMob in the EU/UK.
 *
 * Per Google policy, EU/UK users must see a consent dialog before personalised ads
 * can be shown. The UMP SDK handles this with a Google-provided dialog — no work
 * needed beyond initialising it.
 *
 * Usage:
 *  1. Call gatherConsent() in MainActivity.onCreate before AdManager.initialize()
 *  2. The callback fires when consent is gathered (or immediately if not required)
 *  3. AdManager.initialize() should be called from inside that callback
 */
class ConsentManager(private val activity: Activity) {

    private val tag = "ConsentManager"
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    /**
     * Gathers consent and calls [onReady] when it's safe to initialise the ads SDK.
     */
    fun gatherConsent(onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent info updated — show the form if needed
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) {
                        Log.w(tag, "Consent form error: ${error.message}")
                    }
                    // Whether or not consent was gathered, we can proceed.
                    // AdMob will respect the consent state automatically.
                    onReady()
                }
            },
            { error ->
                Log.w(tag, "Failed to update consent info: ${error.message}")
                // Even if consent gathering fails, allow the app to run.
                // AdMob will fall back to non-personalised ads.
                onReady()
            }
        )
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()
}
