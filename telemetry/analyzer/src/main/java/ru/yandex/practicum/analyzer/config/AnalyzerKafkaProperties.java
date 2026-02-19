package ru.yandex.practicum.analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "analyzer.kafka")
public class AnalyzerKafkaProperties {

    private String bootstrapServers;
    private String groupIdSnapshots;
    private String groupIdHubs;
    private String snapshotsTopic;
    private String hubsTopic;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getGroupIdSnapshots() {
        return groupIdSnapshots;
    }

    public void setGroupIdSnapshots(String groupIdSnapshots) {
        this.groupIdSnapshots = groupIdSnapshots;
    }

    public String getGroupIdHubs() {
        return groupIdHubs;
    }

    public void setGroupIdHubs(String groupIdHubs) {
        this.groupIdHubs = groupIdHubs;
    }

    public String getSnapshotsTopic() {
        return snapshotsTopic;
    }

    public void setSnapshotsTopic(String snapshotsTopic) {
        this.snapshotsTopic = snapshotsTopic;
    }

    public String getHubsTopic() {
        return hubsTopic;
    }

    public void setHubsTopic(String hubsTopic) {
        this.hubsTopic = hubsTopic;
    }
}