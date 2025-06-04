package io.ybigta.text2sql.ingest.demo

import io.ybigta.text2sql.ingest.QaIngester
import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.vectordb.TableDocTbl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import vectordb.QaEmbeddingTbl
import vectordb.QaTbl
import java.nio.file.Path


fun main() {
    val configFilePath = Path.of("./.docs/ingest_config.yaml")
    val config = IngestConfig.fromConfigFile(configFilePath)

    val qaIngester = QaIngester(config)

    transaction(config.pgvector) {
        SchemaUtils.create(QaTbl, TableDocTbl, QaEmbeddingTbl)
    }

    runBlocking(Dispatchers.IO) {
        qaIngester.ingest()
    }
}
