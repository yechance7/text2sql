package io.ybigta.text2sql.ingest.logic.qa_ingest

import io.ybigta.text2sql.ingest.llmendpoint.DomainEntityDocumentGenerateEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.DomainSpecificEntitiesExtractionEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.TableSelectionEndpoint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toSet


/**
 * response of [TableSelectionEndpoint]
 */
data class TableSelection(
    val tableName: String,
    val justification: String
)

data class DomainEntitiyMapping(
    val entityName: String,
    val entityConceptualRole: String,
    val sourceTables: Set<String>,
    val classification: String,
)


// suspend fun domainExtractionLogic(
//     normalizedQa: NormalizedQa,
//     rules: Set<String>,
//     tableSelectionEndpoint: TableSelectionEndpoint,
//     domainSpecificEntitiesExtractionEndpoint: DomainSpecificEntitiesExtractionEndpoint
// ): Set<String> = domainExtractionLogic(normalizedQa.question, normalizedQa.dataSource.map { it.table }.toSet(), rules, tableSelectionEndpoint, domainSpecificEntitiesExtractionEndpoint)

suspend fun domainExtractionLogic(
    qa: Qa,
    tables: Set<String>,
    rules: Set<String>,
    tableSelectionEndpoint: TableSelectionEndpoint,
    domainSpecificEntitiesExtractionEndpoint: DomainSpecificEntitiesExtractionEndpoint,
    domainEntityDocumentGenerateEndpoint: DomainEntityDocumentGenerateEndpoint,
): Set<DomainEntitiyMapping> = coroutineScope {
    val tableSelection = tableSelectionEndpoint.reqeust(qa.question, qa.answer, rules, tables)
    val extractedEntities = domainSpecificEntitiesExtractionEndpoint.reqeust(tableSelection)

    val domainEntitymappings = extractedEntities
        .map {
            async {
                domainEntityDocumentGenerateEndpoint.request(qa.question, tableSelection)
            }
        }
        .asFlow()
        .flatMapMerge { it.await().asFlow() }
        .onEach { println("got it") }
        .toSet()

    return@coroutineScope domainEntitymappings
}