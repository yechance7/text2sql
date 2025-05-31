package io.ybigta.text2sql.ingest.logic.qa_ingest

import io.ybigta.text2sql.ingest.llmendpoint.DomainSpecificEntitiesExtractionEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.MainClauseExtractionEndpoint
import kotlinx.coroutines.coroutineScope
import io.ybigta.text2sql.ingest.llmendpoint.TableSelectionEndpoint


/**
 * response of [TableSelectionEndpoint]
 */
data class TableSelection(
    val tableName: String,
    val justification: String
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
    domainSpecificEntitiesExtractionEndpoint: DomainSpecificEntitiesExtractionEndpoint
): Set<String> = coroutineScope {
    val tableSelection =  tableSelectionEndpoint.reqeust(qa.question, qa.answer, rules, tables)
    val extractedEntities =  domainSpecificEntitiesExtractionEndpoint.reqeust(tableSelection)

    return@coroutineScope extractedEntities
}