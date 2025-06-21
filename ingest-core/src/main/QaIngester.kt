package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.config.LLMEndpointBuilder
import io.ybigta.text2sql.ingest.llmendpoint.QuestionMainClauseExtractionEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.QuestionNormalizeAndStructureEndpoint
import io.ybigta.text2sql.ingest.vectordb.repositories.QaEmbeddingRepository
import io.ybigta.text2sql.ingest.vectordb.repositories.QaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

/**
 * insert QA pair to vectordb.
 * Before inserting QA pair, call LLM to normalize-and-structure question and extract end extract main clauses from question.
 */
class QaIngester(
    private val interval: Duration,
    private val questionNormalizeAndStructureEndpoint: QuestionNormalizeAndStructureEndpoint,
    private val questionMainClauseExtractionEndpoint: QuestionMainClauseExtractionEndpoint,
    private val qaRepository: QaRepository,
    private val qaEmbeddingRepository: QaEmbeddingRepository
) {

    companion object {
        fun fromConfig(ingestConfig: IngestConfig, interval: Duration) = QaIngester(
            interval = interval,
            questionNormalizeAndStructureEndpoint = LLMEndpointBuilder.QaIngest.buildQuestionNormalizeAndStructureEndpoint(ingestConfig),
            questionMainClauseExtractionEndpoint = LLMEndpointBuilder.QaIngest.buildQuestionMainClauseExtractionEndPoint(ingestConfig),
            qaRepository = QaRepository(db = ingestConfig.pgvector),
            qaEmbeddingRepository = QaEmbeddingRepository(db = ingestConfig.pgvector, embeddingModel = ingestConfig.embeddingModel)
        )
    }


    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun ingest(qaList: List<Qa>) = coroutineScope {
        val cnt = AtomicInteger(0)

        qaList
            .channelFlowMapAsync(interval) { qa ->
                val normaliezedQa = normalizeAndStructureQuestionLogic(qa)
                Pair(qa, normaliezedQa)
            }
            .collect { (qa, structuredQa) ->
                val id = qaRepository.insertAndGetId(qa.question, qa.answer, structuredQa)
                qaEmbeddingRepository.insertAllTypes(id, structuredQa)
                logger.info("[{}/{}] done!", cnt.addAndGet(1), qaList.size)
            }
    }

    private suspend fun normalizeAndStructureQuestionLogic(qa: Qa): StructuredQa = coroutineScope {

        logger.debug("requesting llm for normalize and struct")
        val normalizedQa = async { questionNormalizeAndStructureEndpoint.request(qa.question, qa.answer) }
        logger.debug("requesting llm for extract mainClause from")
        val mainClause = async { questionMainClauseExtractionEndpoint.request(qa.question) }

        return@coroutineScope normalizedQa.await().copy(
            mainClause = mainClause.await()
        )
    }
}

@Serializable
data class Qa(
    val question: String, // natural language
    val answer: String // SQL
)

@Serializable
data class StructuredQa(
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
