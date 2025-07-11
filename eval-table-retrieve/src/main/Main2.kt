package io.ybigta.text2sql.eval.table_retrieve

import io.ybigta.text2sql.infer.core.InferResult
import io.ybigta.text2sql.infer.core.Inferer
import io.ybigta.text2sql.infer.core.Question
import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveResult
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblSimiRetrieveResult
import io.ybigta.text2sql.ingest.TableName
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocEmbddingTbl.EmbeddingCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds

val inferConfig = InferConfig.fromConfigFile(Path.of("/home/gitp/gitp/progress/text2sql/.docs/infer_config.yaml"))
val inferer = Inferer.fromConfig(inferConfig)
val dataset = Path.of("/home/gitp/gitp/progress/text2sql/data/validate-set.json")
    .readText()
    .let { Json.decodeFromString<List<DataSet>>(it) }

fun main(): Unit = runBlocking(Dispatchers.IO) {
    val result = dataset.channelFlowMapAsync(1.seconds) { (question, sql) ->
        val q = Question.fromConfig(question, inferConfig)
        inferer.infer(q).let { InferResp.from(it, sql) }

    }

    result.toList()
        .let { Json { prettyPrint = true }.encodeToString(it) }
        .let { Path.of("./dataset1.json").toAbsolutePath().also { path -> path.createFile() }.writeText(it) }
}

// private fun InferResult.toJson(): JsonObject {
//
// }

@Serializable
data class DataSet(
    val question: String,
    val sql: String
)

@Serializable
data class InferResp(
    val question: String,
    val generatedSql: String,
    val answer: String,
    val tblRetriveResults: List<TblRetrieveResp>,
    val qaRetrieveResps: List<QaRetrieveResp>
) {
    companion object {
        fun from(inferResult: InferResult, answer: String) = InferResp(
            question = inferResult.question,
            generatedSql = inferResult.sql,
            answer = answer,
            tblRetriveResults = inferResult.tblRetrivedResults.map { TblRetrieveResp.from(it) },
            qaRetrieveResps = inferResult.qaRetrieveResults.map { QaRetrieveResp.from(it) }
        )
    }
}

@Serializable
data class TblRetrieveResp(
    val tableName: TableName,
    val embeddingCategory: EmbeddingCategory,
    val distance: Float
) {
    companion object {
        fun from(tblRetrieve: TblSimiRetrieveResult) = TblRetrieveResp(
            tableName = tblRetrieve.tableDesc.tableName,
            embeddingCategory = tblRetrieve.embeddingCategory,
            distance = tblRetrieve.distance
        )
    }
}

@Serializable
data class QaRetrieveResp(
    val question: String,
    val dist: Float,
    val searchLevel: Int
) {
    companion object {
        fun from(qaRetrieveResult: QaRetrieveResult) = QaRetrieveResp(
            question = qaRetrieveResult.qa.question,
            dist = qaRetrieveResult.dist,
            searchLevel = qaRetrieveResult.searchLevel
        )
    }
}

