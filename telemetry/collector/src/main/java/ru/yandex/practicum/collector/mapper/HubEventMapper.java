package ru.yandex.practicum.collector.mapper;

import ru.yandex.practicum.collector.dto.hub.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

public final class HubEventMapper {

    private HubEventMapper() {
    }

    public static HubEventAvro map(HubEvent dto) {
        HubEventAvro avro = new HubEventAvro();
        avro.setHubId(dto.getHubId());
        avro.setTimestamp(dto.getTimestamp());

        Object payload = switch (dto.getType()) {
            case DEVICE_ADDED -> {
                DeviceAddedEvent e = (DeviceAddedEvent) dto;
                DeviceAddedEventAvro p = new DeviceAddedEventAvro();
                p.setId(e.getId());
                p.setType(DeviceTypeAvro.valueOf(e.getDeviceType().name()));
                yield p;
            }
            case DEVICE_REMOVED -> {
                DeviceRemovedEvent e = (DeviceRemovedEvent) dto;
                DeviceRemovedEventAvro p = new DeviceRemovedEventAvro();
                p.setId(e.getId());
                yield p;
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEvent e = (ScenarioAddedEvent) dto;
                ScenarioAddedEventAvro p = new ScenarioAddedEventAvro();
                p.setName(e.getName());
                p.setConditions(e.getConditions().stream().map(HubEventMapper::mapCondition).toList());
                p.setActions(e.getActions().stream().map(HubEventMapper::mapAction).toList());
                yield p;
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent e = (ScenarioRemovedEvent) dto;
                ScenarioRemovedEventAvro p = new ScenarioRemovedEventAvro();
                p.setName(e.getName());
                yield p;
            }
        };

        avro.setPayload(payload);
        return avro;
    }

    private static ScenarioConditionAvro mapCondition(ScenarioCondition c) {
        ScenarioConditionAvro a = new ScenarioConditionAvro();
        a.setSensorId(c.getSensorId());
        a.setType(ConditionTypeAvro.valueOf(c.getType().name()));
        a.setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()));
        a.setValue(c.getValue());
        return a;
    }

    private static DeviceActionAvro mapAction(DeviceAction d) {
        DeviceActionAvro a = new DeviceActionAvro();
        a.setSensorId(d.getSensorId());
        a.setType(ActionTypeAvro.valueOf(d.getType().name()));
        a.setValue(d.getValue());
        return a;
    }
}
