package io.ybigta.text2sql.infer.server.config.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.route.InferController

internal fun Application.configInferRoute(
    inferController: InferController
) = this.routing {
    post("/infer") { inferController.infer(call) }
}
