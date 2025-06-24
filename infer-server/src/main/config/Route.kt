package io.ybigta.text2sql.infer.server.config

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.InferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

import kotlinx.serialization.Serializable
import io.ybigta.text2sql.infer.server.InferResp 

@Serializable
data class BatchRequest(val questions: List<String>)

@Serializable
data class BatchResponse(val results: List<InferResp>)


internal fun Application.routeConfig(
    scope: CoroutineScope,
    inferService: InferService,
) {
    routing {
        post("/infer") {
            val question = call.receiveText()
            val inferResp = scope.async { inferService.infer(question) }
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

