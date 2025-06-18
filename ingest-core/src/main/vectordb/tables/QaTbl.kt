package io.ybigta.text2sql.ingest.vectordb.tables

import io.ybigta.text2sql.ingest.logic.qa_ingest.StructuredQa
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb

object QaTbl : IntIdTable("qa", "qa_id") {
    val question = text("question")
    val answer = text("answer")
    val structuredQa = jsonb<StructuredQa>("structured_qa", Json)

    data class Dto(
        val id: Int,
        val question: String,
        val answer: String,
        val structuredQa: StructuredQa
    )
}

