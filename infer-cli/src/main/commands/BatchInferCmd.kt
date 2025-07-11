package io.ybigta.text2sql.infer.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import io.ybigta.text2sql.infer.cli.channelFlowMapAsync
import io.ybigta.text2sql.infer.cli.readJson
import io.ybigta.text2sql.infer.cli.writeJson
import io.ybigta.text2sql.infer.core.Inferer
import io.ybigta.text2sql.infer.core.Question
import io.ybigta.text2sql.infer.core.config.InferConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

@Serializable
private data class OutputSpec(
    val question: String,
    val sql: String
)

class BatchInferCmd() : CliktCommand("batch-infer") {
    private val configFile: Path by option("-c", "--config", help = "path of ingest_config.yaml").path().required()
    private val inputFile: Path by option("-i", "--input", help = "path of input file").path().required()
    private val resultFile: Path by option("-r", "--result", help = "path of result file").path().required()
    private val interval by option("-t", "--interval", help = "interval(ms) of llm request").long().default(2000L)
    private val workerNum by option("-w", "--worker", help = "number of thread used for llm request").int().default(10)

    private val dispatcher by lazy { newFixedThreadPoolContext(workerNum, "worker") }

    private val logger = LoggerFactory.getLogger(this::class.java)
    override fun run() = runBlocking(dispatcher) {

        val questions = readJson<List<String>>(inputFile, logger)!!
        val inferConfig = InferConfig.fromConfigFile(configFile)
        val inferer = Inferer.fromConfig(inferConfig)

        questions
            .channelFlowMapAsync(interval.milliseconds) { question ->
                val inferResult = inferer.infer(Question.fromConfig(question, inferConfig, dispatcher))
                OutputSpec(
                    question = question,
                    sql = inferResult.sql
                )
            }
            .toList()
            .let { writeJson(it, resultFile, logger) }
    }
}