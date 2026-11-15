package com.ed.triage.exception;

public class NoDoctorAvailableException extends RuntimeException {
    public NoDoctorAvailableException(String message) {
        super(message);
    }
}
