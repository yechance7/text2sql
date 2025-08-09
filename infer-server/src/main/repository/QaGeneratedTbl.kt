package io.ybigta.text2sql.infer.server.repository

import io.ybigta.text2sql.infer.server.repository.utils.pgEnumeration
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

internal enum class QaStageStatus {
    PENDING, APPROVED, REJECTED
}

internal object QaGeneratedTbl : IntIdTable("qa_generated", "qa_generated_id") {
    val question = text("question")
    val sql = text("sql")
    val status = pgEnumeration<QaStageStatus>("status", "qa_stage_status")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}