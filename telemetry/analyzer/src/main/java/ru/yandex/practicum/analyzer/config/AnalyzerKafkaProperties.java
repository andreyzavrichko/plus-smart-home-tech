package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "analyzer.kafka")
public class AnalyzerKafkaProperties {

    private String bootstrapServers;
    private String groupIdSnapshots;
    private String groupIdHubs;
    private String snapshotsTopic;
    private String hubsTopic;

}