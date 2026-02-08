package ru.yandex.practicum.collector.mapper;

import ru.yandex.practicum.collector.dto.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

public final class SensorEventMapper {

    private SensorEventMapper() {
    }

    public static SensorEventAvro map(SensorEvent dto) {
        SensorEventAvro avro = new SensorEventAvro();
        avro.setId(dto.getId());
        avro.setHubId(dto.getHubId());
        avro.setTimestamp(dto.getTimestamp());

        Object payload = switch (dto.getType()) {
            case LIGHT_SENSOR_EVENT -> {
                LightSensorEvent e = (LightSensorEvent) dto;
                LightSensorAvro p = new LightSensorAvro();
                p.setLinkQuality(nvl(e.getLinkQuality()));
                p.setLuminosity(nvl(e.getLuminosity()));
                yield p;
            }
            case SWITCH_SENSOR_EVENT -> {
                SwitchSensorEvent e = (SwitchSensorEvent) dto;
                SwitchSensorAvro p = new SwitchSensorAvro();
                p.setState(e.getState());
                yield p;
            }
            case MOTION_SENSOR_EVENT -> {
                MotionSensorEvent e = (MotionSensorEvent) dto;
                MotionSensorAvro p = new MotionSensorAvro();
                p.setLinkQuality(nvl(e.getLinkQuality()));
                p.setMotion(Boolean.TRUE.equals(e.getMotion()));
                p.setVoltage(nvl(e.getVoltage()));
                yield p;
            }
            case TEMPERATURE_SENSOR_EVENT -> {
                TemperatureSensorEvent e = (TemperatureSensorEvent) dto;
                TemperatureSensorAvro p = new TemperatureSensorAvro();
                p.setTemperatureC(nvl(e.getTemperatureC()));
                p.setTemperatureF(nvl(e.getTemperatureF()));
                yield p;
            }
            case CLIMATE_SENSOR_EVENT -> {
                ClimateSensorEvent e = (ClimateSensorEvent) dto;
                ClimateSensorAvro p = new ClimateSensorAvro();
                p.setTemperatureC(nvl(e.getTemperatureC()));
                p.setHumidity(nvl(e.getHumidity()));
                p.setCo2Level(nvl(e.getCo2Level()));
                yield p;
            }
        };

        avro.setPayload(payload);
        return avro;
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
