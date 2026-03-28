package ru.yandex.practicum.commerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NoDeliveryFoundException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.NOT_FOUND;

    public NoDeliveryFoundException(String message) {
        super(message);
    }
}