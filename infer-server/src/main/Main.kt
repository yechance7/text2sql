package io.ybigta.text2sql.infer.server

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    routeConfig()
}

fun Application.routeConfig() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
