package com.manhnv.capstonebakerai.hobby.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HobbyErrorCode implements ErrorCode {
    HOBBY_NOT_FOUND("Hobby was not found", HttpStatus.NOT_FOUND),
    HOBBY_NAME_ALREADY_EXISTS("A hobby with this name already exists", HttpStatus.CONFLICT);

    private final String message;
    private final HttpStatus httpStatus;
}
