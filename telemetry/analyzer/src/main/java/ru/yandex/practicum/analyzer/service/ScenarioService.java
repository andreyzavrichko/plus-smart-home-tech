package ru.yandex.practicum.analyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.Sensor;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ScenarioService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioService.class);

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;

    public ScenarioService(SensorRepository sensorRepository, ScenarioRepository scenarioRepository) {
        this.sensorRepository = sensorRepository;
        this.scenarioRepository = scenarioRepository;
    }

    @Transactional
    public void addSensor(String sensorId, String hubId) {
        Optional<Sensor> existing = sensorRepository.findById(sensorId);
        if (existing.isPresent()) {
            log.debug("Sensor {} already exists, skipping", sensorId);
            return;
        }

        Sensor sensor = new Sensor(sensorId, hubId);
        sensorRepository.save(sensor);
        log.info("Sensor added: id={}, hubId={}", sensorId, hubId);
    }


    @Transactional
    public void removeSensor(String sensorId, String hubId) {
        Optional<Sensor> sensor = sensorRepository.findByIdAndHubId(sensorId, hubId);
        if (sensor.isEmpty()) {
            log.debug("Sensor {} not found in hub {}, nothing to remove", sensorId, hubId);
            return;
        }

        sensorRepository.delete(sensor.get());
        log.info("Sensor removed: id={}, hubId={}", sensorId, hubId);
    }


    @Transactional
    public void addScenario(String hubId, String name,
                            java.util.Map<Sensor, Condition> conditions,
                            java.util.Map<Sensor, Action> actions) {
        Optional<Scenario> existing = scenarioRepository.findByHubIdAndName(hubId, name);

        Scenario scenario;
        if (existing.isPresent()) {
            scenario = existing.get();
            scenario.getConditions().clear();
            scenario.getActions().clear();
            log.debug("Updating existing scenario: hub={}, name={}", hubId, name);
        } else {
            scenario = new Scenario(hubId, name);
            log.debug("Creating new scenario: hub={}, name={}", hubId, name);
        }

        scenario.setConditions(conditions);
        scenario.setActions(actions);

        scenarioRepository.save(scenario);
        log.info("Scenario saved: hub={}, name={}, conditions={}, actions={}",
                hubId, name, conditions.size(), actions.size());
    }


    @Transactional
    public void removeScenario(String hubId, String name) {
        Optional<Scenario> scenario = scenarioRepository.findByHubIdAndName(hubId, name);
        if (scenario.isEmpty()) {
            log.debug("Scenario not found: hub={}, name={}", hubId, name);
            return;
        }

        scenarioRepository.delete(scenario.get());
        log.info("Scenario removed: hub={}, name={}", hubId, name);
    }


    public List<Scenario> getScenariosForHub(String hubId) {
        return scenarioRepository.findByHubId(hubId);
    }


    public Optional<Sensor> findSensor(String sensorId, String hubId) {
        return sensorRepository.findByIdAndHubId(sensorId, hubId);
    }
}