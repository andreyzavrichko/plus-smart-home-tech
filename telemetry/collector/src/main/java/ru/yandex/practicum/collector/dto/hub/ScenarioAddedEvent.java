package ru.yandex.practicum.collector.dto.hub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public final class ScenarioAddedEvent extends HubEvent {

    @NotBlank
    private String name;

    @Valid
    @NotEmpty
    private List<ScenarioCondition> conditions;

    @Valid
    @NotEmpty
    private List<DeviceAction> actions;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<ScenarioCondition> getConditions() { return conditions; }
    public void setConditions(List<ScenarioCondition> conditions) { this.conditions = conditions; }

    public List<DeviceAction> getActions() { return actions; }
    public void setActions(List<DeviceAction> actions) { this.actions = actions; }
}
