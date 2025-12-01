package obj;

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
        CentroControl.registrar(String.format("Semaforo %s: %s (pid=%d) hace WAIT (valor=%d)", nombre, bcp.nombre, bcp.pid, valor));
        if (valor > 0) {
            valor--;
            CentroControl.registrar(String.format("Semaforo %s: otorgado a %s (pid=%d). Nuevo valor=%d", nombre, bcp.nombre, bcp.pid, valor));
        } else {
            colaEspera.add(bcp);
            bcp.estado = BCP.EstadoProceso.BLOQUEADO;
            colaBloq.enlistar(bcp);
            CentroControl.registrar(String.format("Semaforo %s: %s (pid=%d) bloqueado y enviado al agujero negro", nombre, bcp.nombre, bcp.pid));
        }
    }

    public synchronized void signalSem(ColaListos colaListos, ColaBloqueados colaBloq) {
        CentroControl.registrar(String.format("Semaforo %s: SIGNAL llamado. Valor actual=%d", nombre, valor));
        BCP bcp = colaEspera.poll();
        if (bcp != null) {
            bcp.estado = BCP.EstadoProceso.LISTO;
            colaBloq.remover(bcp);
            colaListos.enlistar(bcp);
            CentroControl.registrar(String.format("Semaforo %s: liberado %s (pid=%d) hacia cola de listos", nombre, bcp.nombre, bcp.pid));
        } else {
            valor++;
            CentroControl.registrar(String.format("Semaforo %s: ningun proceso esperando. Valor incrementado a %d", nombre, valor));
        }
    }
}