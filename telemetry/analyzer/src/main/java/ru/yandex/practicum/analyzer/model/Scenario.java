package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "scenarios")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "hub_id")
    private String hubId;

    @Column(name = "name")
    private String name;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "scenario_conditions",
            joinColumns = @JoinColumn(name = "scenario_id"),
            inverseJoinColumns = @JoinColumn(name = "condition_id")
    )
    @MapKeyJoinColumn(name = "sensor_id")
    private Map<Sensor, Condition> conditions = new HashMap<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "scenario_actions",
            joinColumns = @JoinColumn(name = "scenario_id"),
            inverseJoinColumns = @JoinColumn(name = "action_id")
    )
    @MapKeyJoinColumn(name = "sensor_id")
    private Map<Sensor, Action> actions = new HashMap<>();

    public Scenario() {
    }

    public Scenario(String hubId, String name) {
        this.hubId = hubId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Sensor, Condition> getConditions() {
        return conditions;
    }

    public void setConditions(Map<Sensor, Condition> conditions) {
        this.conditions = conditions;
    }

    public Map<Sensor, Action> getActions() {
        return actions;
    }

    public void setActions(Map<Sensor, Action> actions) {
        this.actions = actions;
    }
}