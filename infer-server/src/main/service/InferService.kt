package io.ybigta.text2sql.infer.server.service

import io.ybigta.text2sql.infer.core.InferResp
import io.ybigta.text2sql.infer.core.Inferer
import io.ybigta.text2sql.infer.core.Question
import io.ybigta.text2sql.infer.core.config.InferConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope

internal class InferService(
    private val inferer: Inferer,
    private val inferConfig: InferConfig,
    private val scope: CoroutineScope
) {
    suspend fun infer(question: String): InferResp = coroutineScope {
        val q = Question.fromConfig(question, inferConfig, scope)

        val inferResult = inferer.infer(q)

        val inferResp = InferResp.from(inferResult)

        return@coroutineScope inferResp
    }
}