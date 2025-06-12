package io.ybigta.text2sql.ingest.logic.schema_ingest

import io.ybigta.text2sql.ingest.llmendpoint.StructureSchemaDocEndPoint
import io.ybigta.text2sql.ingest.llmendpoint.TableEntitiesExtractionEndpoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory


private val logger = LoggerFactory.getLogger("ingress.schema-ingress")

@Serializable
data class TableSchemaJson(
    val name: String,
    val purpose: String,
    val summary: String,
    val dependenciesThought: String,
    val keys: String,
    val connectedTables: List<String>,
    val columns: List<Column>,
    val strongEntities: List<String>,
    val weekEntities: List<String>,
    val docName: String
) {
    @Serializable
    data class Column(
        val column: String,
        val description: String
    )

    fun toDescription(): String = """
    table: ${this.name}
    
    # description
    ${this.summary}
    ${this.purpose}
    """.trimIndent()

    fun toDescriptionWithDependencies(): String = """
    table: ${this.name}
    
    # description
    ${this.summary}
    ${this.purpose}
    ${this.dependenciesThought}
    """.trimIndent()
}


/**
 * TODO: INSERT INTO vector_db
 * extracting entities from table doc is done by selecting frequently appeared entities of multiple same [TableEntitiesExtractionEndpoint] request.
 * @param requestNum the number of same llm request to [TableEntitiesExtractionEndpoint].
 * @param frequencyStrong the numeber of entity appearance in multiple llm request treated as strong entity
 * @param frequencyWeek the numeber of entity appearance in multiple llm request treated as strong entity
 */
suspend fun schemaIngrestLogic(
    schemaMarkdown: String,
    structureMkEndpoint: StructureSchemaDocEndPoint,
    tableEntitiesExtractionEndpoint: TableEntitiesExtractionEndpoint,
    tableName: String, // for logging
    requestNum: Int = 7,
    frequencyStrong: Int = 3,
    frequencyWeek: Int = 2,
): TableSchemaJson = coroutineScope {
    logger.debug("reqeust for makrdown doc to json (table={}) ", tableName)
    val tableSchemaJsonWithoutEntities: TableSchemaJson = structureMkEndpoint.request(schemaMarkdown)
    logger.trace("(table={}) \n{}", Json { prettyPrint = true }.encodeToString(tableSchemaJsonWithoutEntities))

    logger.debug("reqeust for entities extraction. will request {} times (table={}) ", tableName, requestNum)
    // request multiple sam llm request then select most frequent
    val extractedEntitiesList = (1..requestNum)
        .map { async { tableEntitiesExtractionEndpoint.request(tableSchemaJsonWithoutEntities) } }
        .awaitAll()

    val entityFrequecies = extractedEntitiesList
        .flatten()
        .groupingBy { it }
        .eachCount()

    val strongEntties = entityFrequecies.filterValues { frequency -> frequencyStrong < frequency }.keys.toList()
    val weekEntities = entityFrequecies.filterValues { frequency -> frequencyWeek < frequency }.keys.toList()

    return@coroutineScope tableSchemaJsonWithoutEntities.copy(
        strongEntities = strongEntties,
        weekEntities = weekEntities
    )
}