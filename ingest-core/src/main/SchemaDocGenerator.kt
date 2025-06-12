package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.config.LLMEndpointBuilder
import io.ybigta.text2sql.ingest.logic.doc_gene.autoGenerateSchemaMkLogic
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * this logic is not part of schema ingest logic.
 * but it helps user to auto-generate schema markdown documents instead of hand-writting them
 */
class SchemaDocGenerator(
    private val ingestConfig: IngestConfig,
    private val sourceDB: Database,
    private val interval: Duration = 3.seconds
) {

    private val schemaMarkdownGenerationEndpoint = LLMEndpointBuilder.SchemaMarkdownGeneration.buildSchemaMarkdownGenerationEndpoint(ingestConfig)
    private val saveDir: Path = ingestConfig.config.resources.schemaMarkdownDir
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun generate() {
        val saveBaseDir = saveDir.toAbsolutePath().normalize().also { it.createDirectories() }

        // query table information from database
        val tableSchemaDocs: Flow<Triple<String, String, String>> = autoGenerateSchemaMkLogic(
            db = sourceDB,
            schemaMarkdownGenerationEndpoint,
            internval = interval
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