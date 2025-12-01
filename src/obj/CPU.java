package obj;
import ui.UIAdapter;

public class CPU {
    private BCP actual = null;

    // Quitamos 'synchronized'
    public void cargar(BCP bcp) {
        // 1. Animación (Bloqueante para simular viaje, pero fuera del monitor)
        // Mantenemos moverNave normal aquí porque queremos que el Planificador espere a llegar antes de ejecutar
        UIAdapter.getInstance().moverNave(bcp, "CPU");

        // 2. Lógica
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