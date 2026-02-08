package ru.yandex.practicum.collector.dto.sensor;

import jakarta.validation.constraints.NotNull;

public final class MotionSensorEvent extends SensorEvent {

    @NotNull
    private Integer linkQuality;

    @NotNull
    private Boolean motion;

    @NotNull
    private Integer voltage;

    public Integer getLinkQuality() { return linkQuality; }
    public void setLinkQuality(Integer linkQuality) { this.linkQuality = linkQuality; }

    public Boolean getMotion() { return motion; }
    public void setMotion(Boolean motion) { this.motion = motion; }

    public Integer getVoltage() { return voltage; }
    public void setVoltage(Integer voltage) { this.voltage = voltage; }
}
