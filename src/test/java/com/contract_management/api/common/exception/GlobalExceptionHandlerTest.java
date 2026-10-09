package com.contract_management.api.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

import com.contract_management.api.modules.contrato.model.Contrato;

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

    @Test
    void deveRetornar400ParaHttpMessageNotReadable() {
        org.springframework.http.converter.HttpMessageNotReadableException ex =
                new org.springframework.http.converter.HttpMessageNotReadableException("JSON syntax error", new org.springframework.mock.http.MockHttpInputMessage(new byte[0]));
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Corpo da requisição inválido"));
    }

    @Test
    void deveRetornar400ParaMethodArgumentTypeMismatch() {
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex =
                new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null);
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("id"));
    }

    @Test
    void deveRetornar409ParaDataIntegrityViolation() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException("foreign key constraint fails");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Operação não permitida"));
    }

    @Test
    void deveRetornar500ELogarStacktraceParaExceptionGenerica() {
        Exception ex = new RuntimeException("Erro inesperado de teste");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Ocorreu um erro interno no servidor", response.getBody().getMessage());
    }
}