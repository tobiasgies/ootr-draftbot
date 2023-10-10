package de.tobiasgies.ootr.draftbot.util

import mu.KLogging
import java.time.Instant
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class Cached<T>(
    val cacheInterval: Duration = 10.minutes,
    val retryInterval: Duration = cacheInterval / 10,
    val loader: () -> T
) {
    private object UNINITIALIZED_VALUE

    private var nextRetrieval: TimeMark = TimeSource.Monotonic.markNow()
    private var _value: Any? = UNINITIALIZED_VALUE

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        val now = Instant.now()
        if (nextRetrieval.hasPassedNow()) {
            try {
                _value = loader()
                nextRetrieval = TimeSource.Monotonic.markNow() + cacheInterval
            } catch (e: Exception) {
                if (_value != UNINITIALIZED_VALUE) {
                    logger.warn(e) {
                        "Error while refreshing cached value for property ${property.name}" +
                                " of class $thisRef, returning stale data and retrying soon."
                    }
                    nextRetrieval = TimeSource.Monotonic.markNow() + retryInterval
                } else {
                    logger.error(e) {
                        "Error while loading initial data for property ${property.name}" +
                                " of class $thisRef. Bubbling up exception."
                    }
                    throw(e)
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }

    companion object : KLogging()
}

inline fun <reified T> cached(
    cacheInterval: Duration = 10.minutes,
    retryInterval: Duration = cacheInterval / 10,
    noinline loader: () -> T
): Cached<T> = Cached(cacheInterval, retryInterval, loader)