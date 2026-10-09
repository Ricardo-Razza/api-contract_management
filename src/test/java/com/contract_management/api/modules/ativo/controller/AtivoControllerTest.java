package com.contract_management.api.modules.ativo.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import com.contract_management.api.modules.ativo.dto.response.AtivoResponseDTO;
import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.ativo.service.AtivoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtivoControllerTest {

    @Mock
    private AtivoService ativoService;

    @InjectMocks
    private AtivoController ativoController;

    @Test
    @DisplayName("Deve listar todos os ativos mapeados para AtivoResponseDTO")
    void deveListarTodosRetornandoDTO() {
        Ativo a1 = Ativo.builder().id(1L).situacao("ATIVO").build();
        Ativo a2 = Ativo.builder().id(2L).situacao("DESATIVADO").build();
        when(ativoService.listarTodos()).thenReturn(List.of(a1, a2));

        ResponseEntity<List<AtivoResponseDTO>> response = ativoController.listarTodos();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getId()).isEqualTo(1L);
        assertThat(response.getBody().get(0).getSituacao()).isEqualTo("ATIVO");
        assertThat(response.getBody().get(1).getId()).isEqualTo(2L);
        assertThat(response.getBody().get(1).getSituacao()).isEqualTo("DESATIVADO");
    }

    @Test
    @DisplayName("Deve buscar por ID retornando AtivoResponseDTO")
    void deveBuscarPorIdRetornandoDTO() {
        Ativo a1 = Ativo.builder().id(1L).situacao("ATIVO").build();
        when(ativoService.buscarPorId(1L)).thenReturn(a1);

        ResponseEntity<AtivoResponseDTO> response = ativoController.buscarPorId(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getSituacao()).isEqualTo("ATIVO");
    }
}
