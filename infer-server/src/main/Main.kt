package io.ybigta.text2sql.infer.server

import io.ktor.server.application.*
import io.ybigta.text2sql.infer.core.Inferer
import io.ybigta.text2sql.infer.core.Langchain4jLogger
import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.server.config.configCallLogging
import io.ybigta.text2sql.infer.server.config.configContentNegotiation
import io.ybigta.text2sql.infer.server.config.route.configInferRoute
import io.ybigta.text2sql.infer.server.config.route.configQaRoute
import io.ybigta.text2sql.infer.server.config.route.configTblDocRoute
import io.ybigta.text2sql.infer.server.controller.InferController
import io.ybigta.text2sql.infer.server.controller.QaController
import io.ybigta.text2sql.infer.server.controller.TblDocController
import io.ybigta.text2sql.infer.server.repository.QaGeneratedRepository
import io.ybigta.text2sql.infer.server.repository.TblDocRepository
import io.ybigta.text2sql.infer.server.service.InferService
import io.ybigta.text2sql.ingest.QaIngester
import io.ybigta.text2sql.ingest.SchemaIngester
import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.vectordb.repositories.QaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newFixedThreadPoolContext
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds


fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)


fun Application.mainModule() {
    val inferConfig = readInferConfig(listOf(Langchain4jLogger()))
    val ingestConfig = readIngestConfig()

    val inferWorkers = newFixedThreadPoolContext(30, "infer-worker")
    val inferScope = CoroutineScope(inferWorkers + SupervisorJob())

    configTblDocRoute(
        TblDocController(
            schemaIngester = SchemaIngester.fromConfig(ingestConfig),
            tblDocRepo = TblDocRepository(ingestConfig.pgvector)
        )
    )

    configInferRoute(
        InferController(
            scope = inferScope,
            inferService = InferService(
                inferer = Inferer.fromConfig(config = inferConfig),
                inferConfig = inferConfig,
                scope = inferScope,
                qaGeneratedRepo = QaGeneratedRepository(ingestConfig.pgvector)
            )
        )
    )

    configQaRoute(
        QaController(
            scope = inferScope,
            qaIngester = QaIngester.fromConfig(ingestConfig = ingestConfig, interval = 0.seconds),
            qaGeneratedRepository = QaGeneratedRepository(ingestConfig.pgvector),
            qaRepository = QaRepository(ingestConfig.pgvector)

        )
    )

    configContentNegotiation()
    configCallLogging()
}

private fun Application.readIngestConfig(): IngestConfig {
    val ingestConfigFilePath = environment
        .config
        .property("text2sql-config.ingest.config-file-path")
        .getString()
        .let { Path.of(it) }

    val ingestConfig = IngestConfig.fromConfigFile(ingestConfigFilePath)

    return ingestConfig
}

private fun Application.readInferConfig(
    langchain4jLoggers: List<Langchain4jLogger> = emptyList()
): InferConfig {
    val inferConfigFilePath = environment
        .config
        .property("text2sql-config.infer.config-file-path")
        .getString()
        .let { Path.of(it) }

    val inferConfig = InferConfig.fromConfigFile(inferConfigFilePath, langchain4jLoggers)

    return inferConfig
}
