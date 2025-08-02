package io.ybigta.text2sql.infer.server.route

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.route.model.InferReq
import io.ybigta.text2sql.infer.server.service.InferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json

internal class InferController(
    private val scope: CoroutineScope,
    private val inferService: InferService,
) {
    suspend fun infer(call: RoutingCall) {
        val inferReq = call.receive<InferReq>()

        val inferResp = scope.async {
            inferService.infer(Json.encodeToString(inferReq))
        }

        call.respond(inferResp.await())
    }
}