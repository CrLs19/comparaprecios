package pe.edu.utp.comparaprecios.negocio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pe.edu.utp.comparaprecios.dominio.Antecedente;
import pe.edu.utp.comparaprecios.integracion.oece.OeceAdapter;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Mapeo OCDS -> Antecedente con un extracto del registro ocds-dgv273-seacev3-2024-2353-29. */
class OeceAdapterMapeoTest {

    @Test
    void mapeaLosCamposDeInteres() throws Exception {
        JsonNode raiz;
        try (InputStream in = getClass().getResourceAsStream("/oece_record_emapa_huaral.json")) {
            raiz = new ObjectMapper().readTree(in);
        }
        Antecedente a = OeceAdapter.mapear(raiz.path("records").path(0).path("compiledRelease"));

        assertEquals("ocds-dgv273-seacev3-2024-2353-29", a.getOcid());
        assertEquals(LocalDate.of(2024, 9, 10), a.getFechaPublicacion());
        assertEquals(LocalDate.of(2024, 9, 26), a.getFechaAdjudicacion());
        assertEquals(new BigDecimal("53492"), a.getValorEstimado());
        assertEquals(new BigDecimal("48114.81"), a.getMontoAdjudicado());
        assertEquals(1, a.getItems().size());
        assertEquals(new BigDecimal("100"), a.getItems().get(0).getCantidad());
        assertEquals(7, a.getNumeroDocumentos());
    }
}
