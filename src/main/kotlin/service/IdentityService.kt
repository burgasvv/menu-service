package org.burgas.service

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.apache.kafka.clients.producer.ProducerRecord
import org.burgas.database.DatabaseFactory
import org.burgas.kafka.KafkaProducer
import org.burgas.serialization.UUIDSerializer
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.util.*

@Suppress("unused")
enum class Authority {
    ADMIN, USER
}

object IdentityTable : UUIDTable("identity") {
    val authority = enumerationByName<Authority>("authority", 250)
    val username = varchar("username", 250).uniqueIndex()
    val password = varchar("password", 250)
    val email = varchar("email", 250).uniqueIndex()
    val enabled = bool("enabled")
    val firstname = varchar("firstname", 250)
    val lastname = varchar("lastname", 250)
    val patronymic = varchar("patronymic", 250)
}

class IdentityEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : EntityClass<UUID, IdentityEntity>(IdentityTable)

    var authority by IdentityTable.authority
    var username by IdentityTable.username
    var password by IdentityTable.password
    var email by IdentityTable.email
    var enabled by IdentityTable.enabled
    var firstname by IdentityTable.firstname
    var lastname by IdentityTable.lastname
    var patronymic by IdentityTable.patronymic
    val dishes by DishEntity via IdentityDishTable
}

@Serializable
data class IdentityRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val authority: Authority? = null,
    val username: String? = null,
    val password: String? = null,
    val email: String? = null,
    val enabled: Boolean? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val patronymic: String? = null
)

@Serializable
data class IdentityShortResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val username: String? = null,
    val email: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val patronymic: String? = null
)

@Serializable
data class IdentityFullResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val username: String? = null,
    val email: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val patronymic: String? = null,
    val dishes: List<DishWithAmountResponse>? = null
)

fun IdentityEntity.toIdentityShortResponse(): IdentityShortResponse {
    return IdentityShortResponse(
        id = this.id.value,
        username = this.username,
        email = this.email,
        firstname = this.firstname,
        lastname = this.lastname,
        patronymic = this.patronymic
    )
}

fun IdentityEntity.toIdentityFullResponse(): IdentityFullResponse {
    return IdentityFullResponse(
        id = this.id.value,
        username = this.username,
        email = this.email,
        firstname = this.firstname,
        lastname = this.lastname,
        patronymic = this.patronymic,
        dishes = this.dishes.map { dishEntity -> dishEntity.toDishWithAmountResponse(this.id.value) }
    )
}

fun IdentityEntity.insert(identityRequest: IdentityRequest) {
    this.authority = identityRequest.authority ?: throw IllegalArgumentException("Identity authority is null")
    this.username = identityRequest.username ?: throw IllegalArgumentException("Identity username is null")
    this.password = BCrypt.hashpw(
        identityRequest.password ?: throw IllegalArgumentException("Identity password is null"),
        BCrypt.gensalt()
    )
    this.email = identityRequest.email ?: throw IllegalArgumentException("Identity email is null")
    this.enabled = identityRequest.enabled ?: throw IllegalArgumentException("Identity enabled is null")
    this.firstname = identityRequest.firstname ?: throw IllegalArgumentException("Identity firstname is null")
    this.lastname = identityRequest.lastname ?: throw IllegalArgumentException("Identity lastname is null")
    this.patronymic = identityRequest.patronymic ?: throw IllegalArgumentException("Identity patronymic is null")
}

fun IdentityEntity.update(identityRequest: IdentityRequest) {
    this.authority = identityRequest.authority ?: this.authority
    this.username = identityRequest.username ?: this.username
    this.email = identityRequest.email ?: this.email
    this.enabled = identityRequest.enabled ?: this.enabled
    this.firstname = identityRequest.firstname ?: this.firstname
    this.lastname = identityRequest.lastname ?: this.lastname
    this.patronymic = identityRequest.patronymic ?: this.patronymic
}

class IdentityService {

    val kafkaProducer = KafkaProducer()

    suspend fun findAll() = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, readOnly = true) {
            IdentityEntity.all().map { identityEntity -> identityEntity.toIdentityShortResponse() }
        }
    }

    suspend fun findById(identityId: UUID) = withContext(Dispatchers.Default) {
        val identityFullResponse = transaction(db = DatabaseFactory.postgres, readOnly = true) {
            (IdentityEntity.findById(identityId) ?: throw IllegalArgumentException("Identity not found"))
                .load(IdentityEntity::dishes)
                .toIdentityFullResponse()
        }
        val producer = kafkaProducer.identityFullResponseKafkaProducer()
        producer.send(ProducerRecord("identity-kafka-topic", identityFullResponse))
        identityFullResponse
    }

    suspend fun create(identityRequest: IdentityRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            IdentityEntity.new { this.insert(identityRequest) }
        }
    }

    suspend fun update(identityRequest: IdentityRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            IdentityEntity.findByIdAndUpdate(
                identityRequest.id ?: throw IllegalArgumentException("Identity id is null")
            ) { identityEntity -> identityEntity.update(identityRequest) }
        }
    }

    suspend fun delete(identityId: UUID) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            (IdentityEntity.findById(identityId) ?: throw IllegalArgumentException("Identity not found"))
                .delete()
        }
    }

    suspend fun changePassword(identityRequest: IdentityRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            IdentityEntity.findByIdAndUpdate(
                identityRequest.id ?: throw IllegalArgumentException("Identity id is null")
            ) { identityEntity ->
                if (identityRequest.password == null) {
                    throw IllegalArgumentException("Identity password is null")
                }
                if (BCrypt.checkpw(identityRequest.password, identityEntity.password)) {
                    throw IllegalArgumentException("Passwords matched")
                }
                identityEntity.apply {
                    this.password = BCrypt.hashpw(identityRequest.password, BCrypt.gensalt())
                }
            }
        }
    }

    suspend fun changeStatus(identityRequest: IdentityRequest) = withContext(Dispatchers.Default) {
        transaction(db = DatabaseFactory.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) {
            IdentityEntity.findByIdAndUpdate(
                identityRequest.id ?: throw IllegalArgumentException("Identity id is null")
            ) { identityEntity ->
                if (identityRequest.enabled == null) {
                    throw IllegalArgumentException("Identity status is null")
                }
                if (identityRequest.enabled == identityEntity.enabled) {
                    throw IllegalArgumentException("Statuses matched")
                }
                identityEntity.apply {
                    this.enabled = identityRequest.enabled
                }
            }
        }
    }
}

fun Application.configureIdentityRoutes() {

    val identityService = IdentityService()

    routing {

        @Suppress("DEPRECATION")
        intercept(ApplicationCallPipeline.Call) {
            if (
                call.request.path().equals("/api/v1/identities/by-id", false) ||
                call.request.path().equals("/api/v1/identities/delete", false)
            ) {
                val principal = call.principal<UserPasswordCredential>()
                    ?: throw IllegalArgumentException("Principal not authenticated")
                val identityIdParam = UUID.fromString(call.parameters["identityId"])
                val identityEntity = transaction(db = DatabaseFactory.postgres, readOnly = true) {
                    IdentityEntity.findById(identityIdParam)
                        ?: throw IllegalArgumentException("Identity not authenticated")
                }
                if (identityEntity.email == principal.name) {
                    proceed()

                } else {
                    throw IllegalArgumentException("Identity not authorized")
                }

            } else if (
                call.request.path().equals("/api/v1/identities/update", false) ||
                call.request.path().equals("/api/v1/identities/change-status", false) ||
                call.request.path().equals("/api/v1/identities/change-password", false)
            ) {
                val principal = call.principal<UserPasswordCredential>()
                    ?: throw IllegalArgumentException("Principal not authenticated")
                val identityRequest = call.receive(IdentityRequest::class)
                val identityId = identityRequest.id ?: throw IllegalArgumentException("Identity id is null in filter")
                val identityEntity = transaction(db = DatabaseFactory.postgres, readOnly = true) {
                    IdentityEntity.findById(identityId) ?: throw IllegalArgumentException("Identity not authenticated")
                }
                if (identityEntity.email == principal.name) {
                    call.attributes[AttributeKey("identityRequest")] = identityRequest
                    proceed()

                } else {
                    throw IllegalArgumentException("Identity not authorized")
                }
            }

            proceed()
        }

        route("/api/v1/identities") {

            post("/create") {
                val identityRequest = call.receive(IdentityRequest::class)
                identityService.create(identityRequest)
                call.respond(HttpStatusCode.Created)
            }

            authenticate("basic-auth-admin") {

                get {
                    call.respond(HttpStatusCode.OK, identityService.findAll())
                }

                put("/change-status") {
                    val identityRequest = call.attributes[AttributeKey<IdentityRequest>("identityRequest")]
                    identityService.changeStatus(identityRequest)
                    call.respond(HttpStatusCode.OK)
                }
            }

            authenticate("basic-auth-all") {

                get("/by-id") {
                    val identityId = UUID.fromString(call.parameters["identityId"])
                    call.respond(HttpStatusCode.OK, identityService.findById(identityId))
                }

                put("/update") {
                    val identityRequest = call.attributes[AttributeKey<IdentityRequest>("identityRequest")]
                    identityService.update(identityRequest)
                    call.respond(HttpStatusCode.OK)
                }

                delete("/delete") {
                    val identityId = UUID.fromString(call.parameters["identityId"])
                    identityService.delete(identityId)
                    call.respond(HttpStatusCode.OK)
                }

                put("/change-password") {
                    val identityRequest = call.attributes[AttributeKey<IdentityRequest>("identityRequest")]
                    identityService.changePassword(identityRequest)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}