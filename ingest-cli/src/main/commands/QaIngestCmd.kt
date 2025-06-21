package io.ybigta.text2sql.ingest.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.ybigta.text2sql.ingest.Qa
import io.ybigta.text2sql.ingest.QaIngester
import io.ybigta.text2sql.ingest.config.IngestConfig
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.seconds

internal class QaIngestCmd : CliktCommand("ingest-qa") {
    private val configFilePath: Path by option("-c", "--config", help = "path of ingest_config.yaml").path().required()


    override fun run() {
        val config = IngestConfig.fromConfigFile(configFilePath)
        val qaIngester = QaIngester.fromConfig(config, 3.seconds)
        val dispatcher = newFixedThreadPoolContext(30, "worker")

        val qaList = config.config
            .resources
            .qaJson
            .toAbsolutePath()
            .normalize()
            .readText()
            .let { Json.decodeFromString<List<Qa>>(it) }

        runBlocking(dispatcher) { qaIngester.ingest(qaList) }
    }

}