package ru.yandex.practicum.aggregator.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Properties;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaConsumer<String, SensorEventAvro> kafkaConsumer(AggregatorKafkaProperties props) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, props.getGroupId());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, props.getConsumer().getKeyDeserializer());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, props.getConsumer().getValueDeserializer());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, props.getConsumer().isEnableAutoCommit());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, props.getConsumer().getAutoOffsetReset());

        return new KafkaConsumer<>(properties);
    }

    @Bean
    public KafkaProducer<String, SensorsSnapshotAvro> kafkaProducer(AggregatorKafkaProperties props) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, props.getProducer().getKeySerializer());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, props.getProducer().getValueSerializer());
        properties.put(ProducerConfig.ACKS_CONFIG, props.getProducer().getAcks());
        properties.put(ProducerConfig.LINGER_MS_CONFIG, props.getProducer().getLingerMs());
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, props.getProducer().getBatchSize());

        return new KafkaProducer<>(properties);
    }
}