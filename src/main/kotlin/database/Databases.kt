package org.burgas.database

import org.burgas.service.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
fun configureDatabases() {

    transaction(db = DatabaseFactory.postgres) {
        SchemaUtils.create(IdentityTable, RestaurantTable, MenuTable, DishTable, IdentityDishTable)

        val identityIdParse = Uuid.parse("0b0a1bf5-a806-4d66-885b-0d6255a2746c")
        val identityId = identityIdParse.toJavaUuid()
        IdentityEntity.findById(identityId) ?: IdentityEntity.new(identityId) {
            this.authority = Authority.ADMIN
            this.username = "burgasvv"
            this.password = BCrypt.hashpw("burgasvv", BCrypt.gensalt())
            this.email = "burgasvv@gmail.com"
            this.enabled = true
            this.firstname = "Бургас"
            this.lastname = "Вячеслав"
            this.patronymic = "Васильевич"
        }

        val restaurantIdParse = Uuid.parse("16d830fc-dbb2-4943-b533-6f4d25e91922")
        val restaurantId = restaurantIdParse.toJavaUuid()
        val restaurantEntity = RestaurantEntity.findById(restaurantId) ?: RestaurantEntity.new(restaurantId) {
            this.name = "Шашлыкофф"
            this.description = "Описание ресторана Шашлыкофф"
        }

        val menuIdParse = Uuid.parse("5d732ec3-0ca3-49a6-acf1-26d5cf6277b7")
        val menuId = menuIdParse.toJavaUuid()
        val menuEntity = MenuEntity.findById(menuId) ?: MenuEntity.new(menuId) {
            this.name = "Карт-бланш Рояль"
            this.description = "Описание меню Карт-бланш Рояль"
            this.restaurant = restaurantEntity
        }

        val firstDishIdParse = Uuid.parse("39412203-bb48-4094-b312-7f9fee997115")
        val firstDishId = firstDishIdParse.toJavaUuid()
        DishEntity.findById(firstDishId) ?: DishEntity.new(firstDishId) {
            this.name = "Курица в соусе с сыром Бри"
            this.about = "Описание блюда Курица в соусе с сыром Бри"
            this.price = BigDecimal(2300.30)
            this.menu = menuEntity
        }

        val secondDishIdParse = Uuid.parse("2853b3f2-d123-421e-9429-68facbdc8199")
        val secondDishId = secondDishIdParse.toJavaUuid()
        DishEntity.findById(secondDishId) ?: DishEntity.new(secondDishId) {
            this.name = "Жареная картошка с беконом"
            this.about = "Описание блюда Жареная картошка с беконом"
            this.price = BigDecimal(1800.60)
            this.menu = menuEntity
        }
    }
}
