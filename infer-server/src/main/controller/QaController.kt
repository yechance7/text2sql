package io.ybigta.text2sql.infer.server.controller

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.repository.QaGeneratedRepository
import io.ybigta.text2sql.infer.server.repository.QaStageStatus
import io.ybigta.text2sql.ingest.Qa
import io.ybigta.text2sql.ingest.QaIngester
import io.ybigta.text2sql.ingest.vectordb.repositories.QaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

internal class QaController(
    private val scope: CoroutineScope,
    private val qaIngester: QaIngester,
    private val qaGeneratedRepository: QaGeneratedRepository,
    private val qaRepository: QaRepository
) {
    /**
     * 수정하려는 generated_qa의 status 값이  [QaStageStatus.PENDING] 여야한다
     * req.status가
     * APPROVED:  테이블 값 없데이트 후 해당 generated_qa를 `qa` 테이블로 복사해서 text2sql 수행시 참고 qa쌍으로
     * 사용될 수 있게 함
     * REJECTED: 그냥 테이블 값만 없데이트
     * PENDING: 이 값은 요청값으로 울 수 없음
     */

    suspend fun updateGeneratedQaStatus(call: RoutingCall) {
        val generatedQaId = call.pathParameters["generated-qa-id"]!!.toInt()
        val updateStatus = call.receive<QaStageStatus>()


        if (updateStatus == QaStageStatus.PENDING) throw IllegalArgumentException("updating status to QaStageStatus.PENDING is not allowed")

        val generatedQa = qaGeneratedRepository.findByIdOrNull(generatedQaId) ?: throw IllegalArgumentException()

        if (generatedQa.status != QaStageStatus.PENDING) {
            call.respondText("status should be pending")
            return
        }


        qaGeneratedRepository.updateStatus(generatedQaId, updateStatus)

        if (updateStatus == QaStageStatus.APPROVED) {
            withContext(scope.coroutineContext) {
                qaIngester.ingest(listOf(Qa(generatedQa.question, generatedQa.sql)))
            }
        }

        call.respondText("ok")
    }
}