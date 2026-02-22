package ru.yandex.practicum.collector.grpc;

import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

public final class ProtoToAvroSensorMapper {

    private ProtoToAvroSensorMapper() {
    }

    public static SensorEventAvro map(SensorEventProto proto) {
        SensorEventAvro avro = new SensorEventAvro();
        avro.setId(proto.getId());
        avro.setHubId(proto.getHubId());
        avro.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));

        Object payload = switch (proto.getPayloadCase()) {
            case MOTION_SENSOR -> {
                MotionSensorProto p = proto.getMotionSensor();
                MotionSensorAvro a = new MotionSensorAvro();
                a.setLinkQuality(p.getLinkQuality());
                a.setMotion(p.getMotion());
                a.setVoltage(p.getVoltage());
                yield a;
            }
            case TEMPERATURE_SENSOR -> {
                TemperatureSensorProto p = proto.getTemperatureSensor();
                TemperatureSensorAvro a = new TemperatureSensorAvro();
                a.setTemperatureC(p.getTemperatureC());
                a.setTemperatureF(p.getTemperatureF());
                yield a;
            }
            case LIGHT_SENSOR -> {
                LightSensorProto p = proto.getLightSensor();
                LightSensorAvro a = new LightSensorAvro();
                a.setLinkQuality(p.getLinkQuality());
                a.setLuminosity(p.getLuminosity());
                yield a;
            }
            case CLIMATE_SENSOR -> {
                ClimateSensorProto p = proto.getClimateSensor();
                ClimateSensorAvro a = new ClimateSensorAvro();
                a.setTemperatureC(p.getTemperatureC());
                a.setHumidity(p.getHumidity());
                a.setCo2Level(p.getCo2Level());
                yield a;
            }
            case SWITCH_SENSOR -> {
                SwitchSensorProto p = proto.getSwitchSensor();
                SwitchSensorAvro a = new SwitchSensorAvro();
                a.setState(p.getState());
                yield a;
            }
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Sensor event payload not set");
        };

        avro.setPayload(payload);
        return avro;
    }
}