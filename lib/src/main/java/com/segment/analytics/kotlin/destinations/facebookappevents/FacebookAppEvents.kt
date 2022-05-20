package com.segment.analytics.kotlin.destinations.facebookappevents

import android.content.Context
import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.segment.analytics.kotlin.android.plugins.AndroidLifecycle
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.DestinationPlugin
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import com.segment.analytics.kotlin.core.utilities.mapTransform
import com.segment.analytics.kotlin.core.utilities.toContent
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
data class FacebookSettings(
    var appId: String,
    var limitedDataUse: Boolean = false,
    var trackScreenEvents: Boolean = false,
    var zeroedAttribution: Boolean = false
)

class FacebookAppEvents(
    private val context: Context
) : DestinationPlugin(), AndroidLifecycle {

    internal var settings: FacebookSettings? = null
    var fbLogger: AppEventsLogger? = null

    override val key: String = "Facebook App Events"

    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)
        if (settings.hasIntegrationSettings(this)) {

            this.fbLogger = AppEventsLogger.newLogger(context)

            analytics.log("Facebook App Events Plugin is enabled")
            this.settings = settings.destinationSettings(key)
            if (type == Plugin.UpdateType.Initial) {
                var limitedDataUse = this.settings?.limitedDataUse
                if (limitedDataUse == true) {
                    FacebookSdk.setDataProcessingOptions(arrayOf<String>("LDU"), 0, 0)
                } else {
                    FacebookSdk.setDataProcessingOptions(null)
                }
            }
        }
    }

    override fun track(payload: TrackEvent): BaseEvent? {
        val name = EVENT_MAPPER[payload.event] ?: payload.event
        val properties = payload.properties.mapTransform(PROPERTY_MAPPER)
        val params = Bundle()
        // FB Event Names must be <= 40 characters
        val truncatedEventName = name.take(40)
        val mappedProperties = properties.toContent()
        var count = 0

        for ((key, value) in mappedProperties) {
            // Facebook has a limit of 25 properties
            if (count < 25) {
                if (value is String) {
                    params.putString(key, value).also { count++ }
                } else if (value is Int) {
                    params.putInt(key, value).also { count++ }
                }
            }
        }

        if (mappedProperties.containsKey("_valueToSum")) {
            var currencyProp =  mappedProperties["currency"] ?: "USD"
            var fbCurrency = Currency.getInstance(currencyProp as String)
            var revenue = mappedProperties["_valueToSum"] as Double
            var purchaseAmount = revenue.toBigDecimal()

                fbLogger?.logEvent(truncatedEventName, revenue, params)
                fbLogger?.logPurchase(purchaseAmount, fbCurrency, params)
        } else {
            fbLogger?.logEvent(truncatedEventName, params)
        }
        return payload
    }

    override fun screen(payload: ScreenEvent): BaseEvent? {
        if (this.settings?.trackScreenEvents == true) {
            // FB Event Names must be <= 40 characters
            // 'Viewed' and 'Screen' with spaces take up 14
            val truncatedEventName = payload.name.take(26)
            val newEventName = "Viewed $truncatedEventName Screen"

            fbLogger?.logEvent(newEventName)
        }
        return payload
   }

    companion object {
        //Rules for transforming a track event name
        private val EVENT_MAPPER = mapOf(
            "Application Installed" to "MOBILE_APP_INSTALL",
            "Application Opened" to "fb_mobile_activate_app",
            "Products Searched" to "fb_mobile_search",
            "Product Viewed" to "fb_mobile_search",
            "Product Added" to "fb_mobile_add_to_cart",
            "Product Added to Wishlist" to "fb_mobile_add_to_wishlist",
            "Payment Info Entered" to "fb_mobile_add_payment_info",
            "Checkout Started" to "fb_mobile_initiated_checkout",
            "Order Completed" to "fb_mobile_purchase"
        )

        private val PROPERTY_MAPPER = mapOf(
            "Revenue"  to "_valueToSum",
            "Currency" to "fb_currency",
            "name" to "fb_description",
            "product_id" to "fb_content_id",
            "category" to "fb_content_type",
            "query" to "fb_search_string",
            "timestamp" to "_logTime",
            "quantity" to "fb_num_items"
        )
    }
}