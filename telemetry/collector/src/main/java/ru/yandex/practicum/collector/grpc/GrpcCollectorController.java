package ru.yandex.practicum.collector.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.collector.avro.AvroBinarySerializer;
import ru.yandex.practicum.collector.kafka.CollectorKafkaProperties;
import ru.yandex.practicum.collector.kafka.KafkaSender;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@GrpcService
public class GrpcCollectorController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcCollectorController.class);

    private final KafkaSender kafkaSender;
    private final CollectorKafkaProperties kafkaProperties;

    public GrpcCollectorController(KafkaSender kafkaSender, CollectorKafkaProperties kafkaProperties) {
        this.kafkaSender = kafkaSender;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.debug("gRPC: Received sensor event from hub: {}, sensor: {}", request.getHubId(), request.getId());

            SensorEventAvro avroEvent = ProtoToAvroSensorMapper.map(request);


            byte[] eventBytes = AvroBinarySerializer.toBytes(avroEvent);


            long timestamp = request.getTimestamp().getSeconds() * 1000 + request.getTimestamp().getNanos() / 1_000_000;
            kafkaSender.send(
                    kafkaProperties.getSensorsTopic(),
                    timestamp,
                    request.getHubId(),
                    eventBytes
            );

            log.info("gRPC: Sensor event processed and sent to Kafka. Hub: {}, Sensor: {}",
                    request.getHubId(), request.getId());


            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC: Error processing sensor event", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to process sensor event: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.debug("gRPC: Received hub event from hub: {}", request.getHubId());


            HubEventAvro avroEvent = ProtoToAvroHubMapper.map(request);


            byte[] eventBytes = AvroBinarySerializer.toBytes(avroEvent);


            long timestamp = request.getTimestamp().getSeconds() * 1000 + request.getTimestamp().getNanos() / 1_000_000;
            kafkaSender.send(
                    kafkaProperties.getHubsTopic(),
                    timestamp,
                    request.getHubId(),
                    eventBytes
            );

            log.info("gRPC: Hub event processed and sent to Kafka. Hub: {}", request.getHubId());


            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("gRPC: Error processing hub event", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to process hub event: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}