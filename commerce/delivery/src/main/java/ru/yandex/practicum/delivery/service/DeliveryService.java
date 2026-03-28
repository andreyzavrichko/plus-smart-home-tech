package ru.yandex.practicum.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryDto;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryState;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.warehouse.AddressDto;
import ru.yandex.practicum.commerce.dto.warehouse.ShippedToDeliveryRequest;
import ru.yandex.practicum.commerce.exception.NoDeliveryFoundException;
import ru.yandex.practicum.commerce.feign.OrderClient;
import ru.yandex.practicum.commerce.feign.WarehouseClient;
import ru.yandex.practicum.delivery.model.Delivery;
import ru.yandex.practicum.delivery.repository.DeliveryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;


    @Transactional
    public DeliveryDto planDelivery(DeliveryDto dto) {
        Delivery delivery = Delivery.builder()
                .orderId(dto.getOrderId())
                .deliveryState(DeliveryState.CREATED)
                .build();
        delivery.setFromAddress(dto.getFromAddress());
        delivery.setToAddress(dto.getToAddress());
        Delivery saved = deliveryRepository.save(delivery);
        return toDto(saved);
    }


    public BigDecimal deliveryCost(OrderDto order) {
        AddressDto warehouse = warehouseClient.getWarehouseAddress();


        Delivery delivery = deliveryRepository.findByOrderId(order.getOrderId())
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Доставка не найдена для заказа: " + order.getOrderId()));

        double base = 5.0;

        double cost = warehouse.getCountry().contains("ADDRESS_2") ? base * 2 : base;

        cost += base;

        if (order.isFragile()) {
            cost += cost * 0.2;
        }

        double weight = order.getDeliveryWeight() != null ? order.getDeliveryWeight() : 0.0;
        cost += weight * 0.3;

        double volume = order.getDeliveryVolume() != null ? order.getDeliveryVolume() : 0.0;
        cost += volume * 0.2;

        String warehouseStreet = warehouse.getStreet();
        String deliveryStreet = delivery.getToStreet();
        if (warehouseStreet == null || !warehouseStreet.equalsIgnoreCase(deliveryStreet)) {
            cost += cost * 0.2;
        }

        return BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP);
    }


    @Transactional
    public void deliveryPicked(UUID orderId) {
        Delivery delivery = findByOrderOrThrow(orderId);
        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);

        orderClient.assembly(orderId);

        warehouseClient.shippedToDelivery(ShippedToDeliveryRequest.builder()
                .orderId(orderId)
                .deliveryId(delivery.getDeliveryId())
                .build());

        log.info("Доставка {} взята в работу, orderId={}", delivery.getDeliveryId(), orderId);
    }


    @Transactional
    public void deliverySuccessful(UUID orderId) {
        Delivery delivery = findByOrderOrThrow(orderId);
        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);
        orderClient.delivery(orderId);
        log.info("Доставка {} завершена успешно, orderId={}", delivery.getDeliveryId(), orderId);
    }


    @Transactional
    public void deliveryFailed(UUID orderId) {
        Delivery delivery = findByOrderOrThrow(orderId);
        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);
        orderClient.deliveryFailed(orderId);
        log.info("Доставка {} провалена, orderId={}", delivery.getDeliveryId(), orderId);
    }


    private Delivery findByOrderOrThrow(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Доставка не найдена для заказа: " + orderId));
    }

    private DeliveryDto toDto(Delivery d) {
        return DeliveryDto.builder()
                .deliveryId(d.getDeliveryId())
                .orderId(d.getOrderId())
                .deliveryState(d.getDeliveryState())
                .fromAddress(d.getFromAddress())
                .toAddress(d.getToAddress())
                .build();
    }
}