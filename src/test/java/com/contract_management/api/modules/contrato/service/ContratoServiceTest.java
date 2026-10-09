package com.contract_management.api.modules.contrato.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.ativo.repository.AtivoRepository;
import com.contract_management.api.modules.contrato.dto.request.ContratoRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.ContratoResponseDTO;
import com.contract_management.api.modules.contrato.model.Contrato;
import com.contract_management.api.modules.contrato.model.ContratoSecretaria;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.repository.ContratoRepository;
import com.contract_management.api.modules.contrato.repository.ContratoSecretariaRepository;
import com.contract_management.api.modules.contrato.repository.TipoRepository;
import com.contract_management.api.modules.equipe.dto.request.MembroEquipeRequestDTO;
import com.contract_management.api.modules.equipe.model.EquipeContrato;
import com.contract_management.api.modules.equipe.model.FuncaoEquipe;
import com.contract_management.api.modules.equipe.repository.EquipeContratoRepository;
import com.contract_management.api.modules.equipe.repository.FuncaoEquipeRepository;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.secretaria.repository.SecretariaRepository;
import com.contract_management.api.modules.servidor.model.Servidor;
import com.contract_management.api.modules.servidor.repository.ServidorRepository;

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

    @Mock
    private EquipeContratoRepository equipeContratoRepository;

    @Mock
    private ServidorRepository servidorRepository;

    @Mock
    private FuncaoEquipeRepository funcaoEquipeRepository;

    @org.mockito.Spy
    private com.contract_management.api.modules.contrato.mapper.ContratoMapper contratoMapper = org.mapstruct.factory.Mappers.getMapper(com.contract_management.api.modules.contrato.mapper.ContratoMapper.class);

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
    void deveListarPaginadoCarregandoRelacionamentosEmLote() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Contrato> pageMock = new PageImpl<>(List.of(contrato), pageable, 1);
        when(contratoRepository.findAll(pageable)).thenReturn(pageMock);
        when(contratoRepository.carregarSecretarias(anyList())).thenReturn(List.of(contrato));
        when(contratoRepository.carregarEquipes(anyList())).thenReturn(List.of(contrato));

        Page<ContratoResponseDTO> resultado = contratoService.listarPaginado(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getContent().size());
        assertEquals(123, resultado.getContent().get(0).getNumero());

        verify(contratoRepository, times(1)).findAll(pageable);
        verify(contratoRepository, times(1)).carregarSecretarias(anyList());
        verify(contratoRepository, times(1)).carregarEquipes(anyList());
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

    @Test
    void deveCriarContratoComEquipeMembrosDesignados() {
        ContratoRequestDTO dto = new ContratoRequestDTO();
        dto.setNumero(300);
        dto.setAno(2026);
        dto.setDataInicio(LocalDate.of(2026, 1, 1));
        dto.setDataFim(LocalDate.of(2026, 12, 31));
        dto.setTipoId(1L);
        dto.setAtivoId(1L);
        dto.setObjeto("Contrato com fiscal");
        dto.setNomeContratado("Fornecedor XYZ");
        dto.setPortariaDesignacao("Portaria 20");
        dto.setDataDesignacao(LocalDate.of(2026, 1, 10));
        dto.setSecretariasIds(List.of(1L));

        MembroEquipeRequestDTO membroDto = new MembroEquipeRequestDTO(5L, 2L);
        dto.setMembros(List.of(membroDto));

        when(contratoRepository.existsByNumeroAndAno(300, 2026)).thenReturn(false);
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(ativoRepository.findById(1L)).thenReturn(Optional.of(ativo));
        when(secretariaRepository.findAllById(List.of(1L))).thenReturn(List.of(secretaria));
        when(contratoRepository.save(any(Contrato.class))).thenAnswer(invocation -> {
            Contrato c = invocation.getArgument(0);
            c.setId(200L);
            return c;
        });

        Servidor servidor = Servidor.builder().id(5L).nome("Carlos").matricula(12345).build();
        FuncaoEquipe funcao = FuncaoEquipe.builder().id(2L).nome("Fiscal Técnico").build();
        when(servidorRepository.findById(5L)).thenReturn(Optional.of(servidor));
        when(funcaoEquipeRepository.findById(2L)).thenReturn(Optional.of(funcao));
        when(equipeContratoRepository.save(any(EquipeContrato.class))).thenAnswer(invocation -> {
            EquipeContrato eq = invocation.getArgument(0);
            eq.setId(1L);
            return eq;
        });

        ContratoResponseDTO resultado = contratoService.criar(dto);

        assertNotNull(resultado);
        assertEquals(300, resultado.getNumero());
        verify(equipeContratoRepository, times(1)).save(any(EquipeContrato.class));
        assertNotNull(resultado.getEquipe());
        assertEquals(1, resultado.getEquipe().size());
        assertEquals(1, resultado.getEquipe().get(0).getMembros().size());
        assertEquals("Carlos", resultado.getEquipe().get(0).getMembros().get(0).getServidorNome());
        assertEquals("Fiscal Técnico", resultado.getEquipe().get(0).getMembros().get(0).getFuncaoNome());
    }
}