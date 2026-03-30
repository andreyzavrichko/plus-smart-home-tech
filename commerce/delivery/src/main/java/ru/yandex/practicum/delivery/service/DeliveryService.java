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

    private static final BigDecimal BASE_RATE        = new BigDecimal("5.0");
    private static final BigDecimal ADDRESS_2_FACTOR = new BigDecimal("2");
    private static final BigDecimal FRAGILE_FACTOR   = new BigDecimal("0.2");
    private static final BigDecimal WEIGHT_FACTOR    = new BigDecimal("0.3");
    private static final BigDecimal VOLUME_FACTOR    = new BigDecimal("0.2");
    private static final BigDecimal DISTANCE_FACTOR  = new BigDecimal("0.2");

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
        log.info("Доставка создана: deliveryId={}, orderId={}", saved.getDeliveryId(), saved.getOrderId());
        return toDto(saved);
    }

    public BigDecimal deliveryCost(OrderDto order) {
        UUID orderId = order.getOrderId();
        AddressDto warehouse = warehouseClient.getWarehouseAddress();

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Доставка не найдена для заказа: " + orderId));

        log.info("[deliveryCost] orderId={} | склад={} | адрес доставки={} | вес={} | объём={} | хрупкий={}",
                orderId, warehouse.getCountry(),
                delivery.getToStreet(),
                order.getDeliveryWeight(), order.getDeliveryVolume(), order.isFragile());

        BigDecimal cost = warehouse.getCountry().contains("ADDRESS_2")
                ? BASE_RATE.multiply(ADDRESS_2_FACTOR)
                : BASE_RATE;
        log.info("[deliveryCost] orderId={} | после адреса склада: {}", orderId, cost);

        cost = cost.add(BASE_RATE);
        log.info("[deliveryCost] orderId={} | после +base: {}", orderId, cost);

        if (order.isFragile()) {
            cost = cost.add(cost.multiply(FRAGILE_FACTOR));
            log.info("[deliveryCost] orderId={} | после хрупкости: {}", orderId, cost);
        }

        BigDecimal weight = order.getDeliveryWeight() != null
                ? BigDecimal.valueOf(order.getDeliveryWeight())
                : BigDecimal.ZERO;
        cost = cost.add(weight.multiply(WEIGHT_FACTOR));
        log.info("[deliveryCost] orderId={} | после веса ({}*0.3): {}", orderId, weight, cost);

        BigDecimal volume = order.getDeliveryVolume() != null
                ? BigDecimal.valueOf(order.getDeliveryVolume())
                : BigDecimal.ZERO;
        cost = cost.add(volume.multiply(VOLUME_FACTOR));
        log.info("[deliveryCost] orderId={} | после объёма ({}*0.2): {}", orderId, volume, cost);

        String warehouseStreet = warehouse.getStreet();
        String deliveryStreet  = delivery.getToStreet();
        if (warehouseStreet == null || !warehouseStreet.equalsIgnoreCase(deliveryStreet)) {
            cost = cost.add(cost.multiply(DISTANCE_FACTOR));
            log.info("[deliveryCost] orderId={} | после расстояния (+20%): {}", orderId, cost);
        }

        BigDecimal result = cost.setScale(2, RoundingMode.HALF_UP);
        log.info("[deliveryCost] orderId={} | итого: {}", orderId, result);
        return result;
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