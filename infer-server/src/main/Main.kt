package io.ybigta.text2sql.infer.server

import io.ktor.server.application.*
import io.ybigta.text2sql.infer.core.Inferer
import io.ybigta.text2sql.infer.core.Langchain4jLogger
import io.ybigta.text2sql.infer.core.config.InferConfig
import io.ybigta.text2sql.infer.server.config.pluginConfig
import io.ybigta.text2sql.infer.server.config.routeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newFixedThreadPoolContext
import java.nio.file.Path


fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)


fun Application.module() {
    // read config file
    val inferConfigFilePath = environment
        .config
        .property("infer-config.file-path")
        .getString()
        .let { Path.of(it) }

    val inferConfig = InferConfig.fromConfigFile(inferConfigFilePath, listOf(Langchain4jLogger()))

    // create class instances
    val inferService = InferService(
        Inferer.fromConfig(inferConfig),
        inferConfig
    )


    val inferRequestDispatcher = newFixedThreadPoolContext(30, "thread-pool")
    val inferRequestScope = CoroutineScope(inferRequestDispatcher + SupervisorJob())


    routeConfig(inferRequestScope, inferService)
    pluginConfig()
}

