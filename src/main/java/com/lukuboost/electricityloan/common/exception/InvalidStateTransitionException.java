package com.lukuboost.electricityloan.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(String currentState, String targetState) {
        super("INVALID_STATE_TRANSITION",
                "Cannot transition from " + currentState + " to " + targetState,
                HttpStatus.CONFLICT);
    }
}
