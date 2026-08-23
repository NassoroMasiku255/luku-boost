package com.lukuboost.electricityloan.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String entityName, String identifier) {
        super("NOT_FOUND", entityName + " not found: " + identifier, HttpStatus.NOT_FOUND);
    }
}
