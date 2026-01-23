package router

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.basicAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.burgas.module
import org.burgas.security.CsrfToken
import org.burgas.service.Authority
import org.burgas.service.IdentityFullResponse
import org.burgas.service.IdentityRequest
import org.burgas.service.IdentityShortResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class IdentityRouterTest {

    @Test
    fun `test identity router endpoints`() = testApplication {
        this.application {
            module()
        }
        val httpClient = this.createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val csrfToken = httpClient.get("/api/v1/security/csrf-token") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
            .body<CsrfToken>()

        httpClient.post("/api/v1/identities/create") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(IdentityRequest(
                authority = Authority.USER,
                username = "user",
                password = "user",
                email = "user@gmail.com",
                enabled = true,
                firstname = "User",
                lastname = "User",
                patronymic = "User"
            ))
        }
            .apply {
                assertEquals(HttpStatusCode.Created, status)
            }

        val identityShortResponse = httpClient.get("/api/v1/identities") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            basicAuth("burgasvv@gmail.com", "burgasvv")
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            .body<List<IdentityShortResponse>>()
            .first { first -> first.email == "user@gmail.com" }

        val identityFullResponse = httpClient.get("/api/v1/identities/by-id") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            parameter("identityId", identityShortResponse.id.toString())
            basicAuth("user@gmail.com", "user")
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            .body<IdentityFullResponse>()

        httpClient.delete("/api/v1/identities/delete") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            parameter("identityId", identityFullResponse.id.toString())
            basicAuth("user@gmail.com", "user")
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }
    }
}