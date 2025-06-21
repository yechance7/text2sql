package io.ybigta.text2sql.ingest.vectordb.tables

import io.ybigta.text2sql.ingest.DomainEntitiyMapping
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb

internal object DomainEntityMappingTbl : IntIdTable("domain_entity_mapping", "domain_entity_mapping_id") {
    val qaId = reference("qa_id", QaTbl)
    val entityMapping = jsonb<DomainEntitiyMapping>("entity_mapping", Json)
}