package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sensors")
public class Sensor {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "hub_id")
    private String hubId;

    public Sensor() {
    }

    public Sensor(String id, String hubId) {
        this.id = id;
        this.hubId = hubId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }
}