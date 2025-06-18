package io.ybigta.text2sql.infer.core.logic.table_retrieve

import dev.langchain4j.model.embedding.EmbeddingModel
import io.ybigta.text2sql.exposed.pgvector.cosDist
import io.ybigta.text2sql.infer.core.Question
import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocEmbddingTbl
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocEmbddingTbl.EmbeddingCategory
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocTbl
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class TblSimiRetrieveLogic(
    private val tblSimiRepository: TblSimiRepository,
    private val resultN: Int = 5
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun retrieve(question: Question): List<TableSchemaJson> = coroutineScope {
        return@coroutineScope retrieveBySimi(question)
    }

    private suspend fun retrieveBySimi(question: Question): List<TableSchemaJson> = coroutineScope {

        val retrieveParamList = listOf(
            TblSimiRetrieveParam(
                queryText = question.normalizedQ.await(),
                filterCondition = setOf(EmbeddingCategory.DESCRIPTION, EmbeddingCategory.ENTITY),
                resultN = resultN
            ),
            TblSimiRetrieveParam(
                queryText = question.normalizedQ.await(),
                filterCondition = setOf(EmbeddingCategory.TABLE_NAME),
                resultN = resultN
            ),
            TblSimiRetrieveParam(
                queryText = question.mainClause.await(),
                filterCondition = setOf(EmbeddingCategory.DESCRIPTION, EmbeddingCategory.ENTITY),
                resultN = resultN
            ),
            TblSimiRetrieveParam(
                queryText = question.mainClause.await(),
                filterCondition = setOf(EmbeddingCategory.TABLE_NAME),
                resultN = resultN
            ),
            // TODO: support queryText can get list of string
            TblSimiRetrieveParam(
                queryText = question.extractedEntities.await().firstOrNull() ?: "",
                filterCondition = setOf(EmbeddingCategory.CONNECTED_TABLES, EmbeddingCategory.ENTITY),
                resultN = resultN
            ),
            // TODO: support queryText can get list of string
            TblSimiRetrieveParam(
                queryText = question.extractedEntities.await().firstOrNull() ?: "",
                filterCondition = setOf(EmbeddingCategory.TABLE_NAME),
                resultN = resultN
            ),
        )

        val retrievedResult = retrieveParamList
            .map { param -> async { tblSimiRepository.findByParam(param) } }
            .map { it.await() }
            .flatten()

        val retrievedTables = retrievedResult
            .map { it.tableSchemaJson }
            .distinct()

        return@coroutineScope retrievedTables
    }
}


data class TblSimiRetrieveResult(
    val tableName: String,
    val tableSchemaJson: TableSchemaJson, // makrdown doc
    val embeddingCategory: EmbeddingCategory,
    val distance: Float
)

/**
 * @param resultN: number of results
 * @param queryText could be main_clause, normalized_question, extracted_concepts
 * @param
 */
data class TblSimiRetrieveParam(
    val queryText: String,
    val filterCondition: Set<TableDocEmbddingTbl.EmbeddingCategory>,
    val resultN: Int
)

class TblSimiRepository(
    private val db: Database,
    private val embeddingModel: EmbeddingModel
) {

    fun findByParam(param: TblSimiRetrieveParam): List<TblSimiRetrieveResult> = transaction(db) {
        val queryTextEmbedding = embeddingModel.embed(param.queryText).content().vector()
        val distance = (TableDocEmbddingTbl.embedding cosDist queryTextEmbedding).alias("distance")

        val query = TableDocTbl
            .innerJoin(TableDocEmbddingTbl)
            .select(TableDocTbl.table, TableDocTbl.schemaJson, distance, TableDocEmbddingTbl.embeddingCategory)
            .andWhere { TableDocEmbddingTbl.embeddingCategory inList param.filterCondition }
            .orderBy(distance)
            .limit(param.resultN)

        query.map {
            TblSimiRetrieveResult(
                tableName = it[TableDocTbl.table],
                tableSchemaJson = it[TableDocTbl.schemaJson],
                embeddingCategory = it[TableDocEmbddingTbl.embeddingCategory],
                distance = it[distance]
            )
        }
    }
}
