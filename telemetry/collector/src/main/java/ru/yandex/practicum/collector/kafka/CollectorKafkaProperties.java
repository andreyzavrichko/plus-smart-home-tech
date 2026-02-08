package ru.yandex.practicum.collector.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "collector.kafka")
public class CollectorKafkaProperties {
    private String bootstrapServers;
    private String sensorsTopic;
    private String hubsTopic;

    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }

    public String getSensorsTopic() { return sensorsTopic; }
    public void setSensorsTopic(String sensorsTopic) { this.sensorsTopic = sensorsTopic; }

    public String getHubsTopic() { return hubsTopic; }
    public void setHubsTopic(String hubsTopic) { this.hubsTopic = hubsTopic; }
}
