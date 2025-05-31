package io.ybigta.text2sql.ingest.demo

import dev.langchain4j.model.chat.Capability
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiChatModelName
import dev.langchain4j.service.AiServices
import io.ybigta.text2sql.ingest.llmendpoint.SchemaMkAutoGenerateEndpoint
import io.ybigta.text2sql.ingest.logic.schema_ingest.autoGenerateSchemaMkLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("MAIN")

    val db = Database.connect(
        "jdbc:postgresql://13.209.199.83:5432/dxp",
        user = "ybigta",
        password = System.getenv("FUCKING")!!
    )

    val model = OpenAiChatModel.builder()
        .apiKey(System.getenv("FUCKING_APIKEY")!!)
        .modelName(OpenAiChatModelName.GPT_4_1_MINI)
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .strictJsonSchema(true)
        // .logRequests(true)
        // .logResponses(true)
        .build()

    val endpoint = AiServices
        .builder(SchemaMkAutoGenerateEndpoint::class.java)
        .chatModel(model)
        .systemMessageProvider { _ -> DemoSystemPrompts.schemaMkAutoGenerateSystemPrompt }
        .build()

    runBlocking(Dispatchers.IO) {
        autoGenerateSchemaMkLogic(db, endpoint)
            .onEach { (tableName, schemaName, mkDoc) -> logger.info("got auto markdown schema doc generation response (tableName=${tableName}) (schemaName=${schemaName})") }
            .toList()
            .onEach { println(it.third) }
    }
}
