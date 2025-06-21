package io.ybigta.text2sql.ingest.vectordb.tables

import io.ybigta.text2sql.ingest.TableDesc
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb

object TableDocTbl : IntIdTable("table_doc", "table_doc_id") {
    val schema = varchar("schema_name", 255)
    val table = varchar("table_name", 255)
    val schemaJson = jsonb<TableDesc>("schema_json", Json)
}

