package obj;
import ui.UIAdapter;

public class CPU {
    private BCP actual = null;

    public synchronized void cargar(BCP bcp) {
        actual = bcp;
        bcp.estado = BCP.EstadoProceso.EJECUTANDO;
        // La nave vuela al SOL y el hilo espera aquí
        UIAdapter.getInstance().moverNave(bcp, "CPU");
        CentroControl.registrar(String.format("CPU: %s aterrizó en el SOL.", bcp.nombre));
    }

    public synchronized void descargar() {
        if (actual != null) {
            CentroControl.registrar(String.format("CPU: %s despegando del SOL.", actual.nombre));
            actual = null;
        }
    }
    public synchronized BCP getActual() { return actual; }
}