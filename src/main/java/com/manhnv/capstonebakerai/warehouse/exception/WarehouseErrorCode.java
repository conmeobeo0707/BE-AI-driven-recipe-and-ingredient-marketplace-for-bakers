package com.manhnv.capstonebakerai.warehouse.exception;
import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
@Getter @AllArgsConstructor
public enum WarehouseErrorCode implements ErrorCode {
    NOT_FOUND("Warehouse record was not found", HttpStatus.NOT_FOUND),
    INVALID_REQUEST("Warehouse request is invalid", HttpStatus.BAD_REQUEST),
    CONFLICT("Warehouse record cannot transition to the requested state", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK("Insufficient available stock", HttpStatus.CONFLICT);
    private final String message;
    private final HttpStatus httpStatus;
}
