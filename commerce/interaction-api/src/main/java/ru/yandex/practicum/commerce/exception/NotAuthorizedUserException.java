package ru.yandex.practicum.commerce.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NotAuthorizedUserException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;

    public NotAuthorizedUserException(String message) {
        super(message);
    }

}