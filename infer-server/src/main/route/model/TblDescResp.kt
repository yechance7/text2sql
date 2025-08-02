package io.ybigta.text2sql.infer.server.app.model

import io.ybigta.text2sql.ingest.TableDesc
import io.ybigta.text2sql.ingest.TableDesc.Column
import io.ybigta.text2sql.ingest.TableDesc.TblRelation
import io.ybigta.text2sql.ingest.TableName
import kotlinx.serialization.Serializable

@Serializable
data class TblDescResp(
    val id: Int,
    val tableName: TableName,
    val embeddingSummary: String,
    val description: String,
    val connectedTables: List<TblRelation>,
    val columns: List<Column>,
    val entities: List<String>,
) {
    companion object {
        fun fromTableDesc(id: Int, tableDesc: TableDesc) = TblDescResp(
            id = id,
            tableName = tableDesc.tableName,
            embeddingSummary = tableDesc.embeddingSummary,
            description = tableDesc.description,
            connectedTables = tableDesc.connectedTables,
            columns = tableDesc.columns,
            entities = tableDesc.entities
        )
    }
}
