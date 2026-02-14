package ru.yandex.practicum.aggregator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aggregator.kafka")
public class AggregatorKafkaProperties {

    private String bootstrapServers;
    private String groupId;
    private String sensorsTopic;
    private String snapshotsTopic;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSensorsTopic() {
        return sensorsTopic;
    }

    public void setSensorsTopic(String sensorsTopic) {
        this.sensorsTopic = sensorsTopic;
    }

    public String getSnapshotsTopic() {
        return snapshotsTopic;
    }

    public void setSnapshotsTopic(String snapshotsTopic) {
        this.snapshotsTopic = snapshotsTopic;
    }
}