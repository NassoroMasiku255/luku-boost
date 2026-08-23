package com.lukuboost.electricityloan.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateOperationException extends BusinessException {

    public DuplicateOperationException(String message) {
        super("DUPLICATE_OPERATION", message, HttpStatus.CONFLICT);
    }
}
