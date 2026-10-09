package com.contract_management.api.modules.contrato.controller;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.common.exception.GlobalExceptionHandler;
import com.contract_management.api.modules.contrato.dto.request.ContratoRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.ContratoResponseDTO;
import com.contract_management.api.modules.contrato.service.ContratoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContratoControllerTest {

    @Mock
    private ContratoService contratoService;

    @InjectMocks
    private ContratoController contratoController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contratoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /contratos - Deve retornar 200 com lista de contratos")
    void listarTodos_DeveRetornar200ELista() throws Exception {
        ContratoResponseDTO dto = new ContratoResponseDTO();
        dto.setId(1L);
        dto.setNumero(10);
        dto.setAno(2026);
        when(contratoService.listarTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/contratos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].numero").value(10))
                .andExpect(jsonPath("$[0].ano").value(2026));
    }

    @Test
    @DisplayName("GET /contratos/{id} - Deve retornar 200 quando contrato existir")
    void buscarPorId_QuandoExistir_DeveRetornar200() throws Exception {
        ContratoResponseDTO dto = new ContratoResponseDTO();
        dto.setId(1L);
        dto.setNumero(10);
        dto.setAno(2026);
        when(contratoService.buscarPorId(1L)).thenReturn(dto);

        mockMvc.perform(get("/contratos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numero").value(10));
    }

    @Test
    @DisplayName("GET /contratos/{id} - Deve retornar 404 quando contrato não existir")
    void buscarPorId_QuandoNaoExistir_DeveRetornar404() throws Exception {
        when(contratoService.buscarPorId(99L)).thenThrow(new EntityNotFoundException("Contrato", 99L));

        mockMvc.perform(get("/contratos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Contrato não encontrado(a) com ID: 99"));
    }

    @Test
    @DisplayName("POST /contratos - Deve retornar 201 quando DTO for válido")
    void criar_QuandoValido_DeveRetornar201() throws Exception {
        ContratoRequestDTO request = new ContratoRequestDTO();
        request.setNumero(10);
        request.setAno(2026);
        request.setDataInicio(LocalDate.of(2026, 1, 1));
        request.setDataFim(LocalDate.of(2026, 12, 31));
        request.setTipoId(1L);
        request.setObjeto("Aquisição de insumos");
        request.setNomeContratado("Empresa Fornecedora LTDA");
        request.setPortariaDesignacao("Portaria 123/2026");
        request.setDataDesignacao(LocalDate.of(2026, 1, 5));
        request.setAtivoId(1L);

        ContratoResponseDTO response = new ContratoResponseDTO();
        response.setId(1L);
        response.setNumero(10);
        response.setAno(2026);
        when(contratoService.criar(any(ContratoRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numero").value(10));
    }

    @Test
    @DisplayName("POST /contratos - Deve retornar 400 quando campos obrigatórios estiverem ausentes")
    void criar_QuandoInvalido_DeveRetornar400() throws Exception {
        ContratoRequestDTO request = new ContratoRequestDTO();
        // Request sem campos obrigatórios

        mockMvc.perform(post("/contratos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.numero").exists());
    }

    @Test
    @DisplayName("PUT /contratos/{id} - Deve retornar 200 quando atualização for válida")
    void atualizar_QuandoValido_DeveRetornar200() throws Exception {
        ContratoRequestDTO request = new ContratoRequestDTO();
        request.setNumero(10);
        request.setAno(2026);
        request.setDataInicio(LocalDate.of(2026, 1, 1));
        request.setDataFim(LocalDate.of(2026, 12, 31));
        request.setTipoId(1L);
        request.setObjeto("Atualização de serviço");
        request.setNomeContratado("Empresa Nova LTDA");
        request.setPortariaDesignacao("Portaria 456/2026");
        request.setDataDesignacao(LocalDate.of(2026, 1, 5));
        request.setAtivoId(1L);

        ContratoResponseDTO response = new ContratoResponseDTO();
        response.setId(1L);
        response.setNumero(10);
        response.setAno(2026);
        when(contratoService.atualizar(eq(1L), any(ContratoRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/contratos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /contratos/{id} - Deve retornar 204")
    void deletar_DeveRetornar204() throws Exception {
        doNothing().when(contratoService).deletar(1L);

        mockMvc.perform(delete("/contratos/1"))
                .andExpect(status().isNoContent());

        verify(contratoService).deletar(1L);
    }
}
