package com.contract_management.api.common.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + " não encontrado(a) com ID: " + id);
    }

    public EntityNotFoundException(String entityName, String field, String value) {
        super(entityName + " não encontrado(a) com " + field + ": " + value);
    }
}