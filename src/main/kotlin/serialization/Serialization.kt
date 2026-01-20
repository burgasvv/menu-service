package org.burgas.serialization

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import org.burgas.service.IdentityFullResponse
import java.nio.charset.StandardCharsets
import java.util.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            }
        )
    }
}

object UUIDSerializer : KSerializer<UUID> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

class IdentityFullResponseSerializer : Serializer<IdentityFullResponse> {
    override fun serialize(topic: String?, data: IdentityFullResponse?): ByteArray? {
        if (data == null) return null
        return Json.encodeToString(IdentityFullResponse.serializer(), data).toByteArray(StandardCharsets.UTF_8)
    }
}

class IdentityFullResponseDeserializer : Deserializer<IdentityFullResponse> {
    override fun deserialize(topic: String?, data: ByteArray?): IdentityFullResponse? {
        if (data == null) return null
        val jsonString = String(data, StandardCharsets.UTF_8)
        return Json.decodeFromString(IdentityFullResponse.serializer(), jsonString)
    }
}