package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.config.LLMEndpointBuilder
import io.ybigta.text2sql.ingest.logic.domain_mapping_ingest.domainExtractionLogic
import io.ybigta.text2sql.ingest.logic.qa_ingest.Qa
import io.ybigta.text2sql.ingest.vectordb.repositories.DomainEntityMappingRepository
import io.ybigta.text2sql.ingest.vectordb.repositories.QaRepository
import io.ybigta.text2sql.ingest.vectordb.repositories.TableDocRepository
import io.ybigta.text2sql.ingest.vectordb.tables.QaTbl
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * domain mapping acts as reasonsing bridge between user question(natural language) and database schema.
 *
 * domain mapping infered from question-anwser pair by LLM.
 *
 * domain mapping is json document that contains
 * - entity name and it's conceptual role within the domain
 * - database tables reqquired to access this entity
 * - classificatioin type(MINOR, MAJOR)
 *
 */
class DomainMappingIngester(
    private val ingestConfig: IngestConfig,
    private val interval: Duration = 1.seconds
) {

    private val sourceTableSelectionEndpoint = LLMEndpointBuilder.DomainEntityMappingIngest.buildSourceTableSelectionEndpoint(ingestConfig)
    private val domainEntityMappingGenerationEndpoint = LLMEndpointBuilder.DomainEntityMappingIngest.buildDomainEntityMappingDocGenerationEndpoint(ingestConfig)

    private val schemaDocRepository = TableDocRepository(db = ingestConfig.pgvector, embeddingModel = ingestConfig.embeddingModel)
    private val qaRepository = QaRepository(db = ingestConfig.pgvector)
    private val domainEntityMappingRepository = DomainEntityMappingRepository(db = ingestConfig.pgvector)

    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun findSourceTables(tableNames: Set<String>): Set<String> = tableNames
        .mapNotNull { tableName -> schemaDocRepository.findDocByTableName(tableName) }
        .map { Json.encodeToString(it) }
        .toSet()

    suspend fun ingest() = coroutineScope {
        val qaList: List<QaTbl.Dto> = qaRepository.findAll()

        var cnt = AtomicInteger(0)

        channelFlow {
            qaList.forEach { qaDto ->
                val result = async {
                    domainExtractionLogic(
                        qa = Qa(qaDto.question, qaDto.answer),
                        tables = findSourceTables(qaDto.structuredQa.dataSource.map { it.table }.toSet()),
                        emptySet(),
                        sourceTableSelectionEndpoint,
                        domainEntityMappingGenerationEndpoint
                    ).map { entityMapping -> Pair(qaDto, entityMapping) }
                }
                delay(interval)
                send(result)
            }
        }
            .map { it.await() }
            .onEach { logger.info("[{}/{}] received llm output!", cnt.addAndGet(1), qaList.size) }
            .flatMapConcat { it.asFlow() }
            .collect { (qaDto, entityMapping) ->
                domainEntityMappingRepository.insertAndGetId(qaDto.id, entityMapping)

            }

    }
}
