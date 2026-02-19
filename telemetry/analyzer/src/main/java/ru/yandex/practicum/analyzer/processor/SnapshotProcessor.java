package ru.yandex.practicum.analyzer.processor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.AnalyzerKafkaProperties;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.service.ActionExecutor;
import ru.yandex.practicum.analyzer.service.ScenarioEvaluator;
import ru.yandex.practicum.analyzer.service.ScenarioService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SnapshotProcessor {

    private static final Logger log = LoggerFactory.getLogger(SnapshotProcessor.class);

    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final AnalyzerKafkaProperties kafkaProperties;
    private final ScenarioService scenarioService;
    private final ScenarioEvaluator scenarioEvaluator;
    private final ActionExecutor actionExecutor;

    public SnapshotProcessor(
            KafkaConsumer<String, SensorsSnapshotAvro> consumer,
            AnalyzerKafkaProperties kafkaProperties,
            ScenarioService scenarioService,
            ScenarioEvaluator scenarioEvaluator,
            ActionExecutor actionExecutor) {
        this.consumer = consumer;
        this.kafkaProperties = kafkaProperties;
        this.scenarioService = scenarioService;
        this.scenarioEvaluator = scenarioEvaluator;
        this.actionExecutor = actionExecutor;
    }

    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(Collections.singletonList(kafkaProperties.getSnapshotsTopic()));
            log.info("SnapshotProcessor subscribed to topic: {}", kafkaProperties.getSnapshotsTopic());

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    continue;
                }

                log.debug("SnapshotProcessor received {} records", records.count());

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    handleSnapshot(record.value());
                }

                Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, SensorsSnapshotAvro>> partitionRecords = records.records(partition);
                    long lastOffset = partitionRecords.getLast().offset();
                    currentOffsets.put(partition, new OffsetAndMetadata(lastOffset + 1));
                }
                consumer.commitSync(currentOffsets);
                log.debug("Committed offsets for {} partitions", currentOffsets.size());
            }

        } catch (WakeupException ignored) {
            log.info("SnapshotProcessor wakeup, shutting down...");
        } catch (Exception e) {
            log.error("Error in SnapshotProcessor", e);
        } finally {
            try {
                log.info("Committing final offsets...");
                consumer.commitSync();
            } finally {
                log.info("Closing SnapshotProcessor consumer...");
                consumer.close();
                log.info("SnapshotProcessor stopped");
            }
        }
    }


    private void handleSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();

        log.debug("Processing snapshot for hub {}, sensors count: {}",
                hubId, snapshot.getSensorsState().size());

        List<Scenario> scenarios = scenarioService.getScenariosForHub(hubId);
        if (scenarios.isEmpty()) {
            log.debug("No scenarios found for hub {}", hubId);
            return;
        }

        log.debug("Found {} scenarios for hub {}", scenarios.size(), hubId);

        List<ScenarioEvaluator.ActionToExecute> actionsToExecute =
                scenarioEvaluator.evaluateScenarios(snapshot, scenarios);

        if (actionsToExecute.isEmpty()) {
            log.debug("No actions to execute for hub {}", hubId);
            return;
        }

        log.info("Hub {}: {} actions to execute", hubId, actionsToExecute.size());

        actionExecutor.executeActions(hubId, actionsToExecute);
    }
}