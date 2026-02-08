package ru.yandex.practicum.collector.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.avro.AvroBinarySerializer;
import ru.yandex.practicum.collector.dto.hub.HubEvent;
import ru.yandex.practicum.collector.dto.sensor.SensorEvent;
import ru.yandex.practicum.collector.kafka.CollectorKafkaProperties;
import ru.yandex.practicum.collector.kafka.KafkaSender;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;

@RestController
@RequestMapping("/events")
public class EventsController {

    private final KafkaSender sender;
    private final CollectorKafkaProperties props;

    public EventsController(KafkaSender sender, CollectorKafkaProperties props) {
        this.sender = sender;
        this.props = props;
    }

    @PostMapping("/sensors")
    public ResponseEntity<Void> collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        var avro = SensorEventMapper.map(event);
        var bytes = AvroBinarySerializer.toBytes(avro);
        sender.send(props.getSensorsTopic(), event.getHubId(), bytes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hubs")
    public ResponseEntity<Void> collectHubEvent(@Valid @RequestBody HubEvent event) {
        var avro = HubEventMapper.map(event);
        var bytes = AvroBinarySerializer.toBytes(avro);
        sender.send(props.getHubsTopic(), event.getHubId(), bytes);
        return ResponseEntity.ok().build();
    }
}
