package ru.yandex.practicum.collector.grpc;

import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.stream.Collectors;

public final class ProtoToAvroHubMapper {

    private ProtoToAvroHubMapper() {
    }

    public static HubEventAvro map(HubEventProto proto) {
        HubEventAvro avro = new HubEventAvro();
        avro.setHubId(proto.getHubId());
        avro.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));

        Object payload = switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> {
                DeviceAddedEventProto p = proto.getDeviceAdded();
                DeviceAddedEventAvro a = new DeviceAddedEventAvro();
                a.setId(p.getId());
                a.setType(mapDeviceType(p.getType()));
                yield a;
            }
            case DEVICE_REMOVED -> {
                DeviceRemovedEventProto p = proto.getDeviceRemoved();
                DeviceRemovedEventAvro a = new DeviceRemovedEventAvro();
                a.setId(p.getId());
                yield a;
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEventProto p = proto.getScenarioAdded();
                ScenarioAddedEventAvro a = new ScenarioAddedEventAvro();
                a.setName(p.getName());
                a.setConditions(p.getConditionList().stream()
                        .map(ProtoToAvroHubMapper::mapCondition)
                        .collect(Collectors.toList()));
                a.setActions(p.getActionList().stream()
                        .map(ProtoToAvroHubMapper::mapAction)
                        .collect(Collectors.toList()));
                yield a;
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEventProto p = proto.getScenarioRemoved();
                ScenarioRemovedEventAvro a = new ScenarioRemovedEventAvro();
                a.setName(p.getName());
                yield a;
            }
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Hub event payload not set");
        };

        avro.setPayload(payload);
        return avro;
    }

    private static DeviceTypeAvro mapDeviceType(DeviceTypeProto proto) {
        return DeviceTypeAvro.valueOf(proto.name());
    }

    private static ScenarioConditionAvro mapCondition(ScenarioConditionProto proto) {
        ScenarioConditionAvro avro = new ScenarioConditionAvro();
        avro.setSensorId(proto.getSensorId());
        avro.setType(ConditionTypeAvro.valueOf(proto.getType().name()));
        avro.setOperation(ConditionOperationAvro.valueOf(proto.getOperation().name()));


        switch (proto.getValueCase()) {
            case BOOL_VALUE -> avro.setValue(proto.getBoolValue());
            case INT_VALUE -> avro.setValue(proto.getIntValue());
            case VALUE_NOT_SET -> avro.setValue(null);
        }

        return avro;
    }

    private static DeviceActionAvro mapAction(DeviceActionProto proto) {
        DeviceActionAvro avro = new DeviceActionAvro();
        avro.setSensorId(proto.getSensorId());
        avro.setType(ActionTypeAvro.valueOf(proto.getType().name()));
        avro.setValue(proto.hasValue() ? proto.getValue() : null);
        return avro;
    }
}