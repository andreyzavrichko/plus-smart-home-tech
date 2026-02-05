package ru.yandex.practicum.collector.dto.sensor;

public final class LightSensorEvent extends SensorEvent {
    private Integer linkQuality;
    private Integer luminosity;

    public Integer getLinkQuality() { return linkQuality; }
    public void setLinkQuality(Integer linkQuality) { this.linkQuality = linkQuality; }

    public Integer getLuminosity() { return luminosity; }
    public void setLuminosity(Integer luminosity) { this.luminosity = luminosity; }
}
