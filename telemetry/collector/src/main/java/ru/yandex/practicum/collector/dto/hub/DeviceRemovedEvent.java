package ru.yandex.practicum.collector.dto.hub;

import jakarta.validation.constraints.NotBlank;

public final class DeviceRemovedEvent extends HubEvent {

    @NotBlank
    private String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
