package io.ybigta.text2sql.infer.server.config.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.controller.QaController


internal fun Application.configQaRoute(
    qaController: QaController
) = this.routing {
    patch("/generated-qa/{generated-qa-id}") { qaController.updateGeneratedQaStatus(call) }
}