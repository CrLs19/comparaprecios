package pe.edu.utp.comparaprecios.dominio;

/**
 * Resultado de aplicar las reglas de comparabilidad a un antecedente.
 * El orden importa: un estado "peor" prevalece sobre uno "mejor".
 */
public enum EstadoComparabilidad {
    UTILIZABLE,
    REVISION,
    DESCARTADO;

    public EstadoComparabilidad peor(EstadoComparabilidad otro) {
        return this.ordinal() >= otro.ordinal() ? this : otro;
    }
}
