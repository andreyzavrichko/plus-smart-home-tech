package ru.yandex.practicum.analyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.model.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ScenarioEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ScenarioEvaluator.class);

    public List<ActionToExecute> evaluateScenarios(SensorsSnapshotAvro snapshot, List<Scenario> scenarios) {
        List<ActionToExecute> actionsToExecute = new ArrayList<>();

        for (Scenario scenario : scenarios) {
            if (checkScenarioConditions(snapshot, scenario)) {
                log.info("Scenario '{}' conditions met for hub {}", scenario.getName(), snapshot.getHubId());


                for (Map.Entry<Sensor, Action> entry : scenario.getActions().entrySet()) {
                    actionsToExecute.add(new ActionToExecute(
                            scenario.getName(),
                            entry.getKey().getId(),
                            entry.getValue()
                    ));
                }
            }
        }

        return actionsToExecute;
    }


    private boolean checkScenarioConditions(SensorsSnapshotAvro snapshot, Scenario scenario) {
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        for (Map.Entry<Sensor, Condition> entry : scenario.getConditions().entrySet()) {
            Sensor sensor = entry.getKey();
            Condition condition = entry.getValue();

            SensorStateAvro sensorState = sensorsState.get(sensor.getId());
            if (sensorState == null) {
                log.debug("Sensor {} not found in snapshot for scenario '{}'",
                        sensor.getId(), scenario.getName());
                return false;
            }

            if (!checkCondition(sensorState, condition)) {
                log.debug("Condition not met for sensor {} in scenario '{}'",
                        sensor.getId(), scenario.getName());
                return false;
            }
        }

        return true;
    }


    private boolean checkCondition(SensorStateAvro sensorState, Condition condition) {
        Object data = sensorState.getData();
        Integer actualValue = extractValue(data, condition.getType());

        if (actualValue == null) {
            log.warn("Could not extract value for condition type {}", condition.getType());
            return false;
        }

        log.debug("Checking condition: type={}, actual={}, expected={}, operation={}",
                condition.getType(), actualValue, condition.getValue(), condition.getOperation());

        return compareValues(actualValue, condition.getValue(), condition.getOperation());
    }


    private Integer extractValue(Object data, ConditionType type) {
        return switch (type) {
            case TEMPERATURE -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                } else if (data instanceof TemperatureSensorAvro temp) {
                    yield temp.getTemperatureC();
                }
                yield null;
            }
            case HUMIDITY -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getHumidity();
                }
                yield null;
            }
            case CO2LEVEL -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getCo2Level();
                }
                yield null;
            }
            case LUMINOSITY -> {
                if (data instanceof LightSensorAvro light) {
                    yield light.getLuminosity();
                }
                yield null;
            }
            case MOTION -> {
                if (data instanceof MotionSensorAvro motion) {
                    yield motion.getMotion() ? 1 : 0;
                }
                yield null;
            }
            case SWITCH -> {
                if (data instanceof SwitchSensorAvro switchSensor) {
                    yield switchSensor.getState() ? 1 : 0;
                }
                yield null;
            }
        };
    }


    private boolean compareValues(Integer actual, Integer expected, ConditionOperation operation) {
        if (expected == null) {
            log.warn("Expected value is null for comparison, condition cannot be evaluated");
            return false;
        }

        return switch (operation) {
            case EQUALS -> actual.equals(expected);
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }


    public record ActionToExecute(String scenarioName, String sensorId, Action action) {
    }
}