package ru.yandex.practicum.commerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NoProductsInShoppingCartException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    public NoProductsInShoppingCartException(String message) {
        super(message);
    }

}