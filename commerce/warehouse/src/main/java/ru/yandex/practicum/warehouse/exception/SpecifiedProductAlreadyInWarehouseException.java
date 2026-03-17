package ru.yandex.practicum.warehouse.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SpecifiedProductAlreadyInWarehouseException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    public SpecifiedProductAlreadyInWarehouseException(String message) {
        super(message);
    }

}