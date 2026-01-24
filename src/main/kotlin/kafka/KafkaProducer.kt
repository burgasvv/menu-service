package org.burgas.kafka

import io.ktor.server.config.ApplicationConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.burgas.serialization.IdentityFullResponseSerializer
import org.burgas.service.IdentityFullResponse

class KafkaProducer {

    private val applicationConfig: ApplicationConfig = ApplicationConfig("application.yaml")

    fun identityFullResponseKafkaProducer(): KafkaProducer<String, IdentityFullResponse> {
        val properties: Map<String, Any> = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to this.applicationConfig.property("ktor.kafka.bootstrap.servers").getString(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to IdentityFullResponseSerializer::class.java
        )
        return KafkaProducer<String, IdentityFullResponse>(properties)
    }
}