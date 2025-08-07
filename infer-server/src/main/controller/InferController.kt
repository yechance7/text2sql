// InferController.kt
package io.ybigta.text2sql.infer.server.controller

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.core.InferResp
import io.ybigta.text2sql.infer.server.controller.model.InferReq
import io.ybigta.text2sql.infer.server.service.InferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
internal data class BatchInferReq(
    val userId: String,
    val questions: List<String>
)

@Serializable
internal data class BatchResponse(
    val results: List<InferResp>
)

internal class InferController(
    private val scope: CoroutineScope,
    private val inferService: InferService,
) {
    suspend fun infer(call: RoutingCall) {
        val inferReq = call.receive<InferReq>()

        val inferResp = withContext(scope.coroutineContext) {
            inferService.infer(inferReq.question)
        }

        call.respond(inferResp)
    }

    suspend fun batchInfer(call: RoutingCall) {
        val batchRequest = call.receive<BatchInferReq>()

        val deferredResults = batchRequest.questions.map { question ->
            scope.async { inferService.infer(question) }
        }

        val results = deferredResults.map { it.await() }

        call.respond(BatchResponse(results))
    }
}