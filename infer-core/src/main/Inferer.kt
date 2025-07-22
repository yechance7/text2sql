package io.ybigta.text2sql.infer.core

import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.core.config.LLMEndpointBuilder
import io.ybigta.text2sql.infer.core.logic.generate.SqlGenerateLogic
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveLogic
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveRepository
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveResult
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.TblRetrieveRepository
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblRetrieveLogic
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblRetrieveResult
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * @param dialect dialect name of sql that will be generated
 */
class Inferer(
    private val tblRetrieveLogic: TblRetrieveLogic,
    private val qaRetrieveLogic: QaRetrieveLogic,
    private val sqlGenerateLogic: SqlGenerateLogic,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * main entry point of text -> sql
     */
    suspend fun infer(question: Question): InferResult = coroutineScope {

        val retrievedTblList: Deferred<List<TblRetrieveResult>> = async { tblRetrieveLogic.retrieve(question) }
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
            TblRetrieveLogic(
                TblRetrieveRepository(
                    db = config.pgvector,
                    embeddingModel = config.embeddingModel
                ),
                LLMEndpointBuilder.SqlGeneration.buildTblSelectionAdjustEndpoint(config)
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
    val tblRetrivedResults: List<TblRetrieveResult>,
    val qaRetrieveResults: List<QaRetrieveResult>
)