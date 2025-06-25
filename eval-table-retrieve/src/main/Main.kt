package io.ybigta.text2sql.eval.table_retrieve

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.ybigta.text2sql.infer.core.Question
import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.core.config.LLMEndpointBuilder
import io.ybigta.text2sql.infer.core.logic.table_refine.TableRefineLogic
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblSimiRepository
import io.ybigta.text2sql.infer.core.logic.table_retrieve.TblSimiRetrieveLogic
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocEmbddingTbl.EmbeddingCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


@Serializable
internal data class InputData(
    val question: String,
    val tables: List<String>
)

@Serializable
data class RetrieveResult(
    val table: String,
    val embeddingCategory: EmbeddingCategory,
    val distance: Float
)

@Serializable
internal data class OutputData(
    val question: String,
    val answerTables: List<String>,
    val retrievedTables: List<RetrieveResult>,
)

internal class EvalTableRetrieveCmd() : CliktCommand("eval-table-retrieve") {
    private val inferConfigFile: Path by option("-c", "--config", help = "path of ingest_config.yaml").path().required()
    private val datasetFile: Path by option("-d", "--dataset", help = "path of dataset.json").path().required()
    private val outputFile: Path by option("-o", "--out", help = "path of dataset.json").path().required()


    override fun run(): Unit = runBlocking(Dispatchers.IO) {
        val inferConfig = InferConfig.fromConfigFile(inferConfigFile)
        val datasetFile = readDatasetFile(datasetFile)

        val tblRetrieveLogic = TblSimiRetrieveLogic(
            TblSimiRepository(
                db = inferConfig.pgvector,
                embeddingModel = inferConfig.embeddingModel
            )
        )

        val tableRefineLogic = TableRefineLogic(
            LLMEndpointBuilder.SqlGeneration.buildTableRefinementEndpoint(inferConfig),
        )

        val fuckingOutputs = runBlocking(Dispatchers.IO) {
            datasetFile
                .channelFlowMapAsync(100.milliseconds) { data ->
                    val resp = Question.fromConfig(data.question, inferConfig)
                        .let { tblRetrieveLogic.retrieve(it) }
                        .let { retrievedTables ->
                            val selectedTables = retrievedTables
                                .map { it.tableDesc }
                                .let { tableDescList -> tableRefineLogic.refineTableDesc(data.question, tableDescList, "") }
                                .let { selectedTableNames -> retrievedTables.filter { it.tableDesc.tableName in selectedTableNames } }

                            selectedTables
                        }
                    val output = OutputData(
                        question = data.question,
                        answerTables = data.tables,
                        retrievedTables = resp.map {
                            RetrieveResult(
                                table = it.tableDesc.tableName.let { (schemaName, tableName) -> "$schemaName.$tableName" },
                                embeddingCategory = it.embeddingCategory,
                                distance = it.distance
                            )
                        }
                    )
                    output
                }
                .toList()
        }

        fuckingOutputs
            .let { Json { prettyPrint = true }.encodeToString(it) }
            .also { resultContent -> outputFile.writeText(resultContent) }
    }
}

fun main(args: Array<String>) = EvalTableRetrieveCmd().main(args)


internal fun readDatasetFile(path: Path): List<InputData> = path
    .readText()
    .let { Json.decodeFromString<List<InputData>>(it) }

internal fun <T, R> Iterable<T>.channelFlowMapAsync(
    interval: Duration,
    transform: suspend (T) -> R
): Flow<R> = channelFlow {
    this@channelFlowMapAsync.forEach { element ->
        launch {
            send(transform(element))
        }
        delay(interval)
    }
}
