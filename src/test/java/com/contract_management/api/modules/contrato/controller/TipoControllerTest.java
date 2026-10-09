package com.contract_management.api.modules.contrato.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import com.contract_management.api.modules.contrato.dto.response.TipoResponseDTO;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.service.TipoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoControllerTest {

    @Mock
    private TipoService tipoService;

    @InjectMocks
    private TipoController tipoController;

    @Test
    @DisplayName("Deve listar todos os tipos mapeados para TipoResponseDTO")
    void deveListarTodosRetornandoDTO() {
        Tipo t1 = Tipo.builder().id(1L).tipoArp("SERVIÇO").build();
        Tipo t2 = Tipo.builder().id(2L).tipoArp("PRODUTO").build();
        when(tipoService.listarTodos()).thenReturn(List.of(t1, t2));

        ResponseEntity<List<TipoResponseDTO>> response = tipoController.listarTodos();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getId()).isEqualTo(1L);
        assertThat(response.getBody().get(0).getTipoArp()).isEqualTo("SERVIÇO");
        assertThat(response.getBody().get(1).getId()).isEqualTo(2L);
        assertThat(response.getBody().get(1).getTipoArp()).isEqualTo("PRODUTO");
    }

    @Test
    @DisplayName("Deve buscar por ID retornando TipoResponseDTO")
    void deveBuscarPorIdRetornandoDTO() {
        Tipo t1 = Tipo.builder().id(1L).tipoArp("SERVIÇO").build();
        when(tipoService.buscarPorId(1L)).thenReturn(t1);

        ResponseEntity<TipoResponseDTO> response = tipoController.buscarPorId(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getTipoArp()).isEqualTo("SERVIÇO");
    }
}
