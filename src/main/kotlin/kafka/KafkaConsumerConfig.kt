package org.burgas.kafka

import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

fun Application.configureKafkaConsumer() {

    val kafkaConsumer = KafkaConsumer()
    launch(Dispatchers.Default) {
        val identityFullResponseKafkaConsumer = kafkaConsumer.identityFullResponseKafkaConsumer()
        while (true) {
            val consumerRecords = identityFullResponseKafkaConsumer.poll(Duration.ofMillis(10))
            consumerRecords.forEach { consumerRecord -> println("Topic: ${consumerRecord.topic()} :: ${consumerRecord.value()}") }
        }
    }
}