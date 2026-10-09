package com.contract_management.api.modules.contrato.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.ativo.api.AtivoConsulta;
import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.contrato.dto.request.AtaRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.contrato.model.AtaRegistroPreco;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.repository.AtaRepository;
import com.contract_management.api.modules.contrato.repository.AtaSecretariaRepository;
import com.contract_management.api.modules.contrato.repository.TipoRepository;
import com.contract_management.api.modules.equipe.api.EquipesVinculadas;
import com.contract_management.api.modules.equipe.api.FuncaoEquipeConsulta;
import com.contract_management.api.modules.secretaria.api.SecretariaConsulta;
import com.contract_management.api.modules.servidor.api.ServidorConsulta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtaServiceTest {

    @Mock
    private AtaRepository ataRepository;

    @Mock
    private AtaSecretariaRepository ataSecretariaRepository;

    @Mock
    private TipoRepository tipoRepository;

    @Mock
    private AtivoConsulta ativoRepository;

    @Mock
    private SecretariaConsulta secretariaRepository;

    @Mock
    private EquipesVinculadas equipeContratoRepository;

    @Mock
    private ServidorConsulta servidorRepository;

    @Mock
    private FuncaoEquipeConsulta funcaoEquipeRepository;

    @InjectMocks
    private AtaService ataService;

    private Tipo tipo;
    private Ativo ativo;
    private AtaRegistroPreco ata;
    private AtaRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        tipo = Tipo.builder().id(1L).tipoArp("MATERIAL").build();
        ativo = Ativo.builder().id(1L).situacao("ATIVO").build();

        ata = AtaRegistroPreco.builder()
                .id(1L)
                .numero(50)
                .ano(2026)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .tipo(tipo)
                .ativo(ativo)
                .objeto("Fornecimento de materiais de escritório")
                .build();

        requestDTO = new AtaRequestDTO();
        requestDTO.setNumero(50);
        requestDTO.setAno(2026);
        requestDTO.setDataInicio(LocalDate.of(2026, 1, 1));
        requestDTO.setDataFim(LocalDate.of(2026, 12, 31));
        requestDTO.setTipoId(1L);
        requestDTO.setAtivoId(1L);
        requestDTO.setObjeto("Fornecimento de materiais de escritório");
    }

    @Test
    @DisplayName("Deve buscar ata por ID com sucesso")
    void deveBuscarPorIdComSucesso() {
        when(ataRepository.findComSecretariasById(1L)).thenReturn(Optional.of(ata));

        AtaResponseDTO response = ataService.buscarPorId(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNumero()).isEqualTo(50);
        assertThat(response.getAno()).isEqualTo(2026);
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando ata não encontrada")
    void deveLancarExceptionQuandoAtaNaoEncontrada() {
        when(ataRepository.findComSecretariasById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ataService.buscarPorId(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Deve criar nova ata com sucesso")
    void deveCriarAtaComSucesso() {
        when(ataRepository.findByNumeroAndAno(50, 2026)).thenReturn(Optional.empty());
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(ativoRepository.findById(1L)).thenReturn(Optional.of(ativo));
        when(ataRepository.save(any(AtaRegistroPreco.class))).thenReturn(ata);
        when(ataRepository.findComSecretariasById(1L)).thenReturn(Optional.of(ata));

        AtaResponseDTO response = ataService.criar(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getNumero()).isEqualTo(50);
        verify(ataRepository, times(1)).save(any(AtaRegistroPreco.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao criar ata com número/ano já existentes")
    void deveRejeitarAtaDuplicada() {
        when(ataRepository.findByNumeroAndAno(50, 2026)).thenReturn(Optional.of(ata));

        assertThatThrownBy(() -> ataService.criar(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já existe");

        verify(ataRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar ata com sucesso")
    void deveDeletarAtaComSucesso() {
        when(ataRepository.existsById(1L)).thenReturn(true);

        ataService.deletar(1L);

        verify(ataRepository, times(1)).deleteById(1L);
    }
}
