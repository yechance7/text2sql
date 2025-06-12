package io.ybigta.text2sql.ingest.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path

/**
 * define spec of `ingest_config.yaml`
 * serialize via kaml(kotlinx serialization yaml package)
 */
@Serializable
data class IngestConfigFileSpec(
    @SerialName("Resources")
    val resources: ResourcesSpec,
    @SerialName("PgVector")
    val pgvector: PgVectorSpec,
    @SerialName("EmbeddingModel")
    val embeddingModel: EmbeddingModelSpec,
    @SerialName("LLMModels")
    val llmModels: List<LLMModelSpec>,
    @SerialName("LLMEndPoints")
    val llmEndPoints: LLMEndPointsSpec
) {
    @Serializable
    data class ResourcesSpec(
        @Serializable(PathSerializer::class)
        val qaJson: Path,
        @Serializable(PathSerializer::class)
        val schemaMarkdownDir: Path
    )

    @Serializable
    data class EmbeddingModelSpec(
        val modelName: String,
        val apiKey: String
    )

    @Serializable
    data class LLMModelSpec(
        val modelName: String,
        val apiKey: String
    )

    @Serializable
    data class PgVectorSpec(
        val jdbcUrl: String,
        val userName: String,
        val password: String
    )

    @Serializable
    data class LLMEndPointsSpec(
        @SerialName("SchemaMarkdownGeneration")
        val schemaMarkdownGeneration: SchemaMarkdownGenerationSpec,

        @SerialName("SchemaIngest")
        val schemaIngest: SchemaIngestSpec,

        @SerialName("QaIngest")
        val qaIngest: QaIngest,

        @SerialName("DomainEntityMappingIngest")
        val domainEntityMappingIngest: DomainEntityMappingIngest,

        ) {
        @Serializable
        data class LLMEndPointSpec(
            val modelName: String,
            val systemPrompt: String
        )

        @Serializable
        data class SchemaMarkdownGenerationSpec(
            @SerialName("SchemaMarkdownGenerationEndpoint")
            val schemaMarkdownGenerationEndpoint: LLMEndPointSpec
        )

        @Serializable
        data class SchemaIngestSpec(
            @SerialName("StrucutureSchemaDocEndpoint")
            val strucutureSchemaDocEndpoint: LLMEndPointSpec,

            @SerialName("TableEntitiesExtractionEndpoint")
            val tableEntitiesExtractionEndpoint: LLMEndPointSpec
        )

        @Serializable
        data class QaIngest(
            @SerialName("QuestionNormalizeAndStructureEndpoint")
            val questionNormalizeAndStructureEndpoint: LLMEndPointSpec,
            @SerialName("QuestionMainClauseExtractionEndPoint")
            val questionMainClauseExtractionEndPoint: LLMEndPointSpec
        )

        @Serializable
        data class DomainEntityMappingIngest(
            @SerialName("SourceTableSelectionEndpoint")
            val sourceTableSelectionEndpoint: LLMEndPointSpec,
            @SerialName("DomainEntitiesExtractionEndpoint")
            val domainEntitiesExtractionEndpoint: LLMEndPointSpec,
            @SerialName("DomainEntityMappingDocGenerationEndpoint")
            val domainEntityMappingDocGenerationEndpoint: LLMEndPointSpec
        )
    }
}

object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Path {
        val pathString = decoder.decodeString()
        return Path.of(pathString)
    }
}