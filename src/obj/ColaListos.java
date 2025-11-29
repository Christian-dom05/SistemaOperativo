import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ColaListos {
    private final Deque<PCB> cola = new ArrayDeque<>();

    public synchronized void encolar(PCB pcb) {
        pcb.estado = PCB.EstadoProceso.LISTO;
        cola.addLast(pcb);
        CentroControl.registrar(String.format("ColaListos: %s (pid=%d) encolado. Tamaño=%d", pcb.nombre, pcb.pid, cola.size()));
    }

    public synchronized PCB desencolar() {
        PCB p = cola.pollFirst();
        if (p != null) CentroControl.registrar(String.format("ColaListos: desencolado %s (pid=%d). Tamaño=%d", p.nombre, p.pid, cola.size()));
        return p;
    }

    public synchronized boolean estaVacia() { return cola.isEmpty(); }

    public synchronized List<PCB> snapshot() { return new ArrayList<>(cola); }
}