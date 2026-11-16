package pe.edu.utp.comparaprecios.integracion.oece;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import pe.edu.utp.comparaprecios.dominio.Antecedente;
import pe.edu.utp.comparaprecios.dominio.ItemContratado;
import pe.edu.utp.comparaprecios.integracion.ClienteHttp;
import pe.edu.utp.comparaprecios.integracion.ServicioExternoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador del Portal de Contrataciones Abiertas (OCDS 1.1).
 * Es el unico componente que conoce la estructura del JSON de la OECE;
 * el resto de la aplicacion trabaja con la clase Antecedente.
 */
@Component
public class OeceAdapter {

    public static final String FUENTE = "OECE";

    private final ClienteHttp http;
    private final String baseUrl;
    private final String source;
    private final int maxPaginas;

    public OeceAdapter(ClienteHttp http,
                       @Value("${oece.base-url}") String baseUrl,
                       @Value("${oece.source}") String source,
                       @Value("${oece.max-paginas}") int maxPaginas) {
        this.http = http;
        this.baseUrl = baseUrl;
        this.source = source;
        this.maxPaginas = maxPaginas;
    }

    /** Recorre las paginas de /api/v1/search y devuelve el ocid y la fecha de cada proceso. */
    public List<ResumenProceso> buscar(String termino) {
        List<ResumenProceso> resultado = new ArrayList<>();
        int pagina = 1;
        boolean haySiguiente = true;
        while (haySiguiente && pagina <= maxPaginas) {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/v1/search")
                    .queryParam("search", termino)
                    .queryParam("source", source)
                    .queryParam("format", "json")
                    .queryParam("page", pagina)
                    .encode().toUriString();
            JsonNode raiz = http.getJson(FUENTE, url);
            JsonNode resultados = raiz.path("results");
            if (!resultados.isArray()) {
                throw new ServicioExternoException(FUENTE, "La busqueda no devolvio el arreglo 'results'", null);
            }
            for (JsonNode r : resultados) {
                JsonNode cr = r.path("compiledRelease");
                resultado.add(new ResumenProceso(cr.path("ocid").asText(), fechaPublicacion(cr.path("tender"))));
            }
            haySiguiente = raiz.path("pagination").path("has_next").asBoolean(false);
            pagina++;
        }
        return resultado;
    }

    /** Consulta /api/v1/record/{ocid} y lo convierte en un Antecedente. */
    public Antecedente obtenerDetalle(String ocid) {
        String url = baseUrl + "/api/v1/record/" + ocid + "?format=json";
        JsonNode raiz = http.getJson(FUENTE, url);
        JsonNode cr = raiz.path("records").path(0).path("compiledRelease");
        if (cr.isMissingNode()) {
            throw new ServicioExternoException(FUENTE, "El registro " + ocid + " no contiene compiledRelease", null);
        }
        Antecedente a = mapear(cr);
        a.setUrlFuente(url);
        return a;
    }

    /** Mapeo OCDS -> Antecedente. Es publico y estatico para poder probarlo sin red. */
    public static Antecedente mapear(JsonNode cr) {
        Antecedente a = new Antecedente();
        JsonNode tender = cr.path("tender");

        a.setOcid(cr.path("ocid").asText());
        a.setEntidad(cr.path("buyer").path("name").asText(null));
        a.setNomenclatura(tender.path("title").asText(null));
        a.setDescripcionProceso(tender.path("description").asText(null));
        a.setMetodo(tender.path("procurementMethodDetails").asText(null));
        a.setFechaPublicacion(fechaPublicacion(tender));
        a.setValorEstimado(monto(tender.path("value")));
        a.setNumeroDocumentos(tender.path("documents").size());

        for (JsonNode i : tender.path("items")) {
            a.getItems().add(new ItemContratado(
                    i.path("description").asText(null),
                    i.path("classification").path("description").asText(null),
                    i.hasNonNull("quantity") ? i.path("quantity").decimalValue() : null,
                    i.path("unit").path("name").asText(null),
                    monto(i.path("totalValue")),
                    i.path("statusDetails").asText(null)));
        }

        JsonNode awards = cr.path("awards");
        a.setNumeroAdjudicaciones(awards.size());
        if (awards.size() > 0) {
            JsonNode award = awards.get(0);
            a.setMontoAdjudicado(monto(award.path("value")));
            a.setFechaAdjudicacion(fecha(award.path("date").asText(null)));
            List<String> proveedores = new ArrayList<>();
            award.path("suppliers").forEach(s -> proveedores.add(s.path("name").asText()));
            a.setProveedor(String.join("; ", proveedores));
        }
        return a;
    }

    private static LocalDate fechaPublicacion(JsonNode tender) {
        LocalDate f = fecha(tender.path("datePublished").asText(null));
        return f != null ? f : fecha(tender.path("tenderPeriod").path("startDate").asText(null));
    }

    private static BigDecimal monto(JsonNode value) {
        return value.hasNonNull("amount") ? value.path("amount").decimalValue() : null;
    }

    private static LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(texto).toLocalDate();
    }
}
