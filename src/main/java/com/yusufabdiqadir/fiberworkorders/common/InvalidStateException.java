package com.yusufabdiqadir.fiberworkorders.common;

/** Thrown when a request is valid but conflicts with the current state of the data (HTTP 409). */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }
}
