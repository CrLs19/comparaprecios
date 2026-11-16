package pe.edu.utp.comparaprecios.negocio;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifica codigos de modelo de toner dentro del texto libre de la descripcion.
 * Los codigos se normalizan (mayusculas, sin espacios ni guiones) para que
 * "TK-3432", "TK 3432" y "tk3432" se reconozcan como el mismo bien.
 */
@Component
public class ExtractorModelo {

    /** Codigos con prefijo de fabricante o con estructura conocida. */
    private static final Pattern CODIGO_PRINCIPAL = Pattern.compile(
            "\\b(?:TK|CE|CF|CB|CC|TN|DR|MLT)[\\s-]?[A-Z]?\\d{2,5}[A-Z]{0,2}\\b"   // Kyocera, HP, Brother, Samsung
                    + "|\\b\\d{3}R\\d{5}\\b"                                     // Xerox 006R04404, 106R03585
                    + "|\\b\\d{2}[A-Z]\\d[A-Z]\\d{2}\\b");                       // Lexmark 58D4X00

    /** Codigo numerico precedido de "COD REF" (por ejemplo Ricoh 418477). */
    private static final Pattern CODIGO_REFERENCIA = Pattern.compile(
            "COD\\.?\\s*REF\\.?\\s*(\\d{6})\\b");

    /** Codigos cortos tipo "213-A" o "650A": solo se usan si no hay otro codigo. */
    private static final Pattern CODIGO_SECUNDARIO = Pattern.compile("\\b\\d{3}-?A\\b");

    public List<String> extraer(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String t = normalizarTexto(texto);
        Set<String> encontrados = new LinkedHashSet<>();

        Matcher m = CODIGO_PRINCIPAL.matcher(t);
        while (m.find()) {
            encontrados.add(normalizarCodigo(m.group()));
        }
        Matcher r = CODIGO_REFERENCIA.matcher(t);
        while (r.find()) {
            encontrados.add(r.group(1));
        }
        if (encontrados.isEmpty()) {
            Matcher s = CODIGO_SECUNDARIO.matcher(t);
            while (s.find()) {
                encontrados.add(normalizarCodigo(s.group()));
            }
        }
        return new ArrayList<>(encontrados);
    }

    public String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.toUpperCase().replaceAll("[\\s-]", "");
    }

    /** Mayusculas y sin tildes, para que las reglas comparen texto homogeneo. */
    public static String normalizarTexto(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase().replaceAll("\\s+", " ").trim();
    }
}
