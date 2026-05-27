/** CORS-настройки из конфигурации cors.allowedHosts. */
package com.whatsmax.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    val allowedHostsCsv = runCatching {
        environment.config.config("cors").property("allowedHosts").getString()
    }.getOrDefault("")
    val hosts = allowedHostsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val appLog = this.log
    install(CORS) {
        if (hosts.isEmpty()) {
            appLog.warn("CORS: allowedHosts пустой — разрешаю anyHost(). Не для продакшна!")
            anyHost()
        } else {
            hosts.forEach { allowHost(it, schemes = listOf("https", "http")) }
        }
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
    }
}
