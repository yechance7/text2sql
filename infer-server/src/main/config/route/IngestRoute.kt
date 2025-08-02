package io.ybigta.text2sql.infer.server.config.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.route.IngestController

internal fun Application.configIngestRoute(
    ingestController: IngestController
) = this.routing {
    get("/table-doc") { ingestController.getAllTblDocs(call) }
    get("/table-doc/{table-doc-id}") { ingestController.getTblDoc(call) }
    put("/table-doc") { ingestController.upsertTblDoc(call) }
}