import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ColaBloqueados {
    private final Deque<PCB> cola = new ArrayDeque<>();

    public synchronized void encolar(PCB pcb) {
        pcb.estado = PCB.EstadoProceso.BLOQUEADO;
        cola.addLast(pcb);
        CentroControl.registrar(String.format("ColaBloqueados: %s (pid=%d) encolado. Tamaño=%d", pcb.nombre, pcb.pid, cola.size()));
    }

    public synchronized void remover(PCB pcb) {
        if (cola.remove(pcb)) {
            CentroControl.registrar(String.format("ColaBloqueados: removido %s (pid=%d). Tamaño=%d", pcb.nombre, pcb.pid, cola.size()));
        }
    }

    public synchronized boolean estaVacia() { return cola.isEmpty(); }

    public synchronized List<PCB> snapshot() { return new ArrayList<>(cola); }
}