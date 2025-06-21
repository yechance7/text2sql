package io.ybigta.text2sql.ingest.vectordb.repositories

import io.ybigta.text2sql.ingest.DomainEntitiyMapping
import io.ybigta.text2sql.ingest.vectordb.tables.DomainEntityMappingTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

class DomainEntityMappingRepository(
    private val db: Database,
) {
    fun insertAndGetId(qaId: Int, entityMapping: DomainEntitiyMapping): Int = transaction(db) {
        DomainEntityMappingTbl.insertAndGetId {
            it[DomainEntityMappingTbl.qaId] = qaId
            it[DomainEntityMappingTbl.entityMapping] = entityMapping
        }.value
    }
}