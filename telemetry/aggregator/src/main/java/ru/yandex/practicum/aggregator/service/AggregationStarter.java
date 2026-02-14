package ru.yandex.practicum.aggregator.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.config.AggregatorKafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class AggregationStarter {

    private static final Logger log = LoggerFactory.getLogger(AggregationStarter.class);

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<byte[], byte[]> producer;
    private final AggregatorKafkaProperties kafkaProperties;

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public AggregationStarter(
            KafkaConsumer<String, SensorEventAvro> consumer,
            KafkaProducer<byte[], byte[]> producer,
            AggregatorKafkaProperties kafkaProperties) {
        this.consumer = consumer;
        this.producer = producer;
        this.kafkaProperties = kafkaProperties;
    }


    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(Collections.singletonList(kafkaProperties.getSensorsTopic()));
            log.info("Subscribed to topic: {}", kafkaProperties.getSensorsTopic());


            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    continue;
                }

                log.debug("Received {} records", records.count());


                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    handleSensorEvent(record.value());
                }


                Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, SensorEventAvro>> partitionRecords = records.records(partition);
                    long lastOffset = partitionRecords.getLast().offset();
                    currentOffsets.put(partition, new OffsetAndMetadata(lastOffset + 1));
                }
                consumer.commitSync(currentOffsets);
                log.debug("Committed offsets for {} partitions", currentOffsets.size());
            }

        } catch (WakeupException ignored) {
            log.info("Wakeup exception caught, shutting down...");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                log.info("Flushing producer...");
                producer.flush();
                log.info("Committing final offsets...");
                consumer.commitSync();
            } finally {
                log.info("Closing consumer...");
                consumer.close();
                log.info("Closing producer...");
                producer.close();
                log.info("Shutdown complete");
            }
        }
    }


    private void handleSensorEvent(SensorEventAvro event) {
        log.debug("Processing event: hubId={}, sensorId={}", event.getHubId(), event.getId());

        Optional<SensorsSnapshotAvro> updatedSnapshot = updateState(event);

        if (updatedSnapshot.isPresent()) {
            SensorsSnapshotAvro snapshot = updatedSnapshot.get();
            sendSnapshot(snapshot);
            log.info("Snapshot updated and sent for hubId={}, sensors count={}",
                    snapshot.getHubId(), snapshot.getSensorsState().size());
        } else {
            log.debug("Snapshot not changed for hubId={}, sensorId={}", event.getHubId(), event.getId());
        }
    }


    private Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();
        Instant eventTimestamp = event.getTimestamp();

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, k -> {
            SensorsSnapshotAvro newSnapshot = new SensorsSnapshotAvro();
            newSnapshot.setHubId(hubId);
            newSnapshot.setTimestamp(eventTimestamp);
            newSnapshot.setSensorsState(new HashMap<>());
            log.info("Created new snapshot for hubId={}", hubId);
            return newSnapshot;
        });

        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);

        if (oldState != null) {

            if (eventTimestamp.isBefore(oldState.getTimestamp())) {
                log.debug("Event is older, skipping. sensorId={}", sensorId);
                return Optional.empty();
            }


            if (eventTimestamp.equals(oldState.getTimestamp()) &&
                    Objects.equals(oldState.getData(), event.getPayload())) {
                log.debug("Sensor data not changed, skipping. sensorId={}", sensorId);
                return Optional.empty();
            }
        }

        SensorStateAvro newState = new SensorStateAvro();
        newState.setTimestamp(eventTimestamp);
        newState.setData(event.getPayload());

        snapshot.getSensorsState().put(sensorId, newState);
        snapshot.setTimestamp(eventTimestamp);

        return Optional.of(snapshot);
    }

    private void sendSnapshot(SensorsSnapshotAvro snapshot) {
        try {
            byte[] key = snapshot.getHubId().getBytes(StandardCharsets.UTF_8);
            byte[] value = AvroBinarySerializer.toBytes(snapshot);

            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(
                    kafkaProperties.getSnapshotsTopic(),
                    null,
                    snapshot.getTimestamp().toEpochMilli(),
                    key,
                    value
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send snapshot for hubId={}", snapshot.getHubId(), exception);
                } else {
                    log.debug("Snapshot sent: topic={}, partition={}, offset={}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

        } catch (Exception e) {
            log.error("Error serializing/sending snapshot for hubId={}", snapshot.getHubId(), e);
        }
    }
}