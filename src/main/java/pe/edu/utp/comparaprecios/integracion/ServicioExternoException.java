package pe.edu.utp.comparaprecios.integracion;

/** Se lanza cuando la OECE o el BCRP no responden o devuelven un formato inesperado. */
public class ServicioExternoException extends RuntimeException {

    private final String fuente;

    public ServicioExternoException(String fuente, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.fuente = fuente;
    }

    public String getFuente() {
        return fuente;
    }
}
