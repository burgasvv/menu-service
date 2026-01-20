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
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection
import java.util.*

object RestaurantTable : UUIDTable("restaurant") {
    val name = varchar("name", 250).uniqueIndex()
    val description = text("description").uniqueIndex()
}

class RestaurantEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : EntityClass<UUID, RestaurantEntity>(RestaurantTable)

    var name by RestaurantTable.name
    var description by RestaurantTable.description
    val menu by MenuEntity optionalBackReferencedOn MenuTable.restaurant
}

@Serializable
data class RestaurantRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class RestaurantShortResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class RestaurantFullResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val name: String? = null,
    val description: String? = null,
    val menu: MenuShortResponse? = null
)

fun RestaurantEntity.toRestaurantShortResponse(): RestaurantShortResponse {
    return RestaurantShortResponse(
        id = this.id.value,
        name = this.name,
        description = this.description
    )
}

fun RestaurantEntity.toRestaurantFullResponse(): RestaurantFullResponse {
    return RestaurantFullResponse(
        id = this.id.value,
        name = this.name,
        description = this.description,
        menu = this.menu?.toMenuShortResponse()
    )
}

fun RestaurantEntity.insert(restaurantRequest: RestaurantRequest) {
    this.name = restaurantRequest.name ?: throw IllegalArgumentException("Restaurant name is null")
    this.description = restaurantRequest.description ?: throw IllegalArgumentException("Restaurant description is null")
}

fun RestaurantEntity.update(restaurantRequest: RestaurantRequest) {
    this.name = restaurantRequest.name ?: this.name
    this.description = restaurantRequest.description ?: this.description
}

class RestaurantService {

    suspend fun findAll() = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, readOnly = true) {
            RestaurantEntity.all().map { restaurantEntity -> restaurantEntity.toRestaurantShortResponse() }
        }
    }

    suspend fun findById(restaurantId: UUID) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, readOnly = true) {
            (RestaurantEntity.findById(restaurantId) ?: throw IllegalArgumentException("Restaurant not found"))
                .load(RestaurantEntity::menu)
                .toRestaurantFullResponse()
        }
    }

    suspend fun create(restaurantRequest: RestaurantRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            RestaurantEntity.new { this.insert(restaurantRequest) }
        }
    }

    suspend fun update(restaurantRequest: RestaurantRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            RestaurantEntity.findByIdAndUpdate(restaurantRequest.id ?: throw IllegalArgumentException("Restaurant id is null")) {
                restaurantEntity -> restaurantEntity.update(restaurantRequest)
            }
        }
    }

    suspend fun delete(restaurantId: UUID) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            (RestaurantEntity.findById(restaurantId) ?: throw IllegalArgumentException("Restaurant not found"))
                .delete()
        }
    }
}

fun Application.configureRestaurantRoutes() {

    val restaurantService = RestaurantService()

    routing {

        route("/api/v1/restaurants") {

            get {
                call.respond(HttpStatusCode.OK, restaurantService.findAll())
            }

            get("/by-id") {
                val restaurantId = UUID.fromString(call.parameters["restaurantId"])
                call.respond(HttpStatusCode.OK, restaurantService.findById(restaurantId))
            }

            authenticate("basic-auth-admin") {

                post("/create") {
                    val restaurantRequest = call.receive(RestaurantRequest::class)
                    restaurantService.create(restaurantRequest)
                    call.respond(HttpStatusCode.Created)
                }

                put("/update") {
                    val restaurantRequest = call.receive(RestaurantRequest::class)
                    restaurantService.update(restaurantRequest)
                    call.respond(HttpStatusCode.OK)
                }

                delete("/delete") {
                    val restaurantId = UUID.fromString(call.parameters["restaurantId"])
                    restaurantService.delete(restaurantId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}