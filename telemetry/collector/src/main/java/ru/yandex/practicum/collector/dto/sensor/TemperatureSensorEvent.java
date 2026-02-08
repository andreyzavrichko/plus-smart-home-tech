package ru.yandex.practicum.collector.dto.sensor;

import jakarta.validation.constraints.NotNull;

public final class TemperatureSensorEvent extends SensorEvent {

    @NotNull
    private Integer temperatureC;

    @NotNull
    private Integer temperatureF;

    public Integer getTemperatureC() { return temperatureC; }
    public void setTemperatureC(Integer temperatureC) { this.temperatureC = temperatureC; }

    public Integer getTemperatureF() { return temperatureF; }
    public void setTemperatureF(Integer temperatureF) { this.temperatureF = temperatureF; }
}
