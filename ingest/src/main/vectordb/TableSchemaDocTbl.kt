package io.ybigta.text2sql.ingest.vectordb

import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb
import pgVector

object TableSchemaDocTbl : IntIdTable("table_schema_doc", "table_schema_doc_id") {
    val schema = varchar("schema_name", 255)
    val table = varchar("table_name", 255)
    val data = jsonb<TableSchemaJson>("data", Json)
    val embedding = pgVector("embedding", 1536) // openai ADA__002 embedding's dimension is 1536
}