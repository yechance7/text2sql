package io.ybigta.text2sql.infer.cli

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.*
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

internal inline fun <reified T> readJson(path: Path, logger: Logger? = null): T? =
    try {
        path.readText().let { Json.decodeFromString<T>(it) }
    } catch (e: IOException) {
        logger?.error(
            """
            failed to read: ${path.toAbsolutePath().normalize()}
            cause: ${e.message}
        """.trimIndent()
        )
        null
    } catch (e: SerializationException) {
        logger?.error(
            """
                failed to parse json: ${path.toAbsolutePath().normalize()}
                cause: ${e.message}
            """.trimIndent()
        )
        null
    }

internal fun writeFile(text: String, path: Path, logger: Logger?) {
    if (path.exists()) {
        logger?.error("${path} is already existing")
        return
    }

    path
        .also { runCatching { it.parent.createDirectories() } }
        .also { it.createFile() }
    path.writeText(text)
}

internal inline fun <reified T> writeJson(value: T, path: Path, logger: Logger?) {
    val text = Json { prettyPrint = true }.encodeToString(value)
    writeFile(text, path, logger)
}
