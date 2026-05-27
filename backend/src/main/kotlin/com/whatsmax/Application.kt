/** Точка входа Ktor-сервера. */
package com.whatsmax

import com.whatsmax.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureCORS()
    configureAuthentication()
    configureRateLimit()
    configureWebSockets()
    configureStatusPages()
    configureRouting()
}
