package org.burgas.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.csrf.CSRF
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ExceptionResponse(
    val status: String? = null,
    val code: Int? = null,
    val message: String? = null
)

@Serializable
data class CsrfToken(val token: String)

fun Application.configureRouting() {

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val exception = ExceptionResponse(
                HttpStatusCode.BadRequest.description,
                HttpStatusCode.BadRequest.value,
                cause.localizedMessage
            )
            call.respond(exception)
        }
    }

    install(Sessions) {
        cookie<CsrfToken>("MY_CSRF")
    }

    install(CSRF) {
        allowOrigin("http://localhost:9000")
        originMatchesHost()
        checkHeader("X-CSRF-Token")
    }

    install(CORS) {
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowSameOrigin = true
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHost("localhost:4200", listOf("http", "https"))
    }

    routing {

        route("/api/v1/security") {

            get("/csrf-token") {
                var csrfToken = call.sessions.get(CsrfToken::class)
                if (csrfToken != null) {
                    call.respond(HttpStatusCode.OK, csrfToken)
                } else {
                    val uuidToken = UUID.randomUUID()
                    csrfToken = CsrfToken(uuidToken.toString())
                    call.sessions.set(csrfToken, CsrfToken::class)
                    call.respond(HttpStatusCode.OK, csrfToken)
                }
            }
        }
    }
}