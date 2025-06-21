package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.config.LLMEndpointBuilder
import io.ybigta.text2sql.ingest.llmendpoint.DomainEntityMappingGenerationEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.SourceTableSelectionEndpoint
import io.ybigta.text2sql.ingest.vectordb.repositories.DomainEntityMappingRepository
import io.ybigta.text2sql.ingest.vectordb.repositories.QaRepository
import io.ybigta.text2sql.ingest.vectordb.repositories.TableDocRepository
import io.ybigta.text2sql.ingest.vectordb.tables.QaTbl
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
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
    private val interval: Duration,
    private val sourceTableSelectionEndpoint: SourceTableSelectionEndpoint,
    private val domainEntityMappingGenerationEndpoint: DomainEntityMappingGenerationEndpoint,
    private val schemaDocRepository: TableDocRepository,
    private val qaRepository: QaRepository,
    private val domainEntityMappingRepository: DomainEntityMappingRepository

) {

    companion object {
        fun fromConfig(ingestConfig: IngestConfig, interval: Duration = 1.seconds) = DomainMappingIngester(
            interval = interval,
            sourceTableSelectionEndpoint = LLMEndpointBuilder.DomainEntityMappingIngest.buildSourceTableSelectionEndpoint(ingestConfig),
            domainEntityMappingGenerationEndpoint = LLMEndpointBuilder.DomainEntityMappingIngest.buildDomainEntityMappingDocGenerationEndpoint(ingestConfig),
            schemaDocRepository = TableDocRepository(db = ingestConfig.pgvector, embeddingModel = ingestConfig.embeddingModel),
            qaRepository = QaRepository(db = ingestConfig.pgvector),
            domainEntityMappingRepository = DomainEntityMappingRepository(db = ingestConfig.pgvector),
        )
    }


    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun findSourceTables(tableNames: Set<String>): Set<String> = tableNames
        .mapNotNull { tableName -> schemaDocRepository.findDocByTableName(tableName) }
        .map { Json.encodeToString(it) }
        .toSet()

    suspend fun ingest() = coroutineScope {
        val qaList: List<QaTbl.Dto> = qaRepository.findAll()

        var cnt = AtomicInteger(0)

        qaList
            .channelFlowMapAsync(interval) { qaDto ->
                domainExtractionLogic(
                    qa = Qa(qaDto.question, qaDto.answer),
                    tables = findSourceTables(qaDto.structuredQa.dataSource.map { it.table }.toSet()),
                    emptySet(),
                ).map { entityMapping -> Pair(qaDto, entityMapping) }
            }
            .onEach { logger.info("[{}/{}] received llm output!", cnt.addAndGet(1), qaList.size) }
            .flatMapConcat { it.asFlow() }
            .collect { (qaDto, entityMapping) -> domainEntityMappingRepository.insertAndGetId(qaDto.id, entityMapping) }
    }


    private suspend fun domainExtractionLogic(
        qa: Qa,
        tables: Set<String>,
        rules: Set<String>,
    ): List<DomainEntitiyMapping> = coroutineScope {

        val tableSelection: Set<TableSelection> = sourceTableSelectionEndpoint.reqeust(qa.question, qa.answer, rules, tables)
        val domainEntitymappings = domainEntityMappingGenerationEndpoint.request(qa.question, tableSelection)

        return@coroutineScope domainEntitymappings
    }
}


/**
 * response of [SourceTableSelectionEndpoint]
 */
data class TableSelection(
    val tableName: String,
    val justification: String
)

@Serializable
data class DomainEntitiyMapping(
    val entityName: String,
    val entityConceptualRole: String,
    val sourceTables: Set<String>,
    val classification: String,
)
