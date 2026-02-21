package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "actions")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ActionType type;

    @Column(name = "value")
    private Integer value;

    public Action() {
    }

    public Action(ActionType type, Integer value) {
        this.type = type;
        this.value = value;
    }

}