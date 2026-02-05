package ru.yandex.practicum.collector.dto.sensor;

import jakarta.validation.constraints.NotNull;

public final class SwitchSensorEvent extends SensorEvent {
    @NotNull private Boolean state;

    public Boolean getState() { return state; }
    public void setState(Boolean state) { this.state = state; }
}
