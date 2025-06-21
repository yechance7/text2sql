package io.ybigta.text2sql.ingest.config

import com.charleskorn.kaml.Yaml
import dev.langchain4j.model.chat.Capability
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import dev.langchain4j.service.AiServices
import io.ybigta.text2sql.ingest.llmendpoint.*
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.Database
import java.nio.file.Path
import kotlin.io.path.readText

/**
 *  create and manage resource specified at ingest_config.yaml.
 *
 *  limitations
 *
 *  - currently only openai model is supported
 *  - can't specifiy user prompt at ingest_config.yaml. only system prompt is allowd because of Langchain4J
 */
class IngestConfig(
    val config: IngestConfigFileSpec
) {
    val llmModels: Map<String, ChatModel>

    val embeddingModel: EmbeddingModel

    val pgvector: Database

    init {
        this.llmModels = config.llmModels
            .map { (modelName, apiKey) ->
                val llmModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                    .strictJsonSchema(true)
                    .logRequests(false)
                    .logResponses(false)
                    .build()
                Pair(modelName, llmModel)
            }
            .toMap()

        this.embeddingModel = config.embeddingModel.let { (modelName, apiKey) ->
            OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build()
        }

        this.pgvector = Database.connect(
            url = config.pgvector.jdbcUrl,
            user = config.pgvector.userName,
            password = config.pgvector.password
        )
    }

    companion object {
        fun fromConfigFile(path: Path): IngestConfig = path
            // .also { require(it.fileName.endsWith("yaml") || it.fileName.endsWith("yml")) }
            .toAbsolutePath()
            .normalize()
            .readText()
            .let { Yaml.default.decodeFromString<IngestConfigFileSpec>(it) }
            .let { config -> IngestConfig(config) }
    }
}

internal object LLMEndpointBuilder {
    object SchemaMarkdownGeneration {
        fun buildSchemaMarkdownGenerationEndpoint(config: IngestConfig): SchemaMarkdownGenerationEndpoint = AiServices
            .builder(SchemaMarkdownGenerationEndpoint::class.java)
            .chatModel(config.llmModels[config.config.llmEndPoints.tableDescGeneration.schemaMarkdownGenerationEndpoint.modelName])
            .systemMessageProvider { _ -> config.config.llmEndPoints.tableDescGeneration.schemaMarkdownGenerationEndpoint.systemPrompt }
            .build()
    }

    object SchemaIngest {
        fun buildStrucutureSchemaDocEndpoint(config: IngestConfig): StructureSchemaDocEndPoint = AiServices
            .builder(StructureSchemaDocEndPoint::class.java)
            .chatModel(config.llmModels[config.config.llmEndPoints.tableDescGeneration.strucutureSchemaDocEndpoint.modelName])
            .systemMessageProvider { _ -> config.config.llmEndPoints.tableDescGeneration.strucutureSchemaDocEndpoint.systemPrompt }
            .build()

        fun buildTableEntitiesExtractionEndpoint(config: IngestConfig): TableEntitiesExtractionEndpoint = AiServices
            .builder(TableEntitiesExtractionEndpoint::class.java)
            .chatModel(config.llmModels[config.config.llmEndPoints.tableDescGeneration.tableEntitiesExtractionEndpoint.modelName])
            .systemMessageProvider { _ -> config.config.llmEndPoints.tableDescGeneration.tableEntitiesExtractionEndpoint.systemPrompt }
            .build()
    }

    object QaIngest {
        fun buildQuestionNormalizeAndStructureEndpoint(config: IngestConfig): QuestionNormalizeAndStructureEndpoint = AiServices
            .builder(QuestionNormalizeAndStructureEndpoint::class.java)
            .chatModel(config.llmModels[config.config.llmEndPoints.qaIngest.questionNormalizeAndStructureEndpoint.modelName])
            .systemMessageProvider { _ -> config.config.llmEndPoints.qaIngest.questionNormalizeAndStructureEndpoint.systemPrompt }
            .build()

        fun buildQuestionMainClauseExtractionEndPoint(config: IngestConfig): QuestionMainClauseExtractionEndpoint = AiServices
            .builder(QuestionMainClauseExtractionEndpoint::class.java)
            .chatModel(config.llmModels[config.config.llmEndPoints.qaIngest.questionMainClauseExtractionEndPoint.modelName])
            .systemMessageProvider { _ -> config.config.llmEndPoints.qaIngest.questionMainClauseExtractionEndPoint.systemPrompt }
            .build()
    }

    object DomainEntityMappingIngest {
        fun buildSourceTableSelectionEndpoint(config: IngestConfig): SourceTableSelectionEndpoint = AiServices
            .builder(SourceTableSelectionEndpoint::class.java)
            .chatModel(config.llmModels[config.config.llmEndPoints.domainEntityMappingIngest.sourceTableSelectionEndpoint.modelName])
            .systemMessageProvider { _ -> config.config.llmEndPoints.domainEntityMappingIngest.sourceTableSelectionEndpoint.systemPrompt }
            .build()

        fun buildDomainEntityMappingDocGenerationEndpoint(config: IngestConfig): DomainEntityMappingGenerationEndpoint = AiServices
            .builder(DomainEntityMappingGenerationEndpoint::class.java)
            .chatModel(config.llmModels[config.config.llmEndPoints.domainEntityMappingIngest.domainEntityMappingDocGenerationEndpoint.modelName])
            .systemMessageProvider { _ -> config.config.llmEndPoints.domainEntityMappingIngest.domainEntityMappingDocGenerationEndpoint.systemPrompt }
            .build()
    }
}
