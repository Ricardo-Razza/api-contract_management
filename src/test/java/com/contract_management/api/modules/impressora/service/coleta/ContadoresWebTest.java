package com.contract_management.api.modules.impressora.service.coleta;

import com.contract_management.api.modules.impressora.model.ColetaContadorItem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContadoresWebTest {
    @Test void jsonAceitaChavesComAspasEZeroSemInventarCamposAusentes() {
        var item = ColetaContadorItem.builder().contadorTotal(100).copiasPrint(70).build();
        assertTrue(ContadoresWeb.aplicarSws(item, "{\"GXI_BILLING_TOTAL_IMP_CNT\": \"120\", GXI_BILLING_COPY_TOTAL_IMP_CNT: 0}"));
        assertEquals(120, item.getContadorTotal());
        assertEquals(0, item.getCopiasCopiador());
        assertEquals(70, item.getCopiasPrint());
        assertNull(item.getCopiasScanner());
    }

    @Test void xmlPreservaZerosENaoDeduzImpressoesDoContadorTotal() {
        var item = new ColetaContadorItem();
        assertTrue(ContadoresWeb.aplicarXml(item, "<u:TotalPrintEnginePageCount>500</u:TotalPrintEnginePageCount>"
                + "<u:ScanImages>0</u:ScanImages><u:TotalImagesScanned>25</u:TotalImagesScanned>"));
        assertEquals(500, item.getContadorTotal());
        assertEquals(0, item.getCopiasScanner());
        assertNull(item.getCopiasPrint());
        assertNull(item.getCopiasCopiador());
    }

    @Test void respostaInvalidaNaoSubstituiLeituraSnmp() {
        var item = ColetaContadorItem.builder().contadorTotal(42).build();
        assertFalse(ContadoresWeb.aplicarSws(item, "{GXI_BILLING_TOTAL_IMP_CNT: 99999999999999}"));
        assertFalse(ContadoresWeb.aplicarXml(item, "<TotalImpressions>-1</TotalImpressions>"));
        assertEquals(42, item.getContadorTotal());
    }
}
