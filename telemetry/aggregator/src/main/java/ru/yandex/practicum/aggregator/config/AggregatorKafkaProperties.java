package ru.yandex.practicum.aggregator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aggregator.kafka")
public class AggregatorKafkaProperties {

    private String bootstrapServers;
    private String groupId;
    private String sensorsTopic;
    private String snapshotsTopic;
    private Consumer consumer = new Consumer();
    private Producer producer = new Producer();

    public static class Consumer {
        private boolean enableAutoCommit = false;
        private String autoOffsetReset = "earliest";
        private String keyDeserializer;
        private String valueDeserializer;

        public boolean isEnableAutoCommit() { return enableAutoCommit; }
        public void setEnableAutoCommit(boolean enableAutoCommit) { this.enableAutoCommit = enableAutoCommit; }

        public String getAutoOffsetReset() { return autoOffsetReset; }
        public void setAutoOffsetReset(String autoOffsetReset) { this.autoOffsetReset = autoOffsetReset; }

        public String getKeyDeserializer() { return keyDeserializer; }
        public void setKeyDeserializer(String keyDeserializer) { this.keyDeserializer = keyDeserializer; }

        public String getValueDeserializer() { return valueDeserializer; }
        public void setValueDeserializer(String valueDeserializer) { this.valueDeserializer = valueDeserializer; }
    }

    public static class Producer {
        private String acks = "1";
        private int lingerMs = 5;
        private int batchSize = 32768;
        private String keySerializer;
        private String valueSerializer;

        public String getAcks() { return acks; }
        public void setAcks(String acks) { this.acks = acks; }

        public int getLingerMs() { return lingerMs; }
        public void setLingerMs(int lingerMs) { this.lingerMs = lingerMs; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public String getKeySerializer() { return keySerializer; }
        public void setKeySerializer(String keySerializer) { this.keySerializer = keySerializer; }

        public String getValueSerializer() { return valueSerializer; }
        public void setValueSerializer(String valueSerializer) { this.valueSerializer = valueSerializer; }
    }

    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getSensorsTopic() { return sensorsTopic; }
    public void setSensorsTopic(String sensorsTopic) { this.sensorsTopic = sensorsTopic; }

    public String getSnapshotsTopic() { return snapshotsTopic; }
    public void setSnapshotsTopic(String snapshotsTopic) { this.snapshotsTopic = snapshotsTopic; }

    public Consumer getConsumer() { return consumer; }
    public void setConsumer(Consumer consumer) { this.consumer = consumer; }

    public Producer getProducer() { return producer; }
    public void setProducer(Producer producer) { this.producer = producer; }
}