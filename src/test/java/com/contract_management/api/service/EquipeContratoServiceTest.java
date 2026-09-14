package com.contract_management.api.service;

import com.contract_management.api.dto.request.EquipeContratoRequestDTO;
import com.contract_management.api.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.exception.BusinessException;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.*;
import com.contract_management.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipeContratoServiceTest {

    @Mock
    private EquipeContratoRepository equipeContratoRepository;

    @Mock
    private AtaRepository ataRepository;

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private AtivoRepository ativoRepository;

    @Mock
    private ServidorRepository servidorRepository;

    @Mock
    private FuncaoEquipeRepository funcaoEquipeRepository;

    @InjectMocks
    private EquipeContratoService equipeContratoService;

    private EquipeContrato equipe;
    private Ativo ativo;
    private Contrato contrato;

    @BeforeEach
    void setUp() {
        ativo = Ativo.builder().id(1L).situacao("ATIVO").build();
        contrato = Contrato.builder().id(10L).numero(1).ano(2026).build();

        equipe = EquipeContrato.builder()
                .id(1L)
                .ativo(ativo)
                .contrato(contrato)
                .membros(new ArrayList<>())
                .build();
    }

    @Test
    void deveBuscarPorIdComSucesso() {
        when(equipeContratoRepository.findById(1L)).thenReturn(Optional.of(equipe));

        EquipeContratoResponseDTO resultado = equipeContratoService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("ATIVO", resultado.getSituacao());
    }

    @Test
    void deveLancarEntityNotFoundExceptionQuandoEquipeNaoExistir() {
        when(equipeContratoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> equipeContratoService.buscarPorId(999L));
    }

    @Test
    void deveLancarBusinessExceptionQuandoOrigemForInvalida() {
        EquipeContratoRequestDTO dto = EquipeContratoRequestDTO.builder()
                .ativoId(1L)
                .ataId(1L)
                .contratoId(2L) // Ambos preenchidos (inválido)
                .build();

        when(ativoRepository.findById(1L)).thenReturn(Optional.of(ativo));

        assertThrows(BusinessException.class, () -> equipeContratoService.salvar(dto));
        verify(equipeContratoRepository, never()).save(any());
    }

    @Test
    void deveLancarEntityNotFoundExceptionAoDeletarEquipeInexistente() {
        when(equipeContratoRepository.existsById(999L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> equipeContratoService.deletar(999L));
        verify(equipeContratoRepository, never()).deleteById(any());
    }
}
