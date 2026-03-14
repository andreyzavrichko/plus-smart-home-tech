package ru.yandex.practicum.warehouse.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NoSpecifiedProductInWarehouseException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    public NoSpecifiedProductInWarehouseException(String message) {
        super(message);
    }

}