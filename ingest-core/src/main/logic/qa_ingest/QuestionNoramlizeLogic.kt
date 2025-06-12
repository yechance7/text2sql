package io.ybigta.text2sql.ingest.logic.qa_ingest

import io.ybigta.text2sql.ingest.llmendpoint.QuestionMainClauseExtractionEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.QuestionNormalizeAndStructureEndpoint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
internal data class Qa(
    val question: String, // natural language
    val answer: String // SQL
)

@Serializable
internal data class StructuredQa(
    val question: String,
    val normalizedQuestion: String,
    val requestedEntities: String,
    val dataSource: List<DataSource>,
    val calculations: List<Calculation>,
    val mainClause: String
) {
    @Serializable
    data class DataSource(
        val table: String,
        val columns: List<String>
    )

    @Serializable
    data class Calculation(
        val operation: String,
        val arguments: List<String>,
        val grouping: List<String>,
        val conditions: String
    )
}

private val logger = LoggerFactory.getLogger("ingress.ga-ingress.normalizeAndStructure")

internal suspend fun normalizeAndStructureQuestionLogic(
    qa: Qa,
    questionNormalizeAndStructureEndpoint: QuestionNormalizeAndStructureEndpoint,
    questionMainClauseExtractionEndpoint: QuestionMainClauseExtractionEndpoint,
): StructuredQa = coroutineScope {

    logger.debug("requesting llm for normalize and struct")
    val normalizedQa = async { questionNormalizeAndStructureEndpoint.request(qa.question, qa.answer) }
    logger.debug("requesting llm for extract mainClause from")
    val mainClause = async { questionMainClauseExtractionEndpoint.request(qa.question) }

    return@coroutineScope normalizedQa.await().copy(
        mainClause = mainClause.await()
    )
}