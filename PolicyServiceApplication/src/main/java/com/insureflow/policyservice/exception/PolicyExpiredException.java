package com.insureflow.policyservice.exception;

public class PolicyExpiredException extends RuntimeException {
    public PolicyExpiredException(String message) { super(message); }
}
