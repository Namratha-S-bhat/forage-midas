package com.jpmc.midascore.consumer;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.component.DatabaseConduit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private final DatabaseConduit databaseConduit;

    @Value("${general.kafka-topic}")
    private String topic;

    public KafkaConsumer(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, Transaction> record) {
        try {
            Transaction transaction = record.value();
            logger.info("Received transaction: {}", transaction);
            logger.info("Received record with key: {}, partition: {}, offset: {}",
                    record.key(), record.partition(), record.offset());

            // Pass the transaction to DatabaseConduit for processing
            databaseConduit.save(transaction);

        } catch (Exception e) {
            logger.error("Error processing transaction: {}", record.value(), e);
        }
    }
}