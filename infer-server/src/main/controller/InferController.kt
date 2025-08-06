package io.ybigta.text2sql.infer.server.controller

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.controller.model.InferReq
import io.ybigta.text2sql.infer.server.service.InferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

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
}