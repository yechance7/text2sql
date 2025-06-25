package io.ybigta.text2sql.infer.core

import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.core.config.LLMEndpointBuilder
import io.ybigta.text2sql.infer.core.logic.generate.SqlGenerateLogic
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveLogic
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveRepository
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveResult
import io.ybigta.text2sql.infer.core.logic.table_refine.TableRefineLogic
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblSimiRepository
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblSimiRetrieveLogic
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblSimiRetrieveResult
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.Path

/**
 * @param dialect dialect name of sql that will be generated
 */
class Inferer(
    private val tblSimiRetrieveLogic: TblSimiRetrieveLogic,
    private val qaRetrieveLogic: QaRetrieveLogic,
    private val tableRefineLogic: TableRefineLogic,
    private val sqlGenerateLogic: SqlGenerateLogic,
) {
    /**
     * main entry point of text -> sql
     */
    suspend fun infer(question: Question): InferResult = coroutineScope {

        val retrievedTblList: Deferred<List<TblSimiRetrieveResult>> = async {
            val retrievedTables = tblSimiRetrieveLogic
                .retrieve(question)

            val selectedTables = retrievedTables
                .map { it.tableDesc }
                .let { tableDescList -> tableRefineLogic.refineTableDesc(question.question, tableDescList, "") }
                .let { selectedTableNames -> retrievedTables.filter { it.tableDesc.tableName in selectedTableNames } }

            selectedTables
        }

        val retrievedQaList: Deferred<List<QaRetrieveResult>> = async { qaRetrieveLogic.retrieve(question) }

        val generatedSql = sqlGenerateLogic.generateCode(
            question,
            retrievedTblList.await().map { it.tableDesc },
            retrievedQaList.await(),
        )

        return@coroutineScope InferResult(
            question = question.question,
            sql = generatedSql,
            tblRetrivedResults = retrievedTblList.await(),
            qaRetrieveResults = retrievedQaList.await()
        )
    }

    companion object {
        fun fromConfig(config: InferConfig) = Inferer(
            TblSimiRetrieveLogic(
                TblSimiRepository(
                    db = config.pgvector,
                    embeddingModel = config.embeddingModel
                )
            ),
            QaRetrieveLogic(
                QaRetrieveRepository(
                    db = config.pgvector,
                    embeddingModel = config.embeddingModel
                ),
                0.03F, // level 1 
                0.13F, // level 2
                0.15F, // level 3
                0.15F, // level 4
            ),
            TableRefineLogic(
                LLMEndpointBuilder.SqlGeneration.buildTableRefinementEndpoint(config),
            ),
            TableRefineLogic(
                LLMEndpointBuilder.SqlGeneration.buildTableRefinementEndpoint(config),
            ),
            SqlGenerateLogic(
                LLMEndpointBuilder.SqlGeneration.buildQuestionEntityExtractionEndpoint(config),
                "postgres"
            )
        )
    }
}

data class InferResult(
    val question: String,
    val sql: String,
    val tblRetrivedResults: List<TblSimiRetrieveResult>,
    val qaRetrieveResults: List<QaRetrieveResult>
)

fun main() {
    val config = Path
        .of("./.docs/infer_config.yaml")
        .let { InferConfig.fromConfigFile(it) }

    val inferer = Inferer.fromConfig(config)

    runBlocking(Dispatchers.IO) {
        val q = Question.fromConfig("개선대책담당자중 가장 많은 개선대책을 담당한 사람을 찾아줘", config)
        val result = inferer.infer(q)

        val resultJson = buildJsonObject {
            put("question", result.question)
            put("sql", result.sql)
            putJsonArray("retrieved_qa") {
                result.qaRetrieveResults.forEach { qa ->
                    addJsonObject {
                        put("distance", qa.dist)
                        put("question", qa.qa.question)
                    }
                }
            }
            putJsonArray("retrieved_tbl") {
                result.tblRetrivedResults.sortedBy { it.distance }.forEach { tbl ->
                    addJsonObject {
                        put("distance", tbl.distance)
                        put("embedding_category", tbl.embeddingCategory.name)
                        put("table", tbl.tableDesc.tableName.tableName)
                    }
                }
            }
        }

        Json { prettyPrint = true }
            .encodeToString(resultJson)
            .also { println(it) }

    }
}