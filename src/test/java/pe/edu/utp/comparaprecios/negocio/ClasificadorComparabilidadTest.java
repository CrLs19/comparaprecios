package pe.edu.utp.comparaprecios.negocio;

import org.junit.jupiter.api.Test;
import pe.edu.utp.comparaprecios.dominio.Antecedente;
import pe.edu.utp.comparaprecios.dominio.ItemContratado;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad.DESCARTADO;
import static pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad.REVISION;
import static pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad.UTILIZABLE;

/** Casos tomados de procesos reales del Portal de Contrataciones Abiertas. */
class ClasificadorComparabilidadTest {

    private final ClasificadorComparabilidad clasificador = new ClasificadorComparabilidad(new ExtractorModelo());

    private static Antecedente proceso(String valorEstimado, String adjudicado, int adjudicaciones, ItemContratado... items) {
        Antecedente a = new Antecedente();
        a.setValorEstimado(new BigDecimal(valorEstimado));
        a.setMontoAdjudicado(adjudicado == null ? null : new BigDecimal(adjudicado));
        a.setNumeroAdjudicaciones(adjudicaciones);
        a.setItems(new java.util.ArrayList<>(List.of(items)));
        return a;
    }

    private static ItemContratado item(String descripcion, String cantidad, String estado) {
        return new ItemContratado(descripcion, null, new BigDecimal(cantidad), "Unidad", null, estado);
    }

    @Test
    void ateTk3432EsUtilizableYCalculaPrecioUnitario() {
        Antecedente a = clasificador.clasificar(proceso("75056.5", "75056.5", 1,
                item("ADQUISICION DE TONER PARA IMPRESORA TK-3432 NEGRO", "136", "CONTRATADO")));
        assertEquals(UTILIZABLE, a.getEstado());
        assertEquals(List.of("TK3432"), a.getModelos());
        assertEquals(new BigDecimal("551.89"), a.getPrecioUnitario());
    }

    @Test
    void emapaHuaralReuneVariosModelosEnUnItem() {
        Antecedente a = clasificador.clasificar(proceso("53492", "48114.81", 1,
                item("De acuerdo a la siguiente distribucion: 24 uni codigo 213-A 30 uni codigo TK-3432 "
                        + "40 uni codigo TK 3182 06 uni codigo TK 1175", "100", "CONTRATADO")));
        assertEquals(DESCARTADO, a.getEstado());
        assertTrue(a.getMotivos().get(0).startsWith("D3"));
        assertNull(a.getPrecioUnitario());
    }

    @Test
    void paqueteDeTintaYUtilesSeDescarta() {
        Antecedente a = clasificador.clasificar(proceso("98955.3", "98766.25", 1,
                item("CONSUMIBLES (TINTA Y TONER)", "1", "CONTRATADO"),
                item("UTILES DE ESCRITORIO", "1", "CONTRATADO")));
        assertEquals(DESCARTADO, a.getEstado());
        assertTrue(a.getMotivos().stream().anyMatch(m -> m.startsWith("D2")));
    }

    @Test
    void procesoCanceladoSeDescarta() {
        Antecedente a = clasificador.clasificar(proceso("132640.39", "80380.81", 1,
                item("ADQUISICION DE TONERS PARA REFINERIA TALARA", "1", "CANCELADO")));
        assertEquals(DESCARTADO, a.getEstado());
        assertTrue(a.getMotivos().get(0).startsWith("D1"));
    }

    @Test
    void cantidadUnoSinModeloQuedaEnRevision() {
        Antecedente a = clasificador.clasificar(proceso("74126", "74126", 1,
                item("ADQUISICION DE TONER PARA IMPRESORAS DE LA MUNICIPALIDAD", "1", "CONTRATADO")));
        assertEquals(REVISION, a.getEstado());
        assertEquals(2, a.getMotivos().size()); // R2 y R3
    }

    @Test
    void diferenciaDeCentimosNoActivaR4() {
        Antecedente a = clasificador.clasificar(proceso("2041.90", "2041.92", 1,
                item("Toner de Impresion para impresora Kyocera TK8737Y Amarillo", "2", "CONTRATADO")));
        assertEquals(UTILIZABLE, a.getEstado());
    }

    @Test
    void adjudicadoMayorAlEstimadoQuedaEnRevision() {
        Antecedente a = clasificador.clasificar(proceso("182240", "232181.52", 1,
                item("toners", "293", "CONTRATADO")));
        assertEquals(REVISION, a.getEstado());
        assertTrue(a.getMotivos().stream().anyMatch(m -> m.startsWith("R4")));
    }
}
