package io.ybigta.text2sql.ingest.vectordb.repositories

import io.ybigta.text2sql.ingest.logic.qa_ingest.StructuredQa
import io.ybigta.text2sql.ingest.vectordb.tables.QaTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

internal class QaRepository(
    private val db: Database,
) {
    fun insertAndGetId(question: String, answer: String, structuredQa: StructuredQa): Int = transaction(db) {
        QaTbl.insertAndGetId {
            it[QaTbl.question] = question
            it[QaTbl.answer] = answer
            it[QaTbl.structuredQa] = structuredQa
        }.value
    }

    fun find(question: String, answer: String): Pair<Int, StructuredQa> = transaction(db) {
        QaTbl
            .select(QaTbl.id, QaTbl.structuredQa)
            .andWhere { QaTbl.question eq question }
            .andWhere { QaTbl.answer eq answer }
            .limit(1)
            .single()
            .let { rw -> Pair(rw[QaTbl.id].value, rw[QaTbl.structuredQa]) }
    }

    fun findAll(): List<QaTbl.Dto> = transaction(db) {
        QaTbl
            .selectAll()
            .map { rw ->
                QaTbl.Dto(
                    rw[QaTbl.id].value,
                    rw[QaTbl.question],
                    rw[QaTbl.answer],
                    rw[QaTbl.structuredQa],
                )
            }
    }

}

