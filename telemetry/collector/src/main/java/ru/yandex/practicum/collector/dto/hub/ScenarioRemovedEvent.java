package ru.yandex.practicum.collector.dto.hub;

import jakarta.validation.constraints.NotBlank;

public final class ScenarioRemovedEvent extends HubEvent {

    @NotBlank
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
