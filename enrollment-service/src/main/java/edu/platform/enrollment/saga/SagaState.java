package edu.platform.enrollment.saga;

public enum SagaState {
    NEW,
    RESERVING,
    DEBITING,
    CONFIRMED,
    COMPENSATING,
    CANCELLED,
    MANUAL_REVIEW
}