package ru.yandex.practicum.collector.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class KafkaSender implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(KafkaSender.class);

    private final KafkaProducer<byte[], byte[]> producer;

    public KafkaSender(KafkaProducer<byte[], byte[]> producer) {
        this.producer = producer;
    }

    public void send(String topic, long timestampMillis, String hubId, byte[] event) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic is null/blank");
        }
        if (hubId == null || hubId.isBlank()) {
            throw new IllegalArgumentException("hubId is null/blank");
        }
        if (event == null) {
            throw new IllegalArgumentException("event is null");
        }

        byte[] keyBytes = hubId.getBytes(StandardCharsets.UTF_8);

        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                topic,
                null,
                timestampMillis,
                keyBytes,
                event
        );

        log.debug("Kafka send -> topic={}, ts={}, hubId={}, payloadSize={}",
                topic, timestampMillis, hubId, event.length);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Kafka send failed -> topic={}, hubId={}, ts={}", topic, hubId, timestampMillis, exception);
                return;
            }
            log.debug("Kafka send ok -> topic={}, partition={}, offset={}, ts={}, hubId={}",
                    metadata.topic(), metadata.partition(), metadata.offset(), timestampMillis, hubId);
        });
    }


    public void send(String topic, String key, byte[] value) {
        long now = System.currentTimeMillis();
        if (key == null) {
            throw new IllegalArgumentException("key(hubId) is null");
        }
        send(topic, now, key, value);
    }

    @Override
    public void close() {
        try {
            log.info("KafkaSender closing: flushing");
            producer.flush();
            log.info("KafkaSender closing: closing");
            producer.close();
            log.info("KafkaSender closed.");
        } catch (Exception e) {
            log.warn("KafkaSender close failed", e);
        }
    }
}
