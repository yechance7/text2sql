package io.ybigta.text2sql.infer.server.repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

internal class QaGeneratedRepository(
    private val db: Database,
) {
    fun insert(question: String, sql: String, status: QaStageStatus): Int = transaction(db) {
        QaGeneratedTbl.insertAndGetId {
            it[QaGeneratedTbl.question] = question
            it[QaGeneratedTbl.sql] = sql
            it[QaGeneratedTbl.status] = status
        }.value
    }

    fun updateStatus(qaGeneratedId: Int, status: QaStageStatus) = transaction(db) {
        val updatedRows = QaGeneratedTbl.update(where = { QaGeneratedTbl.id eq qaGeneratedId }) {
            it[QaGeneratedTbl.status] = status
        }

        if (updatedRows != 1) throw IllegalStateException("excpected exactly one row with id=$qaGeneratedId exists, but found none or multiple")
    }

    fun findByIdOrNull(id: Int): QaGeneratedTbl.Dto? = transaction(db) {
        QaGeneratedTbl
            .select(
                QaGeneratedTbl.question,
                QaGeneratedTbl.sql,
                QaGeneratedTbl.status,
            )
            .where { QaGeneratedTbl.id eq id }
            .singleOrNull()
            ?.let { rr ->
                QaGeneratedTbl.Dto(
                    rr[QaGeneratedTbl.question],
                    rr[QaGeneratedTbl.sql],
                    rr[QaGeneratedTbl.status],
                )
            }
    }
}