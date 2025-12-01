package obj;

public class CPU {
    private BCP actual = null;

    public synchronized void cargar(BCP bcp) {
        actual = bcp;
        bcp.estado = BCP.EstadoProceso.EJECUTANDO;
        CentroControl.registrar(String.format("obj.CPU: cargado %s (pid=%d) en el SOL.", bcp.nombre, bcp.pid));
    }

    public synchronized void descargar() {
        if (actual != null) {
            CentroControl.registrar(String.format("obj.CPU: descargado %s (pid=%d) desde el SOL.", actual.nombre, actual.pid));
            actual = null;
        }
    }

    public synchronized BCP getActual() { return actual; }
}