package io.ybigta.text2sql.infer.server.config

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.InferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async


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
    }
}

