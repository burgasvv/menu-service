package org.burgas

import io.ktor.server.application.*
import org.burgas.database.configureDatabases
import org.burgas.plugin.configureAuthentication
import org.burgas.plugin.configureRouting
import org.burgas.plugin.configureSerialization
import org.burgas.service.configureDishRoutes
import org.burgas.service.configureIdentityDishRoutes
import org.burgas.service.configureIdentityRoutes
import org.burgas.service.configureMenuRoutes
import org.burgas.service.configureRestaurantRoutes

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureRouting()
    configureAuthentication()

    configureIdentityRoutes()
    configureRestaurantRoutes()
    configureMenuRoutes()
    configureDishRoutes()
    configureIdentityDishRoutes()
}
