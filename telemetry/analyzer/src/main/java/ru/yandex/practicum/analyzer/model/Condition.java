package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "conditions")
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ConditionType type;

    @Column(name = "operation")
    @Enumerated(EnumType.STRING)
    private ConditionOperation operation;

    @Column(name = "value")
    private Integer value;

    public Condition(ConditionType type, ConditionOperation operation, Integer value) {
        this.type = type;
        this.operation = operation;
        this.value = value;
    }
}