package org.burgas.service

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.burgas.database.DatabaseFactory
import org.burgas.plugin.UUIDSerializer
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.Connection
import java.util.*

object IdentityDishTable : Table("identity_dish") {
    val identity = reference(
        "identity_id", IdentityTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )
    val dish = reference(
        "dish_id", DishTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )
    val amount = integer("amount").default(0).check { column -> column.greaterEq(0) }
    override val primaryKey: PrimaryKey
        get() = PrimaryKey(arrayOf(identity, dish))
}

@Serializable
data class IdentityDishRequest(
    @Serializable(with = UUIDSerializer::class)
    val identityId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val dishId: UUID,
    val amount: Int
)

class IdentityDishService {

    suspend fun add(identityDishRequest: IdentityDishRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {

            val identityEntity = IdentityEntity.find { IdentityTable.id eq identityDishRequest.identityId }
                .forUpdate(ForUpdateOption.ForUpdate)
                .singleOrNull() ?: throw IllegalArgumentException("Identity not found")

            val dishEntity = DishEntity.find { DishTable.id eq identityDishRequest.dishId }
                .forUpdate(ForUpdateOption.ForUpdate)
                .singleOrNull() ?: throw IllegalArgumentException("Dish not found")

            val identityDish = IdentityDishTable.select(IdentityDishTable.fields)
                .where { (IdentityDishTable.identity eq identityEntity.id) and (IdentityDishTable.dish eq dishEntity.id) }
                .forUpdate(ForUpdateOption.ForUpdate)
                .singleOrNull()

            if (identityDish != null) {
                IdentityDishTable.update({ (IdentityDishTable.identity eq identityEntity.id) and (IdentityDishTable.dish eq dishEntity.id) })
                { updateStatement -> updateStatement[IdentityDishTable.amount] = identityDish[IdentityDishTable.amount] + identityDishRequest.amount }

            } else {
                IdentityDishTable.insert { insertStatement ->
                    insertStatement[IdentityDishTable.identity] = identityEntity.id
                    insertStatement[IdentityDishTable.dish] = dishEntity.id
                    insertStatement[IdentityDishTable.amount] = identityDishRequest.amount
                }
            }
        }
    }

    suspend fun remove(identityDishRequest: IdentityDishRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {

            val identityEntity = IdentityEntity.find { IdentityTable.id eq identityDishRequest.identityId }
                .forUpdate(ForUpdateOption.ForUpdate)
                .singleOrNull() ?: throw IllegalArgumentException("Identity not found")

            val dishEntity = DishEntity.find { DishTable.id eq identityDishRequest.dishId }
                .forUpdate(ForUpdateOption.ForUpdate)
                .singleOrNull() ?: throw IllegalArgumentException("Dish not found")

            val identityDish = IdentityDishTable.select(IdentityDishTable.fields)
                .where { (IdentityDishTable.identity eq identityEntity.id) and (IdentityDishTable.dish eq dishEntity.id) }
                .forUpdate(ForUpdateOption.ForUpdate)
                .singleOrNull()

            if (identityDish != null) {

                if (identityDishRequest.amount > identityDish[IdentityDishTable.amount]) {
                    throw IllegalArgumentException("Input dish amount is bigger than you have")
                }

                if (identityDishRequest.amount == identityDish[IdentityDishTable.amount]) {
                    IdentityDishTable
                        .deleteWhere { (IdentityDishTable.identity eq identityEntity.id) and (IdentityDishTable.dish eq dishEntity.id) }

                } else {
                    IdentityDishTable.update({ (IdentityDishTable.identity eq identityEntity.id) and (IdentityDishTable.dish eq dishEntity.id) })
                    { updateStatement -> updateStatement[IdentityDishTable.amount] = identityDish[IdentityDishTable.amount] - identityDishRequest.amount }
                }

            } else {
                throw IllegalArgumentException("Identity dish not found and can't be modified")
            }
        }
    }
}

fun Application.configureIdentityDishRoutes() {

    val identityDishService = IdentityDishService()

    routing {

        route("/api/v1/identity-dish") {

            authenticate("basic-auth-all") {

                post("/add") {
                    val identityDishRequest = call.receive(IdentityDishRequest::class)
                    identityDishService.add(identityDishRequest)
                    call.respond(HttpStatusCode.OK)
                }

                post("/remove") {
                    val identityDishRequest = call.receive(IdentityDishRequest::class)
                    identityDishService.remove(identityDishRequest)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}