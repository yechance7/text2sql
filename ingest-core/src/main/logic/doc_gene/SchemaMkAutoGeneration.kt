package io.ybigta.text2sql.ingest.logic.doc_gene

import io.ybigta.text2sql.ingest.llmendpoint.SchemaMarkdownGenerationEndpoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


private val logger = LoggerFactory.getLogger("autoGenerateSchemaMkLogic")

/**
 * this logic is not part of schema ingest logic.
 * but it helps user to auto-generate schema markdown documents instead of hand-writting them
 *
 * what it does:
 * 1. query schema information to database
 * 2. request llm to generate markdown about table schema
 */
internal fun autoGenerateSchemaMkLogic(
    db: Database,
    schemaMarkdownGenerationEndpoint: SchemaMarkdownGenerationEndpoint,
    internval: Duration = 5.seconds
): Flow<Triple<String, String, String>> = channelFlow {

    val databaseSchemaIngester: DatabaseSchemaIngester = when (db.vendor) {
        "PostgreSQLNG", "PostgreSQL" -> PostgresSchemaIngester(db)
        else -> throw IllegalStateException("(${db.vendor}) is not supported database")
    }
    databaseSchemaIngester
        .requestTableSchemas()
        .forEach { tableSchema ->
            launch {
                logger.debug("request table markdown generation(schema={},table={})", tableSchema.schemaName, tableSchema.tableName)
                Triple(
                    tableSchema.tableName,
                    tableSchema.schemaName,
                    schemaMarkdownGenerationEndpoint.request(Json.encodeToJsonElement(tableSchema) as JsonObject)
                ).let { send(it) }
            }
            delay(internval)
        }
}


@Serializable
data class TableSchema(
    val tableName: String,
    val schemaName: String,
    val columns: List<Map<String, String?>>
)

