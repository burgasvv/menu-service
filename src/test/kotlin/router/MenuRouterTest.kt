package router

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.burgas.module
import org.burgas.security.CsrfToken
import org.burgas.service.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MenuRouterTest {

    @Test
    fun `test menu router endpoints`() = testApplication {
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
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
        val csrfToken = csrfTokenResponse.body<CsrfToken>()

        httpClient.post("/api/v1/restaurants/create") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(RestaurantRequest(name = "Tender", description = "Описание ресторана Tender"))
        }

        val restaurant = httpClient.get("/api/v1/restaurants") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
            .body<List<RestaurantShortResponse>>()
            .first { first -> first.name == "Tender" }

        httpClient.post("/api/v1/menu/create") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(MenuRequest(name = "Tender Menu", description = "Описание Tender Меню", restaurantId = restaurant.id))
        }
            .apply {
                assertEquals(HttpStatusCode.Created, status)
            }

        val menuForUpdate = httpClient.get("/api/v1/menu") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
            .body<List<MenuShortResponse>>()
            .first { first -> first.name == "Tender Menu" }

        httpClient.put("/api/v1/menu/update") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(MenuRequest(id = menuForUpdate.id, name = "Tender Menu Breakfast", "Описание Tender Menu Breakfast"))
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }

        val menuToFind = httpClient.get("/api/v1/menu") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            .body<List<MenuShortResponse>>()
            .first { first -> first.name == "Tender Menu Breakfast" }

        val menuFullResponse = httpClient.get("/api/v1/menu/by-id") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            parameter("menuId", menuToFind.id.toString())
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            .body<MenuFullResponse>()

        httpClient.delete("/api/v1/menu/delete") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            parameter("menuId", menuFullResponse.id.toString())
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }

        httpClient.delete("/api/v1/restaurants/delete") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            parameter("restaurantId", restaurant.id.toString())
        }
    }
}