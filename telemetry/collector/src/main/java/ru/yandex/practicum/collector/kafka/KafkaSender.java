package ru.yandex.practicum.collector.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

@Service
public class KafkaSender {
    private final KafkaProducer<byte[], byte[]> producer;

    public KafkaSender(KafkaProducer<byte[], byte[]> producer) {
        this.producer = producer;
    }

    public void send(String topic, String key, byte[] value) {
        byte[] k = key == null ? null : key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        producer.send(new ProducerRecord<>(topic, k, value));
    }
}
