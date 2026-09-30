package com.contract_management.api.modules.ferias.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.modules.ferias.dto.request.AgendamentoFeriasRequestDTO;
import com.contract_management.api.modules.ferias.dto.request.VerificacaoConflitoRequestDTO;
import com.contract_management.api.modules.ferias.model.AgendamentoFerias;
import com.contract_management.api.modules.ferias.model.PeriodoAquisitivo;
import com.contract_management.api.modules.ferias.model.StatusFerias;
import com.contract_management.api.modules.ferias.model.TipoAfastamento;
import com.contract_management.api.modules.ferias.repository.AgendamentoFeriasRepository;
import com.contract_management.api.modules.ferias.repository.PeriodoAquisitivoRepository;
import com.contract_management.api.modules.servidor.model.Servidor;
import com.contract_management.api.modules.servidor.repository.ServidorRepository;

class FeriasServiceTest {
    AgendamentoFeriasRepository agendamentos;
    PeriodoAquisitivoRepository periodos;
    ServidorRepository servidores;
    EntityManager em;
    PeriodoAquisitivoService saldo;
    AgendamentoFeriasService service;
    Servidor servidor;
    PeriodoAquisitivo periodo;

    @BeforeEach void setup() {
        agendamentos = mock(AgendamentoFeriasRepository.class);
        periodos = mock(PeriodoAquisitivoRepository.class);
        servidores = mock(ServidorRepository.class);
        em = mock(EntityManager.class);
        saldo = new PeriodoAquisitivoService(periodos, servidores, em, agendamentos);
        service = new AgendamentoFeriasService(agendamentos, servidores, periodos, saldo, em);
        servidor = Servidor.builder().id(1L).nome("Ana").matricula(123).setor("TI").build();
        periodo = PeriodoAquisitivo.builder().id(10L).servidor(servidor).identificador("2025/2026")
                .anoInicio(2025).anoFim(2026).dataInicio(LocalDate.of(2025,1,1)).dataFim(LocalDate.of(2025,12,31))
                .totalDias(30).diasUsados(0).diasRestantes(30).build();
        when(servidores.bloquearPorId(1L)).thenReturn(Optional.of(servidor));
        when(servidores.findById(1L)).thenReturn(Optional.of(servidor));
        when(periodos.bloquearPorId(10L)).thenReturn(Optional.of(periodo));
        when(periodos.findById(10L)).thenReturn(Optional.of(periodo));
        when(agendamentos.saveAndFlush(any())).thenAnswer(inv -> {AgendamentoFerias a=inv.getArgument(0);if(a.getId()==null)a.setId(20L);return a;});
        when(periodos.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }
    AgendamentoFeriasRequestDTO request() {
        var dto = new AgendamentoFeriasRequestDTO();dto.setServidorId(1L);dto.setPeriodoAquisitivoId(10L);
        dto.setTipoAfastamento(TipoAfastamento.FERIAS);dto.setDataInicio(LocalDate.of(2026,10,1));dto.setDataFim(LocalDate.of(2026,10,15));dto.setStatus(StatusFerias.PLANEJADO);return dto;
    }
    AgendamentoFerias existente() {
        var a=AgendamentoFerias.builder().id(20L).servidor(servidor).periodoAquisitivo(periodo).tipoAfastamento(TipoAfastamento.FERIAS)
                .status(StatusFerias.CONFIRMADO).dataInicio(LocalDate.of(2026,10,1)).dataFim(LocalDate.of(2026,10,15)).dias(15).build();
        when(agendamentos.findById(20L)).thenReturn(Optional.of(a));return a;
    }
    void conflitos(AgendamentoFerias a) {when(agendamentos.findConflitos(eq(1L),any(),any(),any(),any(),any())).thenReturn(List.of(a));}

    @Test void planejamentoReservaSaldo() {var resultado=service.criar(request());assertEquals(15,periodo.getDiasRestantes());assertEquals(15,periodo.getDiasUsados());assertEquals(StatusFerias.PLANEJADO,resultado.getStatus());}
    @Test void edicaoReaproveitaSaldoOriginal() {existente();periodo.setDiasUsados(15);periodo.setDiasRestantes(15);var dto=request();dto.setDataFim(LocalDate.of(2026,10,20));service.atualizar(20L,dto);assertEquals(10,periodo.getDiasRestantes());assertEquals(20,periodo.getDiasUsados());}
    @Test void naoTrocaServidorNaEdicao() {existente();var dto=request();dto.setServidorId(2L);assertThrows(BusinessException.class,()->service.atualizar(20L,dto));verify(agendamentos,never()).saveAndFlush(any());}
    @Test void naoUsaPeriodoDeOutroServidorNaEdicao() {existente();periodo.setServidor(Servidor.builder().id(2L).build());assertThrows(BusinessException.class,()->service.atualizar(20L,request()));assertEquals(30,periodo.getDiasRestantes());}
    @Test void sobreposicaoPropriaBloqueiaMesmoComAceite() {conflitos(existente());var dto=request();dto.setConfirmarComConflito(true);assertThrows(BusinessException.class,()->service.criar(dto));assertEquals(30,periodo.getDiasRestantes());}
    @Test void conflitoColegaRequerAceiteExplicito() {var a=existente();a.setServidor(Servidor.builder().id(2L).nome("Bia").build());conflitos(a);var dto=request();dto.setConfirmarComConflito(null);assertThrows(BusinessException.class,()->service.criar(dto));}
    @Test void permiteConflitoColegaComAceite() {var a=existente();a.setServidor(Servidor.builder().id(2L).nome("Bia").build());conflitos(a);var dto=request();dto.setConfirmarComConflito(true);assertTrue(service.criar(dto).getAlertaConflito());}
    @Test void licencaPodeUltrapassarTrintaDiasSemConsumirSaldo() {var dto=request();dto.setTipoAfastamento(TipoAfastamento.LICENCA_SAUDE);dto.setDataFim(LocalDate.of(2026,11,15));assertEquals(46,service.criar(dto).getDias());assertEquals(30,periodo.getDiasRestantes());}
    @Test void feriasNaoUltrapassamTrintaDias() {var dto=request();dto.setDataFim(LocalDate.of(2026,11,1));assertThrows(BusinessException.class,()->service.criar(dto));}
    @Test void rejeitaDatasInvertidas() {var dto=request();dto.setDataFim(dto.getDataInicio().minusDays(1));assertThrows(BusinessException.class,()->service.criar(dto));}
    @Test void rejeitaSaldoInsuficiente() {periodo.setDiasRestantes(5);periodo.setDiasUsados(25);assertThrows(BusinessException.class,()->service.criar(request()));assertEquals(5,periodo.getDiasRestantes());}
    @Test void cancelarDuasVezesEstornaUmaVezEPreservaHistorico() {var a=existente();periodo.setDiasRestantes(15);periodo.setDiasUsados(15);service.cancelar(20L);service.cancelar(20L);assertEquals(30,periodo.getDiasRestantes());assertEquals(0,periodo.getDiasUsados());assertEquals(StatusFerias.CANCELADO,a.getStatus());verify(agendamentos,never()).deleteById(any());}
    @Test void canceladoNaoPodeSerEditado() {existente().setStatus(StatusFerias.CANCELADO);assertThrows(BusinessException.class,()->service.atualizar(20L,request()));}
    @Test void verificacaoDistingueConflitoBloqueante() {conflitos(existente());var dto=new VerificacaoConflitoRequestDTO();dto.setServidorId(1L);dto.setDataInicio(request().getDataInicio());dto.setDataFim(request().getDataFim());assertTrue(service.verificarConflitos(dto).isBloqueante());}
    @Test void saldoDistingueDiasGozadosDeReservados() {var a=existente();a.setDataInicio(LocalDate.now().minusDays(4));a.setDataFim(LocalDate.now().plusDays(10));periodo.setDiasUsados(15);periodo.setDiasRestantes(15);when(agendamentos.findByPeriodoAquisitivoId(10L)).thenReturn(List.of(a));var dto=saldo.toResponseDTO(periodo);assertEquals(5,dto.getDiasGozados());assertEquals(10,dto.getDiasReservados());}
    @Test void planejamentoPassadoContinuaReservadoAteConfirmacao() {var a=existente();a.setStatus(StatusFerias.PLANEJADO);a.setDataInicio(LocalDate.now().minusDays(30));a.setDataFim(LocalDate.now().minusDays(16));periodo.setDiasUsados(15);when(agendamentos.findByPeriodoAquisitivoId(10L)).thenReturn(List.of(a));var dto=saldo.toResponseDTO(periodo);assertEquals(0,dto.getDiasGozados());assertEquals(15,dto.getDiasReservados());}
    @Test void periodoComHistoricoCanceladoNaoPodeSerExcluido() {var a=existente();a.setStatus(StatusFerias.CANCELADO);when(agendamentos.findByPeriodoAquisitivoId(10L)).thenReturn(List.of(a));assertThrows(BusinessException.class,()->saldo.deletar(10L));}
}