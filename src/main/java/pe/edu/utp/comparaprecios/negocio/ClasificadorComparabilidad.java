package pe.edu.utp.comparaprecios.negocio;

import org.springframework.stereotype.Component;
import pe.edu.utp.comparaprecios.dominio.Antecedente;
import pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad;
import pe.edu.utp.comparaprecios.dominio.ItemContratado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad.DESCARTADO;
import static pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad.REVISION;
import static pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad.UTILIZABLE;

/**
 * Aplica las reglas de comparabilidad definidas a partir del analisis exploratorio.
 *
 * Descarte:  D1 sin adjudicacion valida, D2 paquete mixto, D3 varios modelos en un item.
 * Revision:  R1 varios items, R2 cantidad menor o igual a 1, R3 sin codigo de modelo,
 *            R4 adjudicado mayor que el valor estimado, R5 mas de una adjudicacion.
 * Si ninguna regla se activa el antecedente es UTILIZABLE y se calcula el precio unitario.
 */
@Component
public class ClasificadorComparabilidad {

    private static final Set<String> ESTADOS_SIN_ADJUDICACION =
            Set.of("DESIERTO", "CANCELADO", "CONVOCADO", "NULO", "NO SUSCRITO");

    private static final Pattern BIENES_MEZCLADOS = Pattern.compile(
            "\\b(TINTAS?|PAPEL|UTILES|ESCRITORIO|TAMBOR(ES)?|KIT DE MANTENIMIENTO|UNIDAD(ES)? DE IMAGEN)\\b");

    private static final BigDecimal TOLERANCIA_R4 = new BigDecimal("1.01");

    private final ExtractorModelo extractor;

    public ClasificadorComparabilidad(ExtractorModelo extractor) {
        this.extractor = extractor;
    }

    public Antecedente clasificar(Antecedente a) {
        a.setEstado(null);
        a.getMotivos().clear();
        a.setPrecioUnitario(null);

        List<ItemContratado> items = a.getItems();
        Set<String> modelosProceso = new LinkedHashSet<>();
        for (ItemContratado item : items) {
            modelosProceso.addAll(extractor.extraer(item.getDescripcion()));
        }
        a.setModelos(List.copyOf(modelosProceso));

        // D1 - sin adjudicacion valida
        boolean sinMonto = a.getMontoAdjudicado() == null || a.getMontoAdjudicado().signum() <= 0;
        boolean itemSinAdjudicar = items.stream()
                .map(ItemContratado::getEstado)
                .anyMatch(e -> e != null && ESTADOS_SIN_ADJUDICACION.contains(e.toUpperCase()));
        if (sinMonto || itemSinAdjudicar) {
            a.agregarMotivo(DESCARTADO, "D1: proceso sin adjudicacion valida (desierto, cancelado o sin monto adjudicado)");
        }

        // D2 - el proceso agrupa toner con otros bienes
        String textoCompleto = ExtractorModelo.normalizarTexto(
                nvl(a.getDescripcionProceso()) + " " + items.stream()
                        .map(i -> nvl(i.getDescripcion()))
                        .reduce("", (x, y) -> x + " " + y));
        if (BIENES_MEZCLADOS.matcher(textoCompleto).find()) {
            a.agregarMotivo(DESCARTADO, "D2: el proceso agrupa toner con otros bienes (tintas, utiles, tambores u otros)");
        }

        // D3 - un mismo item reune varios modelos
        for (ItemContratado item : items) {
            List<String> modelosItem = extractor.extraer(item.getDescripcion());
            if (modelosItem.size() >= 2) {
                a.agregarMotivo(DESCARTADO, "D3: un solo item reune varios modelos " + modelosItem);
                break;
            }
        }

        // R1 - varios items: el monto adjudicado no esta desagregado por item
        if (items.size() > 1) {
            a.agregarMotivo(REVISION, "R1: el proceso tiene " + items.size()
                    + " items y el monto adjudicado no se publica por item");
        }

        // R2 - cantidad declarada igual a 1 (posible lote)
        BigDecimal cantidad = items.size() == 1 ? items.get(0).getCantidad() : null;
        if (items.size() == 1 && (cantidad == null || cantidad.compareTo(BigDecimal.ONE) <= 0)) {
            a.agregarMotivo(REVISION, "R2: cantidad ausente, no positiva o igual a 1 (posible lote); requiere revision");
        }

        // R3 - no se identifica el codigo del consumible
        if (modelosProceso.isEmpty()) {
            a.agregarMotivo(REVISION, "R3: la descripcion no identifica el codigo del consumible; revisar bases o pliego absolutorio");
        }

        // R4 - adjudicado mayor que el valor estimado (se tolera 1 % por redondeos)
        if (!sinMonto && a.getValorEstimado() != null && a.getValorEstimado().signum() > 0
                && a.getMontoAdjudicado().compareTo(a.getValorEstimado().multiply(TOLERANCIA_R4)) > 0) {
            a.agregarMotivo(REVISION, "R4: el monto adjudicado supera el valor estimado de la convocatoria");
        }

        // R5 - mas de una adjudicacion registrada
        if (a.getNumeroAdjudicaciones() > 1) {
            a.agregarMotivo(REVISION, "R5: el registro contiene " + a.getNumeroAdjudicaciones() + " adjudicaciones");
        }

        a.setCantidad(cantidad);
        if (a.getEstado() == null) {
            a.setEstado(UTILIZABLE);
            a.getMotivos().add("Un solo item homogeneo, con modelo identificado y adjudicacion registrada");
            a.setPrecioUnitario(a.getMontoAdjudicado().divide(cantidad, 2, RoundingMode.HALF_UP));
        }
        return a;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
