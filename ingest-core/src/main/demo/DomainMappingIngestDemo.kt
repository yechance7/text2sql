package io.ybigta.text2sql.ingest.demo

import io.ybigta.text2sql.ingest.DomainMappingIngester
import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.vectordb.TableDocTbl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import vectordb.DomainEntityMappingTbl
import vectordb.QaEmbeddingTbl
import vectordb.QaTbl
import java.nio.file.Path

fun main() {
    val configFilePath = Path.of("./.docs/ingest_config.yaml")
    val config = IngestConfig.fromConfigFile(configFilePath)

    val domainMappingIngester = DomainMappingIngester(config)

    transaction(config.pgvector) {
        SchemaUtils.create(QaTbl, TableDocTbl, QaEmbeddingTbl, DomainEntityMappingTbl)
    }

    runBlocking(Dispatchers.IO) {
        domainMappingIngester.ingest()
    }
}
