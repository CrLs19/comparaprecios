package pe.edu.utp.comparaprecios.negocio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExtractorModeloTest {
    private final ExtractorModelo extractor = new ExtractorModelo();

    @ParameterizedTest
    @ValueSource(strings = {"TK-3432", "tk3432", "TK 3432"})
    void reconoceElMismoModeloConDistintaEscritura(String codigo) {
        assertEquals(List.of("TK3432"), extractor.extraer("Tóner " + codigo));
    }

    @Test
    void noDuplicaElMismoModeloRepetidoEnLaDescripcion() {
        assertEquals(List.of("TK3432"), extractor.extraer("TK-3432 equivalente a TK3432"));
    }

    @Test
    void conservaModelosDistintosParaPermitirElDescarte() {
        assertEquals(List.of("TK3432", "TK3182"), extractor.extraer("Toner TK3432 y TK3182"));
    }

    @Test
    void unaDescripcionAusenteNoInventaUnCodigo() {
        assertTrue(extractor.extraer(null).isEmpty());
        assertTrue(extractor.extraer("Toner para impresora").isEmpty());
    }
}
