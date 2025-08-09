package io.ybigta.text2sql.infer.server.config.route

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.controller.TblDocController

internal fun Application.configTblDocRoute(
    tblDocController: TblDocController
) = this.routing {
    get("/table-doc") { tblDocController.getAllTblDocs(call) }
    get("/table-doc/{table-doc-id}") { tblDocController.getTblDoc(call) }
    put("/table-doc") { tblDocController.upsertTblDoc(call) }
}