package com.dbtraining.reconx.exception;

/** TICKET-ADV072 — 401 Unauthorized: email/password did not match. */
public class InvalidCredentialsException extends ReconException {
    /** @param message a human-readable description of the auth failure */
    public InvalidCredentialsException(String message) { super(message); }
}
