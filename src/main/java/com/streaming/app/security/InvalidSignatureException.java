package com.streaming.app.security;


public class InvalidSignatureException extends RuntimeException {
    public InvalidSignatureException(String msg) {
        super(msg);
    }
}