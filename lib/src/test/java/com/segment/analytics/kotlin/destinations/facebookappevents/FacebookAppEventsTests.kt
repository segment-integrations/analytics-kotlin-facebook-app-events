package com.segment.analytics.kotlin.destinations.facebookappevents

import android.content.Context
import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.Plugin
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONException
import org.json.JSONObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner


class FacebookAppEventsTests {

    private val mockContext = mockk<Context>(relaxed = true)
    private val  mockEventLogger = mockk<AppEventsLogger>(relaxed = true)
    private lateinit var facebookAppEventsDestination: FacebookAppEvents


    @MockK(relaxUnitFun = true)
    lateinit var mockedAnalytics: Analytics

    init {
        MockKAnnotations.init(this)
        mockkStatic(FacebookSdk::class)
        mockkObject(AppEventsLogger.Companion)
        every { AppEventsLogger.Companion.newLogger(mockContext) } returns mockEventLogger
    }

    @BeforeEach
    internal fun setUp() {
        facebookAppEventsDestination = FacebookAppEvents(mockContext)
        facebookAppEventsDestination.analytics = mockedAnalytics
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