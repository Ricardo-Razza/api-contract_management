package com.contract_management.api.modules.contrato.service;

import com.contract_management.api.modules.contrato.dto.request.DocumentoFiltro;
import com.contract_management.api.modules.contrato.dto.response.ContratoResponseDTO;
import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.equipe.dto.response.MembroEquipeResponseDTO;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DocumentoListagemTest {
    private ContratoResponseDTO contrato(long id, int numero, int ano) {
        var item = new ContratoResponseDTO();
        item.setId(id); item.setNumero(numero); item.setAno(ano);
        item.setObjeto("Aquisição de merenda"); item.setTipo("SERVICO"); item.setSituacao("ATIVO");
        item.setDataFim(LocalDate.now().plusDays(45));
        var sec = new SecretariaResponseDTO(); sec.setId(7L); sec.setSigla("SMED"); sec.setNome("Educação");
        item.setSecretarias(List.of(sec));
        var membro = MembroEquipeResponseDTO.builder().servidorNome("João Silva").servidorCargo("Professor").funcaoNome("Fiscal técnico").build();
        item.setEquipe(List.of(EquipeContratoResponseDTO.builder().membros(List.of(membro)).build()));
        return item;
    }

    @Test
    void buscaIgnoraAcentosEEncontraTermosEmCamposDiferentes() {
        var filtro = new DocumentoFiltro(); filtro.setSearch("educacao merenda joao");
        var result = DocumentoListagem.contratos(List.of(contrato(1, 10, 2026)), PageRequest.of(0, 25), filtro);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void buscaNumericaPreservaCorrespondenciaDeToken() {
        assertTrue(DocumentoListagem.busca("1/2026 educação", "1 2026"));
        assertFalse(DocumentoListagem.busca("10 2026", "1"));
    }

    @Test
    void filtrosDeSecretariaPessoaEAnoSaoAplicadosAntesDaPaginacao() {
        var filtro = new DocumentoFiltro(); filtro.setAno(2026); filtro.setSecretarias(List.of(7L)); filtro.setPessoas(List.of("Joao fiscal"));
        var result = DocumentoListagem.contratos(List.of(contrato(1, 1, 2025), contrato(2, 2, 2026), contrato(3, 3, 2026)),
                PageRequest.of(1, 1, Sort.by("id")), filtro);
        assertEquals(2, result.getTotalElements()); assertEquals(3L, result.getContent().getFirst().getId());
    }

    @Test
    void numeroOrdenaPrimeiroPorAnoSemModificarAListaOriginal() {
        var original = List.of(contrato(1, 1, 2026), contrato(2, 20, 2025), contrato(3, 2, 2026));
        var result = DocumentoListagem.contratos(original, PageRequest.of(0, 25, Sort.by("numero")), new DocumentoFiltro());
        assertEquals(List.of(2L, 1L, 3L), result.map(ContratoResponseDTO::getId).getContent());
        assertEquals(1L, original.getFirst().getId());
    }

    @Test
    void vigenciaRespeitaFaixasDeAlertaEVencimento() {
        var filtro = new DocumentoFiltro(); filtro.setVigencia("ATENCAO");
        var result = DocumentoListagem.contratos(List.of(contrato(1, 1, 2026)), PageRequest.of(0, 25), filtro);
        assertEquals(1, result.getTotalElements());
        filtro.setVigencia("CRITICA");
        assertEquals(0, DocumentoListagem.contratos(result.getContent(), PageRequest.of(0, 25), filtro).getTotalElements());
    }

    @Test
    void paginaAlemDoFimRetornaConteudoVazioETotalCorreto() {
        var result = DocumentoListagem.contratos(List.of(contrato(1, 1, 2026)), PageRequest.of(10, 25), new DocumentoFiltro());
        assertTrue(result.isEmpty()); assertEquals(1, result.getTotalElements());
    }

    @Test
    void atasPreservamComparacaoDeTipoEStatusSemDiferencaDeCaixa() {
        var ata = new AtaResponseDTO(); ata.setId(1L); ata.setNumero(1); ata.setAno(2026);
        ata.setTipo("Servico "); ata.setSituacao(" ativo ");
        var filtro = new DocumentoFiltro(); filtro.setTipo("SERVICO"); filtro.setStatus("ATIVO");
        assertEquals(1, DocumentoListagem.atas(List.of(ata), PageRequest.of(0, 25), filtro).getTotalElements());
    }
}
