package com.contract_management.api.modules.contrato.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import com.contract_management.api.modules.contrato.dto.request.ContratoRequestDTO;

import static org.assertj.core.api.Assertions.assertThat;

class ContratoRequestDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private ContratoRequestDTO criarDtoValido() {
        ContratoRequestDTO dto = new ContratoRequestDTO();
        dto.setNumero(10);
        dto.setAno(2026);
        dto.setDataInicio(LocalDate.of(2026, 1, 1));
        dto.setDataFim(LocalDate.of(2026, 12, 31));
        dto.setTipoId(1L);
        dto.setObjeto("Objeto do contrato");
        dto.setNomeContratado("Empresa Contratada");
        dto.setPortariaDesignacao("Portaria 123");
        dto.setDataDesignacao(LocalDate.of(2026, 1, 2));
        dto.setAtivoId(1L);
        return dto;
    }

    @Test
    @DisplayName("DTO válido não deve ter violações")
    void deveAceitarDtoValido() {
        ContratoRequestDTO dto = criarDtoValido();
        Set<ConstraintViolation<ContratoRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar dataFim anterior à dataInicio com a mensagem exata")
    void deveRejeitarDataFimAnteriorADataInicio() {
        ContratoRequestDTO dto = criarDtoValido();
        dto.setDataFim(LocalDate.of(2025, 12, 31)); // anterior a 2026-01-01

        Set<ConstraintViolation<ContratoRequestDTO>> violations = validator.validate(dto);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Data fim não pode ser anterior à data início");
    }

    @Test
    @DisplayName("Deve rejeitar nomeContratado com mais de 255 caracteres")
    void deveRejeitarNomeContratadoMaiorQue255() {
        ContratoRequestDTO dto = criarDtoValido();
        dto.setNomeContratado("A".repeat(256));

        Set<ConstraintViolation<ContratoRequestDTO>> violations = validator.validate(dto);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Nome do contratado deve ter no máximo 255 caracteres");
    }

    @Test
    @DisplayName("Deve rejeitar portariaDesignacao com mais de 100 caracteres")
    void deveRejeitarPortariaMaiorQue100() {
        ContratoRequestDTO dto = criarDtoValido();
        dto.setPortariaDesignacao("P".repeat(101));

        Set<ConstraintViolation<ContratoRequestDTO>> violations = validator.validate(dto);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Portaria de designação deve ter no máximo 100 caracteres");
    }
}
