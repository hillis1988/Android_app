package com.starfleet.idle.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.starfleet.idle.data.GEM_PACKS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages Google Play Billing for gem pack purchases.
 *
 * Flow:
 *  1. connect() — establishes connection to Play Billing
 *  2. queryProducts() — fetches product details from Play Console
 *  3. launchPurchase() — opens the purchase dialog
 *  4. onPurchasesUpdated callback handles the result
 *  5. onPurchaseVerified callback delivers gems to the game
 */
class BillingManager(
    private val context: Context,
    private val onPurchaseVerified: (productId: String) -> Unit
) : PurchasesUpdatedListener, BillingClientStateListener {

    private val tag = "BillingManager"

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun connect() {
        if (billingClient.isReady) {
            _isReady.value = true
            queryProducts()
            return
        }
        billingClient.startConnection(this)
    }

    fun disconnect() {
        billingClient.endConnection()
        _isReady.value = false
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.i(tag, "Billing connected")
            _isReady.value = true
            queryProducts()
            // Also check for any unconsumed purchases from a previous session
            queryPurchases()
        } else {
            Log.e(tag, "Billing setup failed: ${billingResult.debugMessage}")
            _isReady.value = false
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(tag, "Billing disconnected")
        _isReady.value = false
        // Auto-retry connection
        connect()
    }

    private fun queryProducts() {
        val gemProducts = GEM_PACKS.map { pack ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(pack.id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val tipProducts = com.starfleet.idle.data.DEVELOPER_TIPS.map { tip ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(tip.id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val productList = gemProducts + tipProducts

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = details.associateBy { it.productId }
                Log.i(tag, "Loaded ${details.size} product details")
            } else {
                Log.e(tag, "Failed to query products: ${result.debugMessage}")
            }
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        val details = _productDetails.value[productId]
        if (details == null) {
            Log.e(tag, "Product $productId not loaded")
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(tag, "User cancelled purchase")
            }
            else -> {
                Log.e(tag, "Purchase failed: ${result.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        // Grant gems to the player
        purchase.products.forEach { productId ->
            onPurchaseVerified(productId)
        }

        // Consume the purchase so it can be bought again
        // (Gem packs are consumable — they're used up immediately)
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(tag, "Consumed purchase: ${purchase.products}")
            } else {
                Log.e(tag, "Failed to consume: ${result.debugMessage}")
            }
        }
    }

    /**
     * Checks for any purchases made in a previous session that weren't consumed.
     * This handles the case where the user bought gems but the app crashed before granting them.
     */
    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }
}
