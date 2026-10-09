package com.contract_management.api.modules.contrato.controller;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.common.exception.GlobalExceptionHandler;
import com.contract_management.api.modules.contrato.dto.request.AtaRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.contrato.service.AtaService;
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
class AtaControllerTest {

    @Mock
    private AtaService ataService;

    @InjectMocks
    private AtaController ataController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ataController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /atas - Deve retornar 200 com lista de atas")
    void listarTodos_DeveRetornar200ELista() throws Exception {
        AtaResponseDTO dto = new AtaResponseDTO();
        dto.setId(1L);
        dto.setNumero(5);
        dto.setAno(2026);
        when(ataService.listarTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/atas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].numero").value(5))
                .andExpect(jsonPath("$[0].ano").value(2026));
    }

    @Test
    @DisplayName("GET /atas/{id} - Deve retornar 200 quando ata existir")
    void buscarPorId_QuandoExistir_DeveRetornar200() throws Exception {
        AtaResponseDTO dto = new AtaResponseDTO();
        dto.setId(1L);
        dto.setNumero(5);
        dto.setAno(2026);
        when(ataService.buscarPorId(1L)).thenReturn(dto);

        mockMvc.perform(get("/atas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numero").value(5));
    }

    @Test
    @DisplayName("GET /atas/{id} - Deve retornar 404 quando ata não existir")
    void buscarPorId_QuandoNaoExistir_DeveRetornar404() throws Exception {
        when(ataService.buscarPorId(88L)).thenThrow(new EntityNotFoundException("Ata", 88L));

        mockMvc.perform(get("/atas/88"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ata não encontrado(a) com ID: 88"));
    }

    @Test
    @DisplayName("POST /atas - Deve retornar 201 quando DTO for válido")
    void criar_QuandoValido_DeveRetornar201() throws Exception {
        AtaRequestDTO request = new AtaRequestDTO();
        request.setNumero(5);
        request.setAno(2026);
        request.setDataInicio(LocalDate.of(2026, 1, 1));
        request.setDataFim(LocalDate.of(2026, 12, 31));
        request.setTipoId(1L);
        request.setObjeto("Registro de preços de papel");
        request.setAtivoId(1L);

        AtaResponseDTO response = new AtaResponseDTO();
        response.setId(1L);
        response.setNumero(5);
        response.setAno(2026);
        when(ataService.criar(any(AtaRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/atas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numero").value(5));
    }

    @Test
    @DisplayName("POST /atas - Deve retornar 400 quando campos obrigatórios estiverem ausentes")
    void criar_QuandoInvalido_DeveRetornar400() throws Exception {
        AtaRequestDTO request = new AtaRequestDTO();
        // Request sem campos obrigatórios

        mockMvc.perform(post("/atas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.numero").exists());
    }

    @Test
    @DisplayName("PUT /atas/{id} - Deve retornar 200 quando atualização for válida")
    void atualizar_QuandoValido_DeveRetornar200() throws Exception {
        AtaRequestDTO request = new AtaRequestDTO();
        request.setNumero(5);
        request.setAno(2026);
        request.setDataInicio(LocalDate.of(2026, 1, 1));
        request.setDataFim(LocalDate.of(2026, 12, 31));
        request.setTipoId(1L);
        request.setObjeto("Registro de preços atualizado");
        request.setAtivoId(1L);

        AtaResponseDTO response = new AtaResponseDTO();
        response.setId(1L);
        response.setNumero(5);
        response.setAno(2026);
        when(ataService.atualizar(eq(1L), any(AtaRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/atas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /atas/{id} - Deve retornar 204")
    void deletar_DeveRetornar204() throws Exception {
        doNothing().when(ataService).deletar(1L);

        mockMvc.perform(delete("/atas/1"))
                .andExpect(status().isNoContent());

        verify(ataService).deletar(1L);
    }
}
