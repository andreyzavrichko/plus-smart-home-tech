package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryDto;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryState;
import ru.yandex.practicum.commerce.dto.order.CreateNewOrderRequest;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.order.OrderState;
import ru.yandex.practicum.commerce.dto.order.ProductReturnRequest;
import ru.yandex.practicum.commerce.dto.payment.PaymentDto;
import ru.yandex.practicum.commerce.dto.warehouse.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.commerce.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.commerce.exception.NoOrderFoundException;
import ru.yandex.practicum.commerce.exception.NotAuthorizedUserException;
import ru.yandex.practicum.commerce.feign.DeliveryClient;
import ru.yandex.practicum.commerce.feign.PaymentClient;
import ru.yandex.practicum.commerce.feign.WarehouseClient;
import ru.yandex.practicum.order.model.Order;
import ru.yandex.practicum.order.model.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WarehouseClient warehouseClient;
    private final PaymentClient paymentClient;
    private final DeliveryClient deliveryClient;

    public List<OrderDto> getClientOrders(String username) {
        validateUsername(username);
        return orderRepository.findByUsername(username).stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto createNewOrder(String username, CreateNewOrderRequest request) {
        validateUsername(username);

        Order order = Order.builder()
                .username(username)
                .shoppingCartId(request.getShoppingCart().getShoppingCartId())
                .products(request.getShoppingCart().getProducts())
                .state(OrderState.NEW)
                .fragile(false)
                .build();
        order = orderRepository.save(order);

        BookedProductsDto booked = warehouseClient.assemblyProductsForOrder(
                AssemblyProductsForOrderRequest.builder()
                        .orderId(order.getOrderId())
                        .products(order.getProducts())
                        .build());
        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.isFragile());
        order = orderRepository.save(order);

        OrderDto orderDto = OrderMapper.toDto(order);
        BigDecimal productPrice = paymentClient.productCost(orderDto);
        order.setProductPrice(productPrice);

        DeliveryDto delivery = deliveryClient.planDelivery(DeliveryDto.builder()
                .orderId(order.getOrderId())
                .fromAddress(warehouseClient.getWarehouseAddress())
                .toAddress(request.getDeliveryAddress())
                .deliveryState(DeliveryState.CREATED)
                .build());
        order.setDeliveryId(delivery.getDeliveryId());
        order = orderRepository.save(order);

        orderDto = OrderMapper.toDto(order);
        BigDecimal deliveryPrice = deliveryClient.deliveryCost(orderDto);
        order.setDeliveryPrice(deliveryPrice);

        orderDto = OrderMapper.toDto(order);
        BigDecimal totalPrice = paymentClient.getTotalCost(orderDto);
        order.setTotalPrice(totalPrice);

        orderDto = OrderMapper.toDto(order);
        PaymentDto paymentDto = paymentClient.payment(orderDto);
        order.setPaymentId(paymentDto.getPaymentId());
        order.setState(OrderState.ON_PAYMENT);
        order = orderRepository.save(order);

        log.info("Заказ {} создан для {}, статус={}", order.getOrderId(), username, order.getState());
        return OrderMapper.toDto(order);
    }

    @Transactional
    public OrderDto paymentSuccess(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setState(OrderState.PAID);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto assembly(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setState(OrderState.ASSEMBLED);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto delivery(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setState(OrderState.DELIVERED);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto complete(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setState(OrderState.COMPLETED);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        Order order = findOrThrow(orderId);
        BigDecimal deliveryPrice = deliveryClient.deliveryCost(OrderMapper.toDto(order));
        order.setDeliveryPrice(deliveryPrice);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        Order order = findOrThrow(orderId);
        BigDecimal totalPrice = paymentClient.getTotalCost(OrderMapper.toDto(order));
        order.setTotalPrice(totalPrice);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        Order order = findOrThrow(request.getOrderId());
        order.setState(OrderState.PRODUCT_RETURNED);
        orderRepository.save(order);
        warehouseClient.acceptReturn(request.getProducts());
        return OrderMapper.toDto(order);
    }

    private Order findOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Заказ не найден: " + orderId));
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Имя пользователя не должно быть пустым");
        }
    }
}