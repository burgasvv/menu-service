package org.burgas.database

import org.burgas.service.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun configureDatabases() {
    transaction(db = DatabaseFactory.postgres) {
        SchemaUtils.create(IdentityTable, RestaurantTable, MenuTable, DishTable, IdentityDishTable)
    }
}
