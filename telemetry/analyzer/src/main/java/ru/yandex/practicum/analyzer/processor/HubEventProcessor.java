package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.config.AnalyzerKafkaProperties;
import ru.yandex.practicum.analyzer.model.*;
import ru.yandex.practicum.analyzer.service.ScenarioService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HubEventProcessor.class);

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final AnalyzerKafkaProperties kafkaProperties;
    private final ScenarioService scenarioService;


    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            consumer.subscribe(Collections.singletonList(kafkaProperties.getHubsTopic()));
            log.info("HubEventProcessor subscribed to topic: {}", kafkaProperties.getHubsTopic());

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    continue;
                }

                log.debug("HubEventProcessor received {} records", records.count());

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    handleHubEvent(record.value());
                }
            }

        } catch (WakeupException ignored) {
            log.info("HubEventProcessor wakeup, shutting down...");
        } catch (Exception e) {
            log.error("Error in HubEventProcessor", e);
        } finally {
            log.info("Closing HubEventProcessor consumer...");
            consumer.close();
            log.info("HubEventProcessor stopped");
        }
    }

    private void handleHubEvent(HubEventAvro event) {
        String hubId = event.getHubId();

        Object payload = event.getPayload();

        switch (payload) {
            case DeviceAddedEventAvro deviceAdded -> handleDeviceAdded(hubId, deviceAdded);
            case DeviceRemovedEventAvro deviceRemoved -> handleDeviceRemoved(hubId, deviceRemoved);
            case ScenarioAddedEventAvro scenarioAdded -> handleScenarioAdded(hubId, scenarioAdded);
            case ScenarioRemovedEventAvro scenarioRemoved -> handleScenarioRemoved(hubId, scenarioRemoved);
            default -> log.warn("Unknown hub event payload type: {}", payload.getClass());
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        String sensorId = event.getId();
        scenarioService.addSensor(sensorId, hubId);
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        String sensorId = event.getId();
        scenarioService.removeSensor(sensorId, hubId);
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        String scenarioName = event.getName();

        Map<Sensor, Condition> conditions = new HashMap<>();
        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
            String sensorId = conditionAvro.getSensorId();

            Optional<Sensor> sensor = scenarioService.findSensor(sensorId, hubId);
            if (sensor.isEmpty()) {
                log.warn("Sensor {} not found for scenario '{}', skipping condition", sensorId, scenarioName);
                continue;
            }

            ConditionType type = ConditionType.valueOf(conditionAvro.getType().name());
            ConditionOperation operation = ConditionOperation.valueOf(conditionAvro.getOperation().name());

            Integer value = null;
            Object valueObj = conditionAvro.getValue();

            if (valueObj != null) {
                log.debug("Condition value from Kafka: type={}, class={}, value={}",
                        type, valueObj.getClass().getSimpleName(), valueObj);

                if (valueObj instanceof Integer) {
                    value = (Integer) valueObj;
                } else if (valueObj instanceof Boolean) {
                    value = ((Boolean) valueObj) ? 1 : 0;
                } else {
                    log.warn("Unexpected value type: {}", valueObj.getClass());
                }
            }

            log.info("Creating condition: type={}, operation={}, value={}", type, operation, value);
            Condition condition = new Condition(type, operation, value);
            conditions.put(sensor.get(), condition);
        }

        Map<Sensor, Action> actions = new HashMap<>();
        for (DeviceActionAvro actionAvro : event.getActions()) {
            String sensorId = actionAvro.getSensorId();

            Optional<Sensor> sensor = scenarioService.findSensor(sensorId, hubId);
            if (sensor.isEmpty()) {
                log.warn("Sensor {} not found for scenario '{}', skipping action", sensorId, scenarioName);
                continue;
            }

            ActionType actionType = ActionType.valueOf(actionAvro.getType().name());
            Integer actionValue = actionAvro.getValue() != null ? actionAvro.getValue() : null;

            Action action = new Action(actionType, actionValue);
            actions.put(sensor.get(), action);
        }

        scenarioService.addScenario(hubId, scenarioName, conditions, actions);
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String scenarioName = event.getName();
        scenarioService.removeScenario(hubId, scenarioName);
    }
}