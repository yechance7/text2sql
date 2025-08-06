package io.ybigta.text2sql.infer.server.config

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

internal fun Application.configContentNegotiation() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }
}

internal fun Application.configCallLogging() {
    install(CallLogging) {
        level = Level.DEBUG

        format { call ->
            val statusCode = call.response.status() ?: "unhandled"
            val httpMethod = call.request.httpMethod
            val path = call.request.path()
            val ip = call.request.local.remoteHost
            val duration = call.processingTimeMillis()

            "(statusCode:$statusCode) (method:$httpMethod) (path:$path) (ip:$ip) (duration:${duration}ms)"
        }
    }
}
