package router

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.burgas.module
import org.burgas.security.CsrfToken
import org.burgas.service.RestaurantRequest
import org.burgas.service.RestaurantShortResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class RestaurantRouterTest {

    @Test
    fun `test restaurant router endpoints`() = testApplication {
        this.application {
            module()
        }
        val httpClient = this.createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val csrfTokenResponse = httpClient.get("/api/v1/security/csrf-token") {
            header(HttpHeaders.Host, "localhost:9000")
        }
        val csrfToken = csrfTokenResponse.body<CsrfToken>()

        httpClient.post("/api/v1/restaurants/create") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(RestaurantRequest(name = "Бартендер", description = "Описание ресторана Бартендер"))
        }
            .apply {
                assertEquals(HttpStatusCode.Created, status)
            }

        val restaurantsForUpdateResponse = httpClient.get("/api/v1/restaurants") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
        val restaurants = restaurantsForUpdateResponse.body<List<RestaurantShortResponse>>()
        val restaurantForUpdate = restaurants.first { first -> first.name == "Бартендер" }
        httpClient.put("/api/v1/restaurants/update") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(
                RestaurantRequest(
                    id = restaurantForUpdate.id,
                    name = "Бартендер Тест",
                    description = "Описание ресторана Бартендер Тест"
                )
            )
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }

        httpClient.get("/api/v1/restaurants") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }

        val restaurant = httpClient.get("/api/v1/restaurants") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
            .body<List<RestaurantShortResponse>>()
            .first { first -> first.name == "Бартендер Тест" }
        httpClient.get("/api/v1/restaurants/by-id") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            parameter("restaurantId", restaurant.id.toString())
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }

        httpClient.delete("/api/v1/restaurants/delete") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            parameter("restaurantId", restaurant.id.toString())
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }
    }
}