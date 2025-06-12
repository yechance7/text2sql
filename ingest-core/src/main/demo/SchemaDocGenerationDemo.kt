package io.ybigta.text2sql.ingest.demo

import io.ybigta.text2sql.ingest.SchemaDocGenerator
import io.ybigta.text2sql.ingest.config.IngestConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.nio.file.Path

fun main() {
    val logger = LoggerFactory.getLogger("MAIN")


    val sourceDB = Database.connect(
        "jdbc:postgresql://13.209.199.83:5432/dxp",
        user = "ybigta",
        password = System.getenv("SOURCE_DB_PASS")!!
    )

    val configFilePath = Path.of("./.docs/ingest_config.yaml")
    val config = IngestConfig.fromConfigFile(configFilePath)

    val schemaDocGenerator = SchemaDocGenerator(config, sourceDB)

    runBlocking(Dispatchers.IO) {
        schemaDocGenerator.generate()
    }
}

