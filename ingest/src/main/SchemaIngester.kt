package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.config.LLMEndpointBuilder
import io.ybigta.text2sql.ingest.logic.schema_ingest.schemaIngrestLogic
import io.ybigta.text2sql.ingest.vectordb.TableDocEmbeddingRepository
import io.ybigta.text2sql.ingest.vectordb.TableDocRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.seconds

class SchemaIngester(
    private val ingestConfig: IngestConfig
) {
    private val schemaDocDir: Path = ingestConfig.config.resources.schemaMarkdownDir.toAbsolutePath()

    private val structureSchemaDocEndPoint = LLMEndpointBuilder.SchemaIngest.buildStrucutureSchemaDocEndpoint(ingestConfig)
    private val tableEntitiesExtractionEndpoint = LLMEndpointBuilder.SchemaIngest.buildTableEntitiesExtractionEndpoint(ingestConfig)

    private val schemaDocRepository = TableDocRepository(ingestConfig.pgvector, ingestConfig.embeddingModel)
    private val tableDocEmbeddingRepository = TableDocEmbeddingRepository(ingestConfig.pgvector, ingestConfig.embeddingModel)

    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun ingest() {
        val schemaDocs: List<Triple<String, String, String>> = schemaDocDir
            .listDirectoryEntries("*.md")
            .map { it.normalize() }
            .map { path ->
                val schemaDoc = path.readText()
                val (schemaName, tableName) = path.fileName.pathString
                    .split(".")
                    .also { require(it.size == 3) { "schema-doc file name should be <schema_name>.<table_name>.md" } }
                    .let { Pair(it[0], it[1]) }

                logger.info("got schema doc of  schema={} table={}", schemaName, tableName)
                Triple(schemaName, tableName, schemaDoc)
            }

        channelFlow {
            schemaDocs.forEach { (schemaName, tableName, schemaDoc) ->
                delay(2.seconds)
                async {
                    schemaIngrestLogic(
                        schemaDoc,
                        structureSchemaDocEndPoint,
                        tableEntitiesExtractionEndpoint
                    ).let { Pair(schemaName, it) }
                }
                    .let { send(it) }
            }
        }
            .map { it.await() }
            .collect { (schemaname, tableSchema) ->
                val id = schemaDocRepository.insertAndGetId(schemaname, tableSchema.name, tableSchema)
                tableDocEmbeddingRepository.insertAllCategories(id, tableSchema)
            }
    }
}
