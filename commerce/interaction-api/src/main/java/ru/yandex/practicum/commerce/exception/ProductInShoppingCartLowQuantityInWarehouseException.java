package ru.yandex.practicum.commerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ProductInShoppingCartLowQuantityInWarehouseException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    public ProductInShoppingCartLowQuantityInWarehouseException(String message) {
        super(message);
    }

}