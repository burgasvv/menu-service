package org.burgas.service

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
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
                { updateStatement ->
                    updateStatement[IdentityDishTable.amount] =
                        identityDish[IdentityDishTable.amount] + identityDishRequest.amount
                }

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
                    { updateStatement ->
                        updateStatement[IdentityDishTable.amount] =
                            identityDish[IdentityDishTable.amount] - identityDishRequest.amount
                    }
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

        @Suppress("DEPRECATION")
        intercept(ApplicationCallPipeline.Call) {
            if (
                call.request.path().equals("/api/v1/identity-dish/add", false) ||
                call.request.path().equals("/api/v1/identity-dish/remove", false)
            ) {
                val principal = call.principal<UserPasswordCredential>()
                    ?: throw IllegalArgumentException("Principal not authenticated")
                val identityDishRequest = call.receive(IdentityDishRequest::class)
                val identityEntity = transaction(db = DatabaseFactory.postgres, readOnly = true) {
                    IdentityEntity.findById(identityDishRequest.identityId)
                        ?: throw IllegalArgumentException("Identity not authenticated")
                }

                if (identityEntity.email == principal.name) {
                    call.attributes[AttributeKey("identityDishRequest")] = identityDishRequest
                    proceed()

                } else {
                    throw IllegalArgumentException("Identity not authorized")
                }
            }
            proceed()
        }

        route("/api/v1/identity-dish") {

            authenticate("basic-auth-all") {

                post("/add") {
                    val identityDishRequest = call
                        .attributes[AttributeKey<IdentityDishRequest>("identityDishRequest")]
                    identityDishService.add(identityDishRequest)
                    call.respond(HttpStatusCode.OK)
                }

                post("/remove") {
                    val identityDishRequest = call
                        .attributes[AttributeKey<IdentityDishRequest>("identityDishRequest")]
                    identityDishService.remove(identityDishRequest)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}