package com.segment.analytics.kotlin.destinations.facebookappevents

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.emptyJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class FacebookAppEventsTests {
    private  val  mockEventLogger = Mockito.mock(AppEventsLogger::class.java)
//    private val mockContext = mockk<Context>(relaxed = true)
//    private val  mockEventLogger = mockk<AppEventsLogger>(relaxed = true)
    private lateinit var facebookAppEventsDestination: FacebookAppEvents

    init {
    Mockito.mockStatic(FacebookSdk::class.java)
        Mockito.mockStatic(AppEventsLogger::class.java).use { utilities ->
            utilities.`when`<Any>(AppEventsLogger::).thenReturn("Eugen")
            assertThat(StaticUtils.name()).isEqualTo("Eugen")
        }
    }
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.segment.analytics.kotlin.destinations.facebookappevents.test", appContext.packageName)
    }

    @Test
    fun sendsStandardTrack() {
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

        assertNotNull(trackEvent)
        verify {
            mockEventLogger.logEvent("Food Choice", params)
        }
    }
}