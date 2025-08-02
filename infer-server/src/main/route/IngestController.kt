package io.ybigta.text2sql.infer.server.route

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ybigta.text2sql.infer.server.app.model.TblDescReq
import io.ybigta.text2sql.infer.server.app.model.TblDescResp
import io.ybigta.text2sql.infer.server.repository.TblDocRepository
import io.ybigta.text2sql.ingest.SchemaIngester
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class IngestController(
    private val schemaIngester: SchemaIngester,
    private val tblDocRepo: TblDocRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    suspend fun getAllTblDocs(call: RoutingCall) {
        val resp = tblDocRepo
            .findAll()
            .map { (id, tableDesc) -> TblDescResp.fromTableDesc(id, tableDesc) }

        call.respond(resp)
    }

    suspend fun getTblDoc(call: RoutingCall) {
        val tblDocId = call.parameters["table-doc-id"]!!.let { it.toInt() }

        val docs = tblDocRepo.findById(tblDocId)

        if (docs != null) call.respond(docs)
        else call.respondText(text = "table-doc-id:${tblDocId} doesn't exists", status = HttpStatusCode.NotFound)
    }

    suspend fun upsertTblDoc(call: RoutingCall) {
        val tblDoc = call.receive<TblDescReq>()
        val ifExists = tblDocRepo.findById(tblDoc.id) != null

        if (ifExists) tblDocRepo.deleteTblDoc(tblDoc.id)

        schemaIngester.ingest(tblDoc.toTblDesc())

        call.respondText { "upserted ${tblDoc.tableName.schemaName}:${tblDoc.tableName.tableName}" }
    }

}