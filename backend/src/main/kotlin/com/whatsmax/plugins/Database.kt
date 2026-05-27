/** Инициализация PostgreSQL через HikariCP + Exposed. */
package com.whatsmax.plugins

import com.whatsmax.data.database.DatabaseFactory
import io.ktor.server.application.*

fun Application.configureDatabase() {
    val dbConfig = environment.config.config("database")
    DatabaseFactory.init(
        url      = dbConfig.property("url").getString(),
        driver   = dbConfig.property("driver").getString(),
        user     = dbConfig.property("user").getString(),
        password = dbConfig.property("password").getString(),
        maxPool  = dbConfig.property("maxPoolSize").getString().toInt()
    )
    log.info("Database connected and tables created")
}
