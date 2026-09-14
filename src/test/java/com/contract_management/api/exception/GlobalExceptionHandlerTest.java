package com.contract_management.api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void deveSerializarErrorResponseComSucessoParaEntityNotFound() throws Exception {
        EntityNotFoundException ex = new EntityNotFoundException("Contrato", 10L);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleEntityNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        // Garante que o Jackson consegue serializar o ErrorResponse (que possui getters)
        String json = objectMapper.writeValueAsString(response.getBody());
        assertNotNull(json);
        assertTrue(json.contains("\"status\":404"));
        assertTrue(json.contains("Contrato"));
        assertFalse(json.contains("errors"), "Não deve serializar errors quando nulo");
    }

    @Test
    void deveSerializarErrorResponseComSucessoParaBusinessException() throws Exception {
        BusinessException ex = new BusinessException("Regra de negócio violada");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());

        String json = objectMapper.writeValueAsString(response.getBody());
        assertNotNull(json);
        assertTrue(json.contains("\"status\":400"));
        assertTrue(json.contains("Regra de negócio violada"));
    }
}
