package pe.edu.utp.comparaprecios.integracion.oece;

import java.time.LocalDate;

/** Lo minimo que devuelve /api/v1/search para decidir si vale la pena pedir el detalle. */
public record ResumenProceso(String ocid, LocalDate fechaPublicacion) {
}
