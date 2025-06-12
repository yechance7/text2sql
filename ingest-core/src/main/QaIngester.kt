package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.config.LLMEndpointBuilder
import io.ybigta.text2sql.ingest.logic.qa_ingest.Qa
import io.ybigta.text2sql.ingest.logic.qa_ingest.normalizeAndStructureQuestionLogic
import io.ybigta.text2sql.ingest.vectordb.repositories.QaEmbeddingRepository
import io.ybigta.text2sql.ingest.vectordb.repositories.QaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.readText
import kotlin.time.Duration

/**
 * insert QA pair to vectordb.
 * Before inserting QA pair, call LLM to normalize-and-structure question and extract end extract main clauses from question.
 */
class QaIngester(
    private val config: IngestConfig,
    private val interval: Duration
) {
    private val questionNormalizeAndStructureEndpoint = LLMEndpointBuilder
        .QaIngest
        .buildQuestionNormalizeAndStructureEndpoint(config)

    private val questionMainClauseExtractionEndpoint = LLMEndpointBuilder
        .QaIngest
        .buildQuestionMainClauseExtractionEndPoint(config)

    private val qaRepository = QaRepository(db = config.pgvector)
    private val qaEmbeddingRepository = QaEmbeddingRepository(
        db = config.pgvector,
        embeddingModel = config.embeddingModel
    )

    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun ingest() = coroutineScope {
        // read QA pair from file(specified in config file)
        val qaList: List<Qa> = config.config
            .resources
            .qaJson
            .toAbsolutePath()
            .normalize()
            .readText()
            .let { Json.decodeFromString<List<Qa>>(it) }

        var cnt = AtomicInteger(0)

        channelFlow {
            qaList.forEach { qa ->
                val result = async {
                    val normaliezedQa = normalizeAndStructureQuestionLogic(
                        qa,
                        questionNormalizeAndStructureEndpoint,
                        questionMainClauseExtractionEndpoint
                    )
                    Pair(qa, normaliezedQa)
                }
                send(result)
                delay(interval)
            }
        }
            .map { it.await() }
            .collectLatest { (qa, structuredQa) ->
                val id = qaRepository.insertAndGetId(qa.question, qa.answer, structuredQa)
                qaEmbeddingRepository.insertAllTypes(id, structuredQa)
                logger.info("[{}/{}] done!", cnt.addAndGet(1), qaList.size)
            }
    }
}