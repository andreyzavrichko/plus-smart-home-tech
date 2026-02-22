package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

}