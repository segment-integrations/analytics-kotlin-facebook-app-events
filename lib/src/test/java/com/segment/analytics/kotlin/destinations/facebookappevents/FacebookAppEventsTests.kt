package com.segment.analytics.kotlin.destinations.facebookappevents


import android.content.Context
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.destinations.facebookappevents.FacebookAppEvents
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode


class FacebookAppEventsTests {

    private val mockContext = mockk<Context>(relaxed = true)
    private val  mockEventLogger = mockk<AppEventsLogger>(relaxed = true)
    private val facebookAppEventsDestination = FacebookAppEvents(mockContext)

    @MockK(relaxUnitFun = true)
    lateinit var mockedAnalytics: Analytics

    init {
        MockKAnnotations.init(this)
        mockkStatic(FacebookSdk::class)
        mockkStatic(AppEventsLogger::class)
        every { AppEventsLogger.newLogger(mockContext) } returns mockEventLogger
        every { FacebookSdk.isInitialized() } returns true
        every { FacebookSdk.isFullyInitialized() } returns true

        facebookAppEventsDestination.analytics = mockedAnalytics
    }

    @BeforeEach
    internal fun setUp() {
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
                    "zeroedAttribution":false,
                }    
              }
            }
        """.trimIndent()
        )
        facebookAppEventsDestination.update(settingsBlob, Plugin.UpdateType.Initial)

        /* assertions about config */
        Assertions.assertNotNull(facebookAppEventsDestination.settings)
        with(facebookAppEventsDestination.settings!!) {
            Assertions.assertEquals("123bc", appId)
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

        val trackEvent = facebookAppEventsDestination.track(sampleEvent)

        Assertions.assertNotNull(trackEvent)
    }
}