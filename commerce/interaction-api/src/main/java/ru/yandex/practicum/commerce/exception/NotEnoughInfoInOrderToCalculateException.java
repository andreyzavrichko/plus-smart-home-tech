package ru.yandex.practicum.commerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NotEnoughInfoInOrderToCalculateException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    public NotEnoughInfoInOrderToCalculateException(String message) {
        super(message);
    }
}