package io.ybigta.text2sql.infer.server.service

import io.ybigta.text2sql.infer.core.InferResp
import io.ybigta.text2sql.infer.core.Inferer
import io.ybigta.text2sql.infer.core.Question
import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.server.repository.QaGeneratedRepository
import io.ybigta.text2sql.infer.server.repository.QaStageStatus
import kotlinx.coroutines.CoroutineScope

internal class InferService(
    private val inferer: Inferer,
    private val inferConfig: InferConfig,
    private val scope: CoroutineScope,
    private val qaGeneratedRepo: QaGeneratedRepository
) {
    suspend fun infer(question: String): InferResp {
        val q = Question.fromConfig(question, inferConfig, scope)

        val inferResult = inferer.infer(q)

        val inferResp = InferResp.from(inferResult)

        qaGeneratedRepo.insert(
            inferResp.question,
            inferResp.sql,
            QaStageStatus.PENDING
        )

        return inferResp
    }
}