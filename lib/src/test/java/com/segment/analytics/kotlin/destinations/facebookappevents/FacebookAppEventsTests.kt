package com.segment.analytics.kotlin.destinations.facebookappevents

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.AppEventsLogger
import com.segment.analytics.kotlin.core.platform.Plugin
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.FacebookSdk
import com.segment.analytics.kotlin.core.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.util.*


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FacebookAppEventsTests {

    private val  mockEventLogger = mockk<AppEventsLogger>(relaxed = true)
    private lateinit var facebookAppEventsDestination: FacebookAppEvents

    @MockK(relaxUnitFun = true)
    lateinit var mockedAnalytics: Analytics

    val mockContext = spyk(InstrumentationRegistry.getInstrumentation().targetContext)

    // An example settings blob
    private val settingsBlob: Settings = Json.decodeFromString(
        """
            {
              "integrations": {
                "Facebook App Events": {
                    "appEvents":{},
                    "appId":"123abc",
                    "limitedDataUse":false,
                    "trackScreenEvents":true,
                    "zeroedAttribution":false
                }    
              }
            }
        """.trimIndent()
    )

    init {
        MockKAnnotations.init(this)
        facebookAppEventsDestination = FacebookAppEvents(mockContext)
        facebookAppEventsDestination.analytics = mockedAnalytics
        mockkStatic(FacebookSdk::class)
        mockkObject(AppEventsLogger.Companion)
        every { AppEventsLogger.Companion.newLogger(any()) } returns mockEventLogger
    }

    @Test
    fun `settings are updated correctly`() {
        facebookAppEventsDestination.update(settingsBlob, Plugin.UpdateType.Initial)

        /* assertions about config */
        Assertions.assertNotNull(facebookAppEventsDestination.settings)
        with(facebookAppEventsDestination.settings!!) {
            Assertions.assertEquals("123abc", appId)
            Assertions.assertFalse(limitedDataUse)
            Assertions.assertTrue(trackScreenEvents)
            Assertions.assertFalse(zeroedAttribution)
        }
    }

    @Test
    fun `it tracks a standard event`() {
        val sampleEvent = TrackEvent(
            event = "Food Choice",
            properties = buildJsonObject { put("Item Name", "Biscuits") }
        ).apply {
            messageId = "qwerty-1234"
            anonymousId = "anonId"
            integrations = emptyJsonObject
            context = emptyJsonObject
            timestamp = "2021-07-13T00:59:09"
        }

        facebookAppEventsDestination.update(settingsBlob, Plugin.UpdateType.Initial)

        val params = Bundle()
        params.putString("Item Name", "Biscuits")
        val trackEvent = facebookAppEventsDestination.track(sampleEvent)

        Assertions.assertNotNull(trackEvent)
        verify {
            mockEventLogger.logEvent("Food Choice", matchBundle(params))
        }
    }

    @Test
    fun `it tracks and E-Commerce event`(){
        val sampleEvent = TrackEvent(
            event = "Product Viewed",
            properties = buildJsonObject {
                put("Item Name", "Biscuits")
                put("Revenue", 30.76)
            }
        ).apply {
            messageId = "qwerty-1234"
            anonymousId = "anonId"
            integrations = emptyJsonObject
            context = emptyJsonObject
            timestamp = "2021-07-13T00:59:09"
        }

        facebookAppEventsDestination.update(settingsBlob, Plugin.UpdateType.Initial)

        val params = Bundle()
        params.putString("Item Name", "Biscuits")
        var fbCurrency = Currency.getInstance("USD")
        val purchaseAmount = BigDecimal("30.76")
        val revenue = 30.76 as Double
        val trackEvent = facebookAppEventsDestination.track(sampleEvent)

        Assertions.assertNotNull(trackEvent)
        verify {
            mockEventLogger.logEvent("fb_mobile_search", revenue, matchBundle(params))
            mockEventLogger.logPurchase(purchaseAmount, fbCurrency, matchBundle(params))
        }
    }

    @Test
    fun `it tracks a screen event`(){
        val sampleEvent = ScreenEvent(
            name = "Login",
            properties = buildJsonObject {
                put("startup", false)
                put("parent", "MainActivity")
            },
            category = "signup_flow"
        ).apply {
            messageId = "qwerty-1234"
            anonymousId = "anonId"
            integrations = emptyJsonObject
            context = emptyJsonObject
            timestamp = "2021-07-13T00:59:09"
        }

        facebookAppEventsDestination.update(settingsBlob, Plugin.UpdateType.Initial)

        val screenEvent = facebookAppEventsDestination.screen(sampleEvent)

        Assertions.assertNotNull(screenEvent)
        verify {
            mockEventLogger.logEvent("Viewed Login Screen")
        }

    }

    data class BundleMatcher(
        val expectedBundle: Bundle
    ) : Matcher<Bundle> {

        override fun match(arg: Bundle?): Boolean {
            if (arg == null) return false
            return equalBundles(arg, expectedBundle)
        }

        fun equalBundles(one: Bundle, two: Bundle): Boolean {
            if (one.size() != two.size()) return false

            if (!one.keySet().containsAll(two.keySet())) return false

            for (key in one.keySet()) {
                val valueOne = one.get(key)
                val valueTwo = two.get(key)
                if (valueOne is Bundle && valueTwo is Bundle) {
                    if (!equalBundles(valueOne , valueTwo)) return false
                } else if (valueOne != valueTwo) return false
            }

            return true
        }

        override fun toString() = "matchBundle($expectedBundle)"
    }

    private fun MockKMatcherScope.matchBundle(expectedJSON: Bundle): Bundle =
        match(BundleMatcher(expectedJSON))
}
