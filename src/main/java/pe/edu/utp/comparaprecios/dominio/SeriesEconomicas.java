package pe.edu.utp.comparaprecios.dominio;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.TreeMap;

/** Valores mensuales del IPC Lima Metropolitana y del tipo de cambio obtenidos del BCRP. */
public class SeriesEconomicas {

    private final TreeMap<YearMonth, BigDecimal> ipc = new TreeMap<>();
    private final TreeMap<YearMonth, BigDecimal> tipoCambio = new TreeMap<>();

    public void agregarIpc(YearMonth periodo, BigDecimal valor) {
        ipc.put(periodo, valor);
    }

    public void agregarTipoCambio(YearMonth periodo, BigDecimal valor) {
        tipoCambio.put(periodo, valor);
    }

    public Optional<BigDecimal> ipc(YearMonth periodo) {
        return Optional.ofNullable(ipc.get(periodo));
    }

    public Optional<BigDecimal> tipoCambio(YearMonth periodo) {
        return Optional.ofNullable(tipoCambio.get(periodo));
    }

    /** Ultimo mes en el que ambas series tienen dato publicado. */
    public Optional<YearMonth> ultimoPeriodoComun() {
        return ipc.descendingKeySet().stream()
                .filter(tipoCambio::containsKey)
                .findFirst();
    }

    public TreeMap<YearMonth, BigDecimal> getIpc() { return ipc; }
    public TreeMap<YearMonth, BigDecimal> getTipoCambio() { return tipoCambio; }
}
