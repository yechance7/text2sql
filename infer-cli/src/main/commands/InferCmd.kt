package io.ybigta.text2sql.infer.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.ybigta.text2sql.infer.cli.Langchain4jLogger
import io.ybigta.text2sql.infer.core.InferResult
import io.ybigta.text2sql.infer.core.Inferer
import io.ybigta.text2sql.infer.core.Question
import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveResult
import io.ybigta.text2sql.infer.core.logic.table_retrieve.RetrieveCatgory
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblRetrieveResult
import io.ybigta.text2sql.ingest.TableName
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path

internal class InferCmd : CliktCommand("infer") {
    private val configFile: Path by option("-c", "--config", help = "path of ingest_config.yaml").path().required()
    private val question: String by option("-q", "--question", help = "input question").required()
    private val workerNum by option("-w", "--worker", help = "number of thread used for llm request").int().default(5)
    private val trace: Boolean by option("-t", "--trace").flag()
    private val withMetadata by option("-v", "--verbose").flag()

    private val dispatcher: ExecutorCoroutineDispatcher by lazy { newFixedThreadPoolContext(workerNum, "worker") }

    override fun run() = runBlocking(dispatcher) {
        val listeners = if (trace) listOf(Langchain4jLogger()) else emptyList()
        val inferConfig = InferConfig.fromConfigFile(configFile, listeners)
        val inferer = Inferer.fromConfig(inferConfig)

        val inferResult = inferer.infer(Question.fromConfig(question, inferConfig, dispatcher))
        if (withMetadata) echo(Json { prettyPrint = true }.encodeToString(InferResp.from(inferResult)))
        else echo(inferResult.sql)
    }
}

@Serializable
internal data class InferResp(
    val question: String,
    val sql: String,
    val tblRetriveResults: List<TblRetrieveResp>,
    val qaRetrieveResps: List<QaRetrieveResp>
) {
    companion object {
        fun from(inferResult: InferResult) = InferResp(
            question = inferResult.question,
            sql = inferResult.sql,
            tblRetriveResults = inferResult.tblRetrivedResults.map { TblRetrieveResp.from(it) },
            qaRetrieveResps = inferResult.qaRetrieveResults.map { QaRetrieveResp.from(it) }
        )
    }
}

@Serializable
internal data class TblRetrieveResp(
    val tableName: TableName,
    val embeddingCategory: RetrieveCatgory,
    val distance: Float?
) {
    companion object {
        fun from(tblRetrieve: TblRetrieveResult) = TblRetrieveResp(
            tableName = tblRetrieve.tableDesc.tableName,
            embeddingCategory = tblRetrieve.category,
            distance = tblRetrieve.distance
        )
    }
}

@Serializable
internal data class QaRetrieveResp(
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
