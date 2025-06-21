package io.ybigta.text2sql.ingest

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration

internal fun <T, R> Iterable<T>.channelFlowMapAsync(
    interval: Duration,
    transform: suspend (T) -> R
): Flow<R> = channelFlow {
    this@channelFlowMapAsync.forEach { element ->
        launch {
            send(transform(element))
        }
        delay(interval)
    }
}

internal val prettyJson = Json { prettyPrint = true }