package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.config.LLMEndpointBuilder
import io.ybigta.text2sql.ingest.logic.qa_ingest.Qa
import io.ybigta.text2sql.ingest.logic.qa_ingest.StructuredQa
import io.ybigta.text2sql.ingest.logic.qa_ingest.normalizeAndStructureQuestionLogic
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import vectordb.QaEmbeddingRepository
import vectordb.QaRepository
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.seconds

class QaIngester(
    private val config: IngestConfig,
) {
    private val questionNormalizeAndStructureEndpoint = LLMEndpointBuilder
        .QaIngest
        .buildQuestionNormalizeAndStructureEndpoint(config)

    private val questionMainClauseExtractionEndpoint = LLMEndpointBuilder
        .QaIngest
        .buildQuestionMainClauseExtractionEndPoint(config)

    private val qaRepository = QaRepository(db = config.pgvector)
    private val qaEmbeddingRepository = QaEmbeddingRepository(db = config.pgvector, embeddingModel = config.embeddingModel, qaRepository = qaRepository)

    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun ingest() = coroutineScope {
        val qaList: List<Qa> = config.config
            .resources
            .qaJson
            .toAbsolutePath()
            .normalize()
            .readText()
            .let { Json.decodeFromString<List<Qa>>(it) }

        var cnt = 0;
        channelFlow<Deferred<Pair<Qa, StructuredQa>>> {
            qaList.forEach { qa ->
                delay(1.seconds)
                val result = async {
                    val normaliezedQa = normalizeAndStructureQuestionLogic(
                        qa,
                        questionNormalizeAndStructureEndpoint,
                        questionMainClauseExtractionEndpoint
                    )
                    Pair(qa, normaliezedQa)
                }
                send(result)
            }
        }
            .map { it.await() }
            .collectLatest { (qa, structuredQa) ->
                val id = qaRepository.insertAndGetId(qa.question, qa.answer, structuredQa)
                qaEmbeddingRepository.insertAllTypes(qa.question, qa.answer, structuredQa)
                logger.info("[{}/{}] done!", ++cnt, qaList.size)
            }
    }
}