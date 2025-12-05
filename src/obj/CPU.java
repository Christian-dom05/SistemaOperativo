package obj;
import ui.UIAdapter;

public class CPU {
    private BCP actual = null;

    public void cargar(BCP bcp) {
        UIAdapter.getInstance().moverNave(bcp, "CPU");

        synchronized (this) {
            actual = bcp;
            bcp.estado = BCP.EstadoProceso.EJECUTANDO;
            CentroControl.registrar(String.format("CPU: %s aterrizó en el SOL.", bcp.nombre));
        }
    }

    public synchronized void descargar() {
        if (actual != null) {
            CentroControl.registrar(String.format("CPU: %s despegando del SOL.", actual.nombre));
            actual = null;
        }
    }
    public synchronized BCP getActual() { return actual; }
}