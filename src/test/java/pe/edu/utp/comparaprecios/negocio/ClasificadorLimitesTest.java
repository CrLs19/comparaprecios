package pe.edu.utp.comparaprecios.negocio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.utp.comparaprecios.dominio.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad.*;

class ClasificadorLimitesTest {
    private final ClasificadorComparabilidad clasificador = new ClasificadorComparabilidad(new ExtractorModelo());

    private Antecedente antecedente(String cantidad) {
        Antecedente a = new Antecedente();
        a.setMontoAdjudicado(new BigDecimal("100"));
        a.setNumeroAdjudicaciones(1);
        a.getItems().add(new ItemContratado("Toner TK-3432", null,
                cantidad == null ? null : new BigDecimal(cantidad), "Unidad", null, "CONTRATADO"));
        return a;
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-3", "1"})
    void cantidadInvalidaOPosibleLoteNoProducePrecio(String cantidad) {
        Antecedente a = clasificador.clasificar(antecedente(cantidad));
        assertEquals(REVISION, a.getEstado());
        assertNull(a.getPrecioUnitario());
        assertTrue(a.getMotivos().stream().anyMatch(m -> m.startsWith("R2")));
    }

    @Test
    void variasAdjudicacionesNoSeCalculanComoUnSoloItem() {
        Antecedente a = antecedente("2");
        a.setNumeroAdjudicaciones(2);
        clasificador.clasificar(a);
        assertEquals(REVISION, a.getEstado());
        assertNull(a.getPrecioUnitario());
        assertTrue(a.getMotivos().stream().anyMatch(m -> m.startsWith("R5")));
    }

    @Test
    void variosItemsAunqueSeanDelMismoModeloRequierenRevision() {
        Antecedente a = antecedente("2");
        a.getItems().add(new ItemContratado("Toner TK3432", null, BigDecimal.TEN, "Unidad", null, "CONTRATADO"));
        clasificador.clasificar(a);
        assertEquals(REVISION, a.getEstado());
        assertNull(a.getPrecioUnitario());
    }

    @Test
    void reclasificarComoDescartadoEliminaElPrecioAnterior() {
        Antecedente a = clasificador.clasificar(antecedente("2"));
        assertEquals(new BigDecimal("50.00"), a.getPrecioUnitario());
        a.setMontoAdjudicado(null);
        clasificador.clasificar(a);
        assertEquals(DESCARTADO, a.getEstado());
        assertNull(a.getPrecioUnitario());
        assertFalse(a.getMotivos().stream().anyMatch(m -> m.startsWith("Un solo")));
    }
}
