package pe.edu.utp.comparaprecios.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.comparaprecios.api.dto.RespuestaConsulta;
import pe.edu.utp.comparaprecios.dominio.AjusteTemporal;
import pe.edu.utp.comparaprecios.dominio.Antecedente;
import pe.edu.utp.comparaprecios.negocio.OrquestadorConsulta;
import pe.edu.utp.comparaprecios.negocio.TrazabilidadService;
import pe.edu.utp.comparaprecios.persistencia.ConsultaRealizada;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** API propia que consume el dashboard. */
@Validated
@RestController
@RequestMapping("/api/v1")
public class AntecedenteController {

    private final OrquestadorConsulta orquestador;
    private final TrazabilidadService trazabilidad;

    public AntecedenteController(OrquestadorConsulta orquestador, TrazabilidadService trazabilidad) {
        this.orquestador = orquestador;
        this.trazabilidad = trazabilidad;
    }

    /** GET /api/v1/antecedentes?termino=TONER&modelo=TK-3432&desde=2024&hasta=2026 */
    @GetMapping("/antecedentes")
    public RespuestaConsulta buscar(
            @RequestParam @NotBlank @Size(min = 3, max = 60) String termino,
            @RequestParam(required = false) @Size(max = 30) String modelo,
            @RequestParam(required = false) @Min(2010) @Max(2100) Integer desde,
            @RequestParam(required = false) @Min(2010) @Max(2100) Integer hasta) {
        if (desde != null && hasta != null && desde > hasta) {
            throw new IllegalArgumentException("El anio 'desde' no puede ser mayor que 'hasta'");
        }
        return orquestador.consultar(termino.trim(), modelo, desde, hasta);
    }

    /** GET /api/v1/antecedentes/{ocid} */
    @GetMapping("/antecedentes/{ocid}")
    public Antecedente detalle(@PathVariable String ocid) {
        if (!ocid.matches("ocds-[a-z0-9]+-[A-Za-z0-9-]+")) {
            throw new IllegalArgumentException("Formato de ocid no valido");
        }
        return orquestador.consultarProceso(ocid);
    }

    /** GET /api/v1/ajustes?monto=551.89&origen=2024-07&destino=2026-06 */
    @GetMapping("/ajustes")
    public AjusteTemporal ajustar(
            @RequestParam @DecimalMin("0.01") BigDecimal monto,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth origen,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth destino) {
        return orquestador.ajustar(monto, origen, destino);
    }

    /** GET /api/v1/consultas - historial para trazabilidad */
    @GetMapping("/consultas")
    public List<ConsultaRealizada> historial() {
        return trazabilidad.ultimas();
    }
}
