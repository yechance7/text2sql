package io.ybigta.text2sql.infer.server.controller.model

import io.ybigta.text2sql.ingest.TableDesc
import io.ybigta.text2sql.ingest.TableDesc.Column
import io.ybigta.text2sql.ingest.TableDesc.TblRelation
import io.ybigta.text2sql.ingest.TableName
import kotlinx.serialization.Serializable

@Serializable
data class TblDescReq(
    val id: Int,
    val tableName: TableName,
    val embeddingSummary: String,
    val description: String,
    val connectedTables: List<TblRelation>,
    val columns: List<Column>,
    val entities: List<String>,
) {

    fun toTblDesc() = TableDesc(
        tableName = this.tableName,
        embeddingSummary = this.embeddingSummary,
        description = this.description,
        connectedTables = this.connectedTables,
        columns = this.columns,
        entities = this.entities
    )
}
