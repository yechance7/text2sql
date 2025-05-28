import com.charleskorn.kaml.Yaml
import io.ybigta.text2sql.ingest.ConfigFileSpec
import kotlinx.serialization.decodeFromString
import org.junit.Test
import kotlin.io.path.Path
import kotlin.test.assertEquals

class ConfigFileSpecTest {

    @Test
    fun `parse yaml config  file test1`() {
        val sampleConfig = """
        Resources:
            qaJson: ./data/qa.json
            schemaMarkdownDir: ./data/desc

        LLMModels:
            - modelName: gpt-3.5-turbo
              apiKey: secretKey
            - modelName: o3-mini
              apiKey: secretKey

        LLMEndPoints:
            NormalizeAndStruct:
                modelName: o3-mini
                systemPrompt: |
                    line1
                    line2
            MakrdownDescriptionAutoGeneration:
                modelName: gpt-3.5-turbo
                systemPrompt: |
                    line1
                    line2
        """

        val result = Yaml.default.decodeFromString<ConfigFileSpec>(sampleConfig)

        assertEquals(Path("./data/qa.json"), result.resources.qaJson)
        assertEquals(Path("./data/desc"), result.resources.schemaMarkdownDir)

        assertEquals("gpt-3.5-turbo", result.llmModels[0].modelName)
        assertEquals("secretKey", result.llmModels[0].apiKey)

        assertEquals("o3-mini", result.llmModels[1].modelName)
        assertEquals("secretKey", result.llmModels[1].apiKey)

        assertEquals("o3-mini", result.llmEndPoints.normalizedAndStruct.modelName)
        assertEquals("line1\nline2\n", result.llmEndPoints.normalizedAndStruct.systemPrompt)

        assertEquals("gpt-3.5-turbo", result.llmEndPoints.makrdownDescriptionAutoGeneration.modelName)
        assertEquals("line1\nline2\n", result.llmEndPoints.makrdownDescriptionAutoGeneration.systemPrompt)
    }

}
