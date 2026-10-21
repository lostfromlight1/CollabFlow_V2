package com.collabflow.auth.exception;

import com.collabflow.common.exception.ResourceNotFoundException;

/** Auth-specific not-found; maps via shared GlobalExceptionHandler. */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
