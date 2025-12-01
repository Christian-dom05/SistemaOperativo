package obj;
import ui.UIAdapter;
import java.util.LinkedList;
import java.util.Queue;

public class SemaphoroGalactico {
    public final String nombre;
    private int valor;
    private final Queue<BCP> colaEspera = new LinkedList<>();

    public SemaphoroGalactico(String nombre, int inicial) {
        this.nombre = nombre;
        this.valor = inicial;
    }

    public synchronized void waitSem(BCP bcp, ColaListos colaListos, ColaBloqueados colaBloq) {
        // Nave va al portal (Semaforo) visualmente
        UIAdapter.getInstance().moverNave(bcp, nombre);

        if (valor > 0) {
            valor--;
            UIAdapter.getInstance().actualizarSemaforoUI(nombre, valor);
            CentroControl.registrar(String.format("Semaforo %s: %s cruzó el portal.", nombre, bcp.nombre));
        } else {
            colaEspera.add(bcp);
            bcp.estado = BCP.EstadoProceso.BLOQUEADO;
            colaBloq.enlistar(bcp); // Esto la moverá luego a BLOCKED
            CentroControl.registrar(String.format("Semaforo %s: Portal cerrado para %s.", nombre, bcp.nombre));
        }
    }

    public synchronized void signalSem(ColaListos colaListos, ColaBloqueados colaBloq) {
        BCP bcp = colaEspera.poll();
        if (bcp != null) {
            bcp.estado = BCP.EstadoProceso.LISTO;
            colaBloq.remover(bcp);
            colaListos.enlistar(bcp); // Esto la moverá a READY
            CentroControl.registrar(String.format("Semaforo %s: Portal abierto para %s.", nombre, bcp.nombre));
        } else {
            valor++;
            UIAdapter.getInstance().actualizarSemaforoUI(nombre, valor);
        }
    }
}