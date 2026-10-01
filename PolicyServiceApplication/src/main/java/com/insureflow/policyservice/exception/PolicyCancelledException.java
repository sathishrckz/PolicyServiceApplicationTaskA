package com.insureflow.policyservice.exception;

public class PolicyCancelledException extends RuntimeException {
    public PolicyCancelledException(String message) { super(message); }
}
