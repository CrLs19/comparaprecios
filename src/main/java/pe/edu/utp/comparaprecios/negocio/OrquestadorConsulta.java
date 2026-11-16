package pe.edu.utp.comparaprecios.negocio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.utp.comparaprecios.api.dto.RespuestaConsulta;
import pe.edu.utp.comparaprecios.dominio.AjusteTemporal;
import pe.edu.utp.comparaprecios.dominio.Antecedente;
import pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad;
import pe.edu.utp.comparaprecios.dominio.SeriesEconomicas;
import pe.edu.utp.comparaprecios.integracion.ServicioExternoException;
import pe.edu.utp.comparaprecios.integracion.bcrp.BcrpAdapter;
import pe.edu.utp.comparaprecios.integracion.oece.OeceAdapter;
import pe.edu.utp.comparaprecios.integracion.oece.ResumenProceso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio compuesto: coordina la busqueda en la OECE, la lectura del detalle,
 * la clasificacion, el ajuste con series del BCRP y el registro de la consulta.
 */
@Service
public class OrquestadorConsulta {

    private static final Logger log = LoggerFactory.getLogger(OrquestadorConsulta.class);
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final OeceAdapter oece;
    private final BcrpAdapter bcrp;
    private final ClasificadorComparabilidad clasificador;
    private final CalculadoraAjuste calculadora;
    private final ExtractorModelo extractor;
    private final TrazabilidadService trazabilidad;
    private final ExecutorService ejecutor;

    public OrquestadorConsulta(OeceAdapter oece, BcrpAdapter bcrp, ClasificadorComparabilidad clasificador,
                               CalculadoraAjuste calculadora, ExtractorModelo extractor,
                               TrazabilidadService trazabilidad, ExecutorService ejecutorDetalle) {
        this.oece = oece;
        this.bcrp = bcrp;
        this.clasificador = clasificador;
        this.calculadora = calculadora;
        this.extractor = extractor;
        this.trazabilidad = trazabilidad;
        this.ejecutor = ejecutorDetalle;
    }

    public RespuestaConsulta consultar(String termino, String modelo, Integer desde, Integer hasta) {
        long inicio = System.currentTimeMillis();
        LocalDateTime fechaConsulta = LocalDateTime.now(LIMA);
        List<String> advertencias = new ArrayList<>();

        // 1. Busqueda en la OECE (sin este paso no hay resultado: si falla se propaga el error)
        List<ResumenProceso> encontrados = oece.buscar(termino);

        // 2. Filtro por anio de publicacion
        List<ResumenProceso> enRango = encontrados.stream()
                .filter(r -> dentroDelRango(r.fechaPublicacion(), desde, hasta))
                .toList();

        // 3. Detalle de cada proceso, en paralelo
        List<CompletableFuture<Antecedente>> tareas = enRango.stream()
                .map(r -> CompletableFuture.supplyAsync(() -> oece.obtenerDetalle(r.ocid()), ejecutor))
                .toList();
        List<Antecedente> antecedentes = new ArrayList<>();
        int fallidos = 0;
        for (CompletableFuture<Antecedente> t : tareas) {
            try {
                antecedentes.add(t.get(30, TimeUnit.SECONDS));
            } catch (Exception e) {
                fallidos++;
                log.warn("No se pudo leer un registro de la OECE: {}", e.getMessage());
            }
        }
        if (fallidos > 0) {
            advertencias.add(fallidos + " procesos no pudieron leerse desde la OECE y se excluyeron del analisis.");
        }

        // 4. Clasificacion y 5. filtro por modelo
        antecedentes.forEach(clasificador::clasificar);
        if (modelo != null && !modelo.isBlank()) {
            String buscado = extractor.normalizarCodigo(modelo);
            antecedentes = antecedentes.stream()
                    .filter(a -> a.getModelos().contains(buscado) || textoContiene(a, buscado))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        // 6. Ajuste temporal con el BCRP (si falla, se devuelven los precios sin ajustar)
        String estadoBcrp = "NO REQUERIDO";
        YearMonth referencia = null;
        List<Antecedente> utilizables = antecedentes.stream()
                .filter(a -> a.getEstado() == EstadoComparabilidad.UTILIZABLE)
                .toList();
        if (!utilizables.isEmpty()) {
            try {
                YearMonth desdeMes = utilizables.stream().map(this::periodoOrigen).min(Comparator.naturalOrder()).orElseThrow();
                SeriesEconomicas series = bcrp.obtenerSeries(desdeMes, YearMonth.now(LIMA));
                referencia = series.ultimoPeriodoComun()
                        .orElseThrow(() -> new ServicioExternoException(BcrpAdapter.FUENTE, "sin datos publicados en el rango", null));
                for (Antecedente a : utilizables) {
                    try {
                        a.setAjuste(calculadora.ajustar(a.getPrecioUnitario(), periodoOrigen(a), referencia, series));
                    } catch (IllegalArgumentException e) {
                        advertencias.add(a.getOcid() + ": " + e.getMessage());
                    }
                }
                estadoBcrp = "OK";
            } catch (ServicioExternoException e) {
                estadoBcrp = "NO DISPONIBLE";
                advertencias.add("No se pudo consultar el BCRP (" + e.getMessage() + "). Los precios se muestran sin actualizar.");
            }
        }

        antecedentes.sort(Comparator.comparing(Antecedente::getEstado)
                .thenComparing(Antecedente::getFechaPublicacion,
                        Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())));

        RespuestaConsulta parcial = new RespuestaConsulta(null, fechaConsulta, termino, modelo, desde, hasta,
                encontrados.size(), antecedentes.size(),
                contar(antecedentes, EstadoComparabilidad.UTILIZABLE),
                contar(antecedentes, EstadoComparabilidad.REVISION),
                contar(antecedentes, EstadoComparabilidad.DESCARTADO),
                referencia, "OK", estadoBcrp, advertencias, System.currentTimeMillis() - inicio, antecedentes);

        // 7. Registro para trazabilidad
        Long id = trazabilidad.registrar(parcial);
        return new RespuestaConsulta(id, parcial.fechaConsulta(), termino, modelo, desde, hasta,
                parcial.encontradosEnOece(), parcial.analizados(), parcial.utilizables(), parcial.enRevision(),
                parcial.descartados(), referencia, parcial.estadoOece(), estadoBcrp, advertencias,
                parcial.duracionMs(), antecedentes);
    }

    /** Detalle de un solo proceso, clasificado y con ajuste al ultimo mes disponible. */
    public Antecedente consultarProceso(String ocid) {
        Antecedente a = clasificador.clasificar(oece.obtenerDetalle(ocid));
        if (a.getEstado() == EstadoComparabilidad.UTILIZABLE) {
            try {
                SeriesEconomicas series = bcrp.obtenerSeries(periodoOrigen(a), YearMonth.now(LIMA));
                series.ultimoPeriodoComun().ifPresent(ref ->
                        a.setAjuste(calculadora.ajustar(a.getPrecioUnitario(), periodoOrigen(a), ref, series)));
            } catch (ServicioExternoException e) {
                log.warn("Proceso {} sin ajuste: {}", ocid, e.getMessage());
            }
        }
        return a;
    }

    /** Ajuste de un monto cualquiera entre dos meses. */
    public AjusteTemporal ajustar(BigDecimal monto, YearMonth origen, YearMonth destino) {
        YearMonth fin = Optional.ofNullable(destino).orElse(YearMonth.now(LIMA));
        if (fin.isBefore(origen)) {
            throw new IllegalArgumentException("El periodo de destino no puede ser anterior al de origen");
        }
        SeriesEconomicas series = bcrp.obtenerSeries(origen, fin);
        YearMonth ref = destino != null ? destino
                : series.ultimoPeriodoComun().orElseThrow(() -> new IllegalArgumentException("Sin datos del BCRP"));
        return calculadora.ajustar(monto, origen, ref, series);
    }

    private YearMonth periodoOrigen(Antecedente a) {
        LocalDate f = a.getFechaAdjudicacion() != null ? a.getFechaAdjudicacion() : a.getFechaPublicacion();
        return YearMonth.from(f);
    }

    private static boolean dentroDelRango(LocalDate fecha, Integer desde, Integer hasta) {
        if (fecha == null) {
            return false;
        }
        int anio = fecha.getYear();
        return (desde == null || anio >= desde) && (hasta == null || anio <= hasta);
    }

    private static boolean textoContiene(Antecedente a, String codigo) {
        return a.getItems().stream()
                .map(i -> ExtractorModelo.normalizarTexto(String.valueOf(i.getDescripcion())).replaceAll("[\\s-]", ""))
                .anyMatch(t -> t.contains(codigo));
    }

    private static long contar(List<Antecedente> lista, EstadoComparabilidad estado) {
        return lista.stream().filter(a -> a.getEstado() == estado).count();
    }
}
