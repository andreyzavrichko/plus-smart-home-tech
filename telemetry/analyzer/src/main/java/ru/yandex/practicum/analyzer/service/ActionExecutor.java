package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.time.Instant;
import java.util.List;


@Service
public class ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;


    public void executeActions(String hubId, List<ScenarioEvaluator.ActionToExecute> actions) {
        for (ScenarioEvaluator.ActionToExecute actionToExecute : actions) {
            try {
                sendAction(hubId, actionToExecute);
            } catch (Exception e) {
                log.error("Failed to execute action for scenario '{}', sensor {}: {}",
                        actionToExecute.scenarioName(),
                        actionToExecute.sensorId(),
                        e.getMessage());
            }
        }
    }


    private void sendAction(String hubId, ScenarioEvaluator.ActionToExecute actionToExecute) {
        Action action = actionToExecute.action();


        DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                .setSensorId(actionToExecute.sensorId())
                .setType(mapActionType(action.getType()));

        if (action.getValue() != null) {
            actionBuilder.setValue(action.getValue());
        }


        Instant now = Instant.now();
        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName(actionToExecute.scenarioName())
                .setAction(actionBuilder.build())
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build();

        try {
            hubRouterClient.handleDeviceAction(request);
            log.info("Action sent: scenario='{}', sensor={}, type={}",
                    actionToExecute.scenarioName(),
                    actionToExecute.sensorId(),
                    action.getType());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                log.warn("Hub Router unavailable, action not sent");
            } else {
                throw e;
            }
        }
    }


    private ActionTypeProto mapActionType(ru.yandex.practicum.analyzer.model.ActionType type) {
        return switch (type) {
            case ACTIVATE -> ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ActionTypeProto.DEACTIVATE;
            case INVERSE -> ActionTypeProto.INVERSE;
            case SET_VALUE -> ActionTypeProto.SET_VALUE;
        };
    }
}