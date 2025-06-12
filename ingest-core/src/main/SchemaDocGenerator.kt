package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.config.LLMEndpointBuilder
import io.ybigta.text2sql.ingest.logic.schema_ingest.autoGenerateSchemaMkLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds

class SchemaDocGenerator(
    private val ingestConfig: IngestConfig,
    private val sourceDB: Database
) {

    private val schemaMarkdownGenerationEndpoint = LLMEndpointBuilder.SchemaMarkdownGeneration.buildSchemaMarkdownGenerationEndpoint(ingestConfig)
    private val saveDir: Path = ingestConfig.config.resources.schemaMarkdownDir
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun generate() {
        logger.info("auto generating table schema markdown document!!!")
        val saveBaseDir = saveDir.toAbsolutePath().normalize().also { it.createDirectories() }

        val tableSchemaDocs: Flow<Triple<String, String, String>> = autoGenerateSchemaMkLogic(
            db = sourceDB,
            schemaMarkdownGenerationEndpoint,
            internval = 3.seconds
        )

        tableSchemaDocs
            .collect { (tableName, schemaName, markdownDoc) ->
                val savePath = saveBaseDir.resolve("${schemaName}.${tableName}.md")

                if (savePath.exists()) {
                    val movePath = saveBaseDir.resolve("${schemaName}.${tableName}.${System.currentTimeMillis()}.md.bk")
                    savePath.moveTo(movePath)
                    logger.warn("moved pre-existing ${savePath} to ${movePath}")
                }

                savePath.writeText(markdownDoc)
                logger.info("written schema markdown document ${savePath}")
            }
    }

}