package com.segment.analytics.kotlin.destinations.facebookappevents

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.AppEventsLogger
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.Settings
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.emptyJsonObject
import com.segment.analytics.kotlin.core.platform.Plugin
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.FacebookSdk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FacebookAppEventsTests {

    private val  mockEventLogger = mockk<AppEventsLogger>(relaxed = true)
    private lateinit var facebookAppEventsDestination: FacebookAppEvents
    lateinit var mockedAnalytics: Analytics

    val mockContext = spyk(InstrumentationRegistry.getInstrumentation().targetContext)

    @Before
    internal fun setUp() {
        facebookAppEventsDestination = FacebookAppEvents(mockContext)
        mockkStatic(FacebookSdk::class)
        mockkObject(AppEventsLogger.Companion)
        every { AppEventsLogger.Companion.newLogger(any()) } returns mockEventLogger
//        facebookAppEventsDestination.analytics = mockedAnalytics
    }

    @Test
    fun `settings are updated correctly`() {
        // An example settings blob
        val settingsBlob: Settings = Json.decodeFromString(
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

        val params = Bundle()
        params.putString("Item Name", "Biscuits")
        val trackEvent = facebookAppEventsDestination.track(sampleEvent)

        Assertions.assertNotNull(trackEvent)
        verify {
            mockEventLogger.logEvent("Food Choice", params)
        }
    }

    data class JsonObjectMatcher(
        val expectedJSON: JSONObject
    ) : Matcher<JSONObject> {

        override fun match(arg: JSONObject?): Boolean {
            if (arg == null) return false
            return try {
                JSONAssert.assertEquals(expectedJSON, arg, JSONCompareMode.STRICT)
                true
            } catch (e: JSONException) {
                false
            }
        }

        override fun toString() = "matchJSONObject($expectedJSON)"
    }

    private fun MockKMatcherScope.matchJsonObject(expectedJSON: JSONObject): JSONObject =
        match(JsonObjectMatcher(expectedJSON))
}