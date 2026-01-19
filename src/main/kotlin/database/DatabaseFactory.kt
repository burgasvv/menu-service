package org.burgas.database

import io.ktor.server.config.*
import org.jetbrains.exposed.v1.jdbc.Database

class DatabaseFactory {

    companion object {

        var applicationConfig: ApplicationConfig = ApplicationConfig("application.yaml")

        val postgres = Database.connect(
            url = applicationConfig.property("ktor.postgres.url").getString(),
            driver = applicationConfig.property("ktor.postgres.driver").getString(),
            user = applicationConfig.property("ktor.postgres.user").getString(),
            password = applicationConfig.property("ktor.postgres.password").getString()
        )
    }
}