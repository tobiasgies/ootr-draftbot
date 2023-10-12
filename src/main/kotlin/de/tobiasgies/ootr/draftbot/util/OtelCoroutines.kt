package de.tobiasgies.ootr.draftbot.util

import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.extension.kotlin.getOpenTelemetryContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalContracts::class)
suspend fun <T> withOtelContext(
    otelContext: Context,
    block: suspend CoroutineScope.() -> T
): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return withContext(coroutineContext + otelContext.asContextElement()) {
        coroutineContext.getOpenTelemetryContext().makeCurrent()
        block()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Call.executeAsync(): Response {
    val otelContext = coroutineContext.getOpenTelemetryContext()
    return suspendCancellableCoroutine { continuation ->
        otelContext.makeCurrent()
        continuation.invokeOnCancellation {
            this.cancel()
        }
        this.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(value = response, onCancellation = { call.cancel() })
            }
        })
    }
}