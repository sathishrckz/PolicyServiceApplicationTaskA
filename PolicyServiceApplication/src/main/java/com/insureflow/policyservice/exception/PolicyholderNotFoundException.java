package com.insureflow.policyservice.exception;

public class PolicyholderNotFoundException extends RuntimeException {
    public PolicyholderNotFoundException(String message) { super(message); }
}
