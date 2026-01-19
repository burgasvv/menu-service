package org.burgas.database

import org.burgas.service.DishTable
import org.burgas.service.IdentityDishTable
import org.burgas.service.IdentityTable
import org.burgas.service.MenuTable
import org.burgas.service.RestaurantTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun configureDatabases() {
    transaction(db = DatabaseFactory.postgres) {
        SchemaUtils.create(IdentityTable, RestaurantTable, MenuTable, DishTable, IdentityDishTable)
    }
}
