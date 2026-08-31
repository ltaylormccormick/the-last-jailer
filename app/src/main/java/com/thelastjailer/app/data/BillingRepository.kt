package com.thelastjailer.app.data

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/** The one-time "unlock full story" in-app product id. Must match the product configured in Play Console. */
const val UNLOCK_FULL_STORY_PRODUCT_ID = "unlock_full_story"

/**
 * Owns the connection to Google Play Billing and drives [EntitlementRepository.unlockFullStory]
 * off real, verified purchases. Kept separate from [EntitlementRepository] itself so that interface
 * stays a simple, billing-agnostic "what can the player see" gate, matching its own documented
 * intent - this is the only class in the app allowed to know Play Billing exists.
 *
 * [purchaseCompletedTick] is a Compose-observable counter that increments whenever a verified
 * purchase - fresh or restored on launch - unlocks the full story, so a screen holding its own
 * `remember`-cached copy of entitlement state knows to re-read it.
 */
class BillingRepository(
    context: Context,
    private val entitlements: EntitlementRepository
) : PurchasesUpdatedListener {

    var purchaseCompletedTick by mutableStateOf(0)
        private set

    private var productDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Connects to Play Billing, then restores any already-owned purchase and loads product details. */
    fun start() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    restorePastPurchases()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Auto service reconnection is enabled above; nothing to do here.
            }
        })
    }

    /** Releases the billing connection. Call when the owning screen/activity is torn down. */
    fun endConnection() {
        billingClient.endConnection()
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(UNLOCK_FULL_STORY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = result.productDetailsList.firstOrNull()
            }
        }
    }

    /** Queries purchases already owned by this Play account (reinstall, new device) and unlocks silently if found. */
    private fun restorePastPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }

    /** Launches Play's purchase UI for the unlock-full-story product. A no-op if product details haven't loaded yet. */
    fun launchPurchaseFlow(activity: Activity) {
        val details = productDetails ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        purchases
            .filter { UNLOCK_FULL_STORY_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                entitlements.unlockFullStory()
                purchaseCompletedTick++
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { /* nothing further to do on acknowledgement */ }
                }
            }
    }
}
