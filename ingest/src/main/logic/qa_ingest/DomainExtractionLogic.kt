package io.ybigta.text2sql.ingest.logic.qa_ingest

import io.ybigta.text2sql.ingest.llmendpoint.DomainEntityMappingGenerationEndpoint
import io.ybigta.text2sql.ingest.llmendpoint.SourceTableSelectionEndpoint
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable


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
    sourceTableSelectionEndpoint: SourceTableSelectionEndpoint,
    domainEntityMappingGenerationEndpoint: DomainEntityMappingGenerationEndpoint,
): List<DomainEntitiyMapping> = coroutineScope {

    val tableSelection: Set<TableSelection> = sourceTableSelectionEndpoint.reqeust(qa.question, qa.answer, rules, tables)
    val domainEntitymappings = domainEntityMappingGenerationEndpoint.request(qa.question, tableSelection)

    return@coroutineScope domainEntitymappings
}