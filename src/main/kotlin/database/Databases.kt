package org.burgas.database

import org.burgas.service.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
fun configureDatabases() {

    transaction(db = DatabaseFactory.postgres) {
        SchemaUtils.create(IdentityTable, RestaurantTable, MenuTable, DishTable, IdentityDishTable)

        val parse = Uuid.parse("0b0a1bf5-a806-4d66-885b-0d6255a2746c")
        val identityId = parse.toJavaUuid()
        IdentityEntity.findById(identityId) ?:
        IdentityEntity.new(identityId) {
            this.authority = Authority.ADMIN
            this.username = "burgasvv"
            this.password = BCrypt.hashpw("burgasvv", BCrypt.gensalt())
            this.email = "burgasvv@gmail.com"
            this.enabled = true
            this.firstname = "Бургас"
            this.lastname = "Вячеслав"
            this.patronymic = "Васильевич"
        }
    }
}
