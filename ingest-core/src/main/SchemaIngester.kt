package io.ybigta.text2sql.ingest

import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.vectordb.repositories.TableDocEmbeddingRepository
import io.ybigta.text2sql.ingest.vectordb.repositories.TableDocRepository
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * read table information from makrdown documentations then insert into vectordb.
 */
class SchemaIngester(
    private val schemaDocRepository: TableDocRepository,
    private val tableDocEmbeddingRepository: TableDocEmbeddingRepository,
) {
    companion object {
        fun fromConfig(ingestConfig: IngestConfig) = SchemaIngester(
            schemaDocRepository = TableDocRepository(ingestConfig.pgvector, ingestConfig.embeddingModel),
            tableDocEmbeddingRepository = TableDocEmbeddingRepository(ingestConfig.pgvector, ingestConfig.embeddingModel),
        )
    }

    private val logger = LoggerFactory.getLogger(this::class.java)


    suspend fun ingest(tableDesc: TableDesc) {
        val id = schemaDocRepository.insertAndGetId(tableDesc)
        tableDocEmbeddingRepository.insertAllCategories(id, tableDesc)
    }
}

@Serializable
data class TableName(
    val schemaName: String,
    val tableName: String
)

@Serializable
data class TableDesc(
    val tableName: TableName,
    val purpose: String,
    val summary: String,
    val dependenciesThought: String,
    val keys: String,
    val connectedTables: List<TableName>,
    val columns: List<Column>,
    val strongEntities: List<String>,
    val weakEntities: List<String>,
) {
    @Serializable
    data class Column(
        val column: String,
        val description: String
    )

    fun toDescription(): String = """
    table: ${this.tableName}
    
    # description
    ${this.summary}
    ${this.purpose}
    """.trimIndent()

    fun toDescriptionWithDependencies(): String = """
    table: ${this.tableName}
    
    # description
    ${this.summary}
    ${this.purpose}
    ${this.dependenciesThought}
    """.trimIndent()
}
