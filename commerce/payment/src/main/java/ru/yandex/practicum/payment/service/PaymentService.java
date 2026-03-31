package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.order.OrderDto;
import ru.yandex.practicum.commerce.dto.payment.PaymentDto;
import ru.yandex.practicum.commerce.dto.payment.PaymentState;
import ru.yandex.practicum.commerce.exception.NoOrderFoundException;
import ru.yandex.practicum.commerce.exception.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.commerce.feign.OrderClient;
import ru.yandex.practicum.commerce.feign.ShoppingStoreClient;
import ru.yandex.practicum.payment.model.Payment;
import ru.yandex.practicum.payment.repository.PaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final PaymentRepository paymentRepository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;


    public BigDecimal productCost(OrderDto order) {
        validateOrder(order);
        BigDecimal total = BigDecimal.ZERO;
        for (var entry : order.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            long qty = entry.getValue();
            BigDecimal price = shoppingStoreClient.getProduct(productId).getPrice();
            total = total.add(price.multiply(BigDecimal.valueOf(qty)));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalCost(OrderDto order) {
        validateOrder(order);
        BigDecimal productTotal = productCost(order);
        BigDecimal fee = productTotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal delivery = order.getDeliveryPrice() != null
                ? order.getDeliveryPrice()
                : BigDecimal.ZERO;
        return productTotal.add(fee).add(delivery).setScale(2, RoundingMode.HALF_UP);
    }


    @Transactional
    public PaymentDto payment(OrderDto order) {
        validateOrder(order);
        BigDecimal productTotal = productCost(order);
        BigDecimal fee = productTotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal delivery = order.getDeliveryPrice() != null
                ? order.getDeliveryPrice()
                : BigDecimal.ZERO;
        BigDecimal total = productTotal.add(fee).add(delivery).setScale(2, RoundingMode.HALF_UP);

        Payment saved = paymentRepository.save(Payment.builder()
                .orderId(order.getOrderId())
                .productTotal(productTotal)
                .deliveryTotal(delivery)
                .feeTotal(fee)
                .totalPayment(total)
                .state(PaymentState.PENDING)
                .build());

        return toDto(saved);
    }

    @Transactional
    public void paymentSuccess(UUID paymentId) {
        Payment payment = findOrThrow(paymentId);
        payment.setState(PaymentState.SUCCESS);
        paymentRepository.save(payment);
        orderClient.payment(payment.getOrderId());
        log.info("Оплата {} прошла успешно, orderId={}", paymentId, payment.getOrderId());
    }


    @Transactional
    public void paymentFailed(UUID paymentId) {
        Payment payment = findOrThrow(paymentId);
        payment.setState(PaymentState.FAILED);
        paymentRepository.save(payment);
        orderClient.paymentFailed(payment.getOrderId());
        log.info("Оплата {} не прошла, orderId={}", paymentId, payment.getOrderId());
    }


    private Payment findOrThrow(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundException(
                        "Оплата не найдена: " + paymentId));
    }

    private void validateOrder(OrderDto order) {
        if (order == null || order.getProducts() == null || order.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Недостаточно информации в заказе для расчёта");
        }
    }

    private PaymentDto toDto(Payment p) {
        return PaymentDto.builder()
                .paymentId(p.getPaymentId())
                .totalPayment(p.getTotalPayment())
                .deliveryTotal(p.getDeliveryTotal())
                .feeTotal(p.getFeeTotal())
                .build();
    }
}