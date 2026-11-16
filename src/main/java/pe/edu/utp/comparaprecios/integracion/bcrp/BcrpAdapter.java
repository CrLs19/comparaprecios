package pe.edu.utp.comparaprecios.integracion.bcrp;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.utp.comparaprecios.dominio.SeriesEconomicas;
import pe.edu.utp.comparaprecios.integracion.ClienteHttp;
import pe.edu.utp.comparaprecios.integracion.ServicioExternoException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/**
 * Adaptador de BCRPData. Se piden las dos series en una sola llamada:
 *   GET {base}/PN38705PM-PN01207PM/json/{inicio}/{fin}/esp
 *
 * Importante: la API no respeta el orden en que se piden las series (en la
 * prueba del 12/10/2026 devolvio primero el tipo de cambio), por eso cada
 * columna se identifica por el nombre que trae "config.series" y no por posicion.
 */
@Component
public class BcrpAdapter {

    public static final String FUENTE = "BCRP";

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("ENE", 1), Map.entry("FEB", 2), Map.entry("MAR", 3), Map.entry("ABR", 4),
            Map.entry("MAY", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AGO", 8),
            Map.entry("SEP", 9), Map.entry("SET", 9), Map.entry("OCT", 10), Map.entry("NOV", 11),
            Map.entry("DIC", 12));

    private final ClienteHttp http;
    private final String baseUrl;
    private final String serieIpc;
    private final String serieTc;

    public BcrpAdapter(ClienteHttp http,
                       @Value("${bcrp.base-url}") String baseUrl,
                       @Value("${bcrp.serie-ipc}") String serieIpc,
                       @Value("${bcrp.serie-tc}") String serieTc) {
        this.http = http;
        this.baseUrl = baseUrl;
        this.serieIpc = serieIpc;
        this.serieTc = serieTc;
    }

    public SeriesEconomicas obtenerSeries(YearMonth inicio, YearMonth fin) {
        String url = String.format("%s/%s-%s/json/%d-%d/%d-%d/esp", baseUrl, serieIpc, serieTc,
                inicio.getYear(), inicio.getMonthValue(), fin.getYear(), fin.getMonthValue());
        return interpretar(http.getJson(FUENTE, url));
    }

    /** Convierte la respuesta de BCRPData. Publico y estatico para probarlo sin red. */
    public static SeriesEconomicas interpretar(JsonNode raiz) {
        JsonNode series = raiz.path("config").path("series");
        int colIpc = -1;
        int colTc = -1;
        for (int i = 0; i < series.size(); i++) {
            String nombre = series.get(i).path("name").asText().toUpperCase();
            if (nombre.contains("PRECIOS") || nombre.contains("IPC")) {
                colIpc = i;
            } else if (nombre.contains("TIPO DE CAMBIO")) {
                colTc = i;
            }
        }
        if (colIpc < 0 || colTc < 0) {
            throw new ServicioExternoException(FUENTE, "No se reconocieron las series en la respuesta", null);
        }

        SeriesEconomicas resultado = new SeriesEconomicas();
        for (JsonNode p : raiz.path("periods")) {
            YearMonth periodo = periodo(p.path("name").asText());
            JsonNode valores = p.path("values");
            BigDecimal ipc = numero(valores.path(colIpc).asText());
            BigDecimal tc = numero(valores.path(colTc).asText());
            if (ipc != null) resultado.agregarIpc(periodo, ipc);
            if (tc != null) resultado.agregarTipoCambio(periodo, tc);
        }
        return resultado;
    }

    /** "Jul.2024" -> 2024-07 */
    static YearMonth periodo(String nombre) {
        try {
            String[] partes = nombre.split("\\.");
            if (partes.length != 2) {
                throw new IllegalArgumentException("Se espera Mes.AAAA");
            }
            Integer mes = MESES.get(partes[0].trim().toUpperCase(java.util.Locale.ROOT));
            if (mes == null) {
                throw new IllegalArgumentException("Mes no reconocido");
            }
            return YearMonth.of(Integer.parseInt(partes[1].trim()), mes);
        } catch (IllegalArgumentException | java.time.DateTimeException | NullPointerException e) {
            throw new ServicioExternoException(FUENTE, "Periodo no reconocido: " + nombre, e);
        }
    }

    /** El BCRP publica "n.d." cuando todavia no hay dato del mes. */
    private static BigDecimal numero(String texto) {
        if (texto == null || texto.isBlank() || texto.equalsIgnoreCase("n.d.")) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
