package com.yusufabdiqadir.fiberworkorders.common;

/** Thrown when a requested resource does not exist (HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super(resource + " " + id + " was not found");
    }
}
