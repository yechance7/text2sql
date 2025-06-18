package io.ybigta.text2sql.infer.core

import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.core.config.LLMEndpointBuilder
import io.ybigta.text2sql.infer.core.logic.generate.SqlGenerateLogic
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveLogic
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveRepository
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveResult
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblSimiRepository
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblSimiRetrieveLogic
import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson
import kotlinx.coroutines.*
import java.nio.file.Path

/**
 * @param dialect dialect name of sql that will be generated
 */
class Inferer(
    private val tblSimiRetrieveLogic: TblSimiRetrieveLogic,
    private val qaRetrieveLogic: QaRetrieveLogic,
    private val sqlGenerateLogic: SqlGenerateLogic,
) {
    suspend fun infer(question: Question): String = coroutineScope {

        val retrievedTblList: Deferred<List<TableSchemaJson>> = async { tblSimiRetrieveLogic.retrieve(question) }
        val retrievedQaList: Deferred<List<QaRetrieveResult>> = async { qaRetrieveLogic.retrieve(question) }

        retrievedQaList
            .await()
            .onEach { println(it.qa.question) }

        retrievedTblList
            .await()
            .onEach { println(it) }

        val generatedSql = sqlGenerateLogic.generateCode(
            question,
            retrievedTblList.await(),
            retrievedQaList.await(),
        )

        return@coroutineScope generatedSql
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
                0.01F,
                0.01F,
                0.03F,
                0.03F,
            ),
            SqlGenerateLogic(
                LLMEndpointBuilder.SqlGeneration.buildQuestionEntityExtractionEndpoint(config),
                "postgres"
            )
        )
    }
}

fun main() {
    val config = Path
        .of("./.docs/infer_config.yaml")
        .let { InferConfig.fromConfigFile(it) }

    val inferer = Inferer.fromConfig(config)

    runBlocking(Dispatchers.IO) {
        val q = Question.fromConfig("개선대책담당자중 탈주한 사람을 찾아줘", config)
        val generatedSql = inferer.infer(q)
        println(generatedSql)
    }
}