package io.ybigta.text2sql.infer.core.vectordb

import dev.langchain4j.model.embedding.EmbeddingModel
import io.ybigta.text2sql.exposed.pgvector.cosDist
import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocEmbddingTbl
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.transactions.transaction