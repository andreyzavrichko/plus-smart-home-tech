package ru.yandex.practicum.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.commerce.exception.*;

import java.util.Map;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoOrderFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoOrder(NoOrderFoundException ex) {
        log.warn("NoOrderFoundException: {}", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(Map.of("userMessage", ex.getMessage()));
    }

    @ExceptionHandler(NoDeliveryFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoDelivery(NoDeliveryFoundException ex) {
        log.warn("NoDeliveryFoundException: {}", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(Map.of("userMessage", ex.getMessage()));
    }

    @ExceptionHandler(NotEnoughInfoInOrderToCalculateException.class)
    public ResponseEntity<Map<String, String>> handleNotEnoughInfo(
            NotEnoughInfoInOrderToCalculateException ex) {
        log.warn("NotEnoughInfoInOrderToCalculateException: {}", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(Map.of("userMessage", ex.getMessage()));
    }

    @ExceptionHandler(NotAuthorizedUserException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(NotAuthorizedUserException ex) {
        log.warn("NotAuthorizedUserException: {}", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(Map.of("userMessage", ex.getMessage()));
    }

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouseException.class)
    public ResponseEntity<Map<String, String>> handleLowQuantity(
            ProductInShoppingCartLowQuantityInWarehouseException ex) {
        log.warn("LowQuantityInWarehouse: {}", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(Map.of("userMessage", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(Map.of("userMessage", "Внутренняя ошибка сервера"));
    }
}