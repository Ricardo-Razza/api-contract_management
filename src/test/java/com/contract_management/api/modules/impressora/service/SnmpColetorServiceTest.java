package com.contract_management.api.modules.impressora.service;

import org.junit.jupiter.api.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SnmpColetorServiceTest {
    private final SnmpColetorService service = new SnmpColetorService();
    private final CommunityTarget<Address> target = new CommunityTarget<>();

    @Test
    void bloqueiaComandosNaoPermitidosAntesDeEnviarPacote() {
        Snmp snmp = mock(Snmp.class);
        for (int tipo : new int[]{PDU.SET, PDU.INFORM, PDU.TRAP, PDU.RESPONSE, PDU.GETBULK, 12345}) {
            PDU request = new PDU();
            request.setType(tipo);
            assertThrows(SecurityException.class, () -> service.enviarConsulta(snmp, target, request));
        }
        assertThrows(SecurityException.class, () -> service.enviarConsulta(snmp, target, null));
        verifyNoInteractions(snmp);
    }

    @Test
    void permiteSomenteGetEGetNext() throws Exception {
        Snmp snmp = mock(Snmp.class);
        for (int tipo : new int[]{PDU.GET, PDU.GETNEXT}) {
            PDU request = new PDU();
            request.setType(tipo);
            service.enviarConsulta(snmp, target, request);
            verify(snmp).send(same(request), same(target));
        }
        verifyNoMoreInteractions(snmp);
    }

    @Test
    void pantumPriorizaContadorProprietario() throws Exception {
        Snmp snmp = mock(Snmp.class);
        ResponseEvent<Address> event = response("1.3.6.1.4.1.40093.10.3.1.1", new Integer32(8501));
        when(snmp.send(any(PDU.class), same(target))).thenAnswer(invocation -> {
            PDU request = invocation.getArgument(0);
            assertEquals(PDU.GET, request.getType());
            assertEquals("1.3.6.1.4.1.40093.10.3.1.1", request.get(0).getOid().toString());
            return event;
        });
        assertEquals(8501, service.consultarContadorPantum(snmp, target));
        verify(snmp, times(1)).send(any(PDU.class), same(target));
    }

    @Test
    void pantumNaoAceitaZeroPadraoSemContadorProprietario() throws Exception {
        Snmp snmp = mock(Snmp.class);
        ResponseEvent<Address> missing = response("1.3.6.1.4.1.40093.10.3.1.1", org.snmp4j.smi.Null.noSuchObject);
        ResponseEvent<Address> count = response("1.3.6.1.2.1.43.10.2.1.4.1", new Counter32(0));
        ResponseEvent<Address> unit = response("1.3.6.1.2.1.43.10.2.1.3.1", new Integer32(7));
        when(snmp.send(any(PDU.class), same(target))).thenReturn(missing, count, unit);
        assertNull(service.consultarContadorPantum(snmp, target));
    }

    @SuppressWarnings("unchecked")
    private ResponseEvent<Address> response(String oid, Variable value) {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid), value));
        ResponseEvent<Address> event = mock(ResponseEvent.class);
        when(event.getResponse()).thenReturn(pdu);
        return event;
    }

    @Test
    void descobreIndiceVariavelEPreservaContadorZero() throws Exception {
        Snmp snmp = mock(Snmp.class);
        when(snmp.send(any(PDU.class), same(target))).thenAnswer(invocation -> {
            PDU request = invocation.getArgument(0);
            if (request.getType() == PDU.GETNEXT) {
                return response("1.3.6.1.2.1.43.10.2.1.4.7.2", new Counter32(0));
            }
            assertEquals("1.3.6.1.2.1.43.10.2.1.3.7.2", request.get(0).getOid().toString());
            return response("1.3.6.1.2.1.43.10.2.1.3.7.2", new Integer32(7));
        });
        assertEquals(0, service.consultarContadorPadrao(snmp, target));
    }

    @Test
    void naoUsaContadorDeOutraSubarvore() throws Exception {
        Snmp snmp = mock(Snmp.class);
        ResponseEvent<Address> event = response("1.3.6.1.2.1.43.10.2.1.5.1.1", new Counter32(123));
        when(snmp.send(any(PDU.class), same(target)))
                .thenReturn(event);
        assertNull(service.consultarContadorPadrao(snmp, target));
    }

    @Test
    void ignoraUnidadeQueNaoRepresentaPaginas() throws Exception {
        Snmp snmp = mock(Snmp.class);
        ResponseEvent<Address> count = response("1.3.6.1.2.1.43.10.2.1.4.7.2", new Counter32(123));
        ResponseEvent<Address> unit = response("1.3.6.1.2.1.43.10.2.1.3.7.2", new Integer32(6));
        ResponseEvent<Address> end = response("1.3.6.1.2.1.43.10.2.1.5.1.1", new Counter32(456));
        when(snmp.send(any(PDU.class), same(target)))
                .thenReturn(count, unit, end);
        assertNull(service.consultarContadorPadrao(snmp, target));
    }

    @Test
    void encerraQuandoAgenteRepeteOid() throws Exception {
        Snmp snmp = mock(Snmp.class);
        ResponseEvent<Address> event = response("1.3.6.1.2.1.43.10.2.1.4", new Counter32(123));
        when(snmp.send(any(PDU.class), same(target)))
                .thenReturn(event);
        assertNull(service.consultarContadorPadrao(snmp, target));
        verify(snmp, times(1)).send(any(PDU.class), same(target));
    }
}
