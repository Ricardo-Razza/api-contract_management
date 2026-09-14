package com.contract_management.api.service;

import com.contract_management.api.dto.request.ContratoRequestDTO;
import com.contract_management.api.dto.response.ContratoResponseDTO;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private ContratoSecretariaRepository contratoSecretariaRepository;

    @Mock
    private TipoRepository tipoRepository;

    @Mock
    private AtivoRepository ativoRepository;

    @Mock
    private SecretariaRepository secretariaRepository;

    @InjectMocks
    private ContratoService contratoService;

    private Contrato contrato;
    private Tipo tipo;
    private Ativo ativo;
    private Secretaria secretaria;

    @BeforeEach
    void setUp() {
        tipo = Tipo.builder().id(1L).tipoArp("SERVICO").build();
        ativo = Ativo.builder().id(1L).situacao("ATIVO").build();
        secretaria = Secretaria.builder().id(1L).nome("Saúde").sigla("SMS").ativo(ativo).build();

        contrato = Contrato.builder()
                .id(10L)
                .numero(123)
                .ano(2026)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .objeto("Prestação de serviços")
                .nomeContratado("Empresa XYZ")
                .portariaDesignacao("Portaria 01/2026")
                .dataDesignacao(LocalDate.of(2026, 1, 5))
                .tipo(tipo)
                .ativo(ativo)
                .secretarias(new ArrayList<>())
                .equipe(new ArrayList<>())
                .build();

        ContratoSecretaria cs = ContratoSecretaria.builder()
                .id(1L)
                .contrato(contrato)
                .secretaria(secretaria)
                .ativo(ativo)
                .build();
        contrato.getSecretarias().add(cs);
    }

    @Test
    void deveListarTodosSemProblemaNMaisUmUtilizandoJoinFetch() {
        when(contratoRepository.findAllComSecretarias()).thenReturn(List.of(contrato));
        when(contratoRepository.carregarEquipes(anyList())).thenReturn(List.of(contrato));

        List<ContratoResponseDTO> resultado = contratoService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(123, resultado.get(0).getNumero());
        assertEquals(1, resultado.get(0).getSecretarias().size());
        assertEquals("SMS", resultado.get(0).getSecretarias().get(0).getSigla());

        // Verifica que usou as queries em lote e NÃO fez chamadas individuais por contrato
        verify(contratoRepository, times(1)).findAllComSecretarias();
        verify(contratoRepository, times(1)).carregarEquipes(anyList());
        verify(contratoSecretariaRepository, never()).findByContratoId(anyLong());
    }

    @Test
    void deveBuscarPorIdComSucessoCarregandoRelacionamentos() {
        when(contratoRepository.findComSecretariasById(10L)).thenReturn(Optional.of(contrato));
        when(contratoRepository.findComEquipeById(10L)).thenReturn(Optional.of(contrato));

        ContratoResponseDTO resultado = contratoService.buscarPorId(10L);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        verify(contratoRepository, times(1)).findComSecretariasById(10L);
        verify(contratoRepository, times(1)).findComEquipeById(10L);
        verify(contratoSecretariaRepository, never()).findByContratoId(anyLong());
    }

    @Test
    void deveLancarExcecaoQuandoBuscarPorIdInexistente() {
        when(contratoRepository.findComSecretariasById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> contratoService.buscarPorId(99L));
    }

    @Test
    void deveCriarContratoComValidacaoDeDuplicidadeOtimizada() {
        ContratoRequestDTO dto = new ContratoRequestDTO();
        dto.setNumero(200);
        dto.setAno(2026);
        dto.setDataInicio(LocalDate.of(2026, 1, 1));
        dto.setDataFim(LocalDate.of(2026, 12, 31));
        dto.setTipoId(1L);
        dto.setAtivoId(1L);
        dto.setObjeto("Novo contrato");
        dto.setNomeContratado("Fornecedor ABC");
        dto.setPortariaDesignacao("Portaria 10");
        dto.setDataDesignacao(LocalDate.of(2026, 1, 10));
        dto.setSecretariasIds(List.of(1L));

        when(contratoRepository.existsByNumeroAndAno(200, 2026)).thenReturn(false);
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(ativoRepository.findById(1L)).thenReturn(Optional.of(ativo));
        when(secretariaRepository.findAllById(List.of(1L))).thenReturn(List.of(secretaria));
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> {
            Contrato c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        ContratoResponseDTO resultado = contratoService.criar(dto);

        assertNotNull(resultado);
        assertEquals(200, resultado.getNumero());
        // Verifica que usou existsByNumeroAndAno otimizado e saveAll em lote
        verify(contratoRepository, times(1)).existsByNumeroAndAno(200, 2026);
        verify(contratoSecretariaRepository, times(1)).saveAll(anyList());
    }

    @Test
    void deveLancarExcecaoAoCriarContratoComNumeroEAnoDuplicados() {
        ContratoRequestDTO dto = new ContratoRequestDTO();
        dto.setNumero(123);
        dto.setAno(2026);

        when(contratoRepository.existsByNumeroAndAno(123, 2026)).thenReturn(true);

        assertThrows(BusinessException.class, () -> contratoService.criar(dto));
        verify(contratoRepository, never()).save(any());
    }
}
