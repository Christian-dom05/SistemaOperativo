import java.util.LinkedList;
import java.util.Queue;

public class SemaphoroGalactico {
    public final String nombre;
    private int valor;
    private final Queue<PCB> colaEspera = new LinkedList<>();

    public SemaphoroGalactico(String nombre, int inicial) {
        this.nombre = nombre;
        this.valor = inicial;
    }

    // wait / P
    public synchronized void waitSem(PCB pcb, ColaListos colaListos, ColaBloqueados colaBloq) {
        CentroControl.registrar(String.format("Semaforo %s: %s (pid=%d) hace WAIT (valor=%d)", nombre, pcb.nombre, pcb.pid, valor));
        if (valor > 0) {
            valor--;
            CentroControl.registrar(String.format("Semaforo %s: otorgado a %s (pid=%d). Nuevo valor=%d", nombre, pcb.nombre, pcb.pid, valor));
        } else {
            colaEspera.add(pcb);
            pcb.estado = PCB.EstadoProceso.BLOQUEADO;
            colaBloq.encolar(pcb);
            CentroControl.registrar(String.format("Semaforo %s: %s (pid=%d) bloqueado y enviado al agujero negro", nombre, pcb.nombre, pcb.pid));
        }
    }

    // signal / V
    public synchronized void signalSem(ColaListos colaListos, ColaBloqueados colaBloq) {
        CentroControl.registrar(String.format("Semaforo %s: SIGNAL llamado. Valor actual=%d", nombre, valor));
        PCB pcb = colaEspera.poll();
        if (pcb != null) {
            pcb.estado = PCB.EstadoProceso.LISTO;
            colaBloq.remover(pcb);
            colaListos.encolar(pcb);
            CentroControl.registrar(String.format("Semaforo %s: liberado %s (pid=%d) hacia cola de listos", nombre, pcb.nombre, pcb.pid));
        } else {
            valor++;
            CentroControl.registrar(String.format("Semaforo %s: ningun proceso esperando. Valor incrementado a %d", nombre, valor));
        }
    }
}