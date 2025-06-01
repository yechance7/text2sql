package io.ybigta.text2sql.ingest.demo

import dev.langchain4j.model.chat.Capability
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiChatModelName
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName
import dev.langchain4j.service.AiServices
import io.ybigta.text2sql.ingest.llmendpoint.ExtractTableEntitiesEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.SchemaMkAutoGenerateEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.StuctureSchemaMkEndpoint
import io.ybigta.text2sql.ingest.logic.schema_ingest.autoGenerateSchemaMkLogic
import io.ybigta.text2sql.ingest.logic.schema_ingest.schemaIngrestLogic
import io.ybigta.text2sql.ingest.vectordb.TableSchemaDocRepository
import io.ybigta.text2sql.ingest.vectordb.TableSchemaDocTbl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

fun main() {
    val logger = LoggerFactory.getLogger("MAIN")

    /**
     * declare database,LLM connection
     */

    val sourceDB = Database.connect(
        "jdbc:postgresql://13.209.199.83:5432/dxp",
        user = "ybigta",
        password = System.getenv("SOURCE_DB_PASS")!!
    )

    val vectorDB = Database.connect(
        "jdbc:postgresql://localhost:5432/vectordb",
        user = "postgres",
        password = System.getenv("VECTOR_DB_PASS")!!
    )


    val llmModel = OpenAiChatModel.builder()
        .apiKey(System.getenv("LLM_API_KEY")!!)
        .modelName(OpenAiChatModelName.GPT_4_1_MINI)
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true)
        // .logRequests(true)
        // .logResponses(true)
        .build()

    val embeddingModel = OpenAiEmbeddingModel.builder()
        .apiKey(System.getenv("LLM_API_KEY")!!)
        .modelName(OpenAiEmbeddingModelName.TEXT_EMBEDDING_ADA_002)
        .build()

    /**
     * declare database components
     */

    transaction(vectorDB) {
        exec("CREATE EXTENSION IF NOT EXISTS vector;")
        SchemaUtils.create(TableSchemaDocTbl)
    }

    val schemaDocRepo = TableSchemaDocRepository(db = vectorDB, embeddingModel = embeddingModel)

    /**
     * declare llm endpoints
     */
    val schemaMkAutoGenerateEndpoint = AiServices
        .builder(SchemaMkAutoGenerateEndpoint::class.java)
        .chatModel(llmModel)
        .systemMessageProvider { _ -> DemoSystemPrompts.schemaMkAutoGenerateSystemPrompt }
        .build()

    val structureSchemaMkEndpoint = AiServices
        .builder(StuctureSchemaMkEndpoint::class.java)
        .chatModel(llmModel)
        .systemMessageProvider { _ -> DemoSystemPrompts.stuctureSchemaMkEndpointSystemPrompt }
        .build()

    val extractTableEntitiesEndpoint = AiServices
        .builder(ExtractTableEntitiesEndpoint::class.java)
        .chatModel(llmModel)
        .systemMessageProvider { _ -> DemoSystemPrompts.extractTableEntitiesEndpointSystemPrompt }
        .build()

    /**
     * execute actual logic
     */

    runBlocking(Dispatchers.IO) {
        val tableSchemaFlow: Flow<Triple<String, String, String>> = autoGenerateSchemaMkLogic(
            db = sourceDB,
            schemaMkAutoGenerateEndpoint = schemaMkAutoGenerateEndpoint,
            internval = 3.seconds
        )

        tableSchemaFlow
            .onEach { (tableName, schemaName, markdownDoc) -> logger.info("received schema doc for(table=$tableName, schema=$schemaName)") }
            .map { (tableName, schemaName, markdownDoc) ->
                async {
                    schemaIngrestLogic(
                        schemaMarkdown = markdownDoc,
                        structureMkEndpoint = structureSchemaMkEndpoint,
                        extractTableEntitiesEndpoint = extractTableEntitiesEndpoint
                    )
                        .also { schema -> logger.info("received schema json (table=${schema.name})") }
                        .let { schema -> schemaDocRepo.insertAndGetId(schemaName, tableName, schema) }
                }
            }
            .collect { it.await() }
    }

}

