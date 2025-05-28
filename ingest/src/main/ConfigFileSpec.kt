package io.ybigta.text2sql.ingest

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
 *
 * serialize via kaml(kotlinx serialization yaml package)
 */
@Serializable
data class ConfigFileSpec(
    @SerialName("Resources")
    val resources: ResourcesSpec,
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
    data class LLMModelSpec(
        val modelName: String,
        val apiKey: String
    )

    @Serializable
    data class LLMEndPointsSpec(
        @SerialName("NormalizeAndStruct")
        val normalizedAndStruct: LLMEndPointSpec,

        @SerialName("MakrdownDescriptionAutoGeneration")
        val makrdownDescriptionAutoGeneration: LLMEndPointSpec
    ) {
        @Serializable
        data class LLMEndPointSpec(
            val modelName: String,
            val systemPrompt: String
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