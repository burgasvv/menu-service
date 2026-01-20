package org.burgas.service

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.burgas.database.DatabaseFactory
import org.burgas.serialization.UUIDSerializer
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection
import java.util.*

object DishTable : UUIDTable("dish") {
    val name  = varchar("name", 250)
    val about = text("about")
    val price = decimal("price", 10, 2)
    val menu = reference(
        "menu_id", MenuTable.id,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE
    )
}

class DishEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : EntityClass<UUID, DishEntity>(DishTable)

    var name by DishTable.name
    var about by DishTable.about
    var price by DishTable.price
    var menu by MenuEntity referencedOn DishTable.menu
    val identities by IdentityEntity via IdentityDishTable
}

@Serializable
data class DishRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val about: String? = null,
    val price: Double? = null,
    @Serializable(with = UUIDSerializer::class)
    val menuId: UUID? = null
)

@Serializable
data class DishShortResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val about: String? = null,
    val price: Double? = null
)

@Serializable
data class DishWithAmountResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val about: String? = null,
    val price: Double? = null,
    val amount: Int? = null,
    val menu: MenuWithRestaurantResponse? = null
)

@Serializable
data class DishFullResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val about: String? = null,
    val price: Double? = null,
    val menu: MenuWithRestaurantResponse? = null,
    val identities: List<IdentityShortResponse>? = null
)

fun DishEntity.toDishShortResponse(): DishShortResponse {
    return DishShortResponse(
        id = this.id.value,
        name = this.name,
        about = this.about,
        price = this.price.toDouble()
    )
}

fun DishEntity.toDishWithAmountResponse(identityId: UUID): DishWithAmountResponse {
    return DishWithAmountResponse(
        id = this.id.value,
        name = this.name,
        about = this.about,
        price = this.price.toDouble(),
        amount = IdentityDishTable.select(IdentityDishTable.fields)
            .where { (IdentityDishTable.identity eq identityId) and (IdentityDishTable.dish eq this.id.value) }
            .single()[IdentityDishTable.amount],
        menu = this.menu.toMenuWithRestaurantResponse()
    )
}

fun DishEntity.toDishFullResponse(): DishFullResponse {
    return DishFullResponse(
        id = this.id.value,
        name = this.name,
        about = this.about,
        price = this.price.toDouble(),
        menu = this.menu.toMenuWithRestaurantResponse(),
        identities = this.identities.map { identityEntity -> identityEntity.toIdentityShortResponse() }
    )
}

fun DishEntity.insert(dishRequest: DishRequest) {
    this.name = dishRequest.name ?: throw IllegalArgumentException("Dish name is null")
    this.about = dishRequest.about ?: throw IllegalArgumentException("Dish about is null")
    this.price = (dishRequest.price ?: throw IllegalArgumentException("Dish price is null")).toBigDecimal()
    this.menu = MenuEntity.findById(dishRequest.menuId ?: throw IllegalArgumentException("Dish menu id is null"))
        ?: throw IllegalArgumentException("Dish menu not found")
}

fun DishEntity.update(dishRequest: DishRequest) {
    this.name = dishRequest.name ?: this.name
    this.about = dishRequest.about ?: this.about
    this.price = dishRequest.price?.toBigDecimal() ?: this.price
}

class DishService {

    suspend fun findAll() = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, readOnly = true) {
            DishEntity.all().map { dishEntity -> dishEntity.toDishShortResponse() }
        }
    }

    suspend fun findById(dishId: UUID) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, readOnly = true) {
            (DishEntity.findById(dishId) ?: throw IllegalArgumentException("Dish not found"))
                .load(DishEntity::menu, DishEntity::identities)
                .toDishFullResponse()
        }
    }

    suspend fun create(dishRequest: DishRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            DishEntity.new { this.insert(dishRequest) }
        }
    }

    suspend fun update(dishRequest: DishRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            DishEntity.findByIdAndUpdate(dishRequest.id ?: throw IllegalArgumentException("Dish id is null")) {
                dishEntity -> dishEntity.update(dishRequest)
            }
        }
    }

    suspend fun delete(dishId: UUID) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            (DishEntity.findById(dishId) ?: throw IllegalArgumentException("Dish not found"))
                .delete()
        }
    }
}

fun Application.configureDishRoutes() {

    val dishService = DishService()

    routing {

        route("/api/v1/dishes") {

            get {
                call.respond(HttpStatusCode.OK, dishService.findAll())
            }

            get("/by-id") {
                val dishId = UUID.fromString(call.parameters["dishId"])
                call.respond(HttpStatusCode.OK, dishService.findById(dishId))
            }

            authenticate("basic-auth-admin") {

                post("/create") {
                    val dishRequest = call.receive(DishRequest::class)
                    dishService.create(dishRequest)
                    call.respond(HttpStatusCode.Created)
                }

                put("/update") {
                    val dishRequest = call.receive(DishRequest::class)
                    dishService.update(dishRequest)
                    call.respond(HttpStatusCode.OK)
                }

                delete("/delete") {
                    val dishId = UUID.fromString(call.parameters["dishId"])
                    dishService.delete(dishId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}