package router

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.basicAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.burgas.module
import org.burgas.security.CsrfToken
import org.burgas.service.DishRequest
import org.burgas.service.DishShortResponse
import org.burgas.service.MenuRequest
import org.burgas.service.MenuShortResponse
import org.burgas.service.RestaurantRequest
import org.burgas.service.RestaurantShortResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class DishRouterTest {

    @Test
    fun `test dish router endpoints`() = testApplication {
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

        httpClient.post("/api/v1/restaurants/create") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(RestaurantRequest(name = "Tender", description = "Описание ресторана Tender"))
        }

        val restaurantShortResponse = httpClient.get("/api/v1/restaurants") {
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
            setBody(MenuRequest(name = "Tender Menu", description = "Описание Tender Menu", restaurantId = restaurantShortResponse.id))
        }

        val menuShortResponse = httpClient.get("/api/v1/menu") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
            .body<List<MenuShortResponse>>()
            .first { first -> first.name == "Tender Menu" }

        httpClient.post("/api/v1/dishes/create") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(DishRequest(name = "Dish Test", about = "Описание Dish Test", price = 100.50, menuId = menuShortResponse.id))
        }
            .apply {
                assertEquals(HttpStatusCode.Created, status)
            }

        val dishShortResponse = httpClient.get("/api/v1/dishes") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            .body<List<DishShortResponse>>()
            .first { first -> first.name == "Dish Test" }

        httpClient.get("/api/v1/dishes/by-id") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            parameter("dishId", dishShortResponse.id.toString())
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }

        httpClient.put("/api/v1/dishes/update") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            setBody(DishRequest(id = dishShortResponse.id))
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }

        httpClient.delete("/api/v1/dishes/delete") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            parameter("dishId", dishShortResponse.id.toString())
        }
            .apply {
                assertEquals(HttpStatusCode.OK, status)
            }

        httpClient.delete("/api/v1/menu/delete") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            parameter("menuId", menuShortResponse.id.toString())
        }

        httpClient.delete("/api/v1/restaurants/delete") {
            header(HttpHeaders.Host, "localhost:9000")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Origin, "http://localhost:9000")
            header("X-CSRF-Token", csrfToken.token)
            basicAuth("burgasvv@gmail.com", "burgasvv")
            parameter("restaurantId", restaurantShortResponse.id.toString())
        }
    }
}