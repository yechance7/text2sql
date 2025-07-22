package io.ybigta.text2sql.infer.server.config

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.core.InferResp
import io.ybigta.text2sql.infer.server.InferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class BatchRequest(val questions: List<String>)

@Serializable
internal data class BatchResponse(val results: List<InferResp>)

@Serializable
internal data class InferRequest(
    val userId: String,
    val questions: String
)


internal fun Application.routeConfig(
    scope: CoroutineScope,
    inferService: InferService,
) {
    routing {
        post("/infer") {
            val inferRequest = call.receive<InferRequest>()
            val inferResp = scope.async { inferService.infer(Json.encodeToString(inferRequest)) }
            call.respond(inferResp.await())
        }
        post("/infer-batch") {
            val batchRequest = call.receive<BatchRequest>()

            val deferredResults = batchRequest.questions.map { question ->
                scope.async { inferService.infer(question) }
            }

            val results = deferredResults.map { it.await() }

            call.respond(BatchResponse(results))
        }
    }
}

