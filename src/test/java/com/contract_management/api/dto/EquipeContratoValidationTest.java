package com.contract_management.api.dto;

import com.contract_management.api.dto.request.EquipeContratoRequestDTO;
import com.contract_management.api.dto.request.MembroEquipeRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EquipeContratoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void deveValidarCamposInternosDeMembroEquipeQuandoAnotadoComValid() {
        // Membro com servidorId e funcaoId nulos
        MembroEquipeRequestDTO membroInvalido = new MembroEquipeRequestDTO(null, null);

        EquipeContratoRequestDTO dto = EquipeContratoRequestDTO.builder()
                .ativoId(1L)
                .contratoId(10L)
                .membros(List.of(membroInvalido))
                .build();

        Set<ConstraintViolation<EquipeContratoRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(), "Deve conter violações de validação em cascata");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("servidorId")),
                "Deve validar que servidorId é obrigatório");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("funcaoId")),
                "Deve validar que funcaoId é obrigatório");
    }

    @Test
    void naoDeveConterViolacoesQuandoMembroEstiverValido() {
        MembroEquipeRequestDTO membroValido = new MembroEquipeRequestDTO(1L, 2L);

        EquipeContratoRequestDTO dto = EquipeContratoRequestDTO.builder()
                .ativoId(1L)
                .contratoId(10L)
                .membros(List.of(membroValido))
                .build();

        Set<ConstraintViolation<EquipeContratoRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Não deve conter violações para DTO válido");
    }
}
