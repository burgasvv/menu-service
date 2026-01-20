package org.burgas.service

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.burgas.database.DatabaseFactory
import org.burgas.serialization.UUIDSerializer
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection
import java.util.*

object MenuTable : UUIDTable("menu") {
    val name = varchar("name", 250)
    val description = text("description")
    val restaurant = reference(
        "restaurant_id", RestaurantTable.id,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE
    )
        .uniqueIndex()
}

class MenuEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : EntityClass<UUID, MenuEntity>(MenuTable)

    var name by MenuTable.name
    var description by MenuTable.description
    var restaurant by RestaurantEntity referencedOn MenuTable.restaurant
    val dishes by DishEntity referrersOn DishTable.menu
}

@Serializable
data class MenuRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val description: String? = null,
    @Serializable(with = UUIDSerializer::class)
    val restaurantId: UUID? = null
)

@Serializable
data class MenuShortResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class MenuWithRestaurantResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val description: String? = null,
    val restaurant: RestaurantShortResponse? = null
)

@Serializable
data class MenuFullResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val description: String? = null,
    val restaurant: RestaurantShortResponse? = null,
    val dishes: List<DishShortResponse>? = null
)

fun MenuEntity.toMenuShortResponse(): MenuShortResponse {
    return MenuShortResponse(
        id = this.id.value,
        name = this.name,
        description = this.description
    )
}

fun MenuEntity.toMenuWithRestaurantResponse(): MenuWithRestaurantResponse {
    return MenuWithRestaurantResponse(
        id = this.id.value,
        name = this.name,
        description = this.description,
        restaurant = this.restaurant.toRestaurantShortResponse()
    )
}

fun MenuEntity.toMenuFullResponse(): MenuFullResponse {
    return MenuFullResponse(
        id = this.id.value,
        name = this.name,
        description = this.description,
        restaurant = this.restaurant.toRestaurantShortResponse(),
        dishes = this.dishes.map { dishEntity -> dishEntity.toDishShortResponse() }
    )
}

fun MenuEntity.insert(menuRequest: MenuRequest) {
    this.name = menuRequest.name ?: throw IllegalArgumentException("Menu name is null")
    this.description = menuRequest.description ?: throw IllegalArgumentException("Menu description is null")
    this.restaurant = RestaurantEntity.findById(
        menuRequest.restaurantId ?: throw IllegalArgumentException("Menu restaurantId is null")
    )
        ?: throw IllegalArgumentException("Menu restaurant not found")
}

fun MenuEntity.update(menuRequest: MenuRequest) {
    this.name = menuRequest.name ?: this.name
    this.description = menuRequest.description ?: this.description
    this.restaurant = RestaurantEntity.findById(menuRequest.restaurantId ?: UUID.randomUUID()) ?: this.restaurant
}

class MenuService {

    suspend fun findAll() = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, readOnly = true) {
            MenuEntity.all().map { menuEntity -> menuEntity.toMenuShortResponse() }
        }
    }

    suspend fun findById(menuId: UUID) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, readOnly = true) {
            (MenuEntity.findById(menuId) ?: throw IllegalArgumentException("Menu not found"))
                .load(MenuEntity::restaurant, MenuEntity::dishes)
                .toMenuFullResponse()
        }
    }

    suspend fun create(menuRequest: MenuRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            MenuEntity.new { this.insert(menuRequest) }
        }
    }

    suspend fun update(menuRequest: MenuRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            MenuEntity.findByIdAndUpdate(
                menuRequest.id ?: throw IllegalArgumentException("Manu id is null")
            ) { menuEntity ->
                menuEntity.update(menuRequest)
            }
        }
    }

    suspend fun delete(menuId: UUID) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            (MenuEntity.findById(menuId) ?: throw IllegalArgumentException("Menu not found"))
                .delete()
        }
    }
}

fun Application.configureMenuRoutes() {

    val menuService = MenuService()

    routing {

        route("/api/v1/menu") {

            get {
                call.respond(HttpStatusCode.OK, menuService.findAll())
            }

            get("/by-id") {
                val menuId = UUID.fromString(call.parameters["menuId"])
                call.respond(HttpStatusCode.OK, menuService.findById(menuId))
            }

            authenticate("basic-auth-admin") {

                post("/create") {
                    val menuRequest = call.receive(MenuRequest::class)
                    menuService.create(menuRequest)
                    call.respond(HttpStatusCode.Created)
                }

                put("/update") {
                    val menuRequest = call.receive(MenuRequest::class)
                    menuService.update(menuRequest)
                    call.respond(HttpStatusCode.OK)
                }

                delete("/delete") {
                    val menuId = UUID.fromString(call.parameters["menuId"])
                    menuService.delete(menuId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}