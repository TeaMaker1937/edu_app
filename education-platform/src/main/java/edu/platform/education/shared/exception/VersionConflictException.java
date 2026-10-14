package edu.platform.education.shared.exception;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException(String message) {
        super(message);
    }
}