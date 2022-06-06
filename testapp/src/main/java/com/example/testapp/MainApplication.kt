package com.example.testapp

import android.app.Application
import com.facebook.appevents.AppEventsLogger
import com.segment.analytics.kotlin.android.Analytics
import com.segment.analytics.kotlin.core.Analytics
import kotlinx.serialization.json.buildJsonObject
import com.segment.analytics.kotlin.destinations.facebookappevents.FacebookAppEvents
import com.segment.analytics.kotlin.core.*

import kotlinx.serialization.json.put

class MainApplication : Application() {
    companion object {
        lateinit var analytics: Analytics
    }
    override fun onCreate() {
        super.onCreate()

        analytics = Analytics("nYWdWYjzvCOVQ0crfg5CS48GHtn9fYq8", applicationContext) {
            this.collectDeviceId = true
            this.trackApplicationLifecycleEvents = true
            this.trackDeepLinks = true
            this.flushAt = 1
            this.flushInterval = 0
        }
        val logger = AppEventsLogger.newLogger(this)
        Analytics.debugLogsEnabled = true
        analytics.add(FacebookAppEvents(this))
        analytics.track("mainApplication test event")
        analytics.track("View Product", buildJsonObject {
            put("productId", 123)
            put("productName", "Striped trousers")
        });
    }
}