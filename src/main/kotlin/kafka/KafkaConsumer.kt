package org.burgas.kafka

import io.ktor.server.config.ApplicationConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.burgas.serialization.IdentityFullResponseDeserializer
import org.burgas.service.IdentityFullResponse

class KafkaConsumer {

    private val applicationConfig: ApplicationConfig = ApplicationConfig("application.yaml")

    fun identityFullResponseKafkaConsumer(): KafkaConsumer<String, IdentityFullResponse> {
        val properties: Map<String, Any> = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to this.applicationConfig.property("ktor.kafka.bootstrap.servers").getString(),
            ConsumerConfig.GROUP_ID_CONFIG to "identity-group-id",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to IdentityFullResponseDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
        )
        val kafkaConsumer = KafkaConsumer<String, IdentityFullResponse>(properties)
        kafkaConsumer.subscribe(listOf("identity-kafka-topic"))
        return kafkaConsumer
    }
}